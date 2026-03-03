/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.channel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketImplFactory;
import java.nio.channels.SocketChannel;
import java.io.InputStream;
import java.io.OutputStream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Provides a Read-only view of a Netty Channel's socket. This will allow access to non-mutable
 * socket APIs. However, any attempt to modify the socket through the Socket methods will result in 
 * a {@link UnsupportedOperationException}. 
 * 
 * Note: {@link Socket#setSocketImplFactory(SocketImplFactory)} is a static method, so it will not be handled by this
 * implementation. 
 */
public class ReadOnlySocket extends Socket{

    private final InetSocketAddress local;
    private final InetSocketAddress remote;
    private final Channel nettyChannel;
    private final ChannelConfig config;

    /**
     * Create a Read-only view of a Netty Channel's socket.
     * @param channel the Netty Channel
     */
    public ReadOnlySocket(Channel channel){
        this.nettyChannel = channel;
        this.local = (InetSocketAddress) channel.localAddress();
        this.remote = (InetSocketAddress) channel.remoteAddress();
        this.config = channel.config();
    }

    /**
     * Note: Bind is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void bind(SocketAddress bindpoint){
        throw newUnsupportedOperationException("bind(SocketAddress bindpoint)");
    }

    /**
     * Note: Close is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void close(){
        throw newUnsupportedOperationException("close()");
    }

    /**
     * Note: Connect is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void connect(SocketAddress endpoint){
        throw newUnsupportedOperationException("connect(SocketAddress endpoint)");
    }
    
    /**
     * Note: Connect is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void connect(SocketAddress endpoint, int timeout){
        throw newUnsupportedOperationException("connect(SocketAddress endpoint, int timeout)");
    }

    /**
     * Requires downcasting based on active communication library. This would expose mutable
     * APIs, and therefore is not supported and will result in a {@link UnsupportedOperationException}.
     */
    @Override
    public SocketChannel getChannel(){
        throw newUnsupportedOperationException("getChannel()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetAddress getInetAddress(){
        return remote.getAddress();
    }

    /**
     * Netty's I/O model does not support blocking streams. Invoking this method 
     * will result in a {@link UnsupportedOperationException}.
     */
    @Override
    public InputStream getInputStream(){
        throw newUnsupportedOperationException("getInputStream()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getKeepAlive(){
        return config.getOption(ChannelOption.SO_KEEPALIVE);
    }

    /**
     * {@inheritDoc}
     */
    @Override 
    public InetAddress getLocalAddress(){
        return local.getAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override 
    public int getLocalPort(){
        return local.getPort();
    }

    /**
     * {@inheritDoc}
     */
    public SocketAddress getLocalSocketAddress(){
        return nettyChannel.localAddress();
    }

    /**
     * Currently there is no ChannelOption in Netty to support this option. 
     * Invoking this method will result in a {@link UnsupportedOperationException}.
     */
    @Override
    public boolean getOOBInline(){
        throw newUnsupportedOperationException("getOOBInline()");
    }

    /**
     * Netty's I/O model does not support blocking streams. This would require wrapping pipeline
     * handler's write() method. Invoking this method will result in a {@link UnsupportedOperationException}.
     */
    @Override 
    public OutputStream getOutputStream(){
        throw newUnsupportedOperationException("getOutputStream()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort(){
        return remote.getPort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getReceiveBufferSize(){
        return config.getOption(ChannelOption.SO_RCVBUF);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress getRemoteSocketAddress(){
        return nettyChannel.remoteAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getReuseAddress(){
        return config.getOption(ChannelOption.SO_REUSEADDR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSendBufferSize(){
        return config.getOption(ChannelOption.SO_SNDBUF);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSoLinger(){
        return config.getOption(ChannelOption.SO_LINGER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSoTimeout(){
        return config.getOption(ChannelOption.SO_TIMEOUT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getTcpNoDelay(){
        return config.getOption(ChannelOption.TCP_NODELAY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTrafficClass(){
        return config.getOption(ChannelOption.IP_TOS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBound(){
        return local != null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed(){
        return !nettyChannel.isOpen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected(){
        return nettyChannel.isActive();
    }

    /**
     * {@inheritDoc}
     * 
     * Note: This depends on the specific interface implemented by the Netty Channel. Currently, 
     * this implementation uses NioNettyChannel which does provide {@link NioSocketChannel#isInputShutdown()}.
     * Any other Netty Channel implementations will result in a {@link UnsupportedOperationException}. This 
     * could be expanded in the future if the our transport implementation changes to support other Netty Channel
     * types.
     * 
     * @throws UnsupportedOperationException if the Netty Channel is not an instance of {@link NioSocketChannel#isInputShutdown()}.
     */
    @Override
    public boolean isInputShutdown(){
        if(nettyChannel instanceof NioSocketChannel){
            return ((NioSocketChannel) nettyChannel).isInputShutdown();
        }
        throw newUnsupportedOperationException("isInputShutdown()");
    }

    /**
     * {@inheritDoc}
     * 
     * Note: This depends on the specific interface implemented by the Netty Channel. Currently, 
     * this implementation uses NioNettyChannel which does provide {@link NioSocketChannel#isOutputShutdown()}.
     * Any other Netty Channel implementations will result in a {@link UnsupportedOperationException}. This 
     * could be expanded in the future if the our transport implementation changes to support other Netty Channel
     * types.
     * 
     * @throws UnsupportedOperationException if the Netty Channel is not an instance of {@link NioSocketChannel#isOutputShutdown()}.
     */
    @Override
    public boolean isOutputShutdown(){
        if(nettyChannel instanceof NioSocketChannel){
            return ((NioSocketChannel) nettyChannel).isOutputShutdown();
        }
        throw newUnsupportedOperationException("isOutputShutdown()");
    }

    /**
     * Note: sendUrgentData is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void sendUrgentData(int data){
        throw newUnsupportedOperationException("sendUrgentData(int data)");
    }

    /**
     * Note: setKeepAlive is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void setKeepAlive(boolean on){
        throw newUnsupportedOperationException("setKeepAlive(boolean on)");
    }

    /**
     * Note: setOOBInline is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void setOOBInline(boolean on){
        throw newUnsupportedOperationException("setOOBInline(boolean on)");
    }

    /**
     * Note: setPerformancePreferences is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth){
        throw newUnsupportedOperationException("setPerformancePreferences(int connectionTime, int latency, int bandwidth)");
    }

    /**
     * Note: setReceiveBufferSize is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void setReceiveBufferSize(int size){
        throw newUnsupportedOperationException("setReceiveBufferSize(int size)");
    }

    /**
     * Note: setReuseAddress is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void setReuseAddress(boolean on){
        throw newUnsupportedOperationException("setReuseAddress(boolean on)");
    }

    /**
     * Note: setSendBufferSize is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void setSendBufferSize(int size){
        throw newUnsupportedOperationException("setSendBufferSize(int size)");
    }

    /**
     * Note: setSoLinger is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    @Override
    public void setSoLinger(boolean on, int linger){
        throw newUnsupportedOperationException("setSoLinger(boolean on, int linger)");
    }

    /**
     * Note: setSoTimeout is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    public void setSoTimeout(int timeout){
        throw newUnsupportedOperationException("setSoTimeout(int timeout)");
    }   
    
    /**
     * Note: setTcpNoDelay is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    public void setTcpNoDelay(boolean on){
        throw newUnsupportedOperationException("setTcpNoDelay(boolean on)");
    }

    /**
     * Note: setTrafficClass is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    public void setTrafficClass(int tc){
        throw newUnsupportedOperationException("setTrafficClass(int tc)");
    }

    /**
     * Note: shutdownInput is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    public void shutdownInput(){
        throw newUnsupportedOperationException("shutdownInput()");
    }   

    /**
     * Note: shutdownOutput is a mutating method. Invoking this method will result 
     * in a {@link UnsupportedOperationException}.
     */
    public void shutdownOutput(){
        throw newUnsupportedOperationException("shutdownOutput()");
    }   

    /**
     * Utility method to create and an UnsupportedOperationException with a message indicating
     * that mutating the read-only socket is not supported by this implementation.
     * 
     * @param method the name of the method that is attempting to mutate the socket
     * @return a UnsupportedOperationException indicating that mutating the socket is unsupported
     */
    private static UnsupportedOperationException newUnsupportedOperationException(String method){
        return new UnsupportedOperationException("The method " + method + " is attempting to mutate a read-only socket. Mutating this socket object is not supported.");
    }

}
