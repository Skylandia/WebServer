package org.web.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

import org.web.WebLogger;

public class WebServer {

	/**
	 * Creates a webserver on port 8080
	 * 
	 * @throws IOException
	 */
	public WebServer() throws IOException {
		WebLogger.log(Level.INFO, "Started HTTP server");

		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 1);

		// Add all files in the resource directory to the webserver
		File resourceDirectory = new File("src/main/resources");
		for (File page : resourceDirectory.listFiles()) {
			String relativePath = relativePath(resourceDirectory, page);
			server.createContext("/" + relativePath, new PageHandler(relativePath));

			WebLogger.log(Level.INFO, String.format("Registered: /%s", relativePath));
		}

		// Reroute http://example.com/ to http://example.com/index.html
		server.createContext("/", new PageHandler("index.html"));

		//server.setExecutor(null);
		server.start();

		WebLogger.log(Level.INFO, "HTTP server successfully started");
	}

	private static String relativePath(File source, File target) {
		Path sourceFile = Paths.get(source.toURI());
		Path targetFile = Paths.get(target.toURI());
		Path relativePath = sourceFile.relativize(targetFile);
		return relativePath.toString();
	}
}