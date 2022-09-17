package org.web.socket.event;

import org.web.socket.Client;

public interface ClientEvent {
	public void run(Client client);
}