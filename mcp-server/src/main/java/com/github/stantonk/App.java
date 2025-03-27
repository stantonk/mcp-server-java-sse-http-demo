package com.github.stantonk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * MCP Server implementation using Jetty and HttpServletSseServerTransportProvider
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final WeatherGetter weatherGetter = new WeatherGetter();

    public static void main(String[] args) {
        System.out.println("Starting MCP Server...");

        /**
         * Note, HttpServletSseServerTransportProvider extends HttpServlet
         */
        HttpServletSseServerTransportProvider transportProvider =
            new HttpServletSseServerTransportProvider(new ObjectMapper(), "/mcp/message");

        // Create a server with custom configuration
        McpSyncServer syncServer = McpServer.sync(transportProvider)
                .serverInfo("mcp-jetty-server", "0.8.1")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(true, true)     // Enable resource support
                        .tools(true)               // Enable tool support
                        .prompts(true)             // Enable prompt support
                        .logging()                 // Enable logging support
                        .build())
                .build();

        // Register a weather tool
        McpServerFeatures.SyncToolSpecification weatherTool = new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "weather",
                        "fetches weather from lat and long",
                        new McpSchema.JsonSchema(
                                "object",
                                Map.of("latitude", Map.of("type", "number", "minimum", -90, "maximum", 90),
                                        "longitude", Map.of("type", "number", "minimum", -180, "maximum", 180)),
                                List.of("latitude", "longitude"),
                                false)
                ),
                (exchange, arguments) -> {
                    Double latitude = (Double) arguments.get("latitude");
                    Double longitude = (Double) arguments.get("longitude");
//                    String weather = String.format("The weather at %.2f, %.2f is sunny with a temperature of 25°C",
//                                                  latitude, longitude);
                    var weather = "Sorry, unable to fetch weather.";
                    try {
                        weather = weatherGetter.getForecast(latitude, longitude);
                    } catch (Exception e) {
                        log.error("Unable to retrieve weather data", e);
                        throw new RuntimeException(e);
                    }
                    return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(weather)), false);
                }
        );
        syncServer.addTool(weatherTool);

        // Send logging notifications
        syncServer.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                .level(McpSchema.LoggingLevel.INFO)
                .logger("mcp-server")
                .data("Server initialized and ready to handle connections")
                .build());

        log.info("MCP Server info: {}", syncServer.getServerInfo());

        // Set up Jetty with a context handler
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        
        // Add the MCP transport provider as a servlet
        ServletHolder servletHolder = new ServletHolder(transportProvider);
        contextHandler.addServlet(servletHolder, "/*");

        // Start Jetty on port 8080
        Server server = new Server(8080);
        server.setHandler(contextHandler);

        try {
            server.start();
            log.info("Jetty server started on port 8080");
            
            // Add a shutdown hook for clean shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    log.info("Shutting down MCP server...");
                    syncServer.close();
                    server.stop();
                } catch (Exception e) {
                    log.error("Error during shutdown", e);
                }
            }));
            
            server.join(); // Wait for the server to exit
        } catch (Exception e) {
            log.error("Error starting server", e);
            syncServer.close();
            throw new RuntimeException(e);
        }
    }
}
