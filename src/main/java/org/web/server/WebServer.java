package org.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebServer {

	private static final Logger LOGGER = LogManager.getLogger(WebServer.class);

	/**
	 * Creates a webserver on port 80
	 */
	public WebServer() throws IOException {
		this(80);
	}

	public WebServer(int port) throws IOException {
		LOGGER.info("Started HTTP server");

		HttpServer server = HttpServer.create(new InetSocketAddress(port), 1);

		server.createContext("/", exchange -> {
			String path = exchange.getRequestURI().getPath();

			// Serve the index.html page
			if (path.equals("/")) path = "/index.html";

			try (InputStream is = WebServer.class.getResourceAsStream("/static" + path)) {
				// 404 page not found
				if (is == null) {
					exchange.sendResponseHeaders(404, -1);
					return;
				}

				// 200 + page content
				byte[] bytes = is.readAllBytes();
				exchange.getResponseHeaders().add("Content-Type", Files.probeContentType(Path.of(path)));
				exchange.sendResponseHeaders(200, bytes.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(bytes);
				}
			}
		});
		server.start();

		LOGGER.info("HTTP server successfully started");
	}
}