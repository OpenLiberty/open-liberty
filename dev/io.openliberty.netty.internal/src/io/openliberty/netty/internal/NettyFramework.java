/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.wsspi.channelfw.ChannelConfiguration;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.openliberty.netty.internal.http.HttpServerInitializer;

/**
 * Sample (for now) implementation bundle for configuring and setting up Netty services
 */
@Component(configurationPid = "io.openliberty.netty.internal",
    immediate = true, 
    service = { NettyFramework.class, ServerQuiesceListener.class }, 
    property = { "service.vendor=IBM" })
public class NettyFramework implements ServerQuiesceListener {

    private ChannelGroup serverChannels = null;

    /** Reference to the executor service -- required */
    private ExecutorService executorService = null;

    /** server started logic borrowed from CHFWBundle */
    private static AtomicBoolean serverCompletelyStarted = new AtomicBoolean(false);
    private static Queue<Callable<?>> serverStartedTasks = new LinkedBlockingQueue<>();
    private static Object syncStarted = new Object() {
    }; // use brackets/inner class to make lock appear in dumps using class name

    /**
     * The TCP based bootstrap.
     */
    private ServerBootstrap serverBootstrap;

    /**
     * UDP based bootstrap.
     */
    private Bootstrap bootstrap;

    private EventLoopGroup parentGroup;
    private EventLoopGroup childGroup;
    private EventLoopGroup udpGroup;

    @Activate
    protected void activate(Map<String, Object> properties) {
        serverChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
//      udpGroup = new NioEventLoopGroup(0, executorService);

        /*
         * As a demo, this starts up a HTTP server at localhost:9081 which will return 
         * an 200 OK response to any request
         */
        // use the executor service provided by Liberty
        parentGroup = new NioEventLoopGroup(0, executorService);
        childGroup = new NioEventLoopGroup(0, executorService);

        serverBootstrap = createTCPBoostrap(new HttpServerInitializer(), parentGroup, childGroup);
        try {
            runWhenServerStarted(new Callable<Void>() {
                @Override
                public Void call() {
                    try {
                        finishInitServerSocket(serverBootstrap, 9081);
                    } catch (Exception x) {
                        // TODO Auto-generated catch block
                        x.printStackTrace();
                    }
                    return null;
                }
            });
        } catch (Exception x) {
            // TODO Auto-generated catch block
            x.printStackTrace();
        }

    }

    @Deactivate
    protected void deactivate(Map<String, Object> properties, int reason) {
        shutdown();
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        shutdown();
        activate(config);
    }
    
    /**
     * DS method for setting the executor service reference.
     *
     * @param executorService the {@link java.util.concurrent.ExecutorService} to
     *                            queue work to.
     */
    @Reference(service = ExecutorService.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * DS method for clearing the executor service reference.
     * This is a required reference, will be called after deactivate.
     *
     * @param executorService the service instance to clear
     */
    protected void unsetExecutorService(ExecutorService executorService) {
    }

    /**
     * Creates a UDP Bootstrap with the given ChannelInitializer
     * @return Bootstrap
     */
    private Bootstrap createUDPBootstrap(ChannelInitializer<Channel> initializer) {
        return new Bootstrap()
                .group(udpGroup)
                .channel(NioDatagramChannel.class)
                .handler(initializer);
    }

    /**
     * Creates a NIO TCP ServerBootstrap with the given ChannelInitializer
     * @return ServerBootstrap
     */
    private ServerBootstrap createTCPBoostrap(ChannelInitializer<Channel> initializer, EventLoopGroup parentGroup, 
            EventLoopGroup childGroup) {
        return new ServerBootstrap()
                .group(parentGroup, childGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(initializer)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    private void finishInitServerSocket(ServerBootstrap bs, int port) {
        try {
            bs.bind(new InetSocketAddress(port)).sync();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void addServerChannel(Channel channel) {
        serverChannels.add(channel);
    }

    public void shutdown() {
        try {
            serverChannels.close().sync();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (parentGroup != null) {
                parentGroup.shutdownGracefully();
            }
            if (childGroup != null) {
                childGroup.shutdownGracefully();
            }
        }
    }

    @Override
    public void serverStopping() {
        shutdown();
    }

    /**
     * Declarative services method that is invoked once the server is started.
     * Only after this method is invoked is the initial polling for
     * persistent tasks performed.
     * 
     * From CHFWBundle
     *
     * @param ref reference to the ServerStarted service
     */
    @Reference(service = ServerStarted.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.OPTIONAL,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setServerStarted(ServiceReference<ServerStarted> ref) {
        // set will be called when the ServerStarted service has been registered (by the FeatureManager as of 9/2015).  This is a signal that
        // the server is fully started, but before the "smarter planet" message has been output. Use this signal to run tasks, mostly likely tasks that will
        // finish the port listening logic, that need to run at the end of server startup

        Callable<?> task;
        while ((task = serverStartedTasks.poll()) != null) {
            try {
                task.call();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        synchronized (syncStarted) {
            serverCompletelyStarted.set(true);
            syncStarted.notifyAll();
        }
    }

    /**
     * Method is called to run a task if the server has already started, if the server has not started that task is queue to be run when the server start signal
     * has been received.
     *
     * @param callable - task to run
     * @return Callable return null if the task was not ran, but queued, else return the task to denote it has ran.
     * @throws Exception
     */
    public static <T> T runWhenServerStarted(Callable<T> callable) throws Exception {
        synchronized (syncStarted) {
            if (!serverCompletelyStarted.get()) {
                serverStartedTasks.add(callable);
                return null;
            }
        }
        return callable.call();
    }

    /*
     * If the server has not completely started, then wait until it has been.
     * The server will be "completely" stated when the server start signal has been received and any task waiting on that signal before running have now been run.
     */
    @FFDCIgnore({ InterruptedException.class })
    public static void waitServerCompletelyStarted() {
        synchronized (syncStarted) {
            if (serverCompletelyStarted.get() == false) {
                try {
                    syncStarted.wait();
                } catch (InterruptedException x) {
                    // assume we can go one then
                }
            }
        }
        return;
    }

    /**
     * non-blocking method to return the state of server startup with respect to the server being completely started.
     * The server will be "completely" stated when the server start signal has been received and any task waiting on that signal before running have now been run.
     *
     * @return
     */
    public static boolean isServerCompletelyStarted() {
        return serverCompletelyStarted.get();
    }

    /**
     * Declarative Services method for unsetting the ServerStarted service
     *
     * @param ref reference to the service
     */
    protected synchronized void unsetServerStarted(ServiceReference<ServerStarted> ref) {
        // server is shutting down
    }

}
