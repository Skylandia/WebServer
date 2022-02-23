package org.web.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PageHandler implements HttpHandler {
	
	private byte[] pageBytes;
	
	public PageHandler(String pageName) throws IOException {
		File page = new File("src/main/resources/" + pageName);
		FileInputStream pageInputStream = new FileInputStream(page);
		pageBytes = pageInputStream.readAllBytes();
		pageInputStream.close();
	}
	
	@Override
	public void handle(HttpExchange t) throws IOException {
		t.sendResponseHeaders(200, pageBytes.length);
		OutputStream os = t.getResponseBody();
		os.write(pageBytes);
		os.close();
	}
}