# API Gateway se saare Swagger kaise chalayein (Hinglish, saral guide)

## Goal kya hai?

Frontend developer ko **sirf ek URL** chahiye (gateway, port 9999). Us ek jagah se wo
saari microservices ke APIs dekh sake aur test kar sake. Har service ka port (9091, 9092,
9093) bahar kholne ki zarurat nahi.

```
http://localhost:9999/swagger-ui.html
```

---

## 1. Ye kaam kaise karta hai? (pehle idea samjho)

- **Swagger UI ka page** sirf gateway dikhata hai.
- **API docs (OpenAPI JSON)** har service khud banati hai, `/v3/api-docs` par.
- Gateway apne existing route se us service ka `/v3/api-docs` laata hai.

```
Browser ──► gateway:9999/swagger-ui.html                 (UI page, gateway se)
Browser ──► gateway:9999/auth-service/v3/api-docs    ──► auth-service:9091/v3/api-docs
Browser ──► gateway:9999/user-service/v3/api-docs    ──► user-service:9092/v3/api-docs
Browser ──► gateway:9999/product-service/v3/api-docs ──► product-service:9093/v3/api-docs
```

Gateway route me `StripPrefix=1` hai, matlab pehla part (`/auth-service`) hat jata hai,
aur service ko sirf `/v3/api-docs` milta hai.

**Dropdown kahan se aata hai?**
Swagger UI ke upar "Select a definition" dropdown hai. Ye step 3 ki list se banta hai.
Har entry = ek **name** + ek **docs URL**. Naya service add karna hai to bas ek entry aur
jodo.

---

## 2. Kaun si problem, kaun sa step

| Problem | Kaun sa step theek karta hai |
|---|---|
| Gateway me Swagger UI hi nahi hai | Step 1 (dependency) |
| Service ka gateway route nahi hai | Step 2 (route) |
| Dropdown me services nahi dikh rahi | Step 3 (URL list) |
| Docs par 401 aa raha hai (gateway me) | Step 4 (gateway whitelist) |
| Docs par 401 aa raha hai (service me) | Step 5 (service whitelist) |
| "Try it out" service ke port par ja raha hai | Step 6 (forwarded headers) |

---

## 3. Steps

### Step 1: Gateway me Swagger UI ki dependency daalo

