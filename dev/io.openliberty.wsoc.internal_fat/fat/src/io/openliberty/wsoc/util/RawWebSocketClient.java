/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Raw WebSocket client for sending fragmented frames.
 * This bypasses the JSR-356 API to send raw WebSocket protocol frames.
 */
public class RawWebSocketClient {
    
    private static final Logger LOG = Logger.getLogger(RawWebSocketClient.class.getName());
    
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private boolean connected = false;
    
    /**
     * Connect to WebSocket endpoint and perform handshake
     */
    public void connect(String wsUrl) throws Exception {
        URI uri = new URI(wsUrl);
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        
        socket = new Socket(host, port);
        out = socket.getOutputStream();
        in = socket.getInputStream();
        
        // Generate WebSocket key
        byte[] keyBytes = new byte[16];
        new Random().nextBytes(keyBytes);
        String key = Base64.getEncoder().encodeToString(keyBytes);
        
        // Send HTTP upgrade request
        StringBuilder request = new StringBuilder();
        request.append("GET ").append(path).append(" HTTP/1.1\r\n");
        request.append("Host: ").append(host).append(":").append(port).append("\r\n");
        request.append("Upgrade: websocket\r\n");
        request.append("Connection: Upgrade\r\n");
        request.append("Sec-WebSocket-Key: ").append(key).append("\r\n");
        request.append("Sec-WebSocket-Version: 13\r\n");
        request.append("\r\n");
        
        out.write(request.toString().getBytes("UTF-8"));
        out.flush();
        
        // Read response
        byte[] buffer = new byte[4096];
        int bytesRead = in.read(buffer);
        String response = new String(buffer, 0, bytesRead, "UTF-8");
        
        if (!response.contains("101 Switching Protocols")) {
            throw new IOException("WebSocket handshake failed: " + response);
        }
        
        connected = true;
    }
    
