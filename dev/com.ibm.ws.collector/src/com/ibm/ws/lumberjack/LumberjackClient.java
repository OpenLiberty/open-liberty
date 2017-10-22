/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.lumberjack;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.collector.internal.TraceConstants;
import com.ibm.ws.lumberjack.LumberjackEvent.Entry;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 * Client that implements the lumberjack protocol.
 * https://github.com/elastic/logstash-forwarder/blob/master/PROTOCOL.md
 */
public class LumberjackClient {

    private static final TraceComponent tc = Tr.register(LumberjackClient.class);
    private static TraceNLS nls = TraceNLS.getTraceNLS(LumberjackClient.class, TraceConstants.MESSAGE_BUNDLE);

    protected final String UTF_8 = "UTF-8";

    /* Constants used in lumberjack protocol */
    private final static String PROTOCOL_VERSION = "1";

    private final static String DATA_FRAME_TYPE = "D";
    private final static String COMPRESS_FRAME_TYPE = "C";
    private final static String WINDOW_SIZE_FRAME_TYPE = "W";

    /* Helper class for establishing TLS connections */
    private final SSLHelper sslHelper;

    /* Amount of time, in milliseconds, that we consider an unused connection to be still reusable */
    private static final long MAX_KEEPALIVE = 10000;
    /* Time, in milliseconds since epoch, when the connection was last used */
    private long lastUsedTime;

    /* Maintains sequence number across data frames for a request */
    private int seqNumber;

    /* Used for compressing data frames */
    private final Deflater deflater;

    protected BufferedInputStream in;
    protected BufferedOutputStream out;

    public LumberjackClient(String sslConfig, SSLSupport sslSupport) throws SSLException {
        this.sslHelper = new SSLHelper(sslSupport, sslConfig);
        seqNumber = 1;
        deflater = new Deflater(6);
    }

