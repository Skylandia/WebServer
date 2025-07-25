package org.web.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
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

	enum Opcode {
		CONTINUATION(0x0),
		TEXT(0x1),
		BINARY(0x2),
		CONNECTION_CLOSE(0x8),
		PING(0x9),
		PONG(0xA);

		public final int opcode;

		Opcode(int opcode) {
			this.opcode = opcode;
		}

		public static Opcode parse(int value) {
			for (Opcode opcode : Opcode.values()) {
				if (value == opcode.opcode) {
					return opcode;
				}
			}
			return null;
		}
	}

	protected Client(Socket socket, WebSocket host) {
		this.id = UUID.randomUUID();
		this.socket = socket;

		new Thread(() -> {
			try (
				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();
		 		Scanner s = new Scanner(in, StandardCharsets.UTF_8)
			) {
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
					WebSocket.LOGGER.error(e);
				}

				host.triggerEvent(new JSONObject("{'event': 'connect'}"), this);

				// Read
				while (!socket.isClosed()) {
					try { // Ignore JSON errors and don't crash the program
						byte[] header = in.readNBytes(2);

						boolean fin = (header[0] & 0x80) == 0x80;
						boolean rsv1 = (header[0] & 0x40) == 0x40;
						boolean rsv2 = (header[0] & 0x20) == 0x20;
						boolean rsv3 = (header[0] & 0x10) == 0x10;

						if (!fin || rsv1 || rsv2 || rsv3) {
							WebSocket.LOGGER.error("Unknown header received: fin {}, rsv1 {}, rsv2 {}, rsv3 {}", fin, rsv1, rsv2, rsv3);
							continue;
						}

						Opcode opcode = Opcode.parse(header[0] & 0x0F);
						boolean mask = (header[1] & 0x80) == 0x80;
						long payloadLength = (header[1] & 0x7F);
						if (payloadLength == 126) {
							payloadLength = ByteBuffer.wrap(in.readNBytes(2)).getShort();
						} else if (payloadLength == 127) {
							payloadLength = ByteBuffer.wrap(in.readNBytes(8)).getLong();
						}

						if (payloadLength > Integer.MAX_VALUE)
							break;

						byte[] payload;
						if (mask) {
							byte[] maskingKey = in.readNBytes(4);
							payload = in.readNBytes((int) payloadLength);
							for (int i = 0; i < payload.length; i++) {
								payload[i] = (byte) (payload[i] ^ maskingKey[i & 0x3]);
							}
						} else {
							payload = in.readNBytes((int) payloadLength);
						}

						switch (opcode) {
							case TEXT -> host.triggerEvent(new JSONObject(new String(payload)), this);
							case PING -> doPong(payload);
							case CONNECTION_CLOSE -> {
								host.triggerEvent(new JSONObject("{'event': 'disconnect'}"), this);
								return;
							}
                            case null -> WebSocket.LOGGER.error("Unknown opcode");
							default -> WebSocket.LOGGER.error("Unhandled opcode");
						}
					} catch (JSONException e) {
						WebSocket.LOGGER.error(e);
					}
				}
			} catch (IOException e) {
				WebSocket.LOGGER.error(e);
			}
			// This can be triggered before a username is selected
			host.triggerEvent(new JSONObject("{'event': 'disconnect'}"), this);
		}).start();
	}

	private void doPong(byte[] payload) {
		try (ByteArrayOutputStream header = new ByteArrayOutputStream()) {
			header.write(0x8A); // fin + pong
			header.write(payload.length);

			socket.getOutputStream().write(header.toByteArray());
			socket.getOutputStream().write(payload);
		} catch (IOException e) {
			WebSocket.LOGGER.error(e);
		}
	}

	public void emit(String event, JSONObject data) {
		JSONObject packet = new JSONObject();
		packet.put("event", event);
		packet.put("data", data);
		byte[] packetBytes = packet.toString().getBytes(StandardCharsets.UTF_8);
		try {
			ByteArrayOutputStream header = new ByteArrayOutputStream();

			header.write(0x81); // fin + text

			if (packetBytes.length > 65535) {
				header.write(127); // 8 bytes follow
				// Most significant bit is always zero, so signed long is large enough to fully contain
				header.writeBytes(ByteBuffer.allocate(8).putLong(packetBytes.length).array());
			} else if (packetBytes.length > 125) {
				header.write(126); // 2 bytes follow
				header.writeBytes(ByteBuffer.allocate(2).putShort((short) packetBytes.length).array());
			} else {
				header.write(packetBytes.length);
			}

			socket.getOutputStream().write(header.toByteArray());
			socket.getOutputStream().write(packetBytes);
		} catch (IOException e) {
			WebSocket.LOGGER.error(e);
		}
	}
}