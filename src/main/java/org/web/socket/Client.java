package org.web.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
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
				Scanner s = new Scanner(in, StandardCharsets.UTF_8);

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
							(match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
							+ "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
						out.write(response, 0, response.length);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				host.triggerEvent(new JSONObject("{'event': 'connect'}"), this);

				// Read
				while (!socket.isClosed()) {
					try { // Ignore JSON errors and don't crash the program
						byte[] first = new byte[1];
						in.read(first);

						if ((first[0] & 0x0F) != 0x01) // If the last 4 bits of first[0] is not 0001 (text opcode)
							break;

						byte[] length = new byte[1];
						in.read(length);

						byte[] key = new byte[4];
						in.read(key);

						byte[] encodedMessage = new byte[toUnsignedByte(length[0]) - 128]; // We also have to subtract 128 to get the length
						in.read(encodedMessage);

						byte[] decodedMessage = new byte[encodedMessage.length];
						for (int i = 0; i < encodedMessage.length; i++) {
							decodedMessage[i] = (byte) (encodedMessage[i] ^ key[i & 0x3]);
						}
						host.triggerEvent(new JSONObject(new String(decodedMessage)), this);
					} catch (JSONException e) {
						e.printStackTrace();
					}
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
			ByteArrayOutputStream header = new ByteArrayOutputStream();

			header.write(0b10000001); // 1000 0001

			if (packetStr.length() > 125) {
				header.write(126); // 126 (2 bytes follow)
				System.out.println("length:" + packetStr.length());
				header.write(packetStr.length() >>> 8); // First byte of the integer
				header.write(packetStr.length()); // Last byte of the integer
			} else {
				header.write(packetStr.length());
			}

			socket.getOutputStream().write(header.toByteArray());
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