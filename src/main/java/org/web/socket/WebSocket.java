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

    /**
     * Creates a websocket for communication with a client browser on port 80
     */
    public WebSocket() {
        new WebSocket(80);
    }

    /**
     * Creates a websocket for communication with a client browser
     * 
     * @param port The port to host the websocket on
     */
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

    /**
     * Allows listening for ClientEvents passed over the websocket.
     * This is where a client connects or disconnects from the server via it's websocket connection.
     * 
     * @param event The event called by the client
     * @param hook The handler to pass the ClientEvent to every occurance
     */
    public void on(String event, ClientEvent hook) {
        hooks.put(event, hook);
    }

    /**
     * Allows listening for ClientDataEvents passed over the websocket.
     * This is where the client sends a JSON object accompanied with an event name to the server.
     * 
     * @param event The event called by the client
     * @param hook The handler to pass the ClientDataEvent to every occurance
     */
    public void on(String event, ClientDataEvent hook) {
        dataHooks.put(event, hook);
    }

    /**
     * Broadcasts an event accompanied with JSON data to all clients
     * 
     * @param event The event to broadcast to all clients
     * @param data The JSON data to be received by all clients
     */
    public void broadcast(String event, JSONObject data) {
        for (Client client : clients) {
            client.emit(event, data);
        }
    }
}