File: `gateway/pom.xml`

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>${swagger.version}</version>
</dependency>
```

- **webflux** wala artifact hi lena hai, kyunki gateway reactive (WebFlux) hai.
  Services me `webmvc-ui` hai, wo gateway me nahi chalega.
- Parent pom sirf `webmvc` ka version manage karta hai, isliye yahan version likha hai.
  `${swagger.version}` parent ki property hai (3.0.2), to sab modules same version par rahenge.
- **Zaruri:** pom change karne ke baad IntelliJ me **Maven reload** karo (Maven tool window
  me reload button), phir restart karo. Sirf restart karoge to purani classpath rahegi aur
  UI 404 dega.

### Step 2: Har service ka gateway route hona chahiye

Files: `gateway/src/main/resources/application-local.properties` aur `application-dev.properties`

User-service aur auth-service ke routes pehle se the. Product-service ka nahi tha, to ye
add kiya:

```properties
spring.cloud.gateway.server.webflux.routes[2].id=PRODUCT-SERVICE
spring.cloud.gateway.server.webflux.routes[2].uri=lb://PRODUCT-SERVICE
spring.cloud.gateway.server.webflux.routes[2].predicates[0]=Path=/product-service/**
spring.cloud.gateway.server.webflux.routes[2].filters[0]=StripPrefix=1
```

Har line ka matlab:
- `uri=lb://PRODUCT-SERVICE`: Eureka me service dhundo aur load-balance karo.
- `Path=/product-service/**`: jo bhi URL `/product-service/` se shuru ho.
- `StripPrefix=1`: `/product-service` hata ke service ko bhejo.

### Step 3: Dropdown ki list banao

File: `gateway/src/main/resources/application.properties`

```properties
springdoc.swagger-ui.urls[0].name=auth-service
springdoc.swagger-ui.urls[0].url=/auth-service/v3/api-docs
springdoc.swagger-ui.urls[1].name=user-service
springdoc.swagger-ui.urls[1].url=/user-service/v3/api-docs
springdoc.swagger-ui.urls[2].name=product-service
springdoc.swagger-ui.urls[2].url=/product-service/v3/api-docs
```

- `name`: dropdown me jo text dikhega.
- `url`: UI is URL se docs laata hai. Ye **gateway ka path** hai (step 2 ke route se
  guzarta hai), service ka port nahi.
- Gateway ye list `/v3/api-docs/swagger-config` par deta hai, aur UI usi se dropdown banata hai.

### Step 4: Gateway ke JWT filter se docs ko bahar rakho

File: `gateway/src/main/java/org/ecommerce/gateway/constant/OpenEndpoint.java`

```java
"/auth-service/v3/api-docs",
"/user-service/v3/api-docs",
"/product-service/v3/api-docs"
```

- `JwtAuthenticationFilter` har request par token maangta hai, sirf is set ke paths chhod ke.
- Browser docs bina token ke maangta hai. Ye paths nahi daaloge to `401 SEC-005` aayega.
- Filter `startsWith` use karta hai, isliye `/v3/api-docs/swagger-config` jaise sub-paths bhi cover ho jaate hain.
- Sirf docs khule hain. Asli APIs (jaise `/user-service/api/v1/...`) ke liye JWT abhi bhi zaruri hai.
- Swagger UI page (`/swagger-ui.html`) ke liye entry nahi chahiye, kyunki wo gateway khud
  serve karta hai (filter sirf routed requests par chalta hai).

### Step 5: Har service ki apni security me docs allow karo

Har service ki apni Spring Security hai, wahan bhi docs allow karne padte hain.

| Service | Kya karna pada |
|---|---|
| auth-service | Kuch nahi. `SecurityConfig` me `/v3/api-docs/**` pehle se tha. |
| user-service | `SecurityConfig` ke `WHITE_LIST` me `"/v3/api-docs", "/v3/api-docs/**"` add kiya. |
| product-service | Isme koi `SecurityFilterChain` hi nahi tha, to Spring ka default sab kuch lock kar raha tha (token ke saath bhi 401). Naya `config/SecurityConfig.java` banaya: health aur docs open, baaki sab ke liye JWT. |

### Step 6: "Try it out" gateway par call kare, service ke port par nahi

Default me har service apne docs me **apna address** likhti hai:
`"servers": [{"url": "http://localhost:9091"}]`. Fir "Try it out" 9091 par jayega, jo hum
band rakhna chahte hain. Isko do properties se theek karte hain.

**Gateway me** (`application-local.properties` aur `application-dev.properties`):

```properties
spring.cloud.gateway.server.webflux.trusted-proxies=.*
```

Is gateway version me jab tak proxy trusted na ho, wo `X-Forwarded-Host/Proto/Prefix`
headers service ko nahi bhejta. `.*` ka matlab sab trusted. Ye **sirf local/dev** ke liye
theek hai. Stage/qa/prod me asli IP range ka regex do.

**Har service me** (`auth-service`, `user-service`, `product-service` ki `application.properties`):

```properties
server.forward-headers-strategy=framework
```

Isse service `X-Forwarded-*` headers padhti hai, to docs me server URL
`http://localhost:9999/auth-service` aata hai, `http://localhost:9091` nahi.

### Step 7: Service ke ports band karo (deployment)

Ye Spring ka kaam nahi, network ka kaam hai. Sirf 9999 bahar kholo. 9091 se 9094 ko
sirf private network ke andar rakho (Docker port mapping, security group, firewall).

---

## 4. Kaun si files badli

| File | Kya badla |
|---|---|
| `gateway/pom.xml` | `springdoc-openapi-starter-webflux-ui` dependency |
| `gateway/src/main/resources/application.properties` | `springdoc.swagger-ui.urls[...]` list (dropdown) |
| `gateway/.../application-local.properties`, `application-dev.properties` | product-service route, `trusted-proxies` |
| `gateway/.../constant/OpenEndpoint.java` | docs paths JWT check se bahar |
| `user-service/.../config/SecurityConfig.java` | `/v3/api-docs` whitelist |
| `product-service/.../config/SecurityConfig.java` | nayi file, docs whitelisted |
| `auth-service`, `user-service`, `product-service` ki `application.properties` | `server.forward-headers-strategy=framework` |

---

## 5. Check kaise karein

1. Eureka, saari services aur gateway start karo.
2. Browser me kholo: `http://localhost:9999/swagger-ui.html` (ye `/swagger-ui/index.html` par redirect hota hai).
3. Upar "Select a definition" dropdown se service badlo.
4. "Servers" box me `http://localhost:9999/<service>` dikhna chahiye.

Terminal se check:

```bash
curl localhost:9999/v3/api-docs/swagger-config          # dropdown ki list
curl localhost:9999/auth-service/v3/api-docs            # 200
curl localhost:9999/user-service/v3/api-docs            # 200
curl -i localhost:9999/user-service/api/v1/users/me     # 401, asli API protected hai
```

---

## 6. Naya service add karna ho to

1. Gateway me uska route add karo (step 2).
2. `springdoc.swagger-ui.urls[n]` me ek entry add karo (step 3).
3. `OpenEndpoint` me `/<service>/v3/api-docs` add karo (step 4).
4. Service ki security me `/v3/api-docs/**` whitelist karo (step 5).
5. Service ke pom me `springdoc-openapi-starter-webmvc-ui` aur properties me
   `server.forward-headers-strategy=framework` daalo.

---

## 7. Jo problems aayi aur unke reason

| Problem | Reason |
|---|---|
| Gateway par `/swagger-ui.html` 404 | Springdoc jar running classpath me nahi tha. IntelliJ me Maven reload karo, phir restart. |
| Gateway se docs par `401 SEC-005` | `OpenEndpoint` me path nahi tha (step 4). |
| user-service ke docs par 401 | Uski `SecurityConfig` me `/v3/api-docs` nahi tha (step 5). |
| product-service par seedha 9093 par bhi 401 | `SecurityFilterChain` nahi tha, Spring ka default laga (step 5). |
| Gateway se docs par 503 | Service chal nahi rahi ya Eureka me registered nahi hai. |
| "Servers" me `http://localhost:9091` dikh raha hai | Step 6 gateway ya service me chhoot gaya. |
| product-service par `mvn compile` fail (lombok `1.6.3`) | Uske pom me `annotationProcessorPaths` ke lombok me `<version>` nahi hai. `<version>${lombok.version}</version>` add karo. (IntelliJ me chalta hai, sirf command line me dikkat aati hai.) |

---

## 8. Production se pehle

- `trusted-proxies=.*` ko asli proxy range se badlo.
- Prod me UI band karo: har `application-prod.properties` me
  `springdoc.swagger-ui.enabled=false` aur `springdoc.api-docs.enabled=false`.
- Abhi nahi hua: JWT ke liye **Authorize** button. Iske liye har service me `OpenAPI` bean
  (bearer security scheme ke saath) chahiye. Abhi token header me haath se daalna padega.

---

## 9. Kya test hua

- Gateway ka Swagger UI browser me khulta hai, dropdown me auth-service, user-service aur
  product-service dikhti hain.
- Auth-service aur user-service ke docs gateway se load hote hain, server URL
  `http://localhost:9999/<service>` hai.
- Bina token ke asli API par 401 aata hai.
- Step 5 ke baad product-service ke docs gateway se 200 aaye the. Aakhri check me wo band thi,
  isliye wo run dobara nahi hua.
- Test nahi hua: valid credentials se asli login, aur token ke saath "Try it out".
