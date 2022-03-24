package org.web.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.net.httpserver.HttpServer;

public class WebServer {

	/**
	 * Creates a webserver on port 8080
	 * 
	 * @throws IOException
	 */
	public WebServer() throws IOException {
		System.out.println("Starting HTTP server");
		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		File resourceDirectory = new File("src/main/resources");

		for (File page : resourceDirectory.listFiles()) {
			String relativePath = relativePath(resourceDirectory, page);
			server.createContext("/" + relativePath, new PageHandler(relativePath));
			System.out.println("Registered: /" + relativePath);
		}

		server.createContext("/", new PageHandler("index.html"));
		server.setExecutor(null);
		server.start();
		System.out.println("Started HTTP server");
	}

	private static String relativePath(File source, File target) {
		Path sourceFile = Paths.get(source.toURI());
		Path targetFile = Paths.get(target.toURI());
		Path relativePath = sourceFile.relativize(targetFile);
		return relativePath.toString();
	}
}