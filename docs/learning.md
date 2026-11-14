# Learning: the refresh-token cookie flow

[← Back to README](../README.md) · Related: [Security Model](security.md)

This is a walkthrough of every file changed to move the refresh token from the JSON body into an
**HttpOnly cookie**. For each file it explains **what** it is, **why** it exists, **how** each line
works, **where** it comes from and **which** alternatives exist. Every "Real output" block was
produced by running the actual code, not written by hand.

Read it in order. Each stop ends with "Check yourself" questions, and the answers are at the very end.

---

## Part A: The big picture

### The problem

Before, login returned **both** tokens in the JSON body:

```json
{ "data": { "id": 1, "token": "<access>", "refreshToken": "<refresh>" } }
```

The frontend had to keep the refresh token somewhere JavaScript can read, usually `localStorage`.
The refresh token lives **7 days**. If an attacker ever runs JavaScript on your page (an XSS bug, a
compromised npm package), they read it and can stay logged in as that user for 7 days.

**Analogy:** the access token is a day pass you show at every door. The refresh token is the key to
the office that prints new day passes. Before, we handed the key to the visitor's pocket, where any
pickpocket (malicious script) could take it. Now the key lives in a locked box (the HttpOnly cookie)
that the visitor carries but cannot open. Only the office itself can open it.

### The fix

The refresh token goes into a **cookie marked HttpOnly**. The browser stores it and sends it back on
its own, but **JavaScript cannot read it at all**.

```
LOGIN      browser ──email+password──► gateway ──► auth-service
           browser ◄── body: access token
                   ◄── Set-Cookie: __Secure-refresh=<refresh>; HttpOnly; Secure; ...

API CALL   browser ──Authorization: Bearer <access>──► gateway ──► any service

REFRESH    browser ──(no body, the cookie goes automatically)──► gateway ──► auth-service
           browser ◄── body: NEW access token
                   ◄── Set-Cookie: __Secure-refresh=<NEW refresh>   (rotated)

LOGOUT     browser ──(the cookie goes automatically)──► gateway ──► auth-service
           browser ◄── Set-Cookie: __Secure-refresh=; Max-Age=0     (browser deletes it)
```

### The map of stops

The stops follow the path of a request, from the cookie's settings out to the gateway.

| Stop | File | Its job in one line |
|---|---|---|
| 1 | `RefreshCookieProperties` + 4 lines in `application.properties` | The cookie's settings |
| 2 | `RefreshCookieFactory` | Builds, reads and deletes the cookie |
| 3 | `TokenResponse` + `AuthenticationController` | Login, refresh and logout send the cookie |
| 4 | `AuthenticationServiceImpl` + `TokenServiceImpl` | Refresh and logout logic, and one expiry value |
| 5 | `OpenEndpoint` + `CookieEndpointFilter` (gateway) | Logout is open, and an expired Bearer is dropped |
| 6 | Gateway CORS settings | Which websites may call us (the CSRF guard) |
| 7 | `ExceptionHandlerAutoConfig` (utility-service) | Real error codes instead of a generic 401 |
| 8 | The tests | How each piece was proved |

---

## Stop 1: `RefreshCookieProperties` and its properties

**Files:** `auth-service/.../config/RefreshCookieProperties.java` and the `auth.refresh-cookie.*`
lines in `auth-service/src/main/resources/application.properties`.

### What is it?

A **settings holder**: a small Java class whose fields Spring fills automatically from
`application.properties`. It holds the four settings of the cookie: name, path, secure flag and SameSite.

### Why do we need it?

The cookie settings must be **different per environment**. On a laptop you may need `secure=false`.
If the frontend is on another domain, prod needs `same-site=None`. Written inside Java, every change
would need a code change and a redeploy. As properties, you change one line in a profile file.

### The properties lines

```properties
auth.refresh-cookie.name=__Secure-refresh
auth.refresh-cookie.path=/auth-service/api/v1/auth
auth.refresh-cookie.secure=true
auth.refresh-cookie.same-site=Strict
```

| Line | What it means | Why this value |
|---|---|---|
| `name=__Secure-refresh` | The cookie's name in the browser | `__Secure-` is a browser rule: a cookie with this prefix is **rejected unless it also has the Secure flag**. A safety net against shipping it over plain HTTP by mistake. |
| `path=/auth-service/api/v1/auth` | The browser sends the cookie **only** to URLs starting with this path | Calls to user-service or product-service don't carry it, so it can't leak there. It is the **gateway** path (with `/auth-service`), because the browser only ever sees the gateway URL. |
| `secure=true` | Sent **only over HTTPS** | Nobody on the network (café Wi-Fi) can read it |
| `same-site=Strict` | **Not** sent when another website triggers the request | Stops other sites from using the user's cookie (see stop 6) |

