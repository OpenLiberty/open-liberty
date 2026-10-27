/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.impl;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmock.Expectations;
import org.jmock.Mockery;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.openliberty.channel.config.ChannelFrameworkConfig;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.tcp.TCPChannelInitializerImpl;
import io.openliberty.netty.internal.tcp.TCPConfigurationImpl;
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
    static Map<String, Object> configMap;

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
        outputMgr.trace("io.openliberty.netty*=all");
        outputMgr.captureStreams();
        configMap = new HashMap<String, Object>();
        configMap.put(NettyConstants.SCALER_MAX_THREADS_PROPERTY, NettyConstants.SCALER_MAX_THREADS);
        configMap.put(NettyConstants.SCALER_METRICS_WINDOW_PROPERTY, (Long)0L);
        configMap.put(NettyConstants.USE_NATIVE_TRANSPORT, true);
        configMap.put(NettyConstants.SCALER_MIN_THREADS_PROPERTY, NettyConstants.SCALER_MIN_THREADS);
        configMap.put(NettyConstants.SCALER_MAX_THREADS_PROPERTY, NettyConstants.SCALER_MAX_THREADS);
        configMap.put(NettyConstants.SCALER_WINDOW_PROPERTY, NettyConstants.SCALER_WINDOW);
        configMap.put(NettyConstants.SCALER_DOWN_THRESHOLD_PROPERTY, NettyConstants.SCALER_DOWN_THRESHOLD);
        configMap.put(NettyConstants.SCALER_UP_THRESHOLD_PROPERTY, NettyConstants.SCALER_UP_THRESHOLD);
        configMap.put(NettyConstants.SCALER_UP_STEP_PROPERTY, NettyConstants.SCALER_UP_STEP);
        configMap.put(NettyConstants.SCALER_DOWN_STEP_PROPERTY, NettyConstants.SCALER_DOWN_STEP);
        configMap.put(NettyConstants.SCALER_CYCLES_PROPERTY, NettyConstants.SCALER_CYCLES);
    }

    @Before
    public void setup() {
        // Skip test if beta edition is not set
        Assume.assumeTrue(ProductInfo.getBetaEdition());
        testChannels = new ArrayList<Channel>();
        framework = new NettyFrameworkImpl();
        framework.setExecutorService(GlobalEventExecutor.INSTANCE);
        framework.activate(null, configMap);
        options = new HashMap<String, Object>();
    }

    @After
    public void tearDown() throws Exception {
        // Skip tear down if beta edition is not set
        if (!ProductInfo.getBetaEdition()) {
            System.out.println("Beta edition is disabled!! Skipping tests...");
            return;
        }
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
        options.put(TCPConfigurationImpl.LINGER, 10);
    }

    /**
     * Verify that a TCP boostrap can be created as expected
     *
     * @throws Exception
     */
    @Test
    public void testCreateTCPBootstrap() throws Exception {
        setTCPConfig();
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        Assert.assertTrue((boolean) bootstrap.config().options().get(ChannelOption.SO_REUSEADDR));
        Assert.assertEquals(10, bootstrap.config().childOptions().get(ChannelOption.SO_LINGER));
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
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        framework.setServerStarted(null);
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.startInbound(bootstrap, LOCALHOST, TCP_PORT, callback -> {
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
        BootstrapExtended bootstrap = framework.createUDPBootstrapInbound(options);
        bootstrap.handler(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
                // do nothing
            }
        });
        framework.setServerStarted(null);
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.startInbound(bootstrap, LOCALHOST, UDP_PORT, callback -> {
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
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        framework.setServerStarted(null);
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.startInbound(bootstrap, LOCALHOST, TCP_PORT, callback -> {
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
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        framework.setServerStarted(null);
        final CountDownLatch activeChannelLatch = new CountDownLatch(1);
        framework.startInbound(bootstrap, LOCALHOST, TCP_PORT, future -> {
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
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        final CountDownLatch connectionLatch = new CountDownLatch(3);
        framework.setServerStarted(null);
        framework.startInbound(bootstrap, LOCALHOST, TCP_PORT, future -> {
            if (future.isSuccess()) {
                testChannels.add(future.channel());
                connectionLatch.countDown();
            } else {
                Assert.fail("framework failed to start: " + future.cause().getMessage());
                connectionLatch.countDown();
            }
        });
        framework.startInbound(bootstrap, LOCALHOST, secondPort, future -> {
            if (future.isSuccess()) {
                testChannels.add(future.channel());
                connectionLatch.countDown();
            } else {
                Assert.fail("framework failed to start: " + future.cause().getMessage());
                connectionLatch.countDown();
            }
        });
        framework.startInbound(bootstrap, LOCALHOST, thirdPort, future -> {
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

    /**
     * Quiesce notification must not wait for accepted connections' event loops.
     * Use real registered channels because EmbeddedChannel delivers events inline.
     */
    @Test
    public void testQuiesceWithBlockedChildEventLoops() throws Exception {
        final int connectionCount = 128;
        Mockery context = new Mockery();
        ChannelFrameworkConfig channelConfig = context.mock(ChannelFrameworkConfig.class);
        context.checking(new Expectations() {{
            allowing(channelConfig).getDefaultChainQuiesceTimeout(); will(returnValue(60000L));
        }});
        framework.setChannelFWConfig(channelConfig);
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
        framework.setScheduledExecutorService(scheduler);
        ExecutorService stoppingExecutor = Executors.newSingleThreadExecutor();
        List<Socket> clients = new ArrayList<Socket>();
        List<Future<?>> blockedTasks = new ArrayList<Future<?>>();
        CountDownLatch releaseChildren = new CountDownLatch(1);
        CountDownLatch accepted = new CountDownLatch(connectionCount);
        CountDownLatch childEvents = new CountDownLatch(connectionCount);
        CountDownLatch endpointEvent = new CountDownLatch(1);
        Set<Channel> notified = ConcurrentHashMap.newKeySet();
        AtomicInteger invalidEvents = new AtomicInteger();
        Channel endpoint = null;
        try {
            ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
            bootstrap.childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    channel.pipeline().addLast(bootstrap.getBaseInitializer());
                    channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            accepted.countDown();
                            super.channelActive(ctx);
                        }

                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
                            if (event == QuiesceHandler.QUIESCE_EVENT) {
                                if (!ctx.channel().eventLoop().inEventLoop() || !notified.add(ctx.channel())) {
                                    invalidEvents.incrementAndGet();
                                }
                                childEvents.countDown();
                            }
                            super.userEventTriggered(ctx, event);
                        }
                    });
                }
            });
            framework.setServerStarted(null);
            CompletableFuture<Channel> listening = new CompletableFuture<Channel>();
            framework.startInbound(bootstrap, "127.0.0.1", 0, future -> {
                if (future.isSuccess()) {
                    listening.complete(future.channel());
                } else {
                    listening.completeExceptionally(future.cause());
                }
            });
            endpoint = listening.get(10, TimeUnit.SECONDS);
            framework.registerEndpointQuiesce(endpoint, () -> {
                endpointEvent.countDown();
                return null;
            });
            for (int i = 0; i < connectionCount; i++) {
                Socket client = new Socket();
                clients.add(client);
                client.connect((InetSocketAddress) endpoint.localAddress(), 10000);
            }
            Assert.assertTrue("Children did not become active", accepted.await(10, TimeUnit.SECONDS));
            Assert.assertEquals(connectionCount, framework.getActiveChannelsMap().get(endpoint).size());
            Set<EventLoop> childLoops = new HashSet<EventLoop>();
            for (Channel child : framework.getActiveChannelsMap().get(endpoint)) {
                Assert.assertTrue(child.isActive());
                childLoops.add(child.eventLoop());
            }
            CountDownLatch childrenBlocked = new CountDownLatch(childLoops.size());
            for (EventLoop loop : childLoops) {
                blockedTasks.add(loop.submit(() -> {
                    childrenBlocked.countDown();
                    releaseChildren.await();
                    return null;
                }));
            }
            Assert.assertTrue("Child loops did not reach the gate", childrenBlocked.await(10, TimeUnit.SECONDS));

            stoppingExecutor.submit(framework::serverStopping).get(10, TimeUnit.SECONDS);
            Assert.assertTrue(QuiesceState.isQuiesceInProgress());
            Assert.assertFalse(framework.isActive());
            Assert.assertEquals("Child events must still be queued", connectionCount, childEvents.getCount());
            Assert.assertTrue("Endpoint was not notified", endpointEvent.await(10, TimeUnit.SECONDS));
            Assert.assertTrue("Quiesce tasks were not scheduled", scheduler.getTaskCount() >= 2);

            releaseChildren.countDown();
            Assert.assertTrue("Children were not notified", childEvents.await(10, TimeUnit.SECONDS));
            for (EventLoop loop : childLoops) {
                loop.submit(() -> {}).get(10, TimeUnit.SECONDS);
            }
            Assert.assertEquals(connectionCount, notified.size());
            Assert.assertEquals("Duplicate event or wrong event-loop thread", 0, invalidEvents.get());
        } finally {
            releaseChildren.countDown();
            stoppingExecutor.shutdownNow();
            scheduler.shutdownNow();
            for (Socket client : clients) {
                client.close();
            }
            if (endpoint != null) {
                framework.stop(endpoint).get(10, TimeUnit.SECONDS);
            }
            for (Future<?> task : blockedTasks) {
                task.get(10, TimeUnit.SECONDS);
            }
            Assert.assertTrue(stoppingExecutor.awaitTermination(10, TimeUnit.SECONDS));
            Assert.assertTrue(scheduler.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
