package org.web.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PageHandler implements HttpHandler {

	private final String contentType;
	private final byte[] pageBytes;
	
	protected PageHandler(String pageName) throws IOException {
		Path page = Path.of("src/main/resources", pageName);
		FileInputStream pageInputStream = new FileInputStream(page.toFile());
		pageBytes = pageInputStream.readAllBytes();
		pageInputStream.close();
		contentType = Files.probeContentType(page);
	}
	
	@Override
	public void handle(HttpExchange t) throws IOException {
		t.getResponseHeaders().set("Content-Type", contentType);
		t.sendResponseHeaders(200, pageBytes.length);
		try (OutputStream os = t.getResponseBody()) {
			os.write(pageBytes);
		}
	}
}