The lifetime is deliberately **not** here: it reuses `jwt.refresh-expiration`, so there is one value,
not two that can drift apart (stop 4).

### The class, line by line

```java
@ConfigurationProperties(prefix = "auth.refresh-cookie")
public record RefreshCookieProperties(
        @DefaultValue("__Secure-refresh") String name,
        @DefaultValue("/auth-service/api/v1/auth") String path,
        @DefaultValue("true") boolean secure,
        @DefaultValue("Strict") String sameSite) {
}
```

| Code | What it does | Why |
|---|---|---|
| `@ConfigurationProperties(prefix = "auth.refresh-cookie")` | Fill this class from every property starting with `auth.refresh-cookie.` | One annotation instead of one `@Value` per field |
| `public record ...` | A Java **record**: an immutable data class. Java generates the constructor, the getters `name()`, `path()`…, `equals` and `toString`. | Settings never change while the app runs, so immutable is safer. No Lombok needed. |
| the fields | Spring matches `name` → `name`, `same-site` → `sameSite`, and converts `"true"` → `boolean` | Spring Boot "relaxed binding" |
| `@DefaultValue("...")` | Used **if the property is missing** | The app still has safe values if someone deletes the lines |

**Where it comes from:** `@ConfigurationProperties` and `@DefaultValue` are Spring Boot
(`org.springframework.boot.context.properties` and `...properties.bind`). Spring finds this class
because `AuthServiceApplication` already has `@ConfigurationPropertiesScan`.

### Real output

Spring's own binder, run on the project's `application.properties`:

```
FILE   auth.refresh-cookie.name = __Secure-refresh
FILE   auth.refresh-cookie.path = /auth-service/api/v1/auth
FILE   auth.refresh-cookie.same-site = Strict
FILE   auth.refresh-cookie.secure = true
BOUND  RefreshCookieProperties[name=__Secure-refresh, path=/auth-service/api/v1/auth, secure=true, sameSite=Strict]

ONLY same-site=None in file -> RefreshCookieProperties[name=__Secure-refresh, path=/auth-service/api/v1/auth, secure=true, sameSite=None]
NOTHING in file             -> RefreshCookieProperties[name=__Secure-refresh, path=/auth-service/api/v1/auth, secure=true, sameSite=Strict]
```

**How to read it:** `FILE` lines are what's in the file. `BOUND` is the object Spring built; note
`same-site` became `sameSite`. With only one line in the file, that value wins and the rest come from
`@DefaultValue` (this is how a prod profile overrides just one setting). With nothing in the file,
every value is a default, and the app is still safe.

### Which alternatives, and why not

| Option | Why not here |
|---|---|
| `@Value("${...}")` on each field | Four scattered annotations, defaults not in one place, harder to test |
| Constants in code | Can't change per environment without a code change |
| A Lombok `@Data` class | Works, but mutable. A record is shorter and can't be changed by accident. |

### Watch out

- `secure=false` for local HTTP **also** needs a name without `__Secure-` (for example `refresh`).
  Otherwise the browser silently drops the cookie. The factory (stop 2) refuses to start in that case.
- If the gateway route prefix `/auth-service` ever changes, `path` must change too.

### Check yourself

1. Why is the path `/auth-service/api/v1/auth` and not `/api/v1/auth`?
2. In prod the frontend is on `shop.com` and the API on `api-shop.io`. Which one line do you change, and to what?
3. If all four lines are deleted, does the app break?
4. Why is the lifetime not one of these four settings?

---

## Stop 2: `RefreshCookieFactory`

**File:** `auth-service/.../util/RefreshCookieFactory.java`

### What is it?

The **only place** that builds, reads or deletes the refresh cookie. Login, refresh and logout all go
through it.

### Why do we need it?

A browser deletes a cookie only when the "delete" cookie has the **same name, path and flags** as the
original. If login built the cookie in one place and logout built the delete in another, one small
difference (a different path) would mean logout **silently fails** to remove it. One factory makes the
match automatic.

### The code, line by line

```java
@Component
public class RefreshCookieFactory {

    private static final String SECURE_PREFIX = "__Secure-";

    private final RefreshCookieProperties properties;
    private final Duration maxAge;

    public RefreshCookieFactory(RefreshCookieProperties properties, JWTPropertiesConfig jwtProperties) {
        if (!properties.secure() && properties.name().startsWith(SECURE_PREFIX)) {
            throw new IllegalStateException(...);
        }
        if (!properties.secure() && "None".equalsIgnoreCase(properties.sameSite())) {
            throw new IllegalStateException(...);
        }
        this.properties = properties;
        this.maxAge = Duration.ofMillis(jwtProperties.getRefreshExpiration());
    }

    public ResponseCookie build(String refreshToken) {
        return base(refreshToken).maxAge(maxAge).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, properties.name());
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path());
    }
}
```

