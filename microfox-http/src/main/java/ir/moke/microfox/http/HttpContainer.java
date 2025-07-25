package ir.moke.microfox.http;

import ir.moke.microfox.MicrofoxEnvironment;
import ir.moke.microfox.exception.MicrofoxException;
import ir.moke.microfox.http.filter.BaseFilter;
import ir.moke.microfox.http.servlet.BaseServlet;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import static jakarta.servlet.DispatcherType.*;

public class HttpContainer {
    private static final Logger logger = LoggerFactory.getLogger(HttpContainer.class);
    private static final String contextPath = "/";
    private static final Server server = new Server();
    private static final ServletContextHandler context = new ServletContextHandler();

    public static void start() {
        try {
            initializeHttpsConnector();
            initializeHttpConnector();
            initializeHandlers();

            // Start the server
            server.start();
            logger.info("Http server started");
            server.join();
        } catch (Exception e) {
            throw new MicrofoxException(e);
        }
    }

    private static void initializeHttpConnector() {
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(MicrofoxEnvironment.getEnv("MICROFOX_HTTP_HOST"));
        connector.setPort(Integer.parseInt(MicrofoxEnvironment.getEnv("MICROFOX_HTTP_PORT")));
        server.addConnector(connector);
    }

    private static void initializeHttpsConnector() {
        if (MicrofoxEnvironment.getEnv("MICROFOX_KEYSTORE_PATH") != null) {
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(MicrofoxEnvironment.getEnv("MICROFOX_KEYSTORE_PATH"));
            sslContextFactory.setKeyStorePassword(MicrofoxEnvironment.getEnv("MICROFOX_KEYSTORE_PASSWORD"));
            sslContextFactory.setCertAlias(MicrofoxEnvironment.getEnv("MICROFOX_KEYSTORE_ALIAS_NAME"));

            HttpConfiguration httpsConfig = new HttpConfiguration();
            httpsConfig.setSecureScheme("https");
            httpsConfig.setSecurePort(Integer.parseInt(MicrofoxEnvironment.getEnv("MICROFOX_HTTPS_PORT")));
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            ServerConnector httpsConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "HTTP/1.1"),
                    new HttpConnectionFactory(httpsConfig));
            httpsConnector.setPort(Integer.parseInt(MicrofoxEnvironment.getEnv("MICROFOX_HTTPS_PORT")));
            httpsConnector.setHost(MicrofoxEnvironment.getEnv("MICROFOX_HTTP_HOST"));
            server.addConnector(httpsConnector);
        }
    }

    private static void initializeHandlers() {
        context.setContextPath(contextPath);

        /* Rest Apis */
        context.addFilter(BaseFilter.class, "/*", EnumSet.of(FORWARD, ASYNC, REQUEST, INCLUDE, ERROR));
        context.addServlet(BaseServlet.class, "/*");
        server.setHandler(context);
    }

    public static boolean isStarted() {
        return server.isStarted();
    }

    public static void addServlet(Class<? extends Servlet> servletClass, String... paths) {
        for (String path : paths) {
            context.addServlet(servletClass, path);
        }
    }

    public static void addFilter(Class<? extends Filter> filterClass, String... paths) {
        for (String path : paths) {
            context.addFilter(filterClass, path, EnumSet.of(FORWARD, ASYNC, REQUEST, INCLUDE, ERROR));
        }
    }
}
