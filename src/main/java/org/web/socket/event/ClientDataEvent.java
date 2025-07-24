package org.web.socket.event;

import org.json.JSONObject;
import org.web.socket.Client;

public interface ClientDataEvent {
	void run(Client client, JSONObject data);
}