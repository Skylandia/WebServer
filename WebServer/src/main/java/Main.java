import java.io.IOException;
import java.util.HashMap;

import org.json.JSONObject;
import org.web.server.WebServer;
import org.web.socket.Client;
import org.web.socket.WebSocket;

public class Main {
	
	private static HashMap<Client, String> usernames = new HashMap<>();

	public static void main(String[] args) throws IOException {
		new WebServer();
		WebSocket ws = new WebSocket(80);
		ws.on("connect", (client) -> {
			JSONObject data = new JSONObject();
			data.put("sender", "Server");
			data.put("message", "Welcome to the chat!");
			client.emit("chat message", data);
		});
		ws.on("username", (client, data) -> {
			usernames.put(client, data.getString("name"));
		});
		ws.on("chat message", (client, data) -> {
			data.put("sender", usernames.get(client));
			ws.broadcast("chat message", data);
			System.out.println(usernames.get(client) + ": " + data.getString("message"));
		});
	}

}
