import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.web.server.WebServer;
import org.web.socket.Client;
import org.web.socket.WebSocket;

public class Main {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final HashMap<Client, String> usernames = new HashMap<>();

	public static void main(String[] args) throws IOException {
		// Start the webserver and the websocket
		new WebServer();
		LOGGER.info("test");
		registerWebsocket();
	}

	private static void registerWebsocket() {
		WebSocket ws = new WebSocket();

		// Add the listeners for the websocket
		ws.on("connect", (client) -> {
			JSONObject data = new JSONObject();
			data.put("sender", "Server");
			data.put("message", "Welcome to the chat!");
			client.emit("chat message", data);
		});
		ws.on("username", (client, data) -> {
			usernames.put(client, data.getString("name"));
			System.out.println(usernames.get(client) + " joined");
		});
		ws.on("chat message", (client, data) -> {
			data.put("sender", usernames.get(client));
			ws.broadcast("chat message", data);
			System.out.println(usernames.get(client) + ": " + data.getString("message"));
		});
		ws.on("disconnect", (client) -> {
			System.out.println(usernames.get(client) + " left");
		});
	}
}