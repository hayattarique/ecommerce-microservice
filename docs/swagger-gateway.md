# Swagger for all services through the API Gateway

## What is the goal?

A frontend developer needs **one URL** (the gateway, port 9999). From that one place they can
read and test the APIs of every microservice. We do not need to open the port of each service
(9091, 9092, 9093) to the outside.

```
http://localhost:9999/swagger-ui.html
```

---

## 1. How does it work? (understand this first)

- Only the **gateway** shows the Swagger UI page.
- Each **service** creates its own API description (OpenAPI JSON) at `/v3/api-docs`.
- The gateway fetches that description from each service, using the routes it already has.

```
Browser ──► gateway:9999/swagger-ui.html                 (the UI page, from the gateway)
Browser ──► gateway:9999/auth-service/v3/api-docs    ──► auth-service:9091/v3/api-docs
Browser ──► gateway:9999/user-service/v3/api-docs    ──► user-service:9092/v3/api-docs
Browser ──► gateway:9999/product-service/v3/api-docs ──► product-service:9093/v3/api-docs
```

The gateway route uses `StripPrefix=1`. This removes the first part of the path
(`/auth-service`), so the service only receives `/v3/api-docs`.

**Where does the dropdown come from?**
At the top of the Swagger UI there is a dropdown called "Select a definition". It is built from
the list in Step 3. Each item in the list has one **name** and one **docs URL**. To add a new
service to the dropdown, add one more item to that list.

---

## 2. Which problem does each step fix?

| Problem | Step that fixes it |
|---|---|
| The gateway has no Swagger UI | Step 1 (add dependency) |
| A service has no gateway route | Step 2 (add route) |
| The services do not appear in the dropdown | Step 3 (add URL list) |
| Docs return 401 at the gateway | Step 4 (gateway whitelist) |
| Docs return 401 inside a service | Step 5 (service whitelist) |
| "Try it out" calls the service port instead of the gateway | Step 6 (forwarded headers) |

---

## 3. The steps

### Step 1: Add the Swagger UI dependency to the gateway

