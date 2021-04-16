/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * TCP channel handles HTTP CONNECT tunnel requests through a target proxy
 * to a backend server. This involves a very quick lightweight HTTP CONNECT
 * request/response exchange with that proxy.
 */
public class TCPProxyResponse {

    protected TCPProxyConnLink connLink; // @350394C

    /** proxy target hostname and port key */
    public static final String PROXY_TARGET_HOST_PORT = "PROXY_TARGET_HOST_PORT";
    /** username and password key */
    public static final String PROXY_TARGET_USER_PASS = "PROXY_TARGET_USER_PASS";
    /** Static "CONNECT " information */
    private static byte[] PROXY_CONNECT = null;
    /** Static " HTTP/1.0CRLF" information */
    private static byte[] PROXY_HTTPVERSION = null;
    /** Static "Proxy-authorization: basic " information */
    private static byte[] PROXY_AUTHORIZATION = null;
    /** Static "CRLF" information */
    private static final byte[] PROXY_CRLF = { '\r', '\n' };
    /** Write callback used for the async write of proxy buffers */
    private ProxyWriteCallback proxyWriteCB = null;
    /** Read callback used for async read of the proxy server response */
    private ProxyReadCallback proxyReadCB = null;
    /** Flag indicating whether the proxy sent a 200 success back */
    private boolean isProxyResponseValid = false;

    protected static final int STATUS_NOT_DONE = 0;
    protected static final int STATUS_DONE = 1;
    protected static final int STATUS_ERROR = 2;

