/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.ssl.protocol.LibertySSLSocketFactory;

public class WSSocket extends SSLSocket

{
    private static final TraceComponent tc = Tr.register(WSSocket.class, "SSL", "com.ibm.ws.ssl.resources.ssl");
    protected java.net.Socket socket;

    public WSSocket(Socket s) {
        this.socket = s;
    }

    @Override
    public SocketChannel getChannel() {
        return socket.getChannel();
    }

    @Override
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return socket.getKeepAlive();
    }

    @Override
    public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return socket.getOOBInline();
    }

    @Override
    public int getPort() {
        return socket.getPort();
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return socket.getSendBufferSize();
    }

    @Override
    public int getSoLinger() throws SocketException {
        return socket.getSoLinger();
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return socket.getSoTimeout();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return socket.getTcpNoDelay();
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return socket.getTrafficClass();
    }

    @Override
    public boolean isBound() {
        return socket.isBound();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return socket.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return socket.isOutputShutdown();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        socket.sendUrgentData(data);
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        socket.setKeepAlive(on);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        socket.setOOBInline(on);
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        socket.setReuseAddress(on);
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
        socket.setSendBufferSize(size);
    }

    @Override
    public void setSoLinger(boolean on, int l) throws SocketException {
        socket.setSoLinger(on, l);
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        socket.setTcpNoDelay(on);
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        socket.setTrafficClass(tc);
    }

    @Override
    public void shutdownInput() throws IOException {
        socket.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        socket.shutdownOutput();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @Override
    public String toString() {
        return socket.toString();
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        socket.bind(bindpoint);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "connect", new Object[] { endpoint });
        socket.connect(endpoint);

        if (endpoint instanceof java.net.InetSocketAddress) {
            InetSocketAddress ep = (InetSocketAddress) endpoint;
            int port = ep.getPort();
            String host = ep.getHostName();

            //Get the WAS factory and
            SSLSocketFactory factory = (SSLSocketFactory) LibertySSLSocketFactory.getDefault();

            socket = factory.createSocket(socket, host, port, true);

        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "connect");
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "connect", new Object[] { endpoint, timeout });

        socket.connect(endpoint, timeout);

        if (endpoint instanceof java.net.InetSocketAddress) {
            InetSocketAddress ep = (InetSocketAddress) endpoint;
            int port = ep.getPort();
            String host = ep.getHostName();

            //Get the WAS factory and
            SSLSocketFactory factory = (SSLSocketFactory) LibertySSLSocketFactory.getDefault();

            socket = factory.createSocket(socket, host, port, true);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "connect");
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).addHandshakeCompletedListener(listener);
        }
    }

    @Override
    public String[] getEnabledCipherSuites() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getEnabledCipherSuites();
        } else {
            return null;
        }
    }

    @Override
    public String[] getEnabledProtocols() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getEnabledProtocols();
        } else {
            return null;
        }
    }

    @Override
    public boolean getEnableSessionCreation() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getEnableSessionCreation();
        } else {
            return false;
        }
    }

    @Override
    public boolean getNeedClientAuth() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getNeedClientAuth();
        } else {
            return false;
        }
    }

    @Override
    public SSLSession getSession() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getSession();
        } else {
            return null;
        }

        //return this.internalSocket.getSession();
    }

    @Override
    public SSLParameters getSSLParameters() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getSSLParameters();
        } else {
            return null;
        }
    }

    @Override
    public String[] getSupportedCipherSuites() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getSupportedCipherSuites();
        } else {
            return null;
        }
    }

    @Override
    public String[] getSupportedProtocols() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getSupportedProtocols();
        } else {
            return null;
        }
    }

    @Override
    public boolean getUseClientMode() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getUseClientMode();
        } else {
            return false;
        }
    }

    @Override
    public boolean getWantClientAuth() {
        if (socket instanceof SSLSocket) {
            return ((SSLSocket) socket).getWantClientAuth();
        } else {
            return false;
        }
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).removeHandshakeCompletedListener(listener);
        }
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setEnabledCipherSuites(suites);
        }
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setEnabledProtocols(protocols);
        }
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setEnableSessionCreation(flag);
        }
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setNeedClientAuth(need);
        }
    }

    @Override
    public void setSSLParameters(SSLParameters params) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setSSLParameters(params);
        }
    }

    @Override
    public void setUseClientMode(boolean mode) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setUseClientMode(mode);
        }
    }

    @Override
    public void setWantClientAuth(boolean want) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setWantClientAuth(want);
        }
    }

    @Override
    public void startHandshake() throws IOException {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).startHandshake();
        }
    }

}