| Code | What it does | Why |
|---|---|---|
| `@Component` | Spring creates one instance and injects it where needed | The controller receives it in its constructor |
| Constructor parameters | Spring passes the settings (stop 1) and `JWTPropertiesConfig` (the existing `jwt.*` settings) | The cookie needs both its own settings and the token lifetime |
| The two `if ... throw` checks | Refuse to start with settings browsers silently reject | A crash at startup is easy to see. A cookie that silently never arrives costs hours of debugging. |
| `Duration.ofMillis(jwtProperties.getRefreshExpiration())` | Turns `604800000` ms into a `Duration` of 7 days | `Max-Age` comes from the **same** value as the JWT's expiry |
| `build(token)` | The cookie sent after login and refresh: the token, with Max-Age = 7 days | Used by login and refresh |
| `clear()` | Same cookie, **empty value, `Max-Age=0`** | `Max-Age=0` tells the browser "delete this cookie now" |
| `read(request)` | Finds the cookie by name in the incoming request, or `null` | The service then decides what "no cookie" means |
| `base(value)` | The shared part: name, `HttpOnly`, `Secure`, `SameSite`, `Path` | `build` and `clear` both start from here, so they always match |

**Where it comes from:** `ResponseCookie` is Spring Framework (`org.springframework.http`), a builder
for a correct `Set-Cookie` header. `WebUtils.getCookie` is `org.springframework.web.util`. `Cookie` and
`HttpServletRequest` are the Jakarta Servlet API.

### Real output

```
build("eyJhbGc...") -> __Secure-refresh=eyJhbGc...; Path=/auth-service/api/v1/auth; Max-Age=604800; Expires=Fri, 20 Nov 2026 19:39:20 GMT; Secure; HttpOnly; SameSite=Strict
clear()             -> __Secure-refresh=; Path=/auth-service/api/v1/auth; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=Strict
read(with cookie)   -> eyJhbGc...
read(no cookie)     -> null

secure=false + __Secure- name -> auth.refresh-cookie.name starts with __Secure- but auth.refresh-cookie.secure=false; browsers would reject the cookie
secure=false + SameSite=None  -> auth.refresh-cookie.same-site=None requires auth.refresh-cookie.secure=true; browsers would reject the cookie
```

**How to read it:**
- `build`: this exact text becomes the `Set-Cookie` response header. `Max-Age=604800` is seconds
  (7 days). `Expires` is the same moment as a date, for old browsers.
- `clear`: same name, path and flags, but empty value, `Max-Age=0` and an `Expires` date in 1970. The
  browser deletes the cookie.
- The last two lines are the startup errors for invalid combinations.

### Which alternatives, and why not

| Option | Why not here |
|---|---|
| `new jakarta.servlet.http.Cookie(...)` + `response.addCookie` | The Servlet `Cookie` class has no `SameSite` setter, so you'd write the header by hand |
| `@CookieValue("__Secure-refresh")` in the controller | Hard-codes the name; ours comes from properties |
| Building the header string by hand | Easy to get the format or a flag wrong |

### Check yourself

1. Why do `build` and `clear` both start from `base(...)`?
2. What exactly makes the browser delete the cookie on logout?
3. Why does the factory throw at startup instead of just logging a warning?

---

## Stop 3: `TokenResponse` and `AuthenticationController`

**Files:** `auth-service/.../dto/TokenResponse.java`, `auth-service/.../controller/AuthenticationController.java`

### What changed?

The three endpoints now send the refresh token **only** in the cookie, and refresh and logout read it
from the cookie instead of the JSON body.

### `TokenResponse`

```java
public record TokenResponse(Long id, String token) {
}
```

The body of login and refresh. It has **no** `refreshToken` field, so the refresh token can never end
up in the body by mistake. The field names `id` and `token` are the same as before, so frontend code
reading `data.token` keeps working.

### The controller, method by method

```java
@PostMapping(AuthMappingConstant.LOGIN)
public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody AuthenticationRequest request) {
    return withRefreshCookie(authenticationService.authenticate(request), ApiMessages.LOGIN_SUCCESS);
}

@PostMapping(AuthMappingConstant.REFRESH_TOKEN)
public ResponseEntity<ApiResponse<Object>> refreshToken(HttpServletRequest request) {
    return withRefreshCookie(authenticationService.refreshToken(refreshCookieFactory.read(request)),
            ApiMessages.OPERATION_SUCCESSFUL);
}

@PostMapping(AuthMappingConstant.LOGOUT)
public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request) {
    authenticationService.logout(refreshCookieFactory.read(request));
    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.clear().toString())
            .cacheControl(CacheControl.noStore())
            .body(ApiResponse.success(null, ApiMessages.LOGOUT_SUCCESS));
}

private ResponseEntity<ApiResponse<Object>> withRefreshCookie(AuthenticationDto tokens, String message) {
    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.build(tokens.getRefreshToken()).toString())
            .cacheControl(CacheControl.noStore())
            .body(ApiResponse.success(new TokenResponse(tokens.getId(), tokens.getToken()), message));
}
```

