package com.aicompany.map.mcp;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

@Component
public class McpAuthenticationService {
    
    // In production, these would come from a database or configuration
    private static final String VALID_API_KEY = "mcp-weather-api-key-12345";
    private static final String VALID_BEARER_TOKEN = "bearer-token-abcdef123456";
    private static final String VALID_BASIC_AUTH = "Basic bWNwOndlYXRoZXI="; // mcp:weather in base64
    
    public Mono<Boolean> authenticate(ServerRequest request) {
        // Check for API Key authentication
        String apiKey = request.headers().firstHeader("X-API-Key");
        if (apiKey != null) {
            return Mono.just(VALID_API_KEY.equals(apiKey));
        }
        
        // Check for Authorization header (Bearer or Basic)
        String authorization = request.headers().firstHeader("Authorization");
        if (authorization != null) {
            if (authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                return Mono.just(VALID_BEARER_TOKEN.equals(token));
            } else if (authorization.startsWith("Basic ")) {
                return Mono.just(VALID_BASIC_AUTH.equals(authorization));
            }
        }
        
        // For development, allow requests without authentication
        // In production, return false here
        return Mono.just(true); // Change to false for production
    }
    
    public Mono<String> extractClientInfo(ServerRequest request) {
        String userAgent = request.headers().firstHeader("User-Agent");
        String clientId = request.headers().firstHeader("X-Client-ID");
        
        if (clientId != null) {
            return Mono.just("Client: " + clientId);
        } else if (userAgent != null) {
            return Mono.just("User-Agent: " + userAgent);
        }
        
        return Mono.just("Unknown client");
    }
    
    public static class AuthenticationResult {
        public final boolean authenticated;
        public final String clientInfo;
        public final String errorMessage;
        
        public AuthenticationResult(boolean authenticated, String clientInfo, String errorMessage) {
            this.authenticated = authenticated;
            this.clientInfo = clientInfo;
            this.errorMessage = errorMessage;
        }
        
        public static AuthenticationResult success(String clientInfo) {
            return new AuthenticationResult(true, clientInfo, null);
        }
        
        public static AuthenticationResult failure(String errorMessage) {
            return new AuthenticationResult(false, null, errorMessage);
        }
    }
}