    /**
     * Creates a socket using TLS protocol and connects it
     * to the specified host and port.
     *
     * @throws IOException
     * @throws UnknownHostException
     */
    public void connect(String hostName, int port) throws UnknownHostException, IOException {
        if (!sslHelper.isSocketAvailable()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "creating/recreating socket connection to " + hostName + ":" + port);

            SSLSocket socket = sslHelper.createSocket(hostName, port);
            in = new BufferedInputStream(socket.getInputStream());
            out = new BufferedOutputStream(socket.getOutputStream());

            // seqNumber must only be reset to 1 when connection is initialized (as in this case)
            seqNumber = 1;

            // new connection - reset lastUsedTime
            lastUsedTime = System.currentTimeMillis();
        }
    }

    /**
     * Checks whether or not this connection has been used recently enough
     * to still be considered usable
     */
    public boolean isConnectionStale() {
        if (lastUsedTime == 0)
            return false;

        long currentTime = System.currentTimeMillis();
        long timeSinceLastUse = currentTime - lastUsedTime;
        return (timeSinceLastUse > MAX_KEEPALIVE);
    }

    /**
     * Checks if socket is closed.
     *
     * @return
     */
    public boolean isSocketAvailable() {
        return sslHelper.isSocketAvailable();
    }

    /**
     * Closes the socket.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (sslHelper.isSocketAvailable()) {
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            } finally {
                sslHelper.closeSocket();
            }
        }
    }

    /**
     * Write a window frame to the wire.
     *
     * @param windowSize
     * @throws IOException
     */
    public void writeWindowFrame(int windowSize) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        output.write(PROTOCOL_VERSION.getBytes(UTF_8));//Protocol version
        output.write(WINDOW_SIZE_FRAME_TYPE.getBytes(UTF_8)); //Frame type 'W'
        output.write(ByteBuffer.allocate(4).putInt(windowSize).array());//32-bit window size

        lastUsedTime = System.currentTimeMillis();
        out.write(output.toByteArray());
        out.flush();
    }

    /**
     * Write a frame in bytes to the wire. This method is useful for writing a
     * data or a compressed frame to the wire.
     *
     * @param frame
     * @throws IOException
     */
    public void writeFrame(byte[] frame) throws IOException {
        lastUsedTime = System.currentTimeMillis();
        out.write(frame);
        out.flush();
    }

    /**
     * Reads ack frames in bytes from the wire.
     * This method blocks till all the acks are received for
     * frames written to the wire.
     *
     * @throws IOException
     */
    public void readAckFrame() throws IOException {
        byte[] buffer = new byte[6];
        int bytesReceived = 0;

        while ((bytesReceived = bytesReceived + in.read(buffer, bytesReceived, buffer.length - bytesReceived)) != -1) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Ack - total bytes received = " + bytesReceived);

            if (bytesReceived == 6) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Ack - buffer = ", buffer);
                }

                String frameType = new String(Arrays.copyOfRange(buffer, 1, 2), UTF_8);
                if (frameType.equals("A")) {
                    int sequenceNumber = ByteBuffer.wrap(Arrays.copyOfRange(buffer, 2, 6)).getInt();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Ack - received sequence number = " + sequenceNumber + " expecting sequence number = " + (seqNumber - 1));

                    if (sequenceNumber >= (seqNumber - 1)) {
                        break;
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Ack - unknown frame type " + frameType);
                    }
                    //Unknown frame type
                }
                bytesReceived = 0;
                buffer = new byte[6];
            }
        }
    }

    /**
     * Creates data frames for lumberjack protocol
     *
     * @param data List of objects containing key value pairs that will form the message payload
     * @return Data frames as a byte array
     * @throws IOException
     */
    public byte[] createDataFrames(List<Object> dataObjects) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (int i = 0; i < dataObjects.size(); i++) {
            ByteArrayOutputStream dataObjectStream = new ByteArrayOutputStream();
            @SuppressWarnings("unchecked")
            LumberjackEvent<String, String> dataObject = (LumberjackEvent<String, String>) dataObjects.get(i);

            dataObjectStream.write(PROTOCOL_VERSION.getBytes(UTF_8)); //Protocol version
            dataObjectStream.write(DATA_FRAME_TYPE.getBytes(UTF_8)); //Frame type 'D'
            dataObjectStream.write(ByteBuffer.allocate(4).putInt(seqNumber++).array()); //32-bit sequence number
            dataObjectStream.write(ByteBuffer.allocate(4).putInt(dataObject.size()).array()); //32-bit pair count

            for (int j = 0; j < dataObject.size(); j++) {
                Entry<String, String> entry = dataObject.get(j);
                String key = entry.getKey();
                String value = entry.getValue();

                byte[] keyBytes = key.getBytes(UTF_8);
                byte[] valueBytes = value.getBytes(UTF_8);
                dataObjectStream.write(ByteBuffer.allocate(4).putInt(keyBytes.length).array()); //32-bit key length
                dataObjectStream.write(keyBytes); //Key
                dataObjectStream.write(ByteBuffer.allocate(4).putInt(valueBytes.length).array()); //32-bit value length
                dataObjectStream.write(valueBytes);//Value
            }

            output.write(dataObjectStream.toByteArray());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "created data frame for " + dataObjects.size() + " events - last sequence number used = " + (seqNumber - 1));

        return output.toByteArray();
    }

    /**
     * Creates a compressed frame for lumberjack protocol.
     *
     * @param dataFrames Data frames as a byte array
     * @return Compressed frame as a byte array
     * @throws IOException
     */
    public byte[] createCompressedFrame(byte[] dataFrames) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(dataFrames);
        //Compress the bytes using zlib level 6
        byte[] cBytes = new byte[output.size()];
        deflater.setInput(output.toByteArray());
        deflater.finish();
        int compressedLength = deflater.deflate(cBytes);
        //Reset the deflater
        deflater.reset();
        //Clear the output stream and fill it with compressed byte data
        output.reset();

        output.write(PROTOCOL_VERSION.getBytes(UTF_8)); //Protocol version
        output.write(COMPRESS_FRAME_TYPE.getBytes(UTF_8)); //Frame type 'C'
        output.write(ByteBuffer.allocate(4).putInt(compressedLength).array());//32-bit payload length
        output.write(Arrays.copyOf(cBytes, compressedLength));// Compressed byte data
        return output.toByteArray();
    }

    public static class SSLHelper {

        private static final TraceComponent tc = Tr.register(SSLHelper.class);

        private final SSLSocketFactory sslSocketFactory;

        private SSLSocket socket;

        //Socket connection timeout in millseconds
        private static final int SOCKET_CONNECT_TIMEOUT = 5000;

        public SSLHelper(SSLSupport sslSupport, String sslConfigName) throws SSLException {
            JSSEHelper jsseHelper = sslSupport.getJSSEHelper();
            if ((sslConfigName == null || !jsseHelper.doesSSLConfigExist(sslConfigName))
                && !jsseHelper.doesSSLConfigExist("defaultSSLConfig")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "SSL config doesnt exist, no default config to fall back on", this, sslConfigName);
                }
                throw new SSLException(nls.getString("SSLREF_NOTFOUND"));
            }
            SSLContext sslContext = jsseHelper.getSSLContext(sslConfigName, null, null);
            sslSocketFactory = sslContext.getSocketFactory();
        }

        public SSLSocket createSocket(String host, int port) throws UnknownHostException, IOException {
            socket = (SSLSocket) sslSocketFactory.createSocket();
            socket.connect(new InetSocketAddress(host, port), SOCKET_CONNECT_TIMEOUT);
            //Start the SSL handshake which will otherwise happen just before a write operation
            socket.startHandshake();
            return socket;
        }

        public boolean isSocketAvailable() {
            if (socket == null || socket.isClosed())
                return false;
            return true;
        }

        public void closeSocket() throws IOException {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
