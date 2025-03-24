package com.github.stantonk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Hello world!
 */
public class App {

    @Configuration
    @EnableWebMvc
    public static class MyConfig {

        String MESSAGE_ENDPOINT = "/mcp/message";

        @Bean
        public WebMvcSseServerTransport webMvcSseServerTransport() {
            return new WebMvcSseServerTransport(new ObjectMapper(), MESSAGE_ENDPOINT);
        }

        @Bean
        public RouterFunction<ServerResponse> routerFunction(WebMvcSseServerTransport transport) {
            return transport.getRouterFunction();
        }
    }

    public static void main(String[] args) {
        System.out.println("Hello World!");

        MyConfig serverConfig = new MyConfig();
        // Create a server with custom configuration
        McpSyncServer syncServer = McpServer.sync(serverConfig.webMvcSseServerTransport())
                .serverInfo("my-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(true, true)     // Enable resource support
                        .tools(true)         // Enable tool support
                        .prompts(true)       // Enable prompt support
                        .logging()           // Enable logging support
                        .build())
                .build();

// Register tools, resources, and prompts

//        McpServerFeatures.SyncToolSpecification syncToolSpecification = new McpServerFeatures.SyncToolSpecification(
//                new McpSchema.Tool("adder", "adds two numbers together", new McpSchema.JsonSchema())
//        );
//        syncServer.addTool(syncToolSpecification);
//        syncServer.addResource(syncResourceSpecification);
//        syncServer.addPrompt(syncPromptSpecification);

// Send logging notifications
        syncServer.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                .level(McpSchema.LoggingLevel.INFO)
                .logger("custom-logger")
                .data("Server initialized")
                .build());

// Close the server when done
        syncServer.close();
    }
}
