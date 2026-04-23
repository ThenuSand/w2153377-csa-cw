package com.westminster.smartcampus;

import com.westminster.smartcampus.config.SmartCampusApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Boots an embedded Grizzly HTTP server that hosts the JAX-RS application.
 *
 * Running: mvn exec:java  (port 8080 by default, override with -Dport=XXXX)
 */
public class App {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    public static final String BASE_HOST = "http://localhost";

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getProperty("port", "8080"));

        // When running standalone on Grizzly, the @ApplicationPath annotation
        // is not consumed automatically the way a servlet container would
        // consume it. We read the annotation value ourselves and fold it
        // into the base URI so the exact path declared on the Application
        // class is the path the server binds.
        String appPath = resolveApplicationPath();
        URI baseUri = URI.create(BASE_HOST + ":" + port + appPath);

        ResourceConfig config = ResourceConfig.forApplicationClass(SmartCampusApplication.class);
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Smart Campus API ...");
            server.shutdownNow();
        }));

        LOGGER.info("Smart Campus API started at " + baseUri);
        LOGGER.info("Press Ctrl+C to stop.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Reads the @ApplicationPath value from SmartCampusApplication and
     * normalises it so it always starts and ends with a single slash.
     */
    private static String resolveApplicationPath() {
        ApplicationPath annotation = SmartCampusApplication.class.getAnnotation(ApplicationPath.class);
        String path = annotation != null ? annotation.value() : "/";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return path;
    }
}