| Code | What it does | Why |
|---|---|---|
| `login(...)` | Authenticates as before, then hands the result to `withRefreshCookie` | The service still returns both tokens. The **controller** decides where each one goes. |
| `refreshToken(HttpServletRequest request)` | No `@RequestBody` any more. It reads the cookie through the factory. | The browser sends the cookie on its own, so there is nothing to put in a body |
| `logout(HttpServletRequest request)` | Reads the cookie, asks the service to revoke, **always** sends `clear()` | Even if the token was already invalid, the browser must drop it |
| `withRefreshCookie(tokens, message)` | Puts the refresh token in `Set-Cookie`, the access token in the body | One place, so login and refresh behave the same |
| `.header(HttpHeaders.SET_COOKIE, ...toString())` | The `ResponseCookie` from stop 2 becomes the response header | This is the header the browser reads to store the cookie |
| `.cacheControl(CacheControl.noStore())` | `Cache-Control: no-store` | No browser or proxy may cache a response that contains a token |
| `new TokenResponse(id, token)` | The body without the refresh token | The point of the whole change |

The old `RefreshTokenRequest` and `LogoutRequest` DTOs were deleted: nothing sends a body to those
endpoints any more.

### Real output

The real controller and factory, with a mocked service:

```
POST /login -> status=200
   Set-Cookie:    __Secure-refresh=REFRESH-1; Path=/auth-service/api/v1/auth; Max-Age=604800; Expires=...; Secure; HttpOnly; SameSite=Strict
   Cache-Control: no-store
   body:          {"success":true,"message":"Login successful. Welcome back!","data":{"id":1,"token":"ACCESS-1"}}

POST /refresh-token (cookie REFRESH-1) -> status=200
   Set-Cookie:    __Secure-refresh=REFRESH-2; Path=/auth-service/api/v1/auth; Max-Age=604800; ...
   Cache-Control: no-store
   body:          {"success":true,"message":"Operation completed successfully.","data":{"id":1,"token":"ACCESS-2"}}

POST /refresh-token (no cookie) -> status=401
   body:          {"status":401,"errorCode":"AUTH_301","message":"Refresh token is invalid.","path":"/api/v1/auth/refresh-token"}

POST /logout (cookie REFRESH-2) -> status=200
   Set-Cookie:    __Secure-refresh=; Path=/auth-service/api/v1/auth; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; ...
   Cache-Control: no-store
   body:          {"success":true,"message":"You have been logged out successfully.","data":null}
service.logout was called with: REFRESH-2
```

**How to read it:**
- Login: the body has only `token` (`ACCESS-1`); `REFRESH-1` is only in `Set-Cookie`.
- Refresh: the cookie that came in was `REFRESH-1`, and the new one going out is `REFRESH-2`. That is
  **rotation**: every refresh replaces the refresh token.
- Logout: `Max-Age=0`, so the browser deletes the cookie. The service received the token from the cookie.

### Check yourself

1. Login still gets both tokens from the service. Where is the decision made about which token goes where?
2. Why does logout send `clear()` even when the cookie was invalid?
3. What breaks for the frontend if `Cache-Control: no-store` is missing?

---

## Stop 4: `AuthenticationServiceImpl` and `TokenServiceImpl`

**Files:** `auth-service/.../service/impl/AuthenticationServiceImpl.java`,
`auth-service/.../service/impl/TokenServiceImpl.java`, `auth-service/.../service/AuthenticationService.java`

### What changed?

1. Refresh and logout take the refresh token as a plain `String` (from the cookie).
2. Logout finds the user from the **refresh** token, not the access token.
3. Logout is **idempotent**: calling it twice, or with a bad token, is not an error.
4. The database expiry of a refresh token comes from the same property as the JWT and the cookie.

### Refresh

```java
public AuthenticationDto refreshToken(String refreshToken) {
    if (!StringUtils.hasText(refreshToken)) {
        throw new AuthenticationException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
    if (tokenService.validateRefreshToken(refreshToken)) {
        ... // unchanged: load the user, rotate the refresh token, issue a new access token
    }
    ...
}
```

| Code | Why |
|---|---|
| `StringUtils.hasText(refreshToken)` | No cookie (`null`) or an empty one is rejected at once with `AUTH_301`, before any database lookup |
| The rest | Unchanged logic: validate in the DB, load roles from user-service, rotate, issue access token |

### Logout

