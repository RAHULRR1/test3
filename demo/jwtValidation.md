
Step 1: User Sends Request------------------
Angular sends HTTP request to Spring Boot with authorization header.

GET /api/user/profile

Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxNzA5MzAwMDAwfQ.signature_here





Step 2: Spring Boot Extracts bearer token from request header------------

extract authorization token from header by interceptor and validates the token using public key.


=> If Spring Boot doesn't have public key cached:
Calls auth server: GET https://auth.example.com/.well-known/jwks.json  (one time)
and store the public into cache to validate all request



# JWT Token Validation ------------------


==>Verify Signature (Cryptographic Proof)

**Purpose:** Prove token came from auth server and wasn't tampered with

**How it works:**
1. Auth server originally created signature by:
   - Taking `Header + Payload` 
   - Signing with its **private key** (only auth server has this)
   - Producing the **signature**

2. Spring Boot now verifies by:
   - Taking same `Header + Payload` from received token
   - Using auth server's **public key** (cached from JWKS endpoint)
   - Using algorithm from header (e.g., RS256)
   - Computing what signature **should be** if token is legitimate
   - Comparing computed signature with actual signature in token


Why this works: Only private key can create valid signatures; public key can only verify them. Auth server never shares private key.



 ==>Validate Claims (Business Rules)

**Purpose:** Check if token meets security requirements
Spring Boot validates these standard JWT claims:

### 1. Expiration (exp) - Is token still valid?
```
exp = 1709300000 (Unix timestamp)
Current time = 1709290000
Token expires in: 10,000 seconds (~2.7 hours)
Status: ✅ Valid
```
- **If expired** → `401 Unauthorized`

### 2. Issuer (iss) - Did our auth server issue this?
```
Token says: "https://auth.example.com"
We expect: "https://auth.example.com"
Status: ✅ Match
```
- **If mismatch** → Reject (prevents tokens from other auth servers)

### 3. Audience (aud) - Is token meant for our API?
```
Token says: "https://api.example.com"
We expect: "https://api.example.com"
Status: ✅ Match
```
- **If mismatch** → Reject (prevents tokens meant for other APIs)

**All checks must pass** ✅ → Proceed to extract user info





==>Extract User Context (Create Authentication Object)

**Purpose:** Extract user/permission info to use in your application

**What Spring Boot extracts from payload:**

```json
{
  "sub": "user123",              // Subject (user identifier)
  "scope": ["read", "write"],    // Permissions granted
  "client_id": "angular-client", // Which app requested token
  "name": "John Doe",            // Custom claim
  "email": "john@example.com",   // Custom claim
  "roles": ["USER", "ADMIN"]     // Custom claim
}
```

**Spring Boot creates authentication object:**
- **Principal:** User ID from `sub` claim
- **Authorities:** Scopes from `scope` claim (converted to `SCOPE_read`, `SCOPE_write`)
- **Attributes:** All other claims accessible in your code
- **Authenticated:** Set to `true`

**This object is now available in:**
```java
@GetMapping("/api/profile")
public Profile get(@AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();        // "user123"
    String email = jwt.getClaim("email");     // "john@example.com"
    List<String> scopes = jwt.getClaim("scope"); // ["read", "write"]
    // Use this info in your business logic
}
```

**Security context is set** → Your controllers can now access user information and Spring Security can enforce authorization rules.

