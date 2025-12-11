package org.web.socket;

import java.io.*;
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
				DataInputStream in = new DataInputStream(socket.getInputStream());
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
                    return;
				}

				host.triggerEvent(new JSONObject("{'event': 'connect'}"), this);

				// Read
				while (!socket.isClosed()) {
					try { // Don't crash the program over JSON errors
						byte[] header = in.readNBytes(2);

						boolean fin =  (header[0] & 0x80) == 0x80;
						boolean rsv1 = (header[0] & 0x40) == 0x40;
						boolean rsv2 = (header[0] & 0x20) == 0x20;
						boolean rsv3 = (header[0] & 0x10) == 0x10;

						if (!fin || rsv1 || rsv2 || rsv3) {
							WebSocket.LOGGER.error("Unknown header received: fin {}, rsv1 {}, rsv2 {}, rsv3 {}", fin, rsv1, rsv2, rsv3);
							// Does this not warrant disconnection?
                            continue;
						}

						Opcode opcode = Opcode.parse(header[0] & 0x0F);
						boolean mask = (header[1] & 0x80) == 0x80;
						long payloadLength = (header[1] & 0x7F);
						if (payloadLength == 126) {
							payloadLength = in.readUnsignedShort();
						} else if (payloadLength == 127) {
                            // MSB is always 0, so we can use a signed long
							payloadLength = in.readLong();
						}

                        // Don't handle a byte array that can't fit inside a single array
                        // Triggers a disconnect event
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
							case PING -> {
                                doPong(payload);
                                host.triggerEvent(new JSONObject().put("event", "ping"), this);
                            }
                            case PONG -> host.triggerEvent(new JSONObject().put("event", "pong"), this);
							case CONNECTION_CLOSE -> {
								host.triggerEvent(new JSONObject().put("event", "disconnect"), this);
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

	private void doPong(byte[] payload) throws IOException {
		try (ByteArrayOutputStream header = new ByteArrayOutputStream()) {
            header.write(0x8A); // fin + pong
            header.write(payload.length);

            socket.getOutputStream().write(header.toByteArray());
            socket.getOutputStream().write(payload);
        }
    }

	public void emit(String event, JSONObject data) {
		JSONObject packet = new JSONObject();
		packet.put("event", event);
		packet.put("data", data);
		byte[] payload = packet.toString().getBytes(StandardCharsets.UTF_8);
		try(ByteArrayOutputStream header = new ByteArrayOutputStream()) {
			header.write(0x81); // fin + text

			if (payload.length > 65535) {
				header.write(127); // 8 bytes follow
				// Most significant bit is always zero (payload length is never negative), so signed long is large enough to fully contain
				header.writeBytes(ByteBuffer.allocate(8).putLong(payload.length).array());
			} else if (payload.length > 125) {
				header.write(126); // 2 bytes follow
				header.writeBytes(ByteBuffer.allocate(2).putShort((short) payload.length).array());
			} else {
				header.write(payload.length);
			}

			socket.getOutputStream().write(header.toByteArray());
			socket.getOutputStream().write(payload);
		} catch (IOException e) {
            WebSocket.LOGGER.error(e);
        }
	}

    // TODO documentation
    public void ping() {
        UUID uuid = UUID.randomUUID();
        try {
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            header.write(0x89); // fin + ping

            ByteBuffer bb = ByteBuffer.allocate(16);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            byte[] payload = bb.array();

            header.write(payload.length);

            socket.getOutputStream().write(header.toByteArray());
            socket.getOutputStream().write(payload);
        } catch (IOException e) {
            WebSocket.LOGGER.error(e);
        }
    }
}