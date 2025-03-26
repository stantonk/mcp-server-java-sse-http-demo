package com.github.stantonk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransport;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;


/**
 * Hello world!
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    @Configuration
    @EnableWebMvc
    public static class McpConfig {

        @Bean
        WebMvcSseServerTransportProvider webMvcSseServerTransportProvider() {
            var transportProvider = new WebMvcSseServerTransportProvider(new ObjectMapper(), "/mcp/message");
            return transportProvider;
            //TODO: hmm session is null..
//            transportProvider.setSessionFactory();
        }

        @Bean
        RouterFunction<ServerResponse> mcpRouterFunction(WebMvcSseServerTransportProvider transportProvider) {
            return transportProvider.getRouterFunction();
        }
    }

    public static void main(String[] args) {
        System.out.println("Hello World!");

        McpConfig serverConfig = new McpConfig();
        // Create a server with custom configuration
        McpSyncServer syncServer = McpServer.sync(serverConfig.webMvcSseServerTransportProvider())
                .serverInfo("my-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(true, true)     // Enable resource support
                        .tools(true)         // Enable tool support
                        .prompts(true)       // Enable prompt support
                        .logging()           // Enable logging support
                        .build())
                .build();

// Register tools, resources, and prompts

        McpServerFeatures.SyncToolSpecification syncToolSpecification = new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "weather",
                        "fetches weather from lat and long",
                        new McpSchema.JsonSchema(
                                "object",
                                Map.of("latitude", Map.of("type", "number", "minimum", -90, "maxiumum", 90),
                                        "longitude", Map.of("type", "number", "minimum", -180, "maximum", 180)),
                                List.of("latitude", "longitude"),
                                false)
                ),
                (exchange, arguments) -> {
                    String latitude = (String) arguments.get("latitude");
                    String longitude = (String) arguments.get("longitude");
                    //TODO actually implement weather getting
                    return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent("Result: " + latitude + " " + longitude)), false);
                }
        );
        syncServer.addTool(syncToolSpecification);
//        syncServer.addResource(syncResourceSpecification);
//        syncServer.addPrompt(syncPromptSpecification);

// Send logging notifications
        syncServer.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                .level(McpSchema.LoggingLevel.INFO)
                .logger("custom-logger")
                .data("Server initialized")
                .build());

        log.info("{}", syncServer.getServerInfo());

        // Create and configure the Spring application context
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(App.McpConfig.class);

        // Create the DispatcherServlet, which will route HTTP requests to your RouterFunction
        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);

        // Set up Jetty with a context handler and register the DispatcherServlet
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(dispatcherServlet), "/*");

        // Start Jetty on port 8080 (or any port you prefer)
        Server server = new Server(8080);
        server.setHandler(contextHandler);
        try {
            server.start();
        } catch (Exception e) {
            syncServer.close();
            throw new RuntimeException(e);
        }

        try {
            server.join(); // Wait for the server to exit
        } catch (InterruptedException e) {
            syncServer.close();
            throw new RuntimeException(e);
        }
    }
}