    /**
     * Send a binary frame with control over FIN bit
     * @param payload The frame payload
     * @param fin true if this is the final fragment (FIN=1), false for continuation (FIN=0)
     * @param opcode The opcode (0x02 for binary, 0x00 for continuation)
     */
    public void sendBinaryFrame(byte[] payload, boolean fin, int opcode) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        ByteBuffer frame = createFrame(payload, fin, opcode);
        out.write(frame.array(), 0, frame.limit());
        out.flush();
    }
    
    /**
     * Send first fragment of a binary message (FIN=0, opcode=0x02)
     */
    public void sendFirstFragment(byte[] payload) throws IOException {
        sendBinaryFrame(payload, false, 0x02);
    }
    
    /**
     * Send continuation fragment (FIN=0, opcode=0x00)
     */
    public void sendContinuationFragment(byte[] payload) throws IOException {
        sendBinaryFrame(payload, false, 0x00);
    }
    
    /**
     * Send final fragment (FIN=1, opcode=0x00)
     */
    public void sendFinalFragment(byte[] payload) throws IOException {
        sendBinaryFrame(payload, true, 0x00);
    }
    
    /**
     * Send a TEXT frame with control over FIN bit
     * @param payload The frame payload
     * @param fin true if this is the final fragment (FIN=1), false for continuation (FIN=0)
     * @param opcode The opcode (0x01 for text, 0x00 for continuation)
     */
    public void sendTextFrame(byte[] payload, boolean fin, int opcode) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        ByteBuffer frame = createFrame(payload, fin, opcode);
        out.write(frame.array(), 0, frame.limit());
        out.flush();
    }
    
    /**
     * Send first fragment of a TEXT message (FIN=0, opcode=0x01)
     */
    public void sendFirstTextFragment(byte[] payload) throws IOException {
        sendTextFrame(payload, false, 0x01);
    }
    
    /**
     * Send a complete TEXT frame (FIN=1, opcode=0x01)
     */
    public void sendTextFrame(byte[] payload, boolean fin) throws IOException {
        sendTextFrame(payload, fin, fin ? 0x01 : 0x00);
    }
    
    private int lastCloseCode = -1;
    
    /**
     * Get the close code from the last close frame received
     * Returns -1 if no close frame received or connection still open
     */
    public int getCloseCode() {
        return lastCloseCode;
    }
    
    /**
     * Check if connection is closed
     */
    public boolean isClosed() {
        return !connected || socket == null || socket.isClosed();
    }
    
    /**
     * Create a WebSocket frame
     */
    private ByteBuffer createFrame(byte[] payload, boolean fin, int opcode) {
        int payloadLength = payload.length;
        int frameSize = 2 + 4; // header + mask key
        
        if (payloadLength > 65535) {
            frameSize += 8; // extended payload length (64-bit)
        } else if (payloadLength > 125) {
            frameSize += 2; // extended payload length (16-bit)
        }
        
        frameSize += payloadLength;
        
        ByteBuffer frame = ByteBuffer.allocate(frameSize);
        
        // Byte 0: FIN + RSV + opcode
        byte byte0 = (byte) opcode;
        if (fin) {
            byte0 |= 0x80; // Set FIN bit
        }
        frame.put(byte0);
        
        // Byte 1: MASK + payload length
        byte byte1 = (byte) 0x80; // MASK bit set (client must mask)
        
        if (payloadLength <= 125) {
            byte1 |= (byte) payloadLength;
            frame.put(byte1);
        } else if (payloadLength <= 65535) {
            byte1 |= 126;
            frame.put(byte1);
            frame.putShort((short) payloadLength);
        } else {
            byte1 |= 127;
            frame.put(byte1);
            frame.putLong(payloadLength);
        }
        
        // Masking key (4 bytes)
        byte[] maskKey = new byte[4];
        new Random().nextBytes(maskKey);
        frame.put(maskKey);
        
        // Masked payload
        for (int i = 0; i < payloadLength; i++) {
            frame.put((byte) (payload[i] ^ maskKey[i % 4]));
        }
        
        frame.flip();
        return frame;
    }
    
    /**
     * Read a frame from the server (for close frames, etc.)
     * Returns null if connection closed or no data available
     */
    public byte[] readFrame() throws IOException {
        return readFrame(false);
    }
    
    /**
     * Read a frame from the server with opcode detection
     * @param detectClose if true, marks connection as closed when close frame received
     * @return frame payload, or null if connection closed
     */
    private byte[] readFrame(boolean detectClose) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        // Read first 2 bytes
        byte[] header = new byte[2];
        int bytesRead = in.read(header);
        if (bytesRead < 2) {
            connected = false;
            return null;
        }
        
        boolean fin = (header[0] & 0x80) != 0;
        int opcode = header[0] & 0x0F;
        boolean masked = (header[1] & 0x80) != 0;
        int payloadLen = header[1] & 0x7F;
        
        // Read extended payload length if needed
        if (payloadLen == 126) {
            byte[] extLen = new byte[2];
            in.read(extLen);
            payloadLen = ((extLen[0] & 0xFF) << 8) | (extLen[1] & 0xFF);
        } else if (payloadLen == 127) {
            byte[] extLen = new byte[8];
            in.read(extLen);
            payloadLen = (int) ByteBuffer.wrap(extLen).getLong();
        }
        
        // Read payload
        byte[] payload = new byte[payloadLen];
        int totalRead = 0;
        while (totalRead < payloadLen) {
            int read = in.read(payload, totalRead, payloadLen - totalRead);
            if (read == -1) {
                connected = false;
                break;
            }
            totalRead += read;
        }
        
        // Check for close frame (opcode 0x8)
        if (detectClose && opcode == 0x8) {
            connected = false;
            // Parse close code from payload (first 2 bytes, big-endian)
            if (totalRead >= 2) {
                lastCloseCode = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
            }
            return null;
        }
        
        return payload;
    }
    
    /**
     * Wait for and detect a close frame from the server
     * @param timeoutMs maximum time to wait in milliseconds
     * @return true if close frame received, false if timeout
     */
    public boolean waitForClose(int timeoutMs) throws IOException {
        long startTime = System.currentTimeMillis();
        socket.setSoTimeout(100); // 100ms read timeout
        
        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    byte[] frame = readFrame(true);
                    if (!connected) {
                        return true; // Close frame received
                    }
                    // Got a data frame, keep waiting
                } catch (java.net.SocketTimeoutException e) {
                    // Timeout on read, check if still within overall timeout
                    if (!isConnected()) {
                        return true;
                    }
                }
            }
        } finally {
            socket.setSoTimeout(0); // Reset to blocking
        }
        
        return !connected;
    }
    
    /**
     * Read a complete message (accumulating all fragments until FIN=true)
     * This handles server responses that may be fragmented
     */
    public byte[] readCompleteMessage() throws IOException {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        java.io.ByteArrayOutputStream messageBuffer = new java.io.ByteArrayOutputStream();
        boolean finalFrame = false;
        int frameCount = 0;
        
        while (!finalFrame && frameCount < 1000) { // Safety limit
            // Read first 2 bytes
            byte[] header = new byte[2];
            int bytesRead = in.read(header);
            if (bytesRead < 2) {
                connected = false;
                // If we got some data, return it; otherwise null
                return messageBuffer.size() > 0 ? messageBuffer.toByteArray() : null;
            }
            
            finalFrame = (header[0] & 0x80) != 0;
            int opcode = header[0] & 0x0F;
            boolean masked = (header[1] & 0x80) != 0;
            int payloadLen = header[1] & 0x7F;
            
            // Check for close frame
            if (opcode == 0x8) {
                connected = false;
                return messageBuffer.size() > 0 ? messageBuffer.toByteArray() : null;
            }
            
            // Read extended payload length if needed
            if (payloadLen == 126) {
                byte[] extLen = new byte[2];
                in.read(extLen);
                payloadLen = ((extLen[0] & 0xFF) << 8) | (extLen[1] & 0xFF);
            } else if (payloadLen == 127) {
                byte[] extLen = new byte[8];
                in.read(extLen);
                payloadLen = (int) ByteBuffer.wrap(extLen).getLong();
            }
            
            // Read payload
            byte[] payload = new byte[payloadLen];
            int totalRead = 0;
            while (totalRead < payloadLen) {
                int read = in.read(payload, totalRead, payloadLen - totalRead);
                if (read == -1) {
                    connected = false;
                    break;
                }
                totalRead += read;
            }
            
            // Accumulate this frame's payload
            if (totalRead > 0) {
                messageBuffer.write(payload, 0, totalRead);
            }
            
            frameCount++;
        }
        
        return messageBuffer.toByteArray();
    }
    
    /**
     * Check if connection is still open
     */
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    /**
     * Close the connection
     */
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
        connected = false;
    }
}
