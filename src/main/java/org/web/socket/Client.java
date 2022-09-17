package org.web.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class Client {

	public final UUID id;
	private final Socket socket;

	protected Client(Socket socket, WebSocket host) {
		this.id = UUID.randomUUID();
		this.socket = socket;

		Thread clientThread = new Thread(() -> {
			//System.out.println("\nClient connected with UUID: " + id + "\n");
			
			try {
				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();
				Scanner s = new Scanner(in, "UTF-8");
				
				// Handshake
				try {
					String data = s.useDelimiter("\\r\\n\\r\\n").next();
					Matcher get = Pattern.compile("^GET").matcher(data);
					if (get.find()) {
						Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
						match.find();
						byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n" + "Connection: Upgrade\r\n"
								+ "Upgrade: websocket\r\n" + "Sec-WebSocket-Accept: "
								+ Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(
										(match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
								+ "\r\n\r\n").getBytes("UTF-8");
						out.write(response, 0, response.length);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				host.triggerEvent(new JSONObject("{'event': 'connect'}"), this);
				
				// Read
				while (!socket.isClosed()) {
					byte[] first = new byte[1];
					in.read(first);
					
					if ((first[0] & 0x0F) != 0x01) // If the last 4 bits of first[0] is not 0001 (text opcode)
						break;
					
					//System.out.println("Meta: " + (toUnsignedByte(first[0]))); // Should be 129 (or binary 1000 0001)
					
					byte[] length = new byte[1];
					in.read(length);
					
					//System.out.println("Length: " + (toUnsignedByte(length[0]) - 128)); // Should be between 0 and 125
					
					byte[] key = new byte[4];
					in.read(key);
					
					byte[] encodedMessage = new byte[toUnsignedByte(length[0]) - 128]; // We also have to subtract 128 to get the length
					in.read(encodedMessage);
					
					byte[] decodedMessage = new byte[encodedMessage.length];
					for (int i = 0; i < encodedMessage.length; i++) {
						decodedMessage[i] = (byte) (encodedMessage[i] ^ key[i & 0x3]);
					}
					host.triggerEvent(new JSONObject(new String(decodedMessage)), this);
					//System.out.println("Message From " + id + ": " + new String(decodedMessage));
				}
				s.close();
				host.triggerEvent(new JSONObject("{'event': 'disconnect'}"), this);
			} catch (IOException e) {
				e.printStackTrace();
			}			
		});
		clientThread.start();

	}

	public void emit(String event, JSONObject data) {
		JSONObject packet = new JSONObject();
		packet.put("event", event);
		packet.put("data", data);
		String packetStr = packet.toString();
		try {
			byte[] header = new byte[2];
			header[0] = (byte) -127; // 1000 0001
			header[1] = (byte) packetStr.length();
			socket.getOutputStream().write(header);
			socket.getOutputStream().write(packetStr.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static int toUnsignedByte(byte b) {
    	if (b >= 0) return b;
    	return b + 256;
    }
}