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


