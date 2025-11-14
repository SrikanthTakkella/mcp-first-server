package com.aicompany.map.mcp;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.tool.ToolCallbackProvider;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Component
public class McpServerHandler {
    
    private final ToolCallbackProvider toolProvider;
    private final ObjectMapper objectMapper;
    private final McpAuthenticationService authService;
    
    public McpServerHandler(ToolCallbackProvider toolProvider, ObjectMapper objectMapper, McpAuthenticationService authService) {
        this.toolProvider = toolProvider;
        this.objectMapper = objectMapper;
        this.authService = authService;
    }

    public RouterFunction<ServerResponse> mcpRoutes() {
        return route(POST("/mcp"), this::handleMcpRequest);
    }

    private Mono<ServerResponse> handleMcpRequest(ServerRequest request) {
        return authService.authenticate(request)
                .cast(Boolean.class)
                .flatMap((Boolean isAuthenticated) -> {
                    if (!isAuthenticated) {
                        return ServerResponse.status(401)
                                .header("Content-Type", "application/json")
                                .bodyValue(createErrorResponse(-1, -32001, "Unauthorized", "Authentication required"));
                    }
                    
                    return request.bodyToMono(JsonNode.class)
                            .flatMap(this::processMessage)
                            .flatMap(response -> ServerResponse.ok()
                                    .header("Content-Type", "application/json")
                                    .bodyValue(response));
                })
                .onErrorResume(error -> 
                    ServerResponse.badRequest()
                            .header("Content-Type", "application/json")
                            .bodyValue(createErrorResponse(-1, -32603, "Internal error", error.getMessage()))
                );
    }

    private Mono<Object> processMessage(JsonNode message) {
        String method = message.path("method").asText();
        JsonNode params = message.path("params");
        int id = message.path("id").asInt();

        return switch (method) {
            case "initialize" -> handleInitialize(params, id);
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolCall(params, id);
            case "ping" -> handlePing(id);
            default -> Mono.just(createErrorResponse(id, -32601, "Method not found", method));
        };
    }

    private Mono<Object> handleInitialize(JsonNode params, int id) {
        var response = new McpResponse();
        response.jsonrpc = "2.0";
        response.id = id;
        
        // Enhanced server capabilities with authentication info
        var capabilities = new ServerCapabilities();
        capabilities.experimental = java.util.Map.of(
            "authentication", java.util.Map.of(
                "supported", true,
                "methods", java.util.List.of("api-key", "bearer", "basic")
            )
        );
        
        response.result = new InitializeResult(
            "2024-11-05", // protocolVersion
            capabilities,
            new ServerInfo("Weather MCP Server with Auth", "1.0.0")
        );
        return Mono.just(response);
    }

    private Mono<Object> handleToolsList(int id) {
        var tools = java.util.List.of(
            new Tool("getWeatherInfo", 
                    "Get temperature forecast for a city for the next days in celsius",
                    new ToolInputSchema(
                        java.util.Map.of(
                            "name", new SchemaProperty("string", "City name"),
                            "countrycode", new SchemaProperty("string", "Country code (e.g., CA, US, GB)")
                        ),
                        java.util.List.of("name", "countrycode")
                    )
            )
        );
        
        var response = new McpResponse();
        response.jsonrpc = "2.0";
        response.id = id;
        response.result = new ToolsListResult(tools);
        return Mono.just(response);
    }

    private Mono<Object> handleToolCall(JsonNode params, int id) {
        String toolName = params.path("name").asText();
        JsonNode arguments = params.path("arguments");
        
        if (!"getWeatherInfo".equals(toolName)) {
            return Mono.just(createErrorResponse(id, -32602, "Invalid tool name", toolName));
        }

        String cityName = arguments.path("name").asText();
        String countryCode = arguments.path("countrycode").asText();

        // Use Mono.fromCallable to run the blocking tool call safely
        return Mono.fromCallable(() -> {
            // Get the weather service from tool provider and call it
            var weatherResult = callWeatherTool(cityName, countryCode);
            
            var response = new McpResponse();
            response.jsonrpc = "2.0";
            response.id = id;
            response.result = new ToolCallResult(
                java.util.List.of(new ToolContent("text", weatherResult))
            );
            return (Object) response;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private Mono<Object> handlePing(int id) {
        var response = new McpResponse();
        response.jsonrpc = "2.0";
        response.id = id;
        response.result = java.util.Map.of(); // Empty map for ping response
        return Mono.just(response);
    }

    private String callWeatherTool(String cityName, String countryCode) {
        try {
            // This is a simplified approach - in reality you'd want to get the actual tool from the provider
            var weatherService = new com.aicompany.map.service.WeatherService();
            return weatherService.getWeatherInfo(cityName, countryCode);
        } catch (Exception e) {
            return "Error calling weather tool: " + e.getMessage();
        }
    }

    private Object createErrorResponse(int id, int code, String message, String data) {
        var error = new McpError(code, message, data);
        var response = new McpResponse();
        response.jsonrpc = "2.0";
        response.id = id;
        response.error = error;
        return response;
    }

    // Response classes
    public static class McpResponse {
        public String jsonrpc;
        public int id;
        public Object result;
        public McpError error;
    }

    public static class McpError {
        public int code;
        public String message;
        public String data;
        
        public McpError(int code, String message, String data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
    }

    public static class InitializeResult {
        public String protocolVersion;
        public ServerCapabilities capabilities;
        public ServerInfo serverInfo;
        
        public InitializeResult(String protocolVersion, ServerCapabilities capabilities, ServerInfo serverInfo) {
            this.protocolVersion = protocolVersion;
            this.capabilities = capabilities;
            this.serverInfo = serverInfo;
        }
    }

    public static class ServerCapabilities {
        public ToolsCapability tools = new ToolsCapability();
        public java.util.Map<String, Object> experimental = java.util.Map.of();
        
        public static class ToolsCapability {
            public boolean listChanged = true;
        }
    }

    public static class ServerInfo {
        public String name;
        public String version;
        
        public ServerInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }

    public static class ToolsListResult {
        public java.util.List<Tool> tools;
        
        public ToolsListResult(java.util.List<Tool> tools) {
            this.tools = tools;
        }
    }

    public static class Tool {
        public String name;
        public String description;
        public ToolInputSchema inputSchema;
        
        public Tool(String name, String description, ToolInputSchema inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }
    }

    public static class ToolInputSchema {
        public String type = "object";
        public java.util.Map<String, SchemaProperty> properties;
        public java.util.List<String> required;
        
        public ToolInputSchema(java.util.Map<String, SchemaProperty> properties, java.util.List<String> required) {
            this.properties = properties;
            this.required = required;
        }
    }

    public static class SchemaProperty {
        public String type;
        public String description;
        
        public SchemaProperty(String type, String description) {
            this.type = type;
            this.description = description;
        }
    }

    public static class ToolCallResult {
        public java.util.List<ToolContent> content;
        
        public ToolCallResult(java.util.List<ToolContent> content) {
            this.content = content;
        }
    }

    public static class ToolContent {
        public String type;
        public String text;
        
        public ToolContent(String type, String text) {
            this.type = type;
            this.text = text;
        }
    }
}