```java
public void logout(String refreshToken) {
    if (!StringUtils.hasText(refreshToken)) {
        log.info("Logout without a refresh token - nothing to revoke");
        return;
    }
    try {
        if (tokenService.validateRefreshToken(refreshToken)) {
            Long userAccountId = jwtTokenValidator.validateTokenAndGetClaims(refreshToken)
                    .get(USER_ACCOUNT_ID, Long.class);
            tokenService.revokeAllRefreshTokensForUser(userAccountId);
        }
    } catch (AuthenticationException | JwtException | IllegalArgumentException e) {
        log.info("Logout with an unusable refresh token - nothing to revoke: {}", e.getMessage());
    }
}
```

| Code | What it does | Why |
|---|---|---|
| No token → `return` | Nothing to revoke | The user is already logged out from the server's view |
| `validateRefreshToken(refreshToken)` | Checks the token exists in the DB, is active and not expired | Only real sessions are revoked |
| `.get(USER_ACCOUNT_ID, Long.class)` on the **refresh** token | Takes the user from the refresh token's claims | **Before**, logout read the user from the access token, so an expired access token made logout fail. The refresh token already carries `userAccountId`. |
| `revokeAllRefreshTokensForUser(...)` | Marks the user's refresh tokens inactive | Existing method, reused |
| `catch (AuthenticationException \| JwtException \| IllegalArgumentException e)` | Unknown, revoked, expired or malformed token: log and return | Logout should never fail. The controller still clears the cookie. |

`JwtException` is jjwt's parent class for all token problems (`ExpiredJwtException`, bad signature,
and so on). `AuthenticationException` is your own exception, thrown when the DB lookup finds nothing.

### One expiry value (`TokenServiceImpl`)

```java
// Before
refreshTokenEntity.setExpiredAt(LocalDateTime.now().plusDays(7));
// After
refreshTokenEntity.setExpiredAt(LocalDateTime.now().plus(Duration.ofMillis(jwtProperties.getRefreshExpiration())));
```

Before, the JWT's `exp` came from `jwt.refresh-expiration`, but the DB row hard-coded 7 days. If someone
changed the property to 1 day, the JWT, the DB row and the cookie would disagree. Now all three use
`jwt.refresh-expiration`.

### Real output

The real auth-service, called over HTTP:

```
jwt.refresh-expiration=604800000 ms = PT168H = 7 days

POST /api/v1/auth/refresh-token (no cookie)                       -> 401 {"status":401,"errorCode":"AUTH_301","message":"Refresh token is invalid.",...}
POST /api/v1/auth/refresh-token (__Secure-refresh=not-a-real-token) -> 404 {"status":404,"errorCode":"AUTH_302","message":"Refresh token not found.",...}

LOG  AuthenticationServiceImpl - Logout with an unusable refresh token - nothing to revoke: Refresh token not found.
POST /api/v1/auth/logout (__Secure-refresh=not-a-real-token)       -> 200 {"success":true,...}  Set-Cookie: __Secure-refresh=; ...; Max-Age=0; ...

LOG  AuthenticationServiceImpl - Logout without a refresh token - nothing to revoke
POST /api/v1/auth/logout (no cookie)                                -> 200 {"success":true,...}  Set-Cookie: __Secure-refresh=; ...; Max-Age=0; ...
```

**How to read it:** refresh fails with a clear code for each case (no cookie: `AUTH_301`; unknown
token: `AUTH_302`). Logout always returns 200 and always clears the cookie; the log line explains why
nothing was revoked.

### Check yourself

1. Why does logout read the user from the refresh token and not the access token?
2. Why is it acceptable for logout to return 200 with an invalid token?
3. Before this change, what could go wrong if someone set `jwt.refresh-expiration` to one day?

---

## Stop 5: `OpenEndpoint` and `CookieEndpointFilter` (gateway)

**Files:** `gateway/.../constant/OpenEndpoint.java`, `gateway/.../filter/CookieEndpointFilter.java`

### What changed?

1. **Logout is an open endpoint** at the gateway: the gateway no longer demands an access token for it.
2. On refresh and logout, the gateway **removes the `Authorization` header** before forwarding.

### `OpenEndpoint`

```java
Set<String> openApi = Set.of(
        "/auth-service/api/v1/auth/login",
        "/auth-service/api/v1/auth/refresh-token",
        "/auth-service/api/v1/auth/register",
        "/auth-service/api/v1/auth/logout",          // new
        ...);

Set<String> cookieAuthenticated = Set.of(            // new
        "/auth-service/api/v1/auth/refresh-token",
        "/auth-service/api/v1/auth/logout");
```

| Set | Used by | Meaning |
|---|---|---|
| `openApi` | `JwtAuthenticationFilter` | The gateway does not check for an access token on these paths |
| `cookieAuthenticated` | `CookieEndpointFilter` | These paths are authenticated by the cookie, never by a Bearer token |