    protected static final TraceComponent tc = Tr.register(TCPProxyResponse.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     *
     * @param _connLink
     */
    public TCPProxyResponse(TCPProxyConnLink _connLink) { // @350394C
        connLink = _connLink;
    }

    // setup the proxy connect information
    static {
        PROXY_CONNECT = "CONNECT ".getBytes(StandardCharsets.ISO_8859_1);
        PROXY_HTTPVERSION = " HTTP/1.0\r\n".getBytes(StandardCharsets.ISO_8859_1);
        PROXY_AUTHORIZATION = "Proxy-authorization: basic ".getBytes(StandardCharsets.ISO_8859_1);
    }

    protected void setIsProxyResponseValid(boolean newValue) {
        isProxyResponseValid = newValue;
    }

    /**
     * Complete the proxy connect handshake by reading for the response and
     * validating any data.
     */
    protected void proxyReadHandshake() {

        // setup the read buffer - use JIT on the first read attempt. If it
        // works, then any subsequent reads will use that same buffer.
        connLink.getReadInterface().setJITAllocateSize(1024);
        // reader.setBuffer(WsByteBufferPoolManagerImpl.getRef().allocate(1024));

        if (connLink.isAsyncConnect()) {
            // handshake - read the proxy response
            this.proxyReadCB = new ProxyReadCallback();
            readProxyResponse(connLink.getVirtualConnection());

        } else {
            int rc = STATUS_NOT_DONE;
            while (rc == STATUS_NOT_DONE) {
                readProxyResponse(connLink.getVirtualConnection());
                rc = checkResponse(connLink.getReadInterface());
            }
            if (rc == STATUS_ERROR) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "could not complete proxy handshake, read request failed");
                }
                releaseProxyReadBuffer();
                // if (null == this.syncError) {
                if (connLink.isSyncError() == false) {
                    // create a new connect exception
                    connLink.connectFailed(new IOException("Invalid Proxy Server Response "));
                }
            }
        }
    }

    /**
     * Check for a proxy handshake response.
     *
     * @param rsc
     * @return int (status code)
     */
    protected int checkResponse(TCPReadRequestContext rsc) {
        // Parse the proxy server response
        //
        WsByteBuffer[] buffers = rsc.getBuffers();

        // check if the correct response was received
        if (null == buffers) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not complete proxy handshake, null buffers");
            }
            return STATUS_ERROR;
        }
        int status = validateProxyResponse(buffers);
        if (STATUS_DONE == status) {
            releaseProxyReadBuffer();
        }
        return status;
    }

    protected void writeAndShake() {
        // handshake, write the forward proxy buffers
        if (!connLink.isAsyncConnect()) {
            try {
                connLink.getWriteInterface().write(TCPWriteRequestContext.WRITE_ALL_DATA, TCPRequestContext.USE_CHANNEL_TIMEOUT);
                this.releaseProxyWriteBuffer();
                this.proxyReadHandshake();
            } catch (IOException x) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "connectComplete: could not complete sync handshake");
                }
                this.releaseProxyWriteBuffer();
                connLink.connectFailed(x);
            }
        } else {
            this.proxyWriteCB = new ProxyWriteCallback();

            VirtualConnection vcRC = connLink.getWriteInterface().write(TCPWriteRequestContext.WRITE_ALL_DATA, this.proxyWriteCB, false, TCPRequestContext.USE_CHANNEL_TIMEOUT);
            if (null != vcRC) {
                this.proxyWriteCB.complete(vcRC, connLink.getWriteInterface());
            }
        }
    }

    /**
     * Release the proxy connect write buffer.
     */
    protected void releaseProxyWriteBuffer() {
        WsByteBuffer buffer = connLink.getWriteInterface().getBuffer();
        if (null != buffer) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Releasing proxy write buffer: " + buffer);
            }
            buffer.release();
            connLink.getWriteInterface().setBuffer(null);
        }
    }

    /**
     * Release the proxy connect read buffer.
     */
    protected void releaseProxyReadBuffer() {
        WsByteBuffer buffer = connLink.getReadInterface().getBuffer();
        if (null != buffer) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Releasing proxy read buffer: " + buffer);
            }
            buffer.release();
            connLink.getReadInterface().setBuffer(null);
        }
    }

    /**
     * Reads the entire proxy response and checks if the
     * proxyResponseBuffers contains "HTTP/1.0 200 Connection established"
     *
     * @param buffers
     *            the buffers on the TCPReadRequestContext
     * @return int true if the response is valid and false if otherwise
     */
    protected int validateProxyResponse(WsByteBuffer[] buffers) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateProxyResponse");
        }

        int status = STATUS_NOT_DONE;
        // Drain the proxy response
        for (int i = 0; i < buffers.length; i++) {

            buffers[i].flip();
            byte[] data = new byte[buffers[i].limit()];
            buffers[i].get(data);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "buffer: [" + new String(data) + "]");
            }

            // exit if the first buffer does not contain the "HTTP   200"
            // We assume that the first buffer contains the first line of the
            // HTTPResponse
            if (!this.isProxyResponseValid) {
                this.isProxyResponseValid = containsHTTP200(data);
                if (!this.isProxyResponseValid) {
                    // top level methods will call connectFailed
                    status = STATUS_ERROR;
                    break;
                }
            }

            // exit if the end of the response was found
            if (containsEOLDelimiters(data)) {
                status = STATUS_DONE;
                break;
            }

            // we are going to need to do another read, clear this buffer
            buffers[i].clear();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateProxyResponse, rc=" + status);
        }
        return status;
    }

    /**
     * Searches for \r\n\r\n (CR-LF-CR-LF),
     * or \n\n (LF-LF) in a byte array.
     * We dont care here about the order in which these 4
     * control characters appear.
     *
     * @param data
     *            search byte array
     * @return true if found; false if not found
     */
    protected boolean containsEOLDelimiters(byte[] data) {
        int numCRLFs = 0;
        for (int i = 0; i < data.length; i++) {
            if ('\r' == data[i] || '\n' == data[i]) {
                numCRLFs++;
                if (4 == numCRLFs) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "contains 4 consecutive CRs and/or LFs: true");
                    }
                    return true;
                }
                if ((i > 0) && ('\n' == data[i])) {
                    if ('\n' == data[i - 1]) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "contains LF-LF: true");
                        }
                        return true;
                    }
                }
            } else {
                // must be in a row, reset counter
                numCRLFs = 0;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "containsEOLDelimiters: false count=" + numCRLFs);
        }
        return false;
    }

    // /**
    // * Searches for \r\n\r\n i.e. CRLF in a byte array.
    // * We dont care here about the order in which these 4
    // * control characters appear.
    // *
    // * @param data search byte array
    // * @return true if found; false if not found
    // */
    // protected boolean containsCRLF(byte[] data) {
    //
    // // TO-DO: should be able to handle just LFs too..
    // int numCRLFs = 0;
    // for (int i = 0; i < data.length; i++) {
    // if ('\r' == data[i] || '\n' == data[i]) {
    // numCRLFs++;
    // if (4 == numCRLFs) {
    // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    // Tr.debug(tc, "containsCRLF: true");
    // }
    // return true;
    // }
    // } else {
    // // must be four in a row, reset counter
    // numCRLFs = 0;
    // }
    // }
    //
    // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    // Tr.debug(tc, "containsCRLF: false count=" + numCRLFs);
    // }
    // return false;
    // }

    /**
     * Checks if the byte array contains "HTTP 200" in a byte array.
     *
     * @param data
     *            search byte array
     * @return true if found; false if not
     */
    protected boolean containsHTTP200(byte[] data) {

        boolean rc = true;

        // byte comparison to check for HTTP and 200 in the response
        // this code is not pretty, it is designed to be fast
        // code assumes that HTTP/1.0 200 will be contained in one buffer
        //
        if (data.length < 12 || data[0] != 'H' || data[1] != 'T' || data[2] != 'T' || data[3] != 'P' || data[9] != '2' || data[10] != '0' || data[11] != '0') {
            rc = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "containsHTTP200: " + rc);
        }
        return rc;
    }

    /**
     * Used by the asyncronous write of the forward proxy connect buffers
     */
    protected class ProxyWriteCallback implements TCPWriteCompletedCallback {

        /**
         * Called when the write of the proxy buffers has completed successfully.
         *
         * @param inVC
         *            virtual connection associated with this request.
         * @param wsc
         *            the TCPWriteRequestContext associated with this request.
         */
        @Override
        public void complete(VirtualConnection inVC, TCPWriteRequestContext wsc) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ProxyWriteCallback --> complete for " + inVC);
            }
            releaseProxyWriteBuffer();

            // Once the write succeeds we read the proxy response handshake
            //
            proxyReadHandshake();
        }

        /**
         * Called back if an exception occurs while writing the data.
         *
         * @param inVC
         *            virtual connection associated with this request.
         * @param wsc
         *            the TCPWriteRequestContext associated with this request.
         * @param ioe
         *            The exception.
         */
        @Override
        public void error(VirtualConnection inVC, TCPWriteRequestContext wsc, IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ProxyWriteCallback--> error for " + inVC);
                Tr.debug(tc, "ioe: " + ioe);
            }
            releaseProxyWriteBuffer();
            connLink.connectFailed(ioe);
        }
    }

    /**
     * Used for the asyncronous read of the proxy server response.
     */
    protected class ProxyReadCallback implements TCPReadCompletedCallback {

        /**
         * Called when the read of the response has completed successfully.
         * If the response contains "HTTP/1.0 200 Connection established" we
         * call the application callback and return the connect.
         *
         * @param inVC
         *            virtual connection associated with this request.
         * @param wsc
         *            the TCPReadRequestContext associated with this request.
         */
        @Override
        public void complete(VirtualConnection inVC, TCPReadRequestContext wsc) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ProxyReadCallback --> complete for " + inVC);
            }

            int status = checkResponse(wsc);

            if (status == STATUS_DONE) {
                connLink.getApplicationCallback().ready(inVC);

            } else if (status == STATUS_NOT_DONE) {
                // read more proxy responses
                readProxyResponse(inVC);

            } else {
                // Error
                releaseProxyReadBuffer();
                connLink.connectFailed(new IOException("Invalid Proxy Server Response "));
            }
        }

        /**
         * Called back if an exception occurs while reading the response.
         *
         * @param inVC
         *            virtual connection associated with this request.
         * @param wsc
         *            the TCPReadRequestContext associated with this request.
         * @param ioe
         *            The exception.
         */
        @Override
        public void error(VirtualConnection inVC, TCPReadRequestContext wsc, IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ProxyReadCallback--> error for " + inVC);
                Tr.debug(tc, "ioe: " + ioe);
            }
            releaseProxyReadBuffer();
            connLink.connectFailed(ioe);
        }
    }

    /**
     * If SSL tunneling is enabled set the forward proxy connect buffers
     * on the TCPWriteRequestContext.
     * Forward proxy connect buffers contain ->
     * "CONNECT <proxy.TargetHostname:proxy.TargetPort> HTTP/1.0CRLF"
     * "Proxy-authorization: basic <base64 encoded(username:password)>CRLF"
     * "CRLF"
     *
     * Note: Proxy-authorization is optional header.
     *
     * @return boolean true if the forward proxy buffers were set,
     *         false if otherwise
     */
    protected boolean setForwardProxyBuffers(Map<Object, Object> config) {

        byte[] target = (byte[]) config.get(PROXY_TARGET_HOST_PORT);
        if (null == target) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Proxy tunnel attempt missing target host");
            }
            connLink.connectFailed(new IOException("Missing forward proxy target host"));
            return false;
        }
        byte[] authInfo = (byte[]) config.get(PROXY_TARGET_USER_PASS);
        // we're always going to have about 20 to 50 bytes of information, plus
        // the target host:port, plus the option authorization info data
        int size = 100 + target.length + ((null != authInfo) ? authInfo.length : 0);
        WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(size);
        buffer.put(PROXY_CONNECT);
        buffer.put(target);
        buffer.put(PROXY_HTTPVERSION);

        // Has authentication info. been provided
        if (null != authInfo) {
            buffer.put(PROXY_AUTHORIZATION);
            buffer.put(authInfo);
            buffer.put(PROXY_CRLF);
        }
        buffer.put(PROXY_CRLF);
        buffer.flip();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            byte[] output = new byte[buffer.limit()];
            buffer.get(output);
            Tr.debug(tc, "ForwardProxyBuffers[" + new String(output) + "]");
            buffer.position(0);
        }
        connLink.getWriteInterface().setBuffer(buffer);

        return true;
    }

    /**
     * Start a read for the response from the target proxy, this is either the
     * first read or possibly secondary ones if necessary.
     *
     * @param inVC
     */
    protected void readProxyResponse(VirtualConnection inVC) {

        int size = 1;
        if (!this.isProxyResponseValid) {
            // we need at least 12 bytes for the first line
            size = 12;
        }
        if (connLink.isAsyncConnect()) {
            VirtualConnection vcRC = connLink.getReadInterface().read(size, this.proxyReadCB, false, TCPRequestContext.USE_CHANNEL_TIMEOUT);
            if (null != vcRC) {
                this.proxyReadCB.complete(vcRC, connLink.getReadInterface());
            }
        } else {
            try {
                connLink.getReadInterface().read(size, TCPRequestContext.USE_CHANNEL_TIMEOUT);
            } catch (IOException x) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "could not complete proxy handshake, read request failed");
                }
                releaseProxyReadBuffer();
                connLink.connectFailed(x);
            }
        }
    }

}
