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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * An outbound connect must not run on the thread that called startOutbound(), otherwise the caller
     * is blocked for the whole connect and retry sequence. The open listener has to be driven from the
     * framework's executor instead. If the connect ran on the caller's thread, the listener would too.
     *
     * @throws Exception
     */
    @Test
    public void testStartOutboundDoesNotRunConnectOnCallerThread() throws Exception {
        framework.setServerStarted(null);
        try (ServerSocket target = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            BootstrapExtended bootstrapOutbound = framework.createTCPBootstrapOutbound(null);
            bootstrapOutbound.handler(new SimpleChannelInboundHandler<ByteBuf>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
                    // do nothing
                }
            });
            final AtomicReference<Thread> listenerThread = new AtomicReference<Thread>();
            final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
            final CountDownLatch listenerLatch = new CountDownLatch(1);
            framework.startOutbound(bootstrapOutbound, "127.0.0.1", target.getLocalPort(), future -> {
                listenerThread.set(Thread.currentThread());
                if (future.isSuccess()) {
                    testChannels.add(future.channel());
                } else {
                    failure.set(future.cause());
                }
                listenerLatch.countDown();
            });
            Assert.assertTrue("The open listener was not called", listenerLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            Assert.assertNull("The outbound connect failed: " + failure.get(), failure.get());
            Assert.assertNotSame("The outbound connect ran on the caller's thread", Thread.currentThread(), listenerThread.get());
        }
    }

    /**
     * If the thread running an inbound open is interrupted, the open listener must still be told that the
     * bind failed. Otherwise the owner of the channel (for example NettyChain) never leaves STARTING.
     *
     * The port is held by a plain ServerSocket so the bind fails and open() waits between retries. The
     * thread is interrupted during that wait.
     *
     * @throws Exception
     */
    @Test
    public void testInterruptedInboundOpenNotifiesListener() throws Exception {
        setTCPConfig();
        options.put(TCPConfigurationImpl.PORT_OPEN_RETRIES, 5);
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        framework.setServerStarted(null);
        try (ServerSocket blocker = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
            final AtomicReference<Throwable> openerFailure = new AtomicReference<Throwable>();
            final CountDownLatch listenerLatch = new CountDownLatch(1);
            Thread opener = new Thread(() -> {
                try {
                    framework.startInbound(bootstrap, "127.0.0.1", blocker.getLocalPort(), future -> {
                        cause.set(future.cause());
                        listenerLatch.countDown();
                    });
                } catch (Throwable t) {
                    openerFailure.set(t);
                }
            }, "interrupted-open-test");
            opener.start();

            // open() sleeps between bind attempts, which is the only timed wait on this path.
            long deadline = System.currentTimeMillis() + PORT_BIND_TIMEOUT_MS;
            while (opener.getState() != Thread.State.TIMED_WAITING && opener.isAlive() && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            Assert.assertEquals("The bind should be waiting to retry", Thread.State.TIMED_WAITING, opener.getState());
            opener.interrupt();

            Assert.assertTrue("The open listener was not called after the interrupt", listenerLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            opener.join(PORT_BIND_TIMEOUT_MS);
            Assert.assertNull("startInbound threw: " + openerFailure.get(), openerFailure.get());
            Assert.assertTrue("Expected the interrupt to be reported, but got: " + cause.get(), cause.get() instanceof InterruptedException);
        }
    }

    /**
     * An inbound open requested before the server has completely started must be deferred: the channel is
     * registered but not bound, and the listener is not called. When the server-started signal arrives the bind
     * runs on the thread that delivered the signal (not on an executor thread), and has finished by the time
     * setServerStarted returns.
     *
     * @throws Exception
     */
    @Test
    public void testInboundOpenDeferredUntilServerStarted() throws Exception {
        setTCPConfig();
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        final AtomicReference<Thread> listenerThread = new AtomicReference<Thread>();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final CountDownLatch listenerLatch = new CountDownLatch(1);
        // The framework state for "server started" is static, so always leave it started again.
        framework.unsetServerStarted(null);
        try {
            // Port 0: the OS picks a free port, so the test cannot collide with another process.
            Channel channel = framework.startInbound(bootstrap, "127.0.0.1", 0, future -> {
                listenerThread.set(Thread.currentThread());
                failure.set(future.cause());
                listenerLatch.countDown();
            });
            Assert.assertNotNull("startInbound should return the registered channel", channel);
            Assert.assertFalse("The bind must wait for the server-started signal", listenerLatch.await(500, TimeUnit.MILLISECONDS));
            Assert.assertFalse("The channel must not be bound before the server has started", channel.isActive());

            framework.setServerStarted(null);

            // The inbound bind is run in-thread by setServerStarted, so it is complete when that returns.
            Assert.assertTrue("The open listener was not called", listenerLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            Assert.assertNull("The deferred bind failed: " + failure.get(), failure.get());
            Assert.assertSame("The deferred inbound bind should run on the thread that signalled server started",
                              Thread.currentThread(), listenerThread.get());
            Assert.assertTrue(channel.isActive());
            Assert.assertTrue(framework.getActiveChannels().contains(channel));
            testChannels.add(channel);
        } finally {
            framework.setServerStarted(null);
        }
    }

    /**
     * Tasks queued before server start run, in order, on the thread that signals server started. A task may
     * queue another task while it runs (this is what a chain does if it is updated during startup) without
     * deadlocking, and a task that throws must not stop the remaining tasks or the server being marked started.
     *
     * @throws Exception
     */
    @Test
    public void testServerStartedTasksRunOnSignalThread() throws Exception {
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        final Set<Thread> threads = ConcurrentHashMap.newKeySet();
        framework.unsetServerStarted(null);
        try {
            FutureTask<ChannelFuture> first = framework.runWhenServerStarted(() -> {
                order.add("first");
                threads.add(Thread.currentThread());
                // Queued from inside a running task: it is run by the same signal, after this one.
                framework.runWhenServerStarted(() -> {
                    order.add("nested");
                    threads.add(Thread.currentThread());
                    return null;
                });
                return null;
            });
            FutureTask<ChannelFuture> failing = framework.runWhenServerStarted(() -> {
                order.add("failing");
                throw new IllegalStateException("expected by the test");
            });
            FutureTask<ChannelFuture> last = framework.runWhenServerStarted(() -> {
                order.add("last");
                threads.add(Thread.currentThread());
                return null;
            });
            Assert.assertTrue("Tasks must be queued, not run, before server start", order.isEmpty());
            Assert.assertFalse(first.isDone());
            Assert.assertFalse(failing.isDone());
            Assert.assertFalse(last.isDone());
            Assert.assertFalse(NettyFrameworkImpl.isServerCompletelyStarted());

            // Run the signal off the test thread so a deadlock fails the test instead of hanging it.
            final Thread signalThread = new Thread(() -> framework.setServerStarted(null), "server-started-signal-test");
            signalThread.start();
            signalThread.join(PORT_BIND_TIMEOUT_MS);
            Assert.assertFalse("setServerStarted did not return (deadlock?)", signalThread.isAlive());

            Assert.assertEquals(Arrays.asList("first", "failing", "last", "nested"), order);
            Assert.assertTrue(first.isDone());
            Assert.assertTrue(failing.isDone());
            Assert.assertTrue(last.isDone());
            Assert.assertEquals("All tasks should run on the signalling thread", Collections.singleton(signalThread), threads);
            Assert.assertTrue(NettyFrameworkImpl.isServerCompletelyStarted());
            Assert.assertTrue(framework.isActive());

            // Once started, a new task runs immediately on the caller's thread.
            final AtomicReference<Thread> lateThread = new AtomicReference<Thread>();
            framework.runWhenServerStarted(() -> {
                lateThread.set(Thread.currentThread());
                return null;
            });
            Assert.assertSame(Thread.currentThread(), lateThread.get());
        } finally {
            framework.setServerStarted(null);
        }
    }

    /**
     * An open requested while the framework is stopping (server started, framework no longer active) is
     * ignored: no channel is returned and the listener is not called.
     *
     * @throws Exception
     */
    @Test
    public void testStartInboundReturnsNullWhenStopping() throws Exception {
        setTCPConfig();
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        // Server started is static state and marks "framework" active. A second instance that has never been
        // signalled is inactive, so it is "stopping" without needing quiesce or a channel framework config.
        framework.setServerStarted(null);
        NettyFrameworkImpl stopping = new NettyFrameworkImpl();
        Assert.assertTrue("Test setup: the framework should be stopping", stopping.isStopping());

        final AtomicBoolean listenerCalled = new AtomicBoolean(false);
        Channel channel = stopping.startInbound(bootstrap, "127.0.0.1", 0, future -> listenerCalled.set(true));
        Assert.assertNull("No channel should be started while stopping", channel);
        Assert.assertFalse("The listener should not be called while stopping", listenerCalled.get());
    }

    /**
     * A bind that fails because something else holds the port must report the failure to the open listener
     * (this is what takes a chain out of STARTING) and must not leave an active channel behind. Only the
     * failure is asserted: the exception type and message differ between NIO, epoll, kqueue and between
     * operating systems.
     *
     * @throws Exception
     */
    @Test
    public void testInboundBindFailureNotifiesListener() throws Exception {
        assertBindFailsWhilePortIsHeld(false);
    }

    /**
     * With soReuseAddr enabled, a port held by a live listener is a real conflict, not a TIME_WAIT leftover,
     * so SO_REUSEADDR must not make the bind succeed.
     *
     * @throws Exception
     */
    @Test
    public void testInboundBindFailsOnActiveListenerWithReuseAddr() throws Exception {
        assertBindFailsWhilePortIsHeld(true);
    }

    private void assertBindFailsWhilePortIsHeld(boolean reuseAddr) throws Exception {
        setTCPConfig();
        options.put(TCPConfigurationImpl.PORT_OPEN_RETRIES, 0);
        options.put(TCPConfigurationImpl.REUSE_ADDR, Boolean.toString(reuseAddr));
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        framework.setServerStarted(null);
        try (ServerSocket blocker = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            final AtomicReference<ChannelFuture> result = new AtomicReference<ChannelFuture>();
            final CountDownLatch listenerLatch = new CountDownLatch(1);
            Channel channel = framework.startInbound(bootstrap, "127.0.0.1", blocker.getLocalPort(), future -> {
                result.set(future);
                listenerLatch.countDown();
            });
            Assert.assertTrue("The open listener was not called", listenerLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            Assert.assertFalse("The bind should have failed because the port is in use", result.get().isSuccess());
            Assert.assertNotNull(result.get().cause());
            Assert.assertNotNull(channel);
            Assert.assertFalse("A channel whose bind failed must not be active", channel.isActive());
            Assert.assertFalse(framework.getActiveChannels().contains(channel));
            if (reuseAddr) {
                // Proves the probe ran and found the live listener, so the SO_REUSEADDR=true retry was skipped.
                Assert.assertTrue("Expected the probe to detect the active listener",
                                  outputMgr.checkForLiteralTrace("real port conflict"));
            }
        }
    }

    /**
     * With portOpenRetries, a bind that fails because the port is busy is retried and succeeds once the port is
     * released, and only then is the listener told. The release happens while open() is waiting between
     * attempts. Listener success is the assertion; the margins are wide because the retry interval is one second.
     *
     * @throws Exception
     */
    @Test
    public void testInboundBindRetriesUntilPortReleased() throws Exception {
        setTCPConfig();
        options.put(TCPConfigurationImpl.PORT_OPEN_RETRIES, 10);
        // Without reuse, the retry is plain portOpenRetries and does not depend on TIME_WAIT handling.
        options.put(TCPConfigurationImpl.REUSE_ADDR, "false");
        ServerBootstrapExtended bootstrap = framework.createTCPBootstrapInbound(options);
        bootstrap.childHandler(bootstrap.getBaseInitializer());
        framework.setServerStarted(null);

        ServerSocket blocker = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        final int port = blocker.getLocalPort();
        final AtomicBoolean released = new AtomicBoolean(false);
        final AtomicBoolean boundWhileHeld = new AtomicBoolean(false);
        final AtomicReference<ChannelFuture> result = new AtomicReference<ChannelFuture>();
        final CountDownLatch listenerLatch = new CountDownLatch(1);
        Thread releaser = new Thread(() -> {
            try {
                Thread.sleep(1500);
                released.set(true);
                blocker.close();
            } catch (Exception e) {
                // the test fails below because the port is never released
            }
        }, "port-releaser-test");
        try {
            releaser.start();
            // Runs the whole retry loop on this thread; returns when the bind has succeeded or given up.
            Channel channel = framework.startInbound(bootstrap, "127.0.0.1", port, future -> {
                boundWhileHeld.set(future.isSuccess() && !released.get());
                result.set(future);
                listenerLatch.countDown();
            });
            Assert.assertTrue("The open listener was not called", listenerLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            Assert.assertTrue("The bind should succeed after the port is released, but: " + result.get().cause(), result.get().isSuccess());
            Assert.assertFalse("The bind succeeded while the port was still held", boundWhileHeld.get());
            Assert.assertTrue(channel.isActive());
            Assert.assertTrue(framework.getActiveChannels().contains(channel));
            testChannels.add(channel);
        } finally {
            releaser.join(PORT_BIND_TIMEOUT_MS);
            blocker.close();
        }
    }

    /**
     * An outbound connect that fails (nothing is listening) must still notify the listener, from a framework
     * executor thread, and startOutbound itself must return without waiting for the outcome.
     *
     * @throws Exception
     */
    @Test
    public void testOutboundConnectFailureNotifiesListener() throws Exception {
        framework.setServerStarted(null);
        // Take a free port, then close it so the connect is refused.
        int closedPort;
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            closedPort = s.getLocalPort();
        }
        BootstrapExtended bootstrapOutbound = framework.createTCPBootstrapOutbound(null);
        bootstrapOutbound.handler(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
                // do nothing
            }
        });
        final AtomicReference<ChannelFuture> result = new AtomicReference<ChannelFuture>();
        final AtomicReference<Thread> listenerThread = new AtomicReference<Thread>();
        final CountDownLatch listenerLatch = new CountDownLatch(1);
        Channel channel = framework.startOutbound(bootstrapOutbound, "127.0.0.1", closedPort, future -> {
            listenerThread.set(Thread.currentThread());
            result.set(future);
            listenerLatch.countDown();
        });
        Assert.assertNotNull(channel);
        Assert.assertTrue("The open listener was not called after the connect failed", listenerLatch.await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        Assert.assertFalse("The connect should have failed", result.get().isSuccess());
        Assert.assertNotSame("The outbound connect ran on the caller's thread", Thread.currentThread(), listenerThread.get());
        Assert.assertFalse(channel.isActive());
    }
}
