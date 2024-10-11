/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.tcp.TCPChannelInitializerImpl;
import io.openliberty.netty.internal.tcp.TCPConfigurationImpl;
import io.openliberty.netty.internal.tcp.TCPMessageConstants;
import test.common.SharedOutputManager;

/**
 * Basic unit tests for {@link NettyFrameworkImpl}
 */
public class NettyFrameworkImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance()
            .trace(NettyConstants.NETTY_TRACE_STRING);
    private List<Channel> testChannels = null;
    NettyFrameworkImpl framework = null;
    Map<String, Object> options;

    private final String LOCALHOST = "localhost";
    private final int TCP_PORT = 9080;
    private final int UDP_PORT = 9084;
    private final int PORT_BIND_TIMEOUT_MS = 15 * 1000;

    @Rule
    public TestRule rule = outputMgr;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setup() {
        testChannels = new ArrayList<Channel>();
        framework = new NettyFrameworkImpl();
        framework.setExecutorService(GlobalEventExecutor.INSTANCE);
        framework.activate(null, null);
        options = new HashMap<String, Object>();
    }

    @After
    public void tearDown() throws Exception {
        framework.deactivate(null, null);
        framework = null;
        testChannels = null;
        options = null;
        outputMgr.copyTraceStream();
        outputMgr.copyMessageStream();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    private void setTCPConfig() {
        options.put(TCPConfigurationImpl.PORT_OPEN_RETRIES, 14);
        options.put(TCPConfigurationImpl.REUSE_ADDR, "true");
    }

    /**
     * Verify that a TCP boostrap can be created as expected
     * 
     * @throws Exception
     */
    @Test
    public void testCreateTCPBootstrap() throws Exception {
        setTCPConfig();
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrap(options);
        Assert.assertTrue((boolean) bootstrap.config().options().get(ChannelOption.SO_REUSEADDR));
        Assert.assertTrue(bootstrap.getBaseInitializer() instanceof TCPChannelInitializerImpl);
        framework.deactivate(null, null);
    }

    /**
     * Start listening on a TCP channel
     * 
     * @throws Exception
     */
    @Test
    public void testTCPStart() throws Exception {
        setTCPConfig();
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrap(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        framework.setServerStarted(null);
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.start(bootstrap, LOCALHOST, TCP_PORT, callback -> {
            if (callback.isSuccess()) {
                testChannels.add(callback.channel());
            } else {
                Assert.fail("framework failed to start: " + callback.cause().getMessage());
            }
            activeChannelLatch.countDown();
        });
        activeChannelLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Thread.sleep(100); // wait to ensure framework's internal callbacks are handled
        Assert.assertTrue(testChannels.size() == 1);
        Assert.assertTrue(testChannels.get(0).isActive());
        Assert.assertTrue(framework.getActiveChannels().contains(testChannels.get(0)));
    }

    /**
     * Start listening on a UDP channel
     * 
     * @throws Exception
     */
    @Test
    public void testUDPStart() throws Exception {
        BootstrapExtended bootstrap = framework.createUDPBootstrap(options);
        bootstrap.handler(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
                // do nothing
            }
        });
        framework.setServerStarted(null);
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.start(bootstrap, LOCALHOST, UDP_PORT, callback -> {
            if (callback.isSuccess()) {
                testChannels.add(callback.channel());
            } else {
                Assert.fail("framework failed to start: " + callback.cause().getMessage());
            }
            activeChannelLatch.countDown();
        });
        activeChannelLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Thread.sleep(100); // wait to ensure framework's internal callbacks are handled
        Assert.assertTrue(testChannels.size() == 1);
        Assert.assertTrue(testChannels.get(0).isActive());
        Assert.assertTrue(framework.getOutboundConnections().contains(testChannels.get(0)));
    }

    /**
     * Create an outbound UDP channel
     * 
     * @throws Exception
     */
    @Test
    public void testUDPStartOutbound() throws Exception {
    	testUDPStart();
        BootstrapExtended bootstrap = framework.createUDPBootstrapOutbound(null);
        bootstrap.handler(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
                // do nothing
            }
        });
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.startOutbound(bootstrap, LOCALHOST, UDP_PORT, callback -> {
            if (callback.isSuccess()) {
                testChannels.add(callback.channel());
            } else {
                Assert.fail("framework failed to start: " + callback.cause().getMessage());
            }
            activeChannelLatch.countDown();
        });
        activeChannelLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Thread.sleep(100); // wait to ensure framework's internal callbacks are handled
        Assert.assertEquals(2, testChannels.size());
        Assert.assertTrue(testChannels.get(1).isActive());
        Assert.assertTrue(framework.getOutboundConnections().contains(testChannels.get(1)));
    }

    /**
     * Create an inbound TCP channel, then create a new outbound TCP channel and
     * connect to the inbound channel
     * 
     * @throws Exception
     */
    @Test
    public void testTCPStartOutbound() throws Exception {
        testTCPStart();
        BootstrapExtended bootstrapOutbound = framework.createTCPBootstrapOutbound(null);
        bootstrapOutbound.handler(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
                // do nothing
            }
        });
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.startOutbound(bootstrapOutbound, LOCALHOST, TCP_PORT, callback -> {
            if (callback.isSuccess()) {
                testChannels.add(callback.channel());
            } else {
                outputMgr.failWithThrowable("testTCPStartOutbound", callback.cause());
            }
            activeChannelLatch.countDown();
        });
        activeChannelLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Thread.sleep(100); // wait to ensure framework's internal callbacks are handled
        // Ensure we have two channels created and active
        Assert.assertEquals(2, testChannels.size());
        Assert.assertTrue(testChannels.get(1).isActive());
        // Ensure there is an active connection for both the outbound connection started
        // and the endpoint that received it
        Assert.assertEquals(1, framework.getActiveChannelsMap().get(testChannels.get(0)).size());
        Assert.assertTrue(framework.getOutboundConnections().contains(testChannels.get(1)));
    }

    /**
     * Start listening on a TCP channel and verify stop()
     * 
     * @throws Exception
     */
    @Test
    public void testAsyncStop() throws Exception {
        setTCPConfig();
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrap(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        framework.setServerStarted(null);
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.start(bootstrap, LOCALHOST, TCP_PORT, callback -> {
            if (callback.isSuccess()) {
                testChannels.add(callback.channel());
            } else {
                Assert.fail("framework failed to start: " + callback.cause().getMessage());
            }
            activeChannelLatch.countDown();
        });
        activeChannelLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Thread.sleep(100); // wait to ensure framework's internal callbacks are handled
        Assert.assertTrue(testChannels.get(0) != null);
        Assert.assertTrue(testChannels.get(0).isActive());
        Assert.assertEquals(1, framework.getActiveChannels().size());
        Assert.assertTrue(framework.getActiveChannels().contains(testChannels.get(0)));
        ChannelFuture stopFuture = framework.stop(testChannels.get(0));
        stopFuture.await(15, TimeUnit.SECONDS);
        Thread.sleep(100); // wait to ensure framework's internal callbacks are handled
        Assert.assertFalse(testChannels.get(0).isActive());
        Assert.assertTrue(stopFuture.isSuccess());
        Assert.assertFalse(framework.getActiveChannels().contains(testChannels.get(0)));
        Assert.assertTrue(framework.getActiveChannels().isEmpty());

    }

    /**
     * Start listening on a TCP channel and verify stop()
     * 
     * @throws Exception
     */
    @Test
    public void testSyncStop() throws Exception {
        setTCPConfig();
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrap(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        framework.setServerStarted(null);
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.start(bootstrap, LOCALHOST, TCP_PORT, future -> {
            if (future.isSuccess()) {
                testChannels.add(future.channel());
            } else {
                Assert.fail("framework failed to start: " + future.cause().getMessage());
            }
            activeChannelLatch.countDown();
        });
        activeChannelLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Thread.sleep(100); // wait to ensure framework's internal callbacks are handled
        Assert.assertTrue(testChannels.get(0) != null);
        Assert.assertTrue(testChannels.get(0).isActive());
        framework.stop(testChannels.get(0), 10000);
        Assert.assertFalse(testChannels.get(0).isActive());
        Assert.assertFalse(framework.getActiveChannels().contains(testChannels.get(0)));
        Assert.assertTrue(framework.getActiveChannels().isEmpty());
    }

    /**
     * Start listening on three TCP channel, then tear them down individually
     * 
     * @throws Exception
     */
    @Test
    public void testMultipleChannels() throws Exception {
        setTCPConfig();
        int secondPort = TCP_PORT + 1;
        int thirdPort = TCP_PORT + 2;
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrap(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        final CountDownLatch connectionLatch = new CountDownLatch(3);
        framework.setServerStarted(null);
        framework.start(bootstrap, LOCALHOST, TCP_PORT, future -> {
            if (future.isSuccess()) {
                testChannels.add(future.channel());
                connectionLatch.countDown();
            } else {
                Assert.fail("framework failed to start: " + future.cause().getMessage());
                connectionLatch.countDown();
            }
        });
        framework.start(bootstrap, LOCALHOST, secondPort, future -> {
            if (future.isSuccess()) {
                testChannels.add(future.channel());
                connectionLatch.countDown();
            } else {
                Assert.fail("framework failed to start: " + future.cause().getMessage());
                connectionLatch.countDown();
            }
        });
        framework.start(bootstrap, LOCALHOST, thirdPort, future -> {
            if (future.isSuccess()) {
                testChannels.add(future.channel());
                connectionLatch.countDown();
            } else {
                Assert.fail("framework failed to start: " + future.cause().getMessage());
                connectionLatch.countDown();
            }
        });
        connectionLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Thread.sleep(100); // wait to ensure framework's internal callbacks are handled
        Assert.assertTrue(testChannels.get(0) != null);
        Assert.assertTrue(testChannels.get(0).isActive());
        Assert.assertEquals(3, testChannels.size());
        Assert.assertEquals(3, framework.getActiveChannels().size());
        framework.stop(testChannels.get(0), 10000); // sync stop
        Assert.assertFalse(testChannels.get(0).isActive());
        Assert.assertFalse(framework.getActiveChannels().contains(testChannels.remove(0)));
        Assert.assertEquals(2, framework.getActiveChannels().size());
        framework.stop(testChannels.get(0), 10000);
        Assert.assertFalse(testChannels.get(0).isActive());
        Assert.assertFalse(framework.getActiveChannels().contains(testChannels.remove(0)));
        Assert.assertEquals(1, framework.getActiveChannels().size());
        framework.stop(testChannels.get(0), 10000);
        Assert.assertFalse(testChannels.get(0).isActive());
        Assert.assertFalse(framework.getActiveChannels().contains(testChannels.remove(0)));
        Assert.assertTrue(framework.getActiveChannels().isEmpty());
    }
}
