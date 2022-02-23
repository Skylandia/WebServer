package org.web.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;
import org.web.socket.event.*;

public class WebSocket {
	
	private ArrayList<Client> clients = new ArrayList<>();
	
	private HashMap<String, ClientEvent> hooks = new HashMap<>();
	private HashMap<String, ClientDataEvent> dataHooks = new HashMap<>();

    public WebSocket(int port) {
        Thread serverThread = new Thread(() -> {
        	ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Started WebSocket server");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    clients.add(new Client(clientSocket, this));
                }
            } catch (IOException e) {
                System.err.println("Unable to process client request");
                e.printStackTrace();
            }
        });
        serverThread.start();
    }
    protected void triggerEvent(JSONObject packet, Client client) {
    	String event = packet.getString("event");
    	if (event.equals("connect") || event.equals("disconnect")) {
    		if (hooks.containsKey(event)) {
        		hooks.get(event).run(client);
        	}
    		if (event.equals("disconnect")) {
    			clients.remove(client);
    		}
    	} else {
    		if (dataHooks.containsKey(event)) {
    			dataHooks.get(event).run(client, new JSONObject(packet.getString("data")));
        	}
    	}
    }
    public void on(String event, ClientEvent hook) {
    	hooks.put(event, hook);
    }
    public void on(String event, ClientDataEvent hook) {
    	dataHooks.put(event, hook);
    }
    public void broadcast(String event, JSONObject data) {
    	for (Client client : clients) {
    		client.emit(event, data);
    	}
    }
}