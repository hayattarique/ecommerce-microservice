# 🔐 Security Model

[← Back to README](../README.md)

Authentication is **stateless JWT**. `auth-service` is the only token issuer; every other service is
a verifier.

**Access token claims**

```json
{
  "sub": "1",
  "userAccountId": 42,
  "email": "jane@example.com",
  "roles": ["ADMIN"],
  "permissions": ["USER_READ", "USER_WRITE"],
  "tokenType": "ACCESS_TOKEN",
  "iat": 1753900000,
  "exp": 1753903600
}
```

Embedding roles and permissions means **authorization needs no network call** — any service decides
locally from the token alone.

**Refresh tokens deliberately omit them.** Authorization data is re-fetched from `user-service` on
every refresh, so a permission change takes effect within one access-token lifetime instead of
requiring re-login.

**Two credentials, two purposes**

| Channel | Credential | Used for |
|---|---|---|
| Client → service | `Authorization: Bearer <jwt>` | End-user requests |
| Service → service | `X-Internal-Api-Key` | The internal API, on its own filter chain that never touches JWT |

## Where each token lives

| Token | Sent to the client in | Kept by the browser in | Sent back as |
|---|---|---|---|
| Access token | the JSON body of login and refresh (`data.token`) | **memory only** (a JS variable) — never `localStorage`/`sessionStorage` | `Authorization: Bearer <jwt>` |
| Refresh token | an **HttpOnly cookie** only — never in a body | the cookie jar; JavaScript cannot read it | automatically, as the cookie |
| Fingerprint | an **HttpOnly cookie** `__Secure-fgp`, `Path=/` | the cookie jar; JavaScript cannot read it | automatically, on every call |

**Fingerprint (token sidejacking defence).** At login and refresh auth-service creates a random
256-bit fingerprint (`TokenFingerprint`). The raw value goes into the `__Secure-fgp` cookie; its
SHA-256 goes into the access token's `fgp` claim. The gateway (`TokenFingerprintVerifier`, called from
`JwtAuthenticationFilter`) hashes the cookie on every protected call and compares it with the claim:
no cookie or no match → `401 SEC-006`. A copied access token is useless without the browser's cookie.
`auth.fingerprint-cookie.name` and `gateway.token-fingerprint.cookie-name` must be equal. The access
token lives **15 minutes** (`jwt.expiration=900000`).

An XSS bug can still use the access token while the page is open, but it can no longer steal the
long-lived refresh token.

**The cookie** (`auth.refresh-cookie.*` in auth-service, built by `RefreshCookieFactory`)

| Attribute | Value | Why |
|---|---|---|
| Name | `__Secure-refresh` | The browser refuses it unless it is also `Secure` |
| `HttpOnly` | yes | Not readable by JavaScript |
| `Secure` | yes | HTTPS only |
| `SameSite` | `Strict` (`None` if frontend and API are on different sites) | Not sent on cross-site requests |
| `Path` | `/auth-service/api/v1/auth` — the **gateway** path the browser sees | Sent only to the auth endpoints |
| `Max-Age` | `jwt.refresh-expiration` — the same value as the JWT `exp` and the DB row | All three expire together |

The factory refuses to start with combinations browsers silently drop (`__Secure-` name or
`SameSite=None` without `Secure`).

**The endpoints**

| Endpoint | Body in | Credential | Response |
|---|---|---|---|
| `POST /auth/login` | email + password | — | access token in body, sets the cookie |
| `POST /auth/refresh-token` | none | the cookie | new access token in body, sets a **rotated** cookie |
| `POST /auth/logout` | none | the cookie (no access token needed) | revokes the user's refresh tokens, clears the cookie (`Max-Age=0`) |

All three send `Cache-Control: no-store`. Logout is idempotent: a missing, expired or revoked cookie
is not an error. Refresh and logout are open at the gateway, and the gateway's `CookieEndpointFilter`
drops any `Authorization` header on them — otherwise an expired access token, which most HTTP clients
attach to every call, would be rejected by the services' JWT filter exactly when a refresh is needed.

**CSRF.** A browser attaches the cookie on its own, so a foreign site could try to make it call
refresh or logout. The gateway's CORS configuration (`gateway.allowed-origins`, the only CORS
configuration in the system) rejects with 403 any request whose `Origin` is not listed, before it is
routed. Together with `SameSite` this blocks cross-site use of the cookie. Same-origin requests, such
as Swagger UI served by the gateway, are not affected.

**What the frontend must do**

- Send `credentials: 'include'` (axios: `withCredentials: true`) on login, refresh and logout.
- Keep the access token in memory. On page load, and on a `401`, call refresh once, then retry.
- Allow only **one refresh in flight**, across tabs too: the cookie rotates, so a second parallel
  refresh with the old cookie fails.


