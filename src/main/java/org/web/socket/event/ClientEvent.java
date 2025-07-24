package org.web.socket.event;

import org.web.socket.Client;

public interface ClientEvent {
	void run(Client client);
}