# 🔐 MCP Authentication Guide

## Overview

Model Context Protocol (MCP) authentication varies by transport and deployment scenario. This guide covers all major authentication methods and how to implement them.

## 🔧 Authentication Methods Implemented

### 1. **HTTP API Key Authentication**
```http
X-API-Key: mcp-weather-api-key-12345
```
**Use Case**: Simple service-to-service authentication
**Security Level**: Medium (ensure HTTPS in production)

### 2. **HTTP Bearer Token Authentication**  
```http
Authorization: Bearer bearer-token-abcdef123456
```
**Use Case**: OAuth2, JWT tokens, session tokens
**Security Level**: High (with proper token validation)

### 3. **HTTP Basic Authentication**
```http
Authorization: Basic bWNwOndlYXRoZXI=
```
**Use Case**: Simple username/password authentication
**Security Level**: Low-Medium (base64 is not encryption!)

## 🚀 Usage Examples

### Client Authentication Examples

#### **API Key Authentication**
```javascript
fetch('http://localhost:8080/mcp', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': 'mcp-weather-api-key-12345'
  },
  body: JSON.stringify({
    jsonrpc: '2.0',
    id: 1,
    method: 'tools/list'
  })
});
```

#### **Bearer Token Authentication**
```javascript
fetch('http://localhost:8080/mcp', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer bearer-token-abcdef123456'
  },
  body: JSON.stringify({
    jsonrpc: '2.0',
    id: 1,
    method: 'tools/call',
    params: {
      name: 'getWeatherInfo',
      arguments: { name: 'London', countrycode: 'GB' }
    }
  })
});
```

#### **Basic Authentication**
```bash
# Using curl
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic bWNwOndlYXRoZXI=" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {"tools": {}},
      "clientInfo": {"name": "test-client", "version": "1.0.0"}
    }
  }'
```

## 🏗️ Authentication Flow

```
┌─────────────┐    1. Request with Auth    ┌─────────────────┐
│ MCP Client  │ ─────────────────────────► │   MCP Server    │
│             │                            │                 │
│             │    2. Validate Auth        │ ┌─────────────┐ │
│             │ ◄────────────────────────── │ │ Auth Service│ │
│             │                            │ └─────────────┘ │
│             │    3. Process Request      │                 │
│             │ ◄────────────────────────── │   Tool Handler  │
│             │                            │                 │
│             │    4. Return Response      │                 │
│             │ ◄────────────────────────── │                 │
└─────────────┘                            └─────────────────┘
```

## 🔒 Authentication Responses

### **Successful Authentication**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocol_version": "2024-11-05",
    "capabilities": {
      "tools": {"list_changed": true},
      "experimental": {
        "authentication": {
          "supported": true,
          "methods": ["api-key", "bearer", "basic"]
        }
      }
    },
    "server_info": {
      "name": "Weather MCP Server with Auth",
      "version": "1.0.0"
    }
  }
}
```

### **Authentication Failure**
```json
{
  "jsonrpc": "2.0", 
  "id": 1,
  "error": {
    "code": -32001,
    "message": "Unauthorized",
    "data": "Authentication required"
  }
}
```
**HTTP Status**: `401 Unauthorized`

## 🚦 Testing Authentication

### Run the Test Suite
```bash
cd "/Users/srikanthtakkellapati/Downloads/map 2"
node test-mcp-auth.js
```

### Manual Testing

#### **Test Valid API Key**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mcp-weather-api-key-12345" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

#### **Test Invalid Authentication**  
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "X-API-Key: invalid-key" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

## 🔐 Production Security Configuration

### 1. **Environment Variables**
```bash
# Set in production environment
export MCP_API_KEY="your-secure-api-key-here"
export MCP_JWT_SECRET="your-jwt-secret-here"
export MCP_AUTH_ENABLED="true"
```

### 2. **Update Authentication Service**
```java
@Component  
public class McpAuthenticationService {
    
    @Value("${mcp.auth.enabled:false}")
    private boolean authEnabled;
    
    @Value("${mcp.auth.api-key}")
    private String validApiKey;
    
    public Mono<Boolean> authenticate(ServerRequest request) {
        if (!authEnabled) {
            return Mono.just(true); // Dev mode
        }
        
        // Production authentication logic
        String apiKey = request.headers().firstHeader("X-API-Key");
        return Mono.just(validApiKey.equals(apiKey));
    }
}
```

### 3. **Security Best Practices**

#### **API Keys**
- Generate cryptographically secure random keys (32+ characters)
- Store in environment variables, not code
- Rotate keys regularly
- Use different keys per client/environment

#### **Bearer Tokens**
- Implement JWT with proper expiration
- Validate signatures and claims
- Use HTTPS only
- Implement token refresh mechanism

#### **Basic Auth**
- Hash passwords with bcrypt/scrypt
- Use HTTPS only (base64 is not secure)
- Implement account lockout after failed attempts
- Consider deprecating in favor of token-based auth

## 🔄 Advanced Authentication

### **JWT Token Validation**
```java
public Mono<Boolean> validateJwtToken(String token) {
    return Mono.fromCallable(() -> {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);
            
            // Check expiration, issuer, audience, etc.
            return jwt.getExpiresAt().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }).subscribeOn(Schedulers.boundedElastic());
}
```

### **Rate Limiting**
```java
@Component
public class RateLimitingService {
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    
    public boolean isAllowed(String clientId) {
        AtomicInteger count = requestCounts.computeIfAbsent(clientId, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= 100; // 100 requests per minute
    }
}
```

## 🌐 Transport-Specific Authentication

### **HTTP Transport** (Current Implementation)
- Header-based authentication (API keys, Bearer tokens, Basic auth)
- Works with web clients, curl, Postman
- Compatible with OAuth2/OpenID Connect

### **Stdio Transport** (Not implemented)
```json
{
  "jsonrpc": "2.0",
  "method": "initialize", 
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {"tools": {}},
    "clientInfo": {"name": "client", "version": "1.0"},
    "authentication": {
      "type": "certificate",
      "certificate": "base64-encoded-cert"
    }
  }
}
```

### **WebSocket Transport** (Not implemented)
- Connection-level authentication during handshake
- Session-based authentication after connection
- Token refresh over existing connection

## 📊 Authentication Status

| Method | Status | Security Level | Use Case |
|--------|--------|----------------|----------|
| API Key | ✅ Implemented | Medium | Service-to-service |
| Bearer Token | ✅ Implemented | High | OAuth2/JWT |
| Basic Auth | ✅ Implemented | Low | Simple auth |
| Client Certs | ❌ Not implemented | Very High | Enterprise |
| OAuth2 Flow | ❌ Not implemented | High | Web apps |
| Session Tokens | ❌ Not implemented | Medium | Stateful apps |

## 🚀 Quick Start Commands

```bash
# Start server
./mvnw spring-boot:run

# Test authentication
node test-mcp-auth.js

# Test with valid API key
curl -H "X-API-Key: mcp-weather-api-key-12345" \
     -X POST http://localhost:8080/mcp \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

Your MCP server now supports industry-standard authentication methods! 🔐✨