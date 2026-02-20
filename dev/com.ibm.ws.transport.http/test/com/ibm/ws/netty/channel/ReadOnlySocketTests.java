/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.channel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.openliberty.http.netty.channel.ReadOnlySocket;

/**
 * Tests the {@link ReadOnlySocket} APIs. All supported API calls are tested
 * against a mocked {@link NioSocketChannel}. Since the socket object is meant
 * to be read-only, all APIs that mutate the socket are expected to return a
 * {@link UnsupportedOperationException}
 */
public class ReadOnlySocketTests {

    private SocketChannel channel;
    private SocketChannel nonNioChannel;

    private SocketChannelConfig config;
    private InetSocketAddress local;
    private InetSocketAddress remote;

    private ReadOnlySocket socket;
    private ReadOnlySocket nonNioSocket;

    @Before
    public void setUp() {
        channel = mock(NioSocketChannel.class);
        nonNioChannel = mock(SocketChannel.class);

        config = mock(SocketChannelConfig.class);
        local = new InetSocketAddress("localhost", 8080);
        remote = new InetSocketAddress("192.168.1.100", 9090);

        when(channel.localAddress()).thenReturn(local);
        when(channel.remoteAddress()).thenReturn(remote);
        when(channel.config()).thenReturn(config);

        socket = new ReadOnlySocket(channel);
        nonNioSocket = new ReadOnlySocket(nonNioChannel);
    }

    @Test
    public void testReadOnlySocketAPIs() {
        testSupportedAPIs();
        testUnsupportedAPIs();
    }

    /**
     * Runs all supported APIs and verifies expected values against the test objects.
     */
    private void testSupportedAPIs() {
        Assert.assertTrue(remote.getAddress().equals(socket.getInetAddress()));

        when(config.getOption(ChannelOption.SO_KEEPALIVE)).thenReturn(true);
        Assert.assertTrue(socket.getKeepAlive());
        when(config.getOption(ChannelOption.SO_KEEPALIVE)).thenReturn(false);
        Assert.assertFalse(socket.getKeepAlive());

        Assert.assertEquals(socket.getLocalAddress(), local.getAddress());

        Assert.assertEquals(socket.getLocalPort(), 8080);

        Assert.assertEquals(socket.getPort(), 9090);

        Assert.assertEquals(socket.getLocalSocketAddress(), channel.localAddress());

        when(config.getOption(ChannelOption.SO_RCVBUF)).thenReturn(1);
        Assert.assertEquals(socket.getReceiveBufferSize(), 1);

        Assert.assertTrue(remote.equals(socket.getRemoteSocketAddress()));

        when(config.getOption(ChannelOption.SO_REUSEADDR)).thenReturn(true);
        Assert.assertTrue(socket.getReuseAddress());
        when(config.getOption(ChannelOption.SO_REUSEADDR)).thenReturn(false);
        Assert.assertFalse(socket.getReuseAddress());

        when(config.getOption(ChannelOption.SO_SNDBUF)).thenReturn(2);
        Assert.assertEquals(socket.getSendBufferSize(), 2);

        when(config.getOption(ChannelOption.SO_LINGER)).thenReturn(3);
        Assert.assertEquals(socket.getSoLinger(), 3);

        when(config.getOption(ChannelOption.SO_TIMEOUT)).thenReturn(4);
        Assert.assertEquals(socket.getSoTimeout(), 4);

        when(config.getOption(ChannelOption.TCP_NODELAY)).thenReturn(true);
        Assert.assertTrue(socket.getTcpNoDelay());
        when(config.getOption(ChannelOption.TCP_NODELAY)).thenReturn(false);
        Assert.assertFalse(socket.getTcpNoDelay());

        when(config.getOption(ChannelOption.IP_TOS)).thenReturn(5);
        Assert.assertEquals(socket.getTrafficClass(), 5);

        Assert.assertTrue(socket.isBound());

        when(channel.isOpen()).thenReturn(false);
        Assert.assertTrue(socket.isClosed());
        when(channel.isOpen()).thenReturn(true);
        Assert.assertFalse(socket.isClosed());

        when(channel.isActive()).thenReturn(true);
        Assert.assertTrue(socket.isConnected());
        when(channel.isActive()).thenReturn(false);
        Assert.assertFalse(socket.isConnected());

        when(channel.isInputShutdown()).thenReturn(true);
        Assert.assertTrue(socket.isInputShutdown());
        when(channel.isInputShutdown()).thenReturn(false);
        Assert.assertFalse(socket.isInputShutdown());

        when(channel.isOutputShutdown()).thenReturn(true);
        Assert.assertTrue(socket.isOutputShutdown());
        when(channel.isOutputShutdown()).thenReturn(false);
        Assert.assertFalse(socket.isOutputShutdown());
    }

    /**
     * Runs all unsupported APIs. This method will fail the test if any of the methods
     * does not throw the expected {@link UnsupportedOperationException}
     */
    private void testUnsupportedAPIs() {
        assertUnsupported(() -> socket.bind(local));
        assertUnsupported(() -> socket.close());
        assertUnsupported(() -> socket.connect(local));
        assertUnsupported(() -> socket.connect(local, 1));
        assertUnsupported(() -> socket.getChannel());
        assertUnsupported(() -> socket.getInputStream());
        assertUnsupported(() -> socket.getOOBInline());
        assertUnsupported(() -> socket.getOutputStream());
        assertUnsupported(() -> nonNioSocket.isInputShutdown()); //Currently only NioSocketChannel is supported
        assertUnsupported(() -> nonNioSocket.isOutputShutdown());//Currently only NioSocketChannel is supported
        assertUnsupported(() -> socket.sendUrgentData(2));
        assertUnsupported(() -> socket.setKeepAlive(true));
        assertUnsupported(() -> socket.setOOBInline(true));
        assertUnsupported(() -> socket.setPerformancePreferences(3, 4, 5));
        assertUnsupported(() -> socket.setReceiveBufferSize(6));
        assertUnsupported(() -> socket.setReuseAddress(true));
        assertUnsupported(() -> socket.setSendBufferSize(7));
        assertUnsupported(() -> socket.setSoLinger(true, 8));
        assertUnsupported(() -> socket.setSoTimeout(9));
        assertUnsupported(() -> socket.setTcpNoDelay(true));
        assertUnsupported(() -> socket.setTrafficClass(10));
        assertUnsupported(() -> socket.shutdownInput());
        assertUnsupported(() -> socket.shutdownOutput());
    }

    /**
     * Utility test method that fails the test if an {@link UnsupportedOperationException}
     * is not thrown.
     */
    private void assertUnsupported(Runnable runnable) {
        try {
            runnable.run();
        } catch (UnsupportedOperationException e) {
            return;
        }
        Assert.fail("Method did not throw expected UnsupportedOperationException");
    }

}