File: `gateway/pom.xml`

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>${swagger.version}</version>
</dependency>
```

- Use the **webflux** version. The gateway is a reactive (WebFlux) application. The services
  use the `webmvc-ui` version, and it does not work in the gateway.
- The parent pom only manages the version of the `webmvc` library, so we write the version here.
  `${swagger.version}` comes from the parent pom (3.0.2), so every module uses the same version.
- **Important:** after you change a pom file, **reload Maven in IntelliJ** (use the reload button
  in the Maven window) and then restart the app. If you only restart, IntelliJ keeps the old
  libraries and the Swagger page shows a 404 error.

### Step 2: Make sure every service has a gateway route

Files: `gateway/src/main/resources/application-local.properties` and `application-dev.properties`

user-service and auth-service already had routes. product-service did not, so we added this:

```properties
spring.cloud.gateway.server.webflux.routes[2].id=PRODUCT-SERVICE
spring.cloud.gateway.server.webflux.routes[2].uri=lb://PRODUCT-SERVICE
spring.cloud.gateway.server.webflux.routes[2].predicates[0]=Path=/product-service/**
spring.cloud.gateway.server.webflux.routes[2].filters[0]=StripPrefix=1
```

What each line means:
- `uri=lb://PRODUCT-SERVICE`: find the service in Eureka and share the load between instances.
- `Path=/product-service/**`: match every URL that starts with `/product-service/`.
- `StripPrefix=1`: remove `/product-service` before sending the request to the service.

### Step 3: Create the dropdown list

File: `gateway/src/main/resources/application.properties`

```properties
springdoc.swagger-ui.urls[0].name=auth-service
springdoc.swagger-ui.urls[0].url=/auth-service/v3/api-docs
springdoc.swagger-ui.urls[1].name=user-service
springdoc.swagger-ui.urls[1].url=/user-service/v3/api-docs
springdoc.swagger-ui.urls[2].name=product-service
springdoc.swagger-ui.urls[2].url=/product-service/v3/api-docs
```

- `name`: the text shown in the dropdown.
- `url`: where the UI loads the docs from. This is a **gateway path** (it uses the route from
  Step 2). It is not the service's own port.
- The gateway publishes this list at `/v3/api-docs/swagger-config`. The UI reads it to build
  the dropdown.

### Step 4: Let the docs pass the gateway's JWT filter

File: `gateway/src/main/java/org/ecommerce/gateway/constant/OpenEndpoint.java`

```java
"/auth-service/v3/api-docs",
"/user-service/v3/api-docs",
"/product-service/v3/api-docs"
```

- `JwtAuthenticationFilter` asks for a token on every request, except for the paths in this list.
- The browser loads the docs without a token. If these paths are missing, the gateway returns
  `401 SEC-005`.
- The filter uses `startsWith`, so sub-paths such as `/v3/api-docs/swagger-config` are covered too.
- Only the docs are open. The real APIs (for example `/user-service/api/v1/...`) still need a JWT.
- The Swagger page itself (`/swagger-ui.html`) needs no entry. The gateway serves it directly,
  and the filter only runs on requests that are routed to a service.

### Step 5: Allow the docs in each service's own security

Each service has its own Spring Security, so the docs must be allowed there too.

| Service | What we did |
|---|---|
| auth-service | Nothing. Its `SecurityConfig` already allowed `/v3/api-docs/**`. |
| user-service | Added `"/v3/api-docs", "/v3/api-docs/**"` to `WHITE_LIST` in `SecurityConfig`. |
| product-service | It had **no** `SecurityFilterChain`, so Spring's default rules locked every URL (401 even with a token). We added a new `config/SecurityConfig.java`: health and docs are open, everything else needs a JWT. |

### Step 6: Make "Try it out" call the gateway

By default, each service writes **its own address** in its docs:
`"servers": [{"url": "http://localhost:9091"}]`. Then "Try it out" would send requests to port
9091, which is the port we want to keep closed. Two settings fix this.

**In the gateway** (`application-local.properties` and `application-dev.properties`):

```properties
spring.cloud.gateway.server.webflux.trusted-proxies=.*
```

In this gateway version, the gateway does not add the `X-Forwarded-Host`, `X-Forwarded-Proto` and
`X-Forwarded-Prefix` headers unless a proxy is trusted. `.*` trusts everyone. This is fine for
**local and dev only**. For stage, qa and prod, use a regex for the real proxy IP range.

**In each service** (`application.properties` of `auth-service`, `user-service`, `product-service`):

```properties
server.forward-headers-strategy=framework
```

This makes the service read the `X-Forwarded-*` headers. The server URL in the docs then becomes
`http://localhost:9999/auth-service` instead of `http://localhost:9091`.

### Step 7: Close the service ports (deployment)

This is a network task, not a Spring task. Open only port 9999 to the outside. Keep ports 9091 to
9094 reachable only inside the private network (Docker port mapping, security group or firewall).

---

## 4. Files that were changed

| File | What changed |
|---|---|
| `gateway/pom.xml` | Added the `springdoc-openapi-starter-webflux-ui` dependency |
| `gateway/src/main/resources/application.properties` | Added the `springdoc.swagger-ui.urls[...]` list (the dropdown) |
| `gateway/.../application-local.properties`, `application-dev.properties` | Added the product-service route and `trusted-proxies` |
| `gateway/.../constant/OpenEndpoint.java` | Docs paths skip the JWT check |
| `user-service/.../config/SecurityConfig.java` | Allowed `/v3/api-docs` |
| `product-service/.../config/SecurityConfig.java` | New file, with the docs allowed |
| `application.properties` of auth-service, user-service, product-service | Added `server.forward-headers-strategy=framework` |

---

## 5. How to check that it works

1. Start Eureka, all services and the gateway.
2. Open `http://localhost:9999/swagger-ui.html`. It redirects to `/swagger-ui/index.html`.
3. Use the "Select a definition" dropdown to switch between services.
4. Look at the "Servers" box. It should show `http://localhost:9999/<service>`.

Checks from the terminal:

```bash
curl localhost:9999/v3/api-docs/swagger-config          # the dropdown list
curl localhost:9999/auth-service/v3/api-docs            # should return 200
curl localhost:9999/user-service/v3/api-docs            # should return 200
curl -i localhost:9999/user-service/api/v1/users/me     # should return 401: real APIs stay protected
```

---

## 6. Adding a new service later

1. Add a gateway route for it (Step 2).
2. Add one more `springdoc.swagger-ui.urls[n]` item (Step 3).
3. Add `/<service>/v3/api-docs` to `OpenEndpoint` (Step 4).
4. Allow `/v3/api-docs/**` in the service's security config (Step 5).
5. Add `springdoc-openapi-starter-webmvc-ui` to the service's pom, and add
   `server.forward-headers-strategy=framework` to its properties (Step 6).

---

## 7. Problems we hit and their causes

| Problem | Cause |
|---|---|
| `/swagger-ui.html` returns 404 on the gateway | The springdoc library was not in the running app's classpath. Reload Maven in IntelliJ, then restart. |
| Docs return `401 SEC-005` through the gateway | The path is missing in `OpenEndpoint` (Step 4). |
| user-service docs return 401 | `/v3/api-docs` is missing in its `SecurityConfig` (Step 5). |
| product-service returns 401, even directly on port 9093 | It had no `SecurityFilterChain`, so Spring's default rules applied (Step 5). |
| Docs return 503 through the gateway | The service is not running, or it is not registered in Eureka. |
| The "Servers" box shows `http://localhost:9091` | Step 6 is missing in the gateway or in the service. |
| `mvn compile` fails in product-service with lombok `1.6.3` | In its pom, the lombok entries inside `annotationProcessorPaths` have no `<version>`. Add `<version>${lombok.version}</version>`. IntelliJ builds fine, only the command line fails. |

---

## 8. Before going to production

- Replace `trusted-proxies=.*` with the real proxy IP range.
- Turn the UI off in prod: set `springdoc.swagger-ui.enabled=false` and
  `springdoc.api-docs.enabled=false` in each `application-prod.properties`.
- Not done yet: an **Authorize** button for the JWT. Each service would need an `OpenAPI` bean
  with a bearer security scheme. For now, the token must be added by hand as a header.

---

## 9. What was tested

- The gateway Swagger UI opens in a browser. The dropdown lists auth-service, user-service and
  product-service.
- The docs of auth-service and user-service load through the gateway. Their server URL is
  `http://localhost:9999/<service>`.
- A real API call without a token returns 401.
- After Step 5, product-service docs returned 200 through the gateway. At the last check the
  service was stopped, so that run was not repeated.
- Not tested: a real login with valid credentials, and "Try it out" with a token.
