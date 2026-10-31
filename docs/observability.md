# 🔭 Distributed Tracing

[← Back to README](../README.md)

A single login touches three services: the gateway routes it, `auth-service` authenticates, and
`auth-service` calls `user-service` for roles and permissions. That used to produce three unrelated
log streams and no way to answer *which* hop was slow. It now produces **one trace**, every log line
involved carries the id that leads back to it, and the caller gets that id handed back in the
response.

**Start the backend** — a single Jaeger container:

```bash
docker run -d --name jaeger -p 16686:16686 -p 4318:4318 jaegertracing/jaeger:2.20.0
```

UI at **http://localhost:16686**. It speaks OTLP/HTTP natively on `:4318`, which is exactly where the
services export — no separate collector in between.

## What is instrumented

`spring-boot-starter-opentelemetry` is on `gateway`, `auth-service`, `user-service` and
`product-service`. Micrometer Observation creates the spans; the OpenTelemetry bridge exports them
over OTLP.

| Hop | Where the span comes from |
|---|---|
| Client → gateway | Server-side observation on the reactive edge |
| Gateway → `lb://AUTH-SERVICE` | Gateway routing filter, context forwarded downstream |
| `auth-service` → `user-service` | `RestClient` client observation — see [Outbound calls](#outbound-calls-keep-the-trace-intact) |
| Request arriving at any service | Server-side observation, one per service |

Context travels as the **W3C `traceparent` header**, so no service passes trace ids by hand.

> **One starter, not three.** Most tracing guides tell you to add `micrometer-tracing-bridge-otel`
> and `opentelemetry-exporter-otlp` by hand. That was the Boot 3.x recipe. Boot 4 collapsed them into
> `spring-boot-starter-opentelemetry`, which pulls both at `runtime` scope. Declaring them explicitly
> is redundant, and promotes the bridge to `compile` scope where application code can accidentally
> import OpenTelemetry SDK classes directly.

## Configuration

Identical in every traced service:

| Property | Value | Why |
|---|---|---|
| `management.tracing.export.enabled` | `true` | Master switch for span export |
| `management.tracing.sampling.probability` | `1.0` | Sample every request. Right for development, far too expensive for production traffic |
| `management.otlp.metrics.export.enabled` | `false` | The starter also pulls an OTLP *metrics* exporter; Jaeger accepts traces only, so metrics export is switched off |

No endpoint is configured: spans go to Boot's default, `http://localhost:4318/v1/traces` — Jaeger's
OTLP/HTTP ingest.

Gateway only:

| Property | Value | Why |
|---|---|---|
| `spring.reactor.context-propagation` | `auto` | See below — without it the gateway logs no trace ids at all |

The service name in Jaeger is **not** configured — it is derived from `spring.application.name`, so
services appear as `ECOMMERCE-GATEWAY`, `AUTH-SERVICE` and `USER-SERVICE` for free. Setting
`management.opentelemetry.resource-attributes.service.name` to the same value would be redundant.

> Property names moved in Boot 4. The 3.x guides say `management.otlp.tracing.endpoint`; that name
> still resolves as a deprecated alias, so a copied snippet appears to work and you quietly end up
> with two naming conventions in one file.

## The reactive trap: `spring.reactor.context-propagation`

On WebFlux the trace context lives in the **Reactor Context**, but MDC and `Tracer.currentSpan()`
both read a **ThreadLocal**. Bridging the two requires Reactor's automatic context propagation, and
Boot does not enable it by default:

```java
// ReactorAutoConfiguration, spring-boot-reactor 4.1.0
ReactorAutoConfiguration(ReactorProperties properties) {
    if (properties.getContextPropagation() == ContextPropagationMode.AUTO) {
        Hooks.enableAutomaticContextPropagation();
    }
}
```

`spring.reactor.context-propagation` defaults to `limited`. Under it the gateway exports spans
perfectly, they arrive in Jaeger, and **every log line still prints blank ids** — because nothing
populates MDC on the event-loop thread. Nothing fails and nothing warns.

Same jar, same request, one property changed:

| `spring.reactor.context-propagation` | Gateway log line |
|---|---|
| `auto` | `[traceId=d64784a0d1213c7a3b6f4bb9b4e0f3ac] [spanId=ed95c3178ae32b2b]` |
| `limited` *(default)* | `[traceId=] [spanId=]` |

`auto` restores ThreadLocals around every operator, so it is not free — but it is the supported way
to mix reactive code with any ThreadLocal-based API, tracing and MDC included. The MVC services
(`auth-service`, `user-service`) do not need it for controller logging, since thread-per-request
binds MDC naturally.

## Logs carry the trace id

The services run on **Log4j2** rather than Logback. `spring-boot-starter-logging` is excluded once in
`ecommerce-parent` and `spring-boot-starter-log4j2` declared there in its place, so all reactor
modules inherit it; classes use Lombok's `@Log4j2`.

> A per-starter `<exclusion>` would not have been enough — nearly every Boot starter pulls
> `spring-boot-starter` transitively, so Logback comes back through whichever one you missed. The
> exclusion is declared in `<dependencyManagement>`, where it applies to every resolution path at
> once.

Boot's `logging.pattern.correlation` is a **Logback** property and does nothing here. On Log4j2 the
ids are read straight out of MDC in `log4j2-spring.xml`:

```xml
<Property name="PATTERN">%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level [traceId=%X{traceId}] [spanId=%X{spanId}] %logger{36} - %msg%ex%n</Property>
```

Those two key names are not arbitrary. `Slf4JEventListener` in the OpenTelemetry bridge writes
exactly `traceId` and `spanId` into MDC on every scope change, and `log4j-slf4j2-impl` maps SLF4J's
MDC onto Log4j2's `ThreadContext` — which is what `%X` reads.

```
2026-08-23 12:12:21.239 [reactor-http-nio-3] WARN  [traceId=ad7fdf082a8b1066f13c590bcf483576] [spanId=3643c7ab7ca1af87] o.e.g.filter.JwtAuthenticationFilter - Gateway rejected POST /user-service/api/v1/users — [SEC-005] Authentication required
```

Paste that trace id into Jaeger's search box and the whole request comes back.

**Startup lines always show `[traceId=] [spanId=]`, and that is correct** — there is no request in
flight, so there is no trace to belong to. Only lines logged *inside* a request carry ids. Worth
knowing before spending an afternoon debugging configuration that works.

## Handing the trace id back to the client

Every response carries the trace id of the request that produced it:

```
HTTP/1.1 200 OK
X-Trace-Id: ad7fdf082a8b1066f13c590bcf483576
```

It is the same id the gateway's log lines and the Jaeger trace carry. One value ties together what
the user saw, what the services logged, and the trace in Jaeger.

**Why bother, when the trace is already in Jaeger?** Because of who is holding the evidence.

Without it, a bug report is *"checkout failed around 3pm"*. Triage means guessing a time window,
guessing the user, and grepping three services hoping to find the right request among thousands.
With it, the report is *"checkout failed, reference `ad7fdf08…`"* — one paste into Jaeger returns
that exact request, every hop, every timing.

Three things make it worth the one header:

- **The client is the only party present at every failure.** A timeout, a dropped connection or a
  gateway-level rejection can leave little or nothing useful server-side. The caller always knows
  something went wrong, and now it knows *which* request went wrong.
- **It survives the handoff between humans.** The id goes into an error toast, a screenshot, a
  support ticket, a Slack message. It turns "it's broken" into a primary key.
- **It costs 32 hex characters.** A trace id is an opaque random identifier — no internal hostnames,
  no schema, no user data, and it is not a credential.

The implementation is a single `GlobalFilter`, `TraceIdResponseFilter`:

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    exchange.getResponse().beforeCommit(() -> Mono.fromRunnable(() -> {   // (1)
        Span span = tracer.currentSpan();
        if (span != null) {
            exchange.getResponse().getHeaders()
                    .set(TRACE_ID_HEADER, span.context().traceId());
        }
    }));
    return chain.filter(exchange);
}
```

**(1) The span is read in `beforeCommit`, not in the method body.** The body of `filter()` runs at
*assembly* time, before the chain is ever subscribed — `tracer.currentSpan()` returns `null` there
and the header is silently never written. `beforeCommit` runs inside the pipeline, late enough for
the trace context to be on the thread but still before headers are flushed. The work goes inside
`Mono.fromRunnable` rather than beside it, because the contract requires the action to be deferred so
it sequences correctly against other commit callbacks.

`set`, not `add`: a retried or redirected route would otherwise append a second value to the header.

**Filter order decides whether errors get the header.** `JwtAuthenticationFilter` rejects a bad
token by writing the 401 itself, *without* calling the chain — so any filter ordered after it never
runs for that request. `TraceIdResponseFilter` must therefore be ordered **before** it. Today it has
no `@Order` (lowest precedence) while `JwtAuthenticationFilter` sits at `HIGHEST_PRECEDENCE`, so
gateway-rejected 401s do not carry the header yet — see [Not covered yet](#not-covered-yet).

> `HIGHEST_PRECEDENCE - 1` is **not** the way to put something first.
> `Ordered.HIGHEST_PRECEDENCE` is `Integer.MIN_VALUE`, so subtracting one overflows to
> `Integer.MAX_VALUE` and sends the filter to the very back. Move the other filter to
> `HIGHEST_PRECEDENCE + 1` instead.

> Browser clients cannot read a custom response header cross-origin unless the server also sends
> `Access-Control-Expose-Headers: X-Trace-Id`. Not configured yet — no browser front-end exists.

## Outbound calls keep the trace intact

The classic way to lose a trace is a hand-built HTTP client. A client created with
`WebClient.builder()` or `RestClient.builder()` is **not** Boot's auto-configured builder, so it
carries no observation instrumentation: the outgoing call leaves without a `traceparent` header and
the next service starts a brand-new, unlinked trace. Nothing fails and nothing warns.

`auth-service` avoids this by construction. Its calls to `user-service` are declarative HTTP
interfaces (`InternalClient`, `UserClient`) registered with `@ImportHttpServices`, so Boot builds
them from its own `RestClient.Builder` — instrumentation included. `RestClientConfig` only
*customises* that builder (connection pool, error translation, bearer-token forwarding) and never
replaces it.

## Not covered yet

- **Database spans** — needs `datasource-micrometer-spring-boot`. `user-service` is the only service doing real query work, and a slow query currently shows as unexplained time inside its server span
- **Trace id on gateway 401s** — `TraceIdResponseFilter` needs to run before `JwtAuthenticationFilter`; until then, requests the gateway rejects come back without `X-Trace-Id`
- **Custom spans** — `@Observed` needs `management.observations.annotations.enabled=true` (off by default) plus `aspectjweaver`; present in `auth-service` and `user-service`, absent in `gateway`
- **Log shipping** — log lines carry trace ids, but nothing collects them; joining logs to a trace is still manual (ELK is on the roadmap)
- **Sampling strategy** — `1.0` everywhere; production needs a lower rate, or tail-based sampling at a collector
- **`discovery-server` and `config-server`** — not instrumented, though both log with the same `%X{traceId}` pattern, so their id slots are permanently blank