Without `logout` in `openApi`, a user whose access token had expired would get a 401 from the
gateway and could not log out.

### `CookieEndpointFilter`

**The problem it solves:** most frontend HTTP clients attach `Authorization: Bearer ...` to **every**
call. When the frontend calls refresh, that access token has normally just expired. Each service's
shared `JWTFilter` (utility-service) rejects an **expired** Bearer on every path, so refresh would be
rejected exactly when it is needed. This was proved by running it: refresh with an expired Bearer
returned `401 "JWT token expired"`.

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CookieEndpointFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        boolean hasAuthorization = exchange.getRequest().getHeaders().containsHeader(HttpHeaders.AUTHORIZATION);

        if (hasAuthorization && OpenEndpoint.cookieAuthenticated.contains(path)) {
            ServerWebExchange withoutAuthorization = exchange.mutate()
                    .request(request -> request.headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION)))
                    .build();
            return chain.filter(withoutAuthorization);
        }
        return chain.filter(exchange);
    }
}
```

| Code | What it does | Why |
|---|---|---|
| `implements GlobalFilter` | Runs on **every** request routed by the gateway | Spring Cloud Gateway's hook for code that sees each request |
| `@Order(Ordered.HIGHEST_PRECEDENCE + 1)` | Runs very early, right after `JwtAuthenticationFilter` | The header must be gone before the request is forwarded |
| `getURI().getPath()` | The path the browser called, e.g. `/auth-service/api/v1/auth/refresh-token` | Compared with `cookieAuthenticated` |
| `exchange.mutate().request(... headers.remove(AUTHORIZATION))` | Builds a **copy** of the request without the header | Requests in WebFlux are immutable, so you change them by making a modified copy |
| `return chain.filter(...)` | Passes the (copied) request to the next filter and then to auth-service | Every GlobalFilter must call the chain or the request stops there |
| `Mono<Void>` | The reactive "I'll finish later" type | The gateway is reactive (WebFlux), so filters return `Mono` instead of blocking |

**Which alternatives, and why not:**
- Change the shared `JWTFilter` to skip `/api/v1/auth/**`: it lives in utility-service, so it would
  change every service's security.
- A route filter `RemoveRequestHeader=Authorization` on the whole auth-service route: it would also
  strip the header from future auth-service endpoints that need it, like "change password".

### Real output

The real gateway, with auth-service replaced by a stub that records what reaches it:

```
POST refresh   Authorization=Bearer expired    -> 200 | reached auth-service=true  Authorization seen there=null
POST login     Authorization=Bearer some-token -> 200 | reached auth-service=true  Authorization seen there=Bearer some-token
```

**How to read it:** on refresh the header was removed (`null` at auth-service). On login it arrived
unchanged, proving the filter touches only the two cookie endpoints.

### Check yourself

1. What would the user see on logout if `logout` were not in `openApi` and their access token had expired?
2. Why is the header removed at the gateway rather than in the shared `JWTFilter`?
3. Why does the filter use `exchange.mutate()` instead of just removing the header from the request?

---

## Stop 6: CORS in the gateway (the CSRF guard)

**Files:** `gateway/src/main/resources/application.properties` (gateway); the CORS beans were
**removed** from auth-service `ApplicationConfig` and user-service `AppConfig` (deleted).

### Two words first

- **CORS** (Cross-Origin Resource Sharing): the browser rule that decides whether a page on one
  website (`origin`, e.g. `http://localhost:3000`) may call an API on another (`http://localhost:9999`).
  The server answers with `Access-Control-Allow-*` headers.
- **CSRF** (Cross-Site Request Forgery): a bad website makes the user's browser call **our** API. The
  browser attaches **our** cookie on its own. If the server accepts it, the bad site has used the
  user's session without ever seeing the cookie.

Moving the refresh token into a cookie **creates** a CSRF risk that did not exist before, because the
browser now sends a credential automatically.

### The settings

```properties
gateway.allowed-origins=http://localhost:3000,http://localhost:5173

spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-origins=${gateway.allowed-origins}
spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowed-headers=Authorization,Content-Type
spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allow-credentials=true
spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].exposed-headers=X-Trace-Id
spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].max-age=3600
```

| Line | What it does | Why |
|---|---|---|
| `gateway.allowed-origins` | Our own property: the frontend websites we trust | One list, overridden per environment |
| `[/**]` | Applies to every path | One rule for the whole gateway |
| `allowed-origins=${gateway.allowed-origins}` | Only these websites get CORS permission | **Never `*`**: browsers refuse `*` together with credentials |
| `allowed-methods` | HTTP methods the frontend may use | |
| `allowed-headers=Authorization,Content-Type` | Request headers the frontend may send | If the frontend adds another header, add it here, or the preflight fails |
| `allow-credentials=true` | Lets the browser send and receive **cookies** on cross-origin calls | Required for the refresh cookie |
| `exposed-headers=X-Trace-Id` | Lets frontend JavaScript **read** this response header | So the frontend can show the trace id in error messages |
| `max-age=3600` | The browser may cache the preflight answer for 1 hour | Fewer `OPTIONS` requests |

**Why only in the gateway:** the browser only talks to the gateway. The old service beans used `*`,
which can't work with cookies, and two layers adding CORS headers produce duplicate headers that
browsers reject.

**How it blocks CSRF:** Spring's CORS processor does not only add headers. For a request whose
`Origin` is **not** in the list, it **rejects it with 403 before routing**. So a bad site's request
never reaches auth-service. Requests from the gateway's own origin (Swagger UI) are not CORS requests
at all, so they pass. Together with `SameSite=Strict` on the cookie, that is two independent layers.

### Real output

The real gateway:

```
OPTIONS refresh  Origin=http://localhost:3000 (preflight) -> 200 | ACAO=http://localhost:3000 ACAC=true ACAM=GET,POST,PUT,PATCH,DELETE,OPTIONS ACEH=X-Trace-Id | reached auth-service=false
OPTIONS refresh  Origin=https://evil.example  (preflight) -> 403 | ACAO=null ACAC=null                                                           | reached auth-service=false
POST    refresh  Origin=https://evil.example              -> 403 | ACAO=null                                                                     | reached auth-service=false
POST    refresh  Origin=http://localhost:3000             -> 200 | ACAO=http://localhost:3000 ACAC=true ACEH=X-Trace-Id                           | reached auth-service=true
POST    logout   Origin=http://localhost:<gateway port> (same origin) -> 200 | ACAO=null                                                         | reached auth-service=true
```

(`ACAO` = Access-Control-Allow-Origin, `ACAC` = …-Allow-Credentials, `ACAM` = …-Allow-Methods,
`ACEH` = …-Expose-Headers.)

**How to read it:**
- Line 1: the browser's "may I?" question (preflight) from our frontend gets a yes, with credentials.
- Lines 2 and 3: the bad site gets 403 and **never reaches auth-service**, preflight or real POST.
- Line 4: our frontend's real POST passes and can read `X-Trace-Id`.
- Line 5: same-origin (Swagger UI on the gateway) passes without any CORS headers, as it should.

### Check yourself

1. Why does putting the refresh token in a cookie create a CSRF risk that the body-based version did not have?
2. Why can't `allowed-origins` be `*` here?
3. Which two independent layers stop a bad website from using the cookie?

---

## Stop 7: `ExceptionHandlerAutoConfig` (utility-service)

**Files:** `utility-service/.../ExceptionHandlerAutoConfig.java`,
`utility-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### The problem it solves (found while reviewing this work)

`GlobalExceptionHandler` lives in utility-service. Each service only scans its own package, so the
handler was **never loaded**. A `BusinessException` (wrong refresh token, user-service down, and so
on) fell through to Spring's `/error` page, which Spring Security blocks, so every business error came
back as the same generic 401.

Real output **before** the fix:

```
POST /api/v1/auth/refresh-token (no cookie) -> 401 {"status":401,"errorCode":"Unauthorized","message":"Authentication required","path":"/error"}
```

and **after**:

```
POST /api/v1/auth/refresh-token (no cookie) -> 401 {"status":401,"errorCode":"AUTH_301","message":"Refresh token is invalid.","path":"/api/v1/auth/refresh-token"}
```

The path `/error` and the generic message were the clue.

### The code

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(GlobalExceptionHandler.class)
public class ExceptionHandlerAutoConfig {
}
```

and one new line in `AutoConfiguration.imports`:

```
org.ecommerce.utility.SecurityAutoConfig
org.ecommerce.utility.ExceptionHandlerAutoConfig
```

| Code | What it does | Why |
|---|---|---|
| `@AutoConfiguration` | Marks a class Spring Boot loads automatically from a library | The services don't need to know it exists |
| The `AutoConfiguration.imports` file | The list Spring Boot reads to find a library's auto-configurations | Without the line, the class is never loaded. `SecurityAutoConfig` was already registered this way. |
| `@ConditionalOnWebApplication(type = SERVLET)` | Only in servlet (Spring MVC) apps | `GlobalExceptionHandler` uses `HttpServletRequest`; it must not load in a non-web app |
| `@Import(GlobalExceptionHandler.class)` | Registers the existing handler as a bean | The handler itself is unchanged |

**Effect:** every service that depends on utility-service (auth, user, product) now loads the handler
and returns its real error codes. This was checked with a test in auth-service; user-service's existing
test passes with the handler loaded, and product-service was not run (its build has a separate lombok
version problem). After changing utility-service, run `mvn install` in it, because the services build
against the installed jar. The Jenkins pipelines already do this.

### Check yourself

1. Why was `GlobalExceptionHandler` not loaded, even though it has `@RestControllerAdvice`?
2. What clue in the old error response showed the request had gone to `/error`?
3. Why is the fix in utility-service and not in auth-service?

---

## Stop 8: The tests

Each piece has a test that runs the real code path. Run a module's tests with `mvn test` inside it.

| Test | Module | Kind | What it proves |
|---|---|---|---|
| `RefreshCookieFactoryTest` (6) | auth-service | Unit | Every cookie attribute; `clear()` matches `build()`; reading; both startup guards; insecure local cookie allowed without the prefix |
| `AuthenticationControllerCookieTest` (5) | auth-service | MockMvc, real controller + factory, mocked service | Login cookie and body; refresh rotation; no cookie → 401; logout without access token clears the cookie |
| `AuthenticationServiceImplLogoutTest` (5) | auth-service | Unit with Mockito | Logout reads the user from the refresh token; no token, unknown token and expired token do not throw; refresh with no token is rejected before any lookup |
| `AuthCookieEndpointsIntegrationTest` (3) | auth-service | Full application over HTTP (needs the local DB) | Real error codes through Spring Security and `GlobalExceptionHandler`; logout clears the cookie |
| `CorsAndOriginIntegrationTest` (7) | gateway | Full gateway, auth-service replaced by a stub | Preflight allowed and rejected; foreign POST never reaches auth-service; same origin passes; `X-Trace-Id` exposed; `Authorization` removed on refresh/logout but kept on login |

**Unit vs integration:** a unit test runs one class with its dependencies faked (fast, no Spring). An
integration test starts the real application and calls it over HTTP (slower, but it catches wiring
problems). Stop 7's bug was invisible to the unit tests and only the integration test caught it.

---

## Answers to "Check yourself"

<details>
<summary>Stop 1</summary>

1. The browser only sees the gateway URL, which includes `/auth-service`. A cookie path must match the
   URL the browser calls, not the path inside auth-service.
2. `auth.refresh-cookie.same-site=None` in the prod profile (and keep `secure=true`; the factory refuses
   `None` without it). Different sites mean `Strict` would never send the cookie.
3. No. Every field has a `@DefaultValue`, and the defaults are the safe values.
4. It already exists as `jwt.refresh-expiration`. Keeping one value means the JWT, the DB row and the
   cookie always expire together.
</details>

<details>
<summary>Stop 2</summary>

1. So both always have the same name, path and flags. The browser only deletes a cookie when these match.
2. The response cookie with the same name and path, an empty value and `Max-Age=0`.
3. A wrong combination doesn't fail loudly in the browser: the cookie is just silently dropped. Failing
   at startup makes the mistake visible immediately.
</details>

<details>
<summary>Stop 3</summary>

1. In the controller, in `withRefreshCookie`: refresh token into `Set-Cookie`, access token into the body.
2. The browser must drop the cookie in every case, otherwise a dead cookie keeps being sent.
3. A browser or proxy could store the response containing the access token and show it again later.
</details>

<details>
<summary>Stop 4</summary>

1. The access token is usually expired at logout time, and reading it then fails. The refresh token is
   the session's real credential and carries `userAccountId`.
2. Logging out is about ending the session. If the token is already invalid, the session is already
   over, so there is nothing to fail.
3. The JWT would expire after one day but the DB row after seven, so the two checks would disagree.
</details>

<details>
<summary>Stop 5</summary>

1. A 401 "Authentication required" from the gateway, and the logout would never reach auth-service.
2. `JWTFilter` is shared by every service. Changing it would change security everywhere. The gateway
   filter only affects the two cookie endpoints.
3. WebFlux requests are immutable. `mutate()` builds a modified copy that is passed down the chain.
</details>

<details>
<summary>Stop 6</summary>

1. The browser attaches cookies automatically, even to requests started by another website. A token in
   the body is only sent when our own JavaScript adds it.
2. Browsers refuse `Access-Control-Allow-Origin: *` together with `Allow-Credentials: true`, and cookies
   need credentials.
3. `SameSite=Strict` on the cookie (the browser doesn't send it), and the gateway CORS config (the
   gateway rejects foreign origins with 403).
</details>

<details>
<summary>Stop 7</summary>

1. `@RestControllerAdvice` only works if the class is loaded as a bean. The services scan only their
   own packages, and nothing imported the handler.
2. `"path":"/error"` and the generic "Authentication required" message.
3. The same problem affected every service that uses utility-service. Fixing it once in the library
   fixes it for all of them.
</details>
