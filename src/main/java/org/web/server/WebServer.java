package org.web.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.sun.net.httpserver.HttpServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebServer {

	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * Creates a webserver on port 80
     */
	public WebServer() throws IOException {
		LOGGER.info("Started HTTP server");

		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 1);

		// Add all files in the resource directory to the webserver
		File resourceDirectory = new File("src/main/resources");
		if (!resourceDirectory.exists()) {
			LOGGER.error("Folder 'src/main/resources' does not exist!");
			System.exit(0);
		}
		int resourceCount = addResources(server, resourceDirectory, resourceDirectory);

        LOGGER.info("Finished registering {} resources", resourceCount);

		// Reroute http://example.com/ to http://example.com/index.html
		server.createContext("/", new PageHandler("/index.html"));

		LOGGER.info("Rerouted root page '/' to '/index.html'");

		//server.setExecutor(null);
		server.start();

		LOGGER.info("HTTP server successfully started");
	}

	private static int addResources(HttpServer server, File resourceDirectory, File searchDirectory) throws IOException {
		int resourceCount = 0;
		for (File resource : Objects.requireNonNull(searchDirectory.listFiles())) {
			if (resource.isDirectory()) {
				resourceCount += addResources(server, resourceDirectory, resource);
				continue;
			}

			addResource(server, resourceDirectory, resource);
			resourceCount++;
		}
		return resourceCount;
	}

	private static void addResource(HttpServer server, File resourceDirectory, File resource) throws IOException {
		String relativePath = relativePath(resourceDirectory, resource);
		server.createContext("/" + relativePath, new PageHandler(relativePath));
		LOGGER.info("Registered: {}", relativePath);
	}

	private static String relativePath(File source, File target) {
		Path sourceFile = Paths.get(source.toURI());
		Path targetFile = Paths.get(target.toURI());
		Path relativePath = sourceFile.relativize(targetFile);
		return relativePath.toString().replace('\\', '/');
	}
}