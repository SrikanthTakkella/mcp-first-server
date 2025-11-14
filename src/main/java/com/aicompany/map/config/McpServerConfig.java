package com.aicompany.map.config;

import com.aicompany.map.mcp.McpServerHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class McpServerConfig {

    @Bean
    public RouterFunction<ServerResponse> mcpServerRoutes(McpServerHandler handler) {
        return handler.mcpRoutes();
    }
}