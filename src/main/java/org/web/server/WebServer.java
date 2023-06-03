package org.web.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.net.httpserver.HttpServer;

import org.web.WebLogger;

public class WebServer {

	/**
	 * Creates a webserver on port 80
	 *
	 * @throws IOException
	 */
	public WebServer() throws IOException {
		WebLogger.log("Started HTTP server");
		WebLogger.buffer();

		HttpServer server = HttpServer.create(new InetSocketAddress(80), 1);

		// Add all files in the resource directory to the webserver
		File resourceDirectory = new File("src/main/resources");
		if (!resourceDirectory.exists()) {
			WebLogger.error("Folder 'src/main/resources' does not exist!");
			System.exit(0);
		}
		int resourceCount = addResources(server, resourceDirectory, resourceDirectory);

		WebLogger.log("Finished registering " + resourceCount + " resources");
		WebLogger.buffer();

		// Reroute http://example.com/ to http://example.com/index.html
		server.createContext("/", new PageHandler("/index.html"));

		WebLogger.log("Rerouted root page '/' to '/index.html'");
		WebLogger.buffer();

		//server.setExecutor(null);
		server.start();

		WebLogger.log("HTTP server successfully started");
		WebLogger.buffer();
	}

	private static int addResources(HttpServer server, File resourceDirectory, File searchDirectory) throws IOException {
		int resourceCount = 0;
		for (File resource : searchDirectory.listFiles()) {
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
		WebLogger.log(String.format("Registered: /%s", relativePath));
	}

	private static String relativePath(File source, File target) {
		Path sourceFile = Paths.get(source.toURI());
		Path targetFile = Paths.get(target.toURI());
		Path relativePath = sourceFile.relativize(targetFile);
		return relativePath.toString().replace('\\', '/');
	}
}