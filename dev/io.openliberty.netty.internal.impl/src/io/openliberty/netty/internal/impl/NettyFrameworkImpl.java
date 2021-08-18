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
package io.openliberty.netty.internal.impl;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.ServerBootstrapConfiguration;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tcp.TCPChannelInitializerImpl;
import io.openliberty.netty.internal.tcp.TCPChannelMessageConstants;
import io.openliberty.netty.internal.tcp.TCPConfigConstants;
import io.openliberty.netty.internal.tcp.TCPConfigurationImpl;

/**
 * Liberty NettyFramework implementation bundle
 */
@Component(configurationPid = "io.openliberty.netty.internal",
    immediate = true, 
    service = { NettyFramework.class, ServerQuiesceListener.class }, 
    property = { "service.vendor=IBM" })
public class NettyFrameworkImpl implements ServerQuiesceListener, NettyFramework {

    private static final TraceComponent tc = Tr.register(NettyFrameworkImpl.class, TCPChannelMessageConstants.NETTY_TRACE_NAME, TCPChannelMessageConstants.NETTY_BUNDLE);

    /** Reference to the executor service -- required */
    private ExecutorService executorService = null;

    /** server started logic borrowed from CHFWBundle */
    private static AtomicBoolean serverCompletelyStarted = new AtomicBoolean(false);
    private static Queue<Callable<?>> serverStartedTasks = new LinkedBlockingQueue<>();
    private static Object syncStarted = new Object() {
    }; // use brackets/inner class to make lock appear in dumps using class name
    Set<Channel> activeChannels = ConcurrentHashMap.newKeySet();

    private EventLoopGroup parentGroup;
    private EventLoopGroup childGroup;
    
    private CHFWBundle chfw;
    private volatile boolean isActive = false;
    private final int timeBetweenRetriesMsec = 1000; // make this non-configurable

    
    /**
     * DS method for setting the required channel framework service.
     * TODO: for now this reference is needed for access to EndPointMgr. That code will be split out.
     *
     * @param bundle
     */
    @Reference(name = "chfwBundle")
    protected void setChfwBundle(CHFWBundle bundle) {
        chfw = bundle;
    }

    /**
     * This is a required static reference, this won't
     * be called until the component has been deactivated
     *
     * @param bundle CHFWBundle instance to unset
     */
    protected void unsetChfwBundle(CHFWBundle bundle) {}

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) {
        // use the executor service provided by Liberty
        parentGroup = new NioEventLoopGroup(1, executorService);
        childGroup = new NioEventLoopGroup(0, executorService);
    }

    @Deactivate
    protected void deactivate(ComponentContext context, Map<String, Object> properties) {
        serverStopping();
    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Processing config", config);
        }
        // TODO: update any framework-specific config
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
        this.executorService = null;
    }

    /**
     * When notified that the server is going to stop, pre-quiesce all chains in the runtime.
     * This will be called before services start getting torn down..
     *
     * @see com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener#serverStopping()
     */
    @Override
    public void serverStopping() {
        if (isActive) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Destroying all endpoints");
            }
            isActive = false;
            // If the system is configured to quiesce connections..
            long timeout = getDefaultChainQuiesceTimeout();

            // set up future to log channel stopped messages
            for (Channel channel : activeChannels) {
                channel.closeFuture().addListener(future -> {
                    logChannelStopped(channel);
                });
            }
            Future<?> parent = null;
            Future<?> child = null;
            if (parentGroup != null) {
                parent = parentGroup.shutdownGracefully(0, timeout, TimeUnit.MILLISECONDS);
            }
            if (childGroup != null) {
                child = childGroup.shutdownGracefully(0, timeout, TimeUnit.MILLISECONDS);
            }
            if (parent != null) {
                parent.awaitUninterruptibly();
            }
            if (child != null) {
                child.awaitUninterruptibly();
            }
        }
    }

    /**
     * Declarative services method that is invoked once the server is started.
     * Only after this method is invoked is the initial polling for
     * persistent tasks performed.
     * 
     * {@See CHFWBundle}
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
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Netty Framework signaled- Server Completely Started signal received");
        }
        while ((task = serverStartedTasks.poll()) != null) {
            try {
                task.call();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "caught exception performing late cycle server startup task: " + e);
                }
            }
        }

        synchronized (syncStarted) {
            serverCompletelyStarted.set(true);
            isActive = true;
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
    
    @Override
    public ServerBootstrapExtended createTCPBootstrap(Map<String, Object> tcpOptions) throws NettyException {
        ServerBootstrapConfiguration config = new TCPConfigurationImpl(tcpOptions, true);
        ServerBootstrapExtended bs = new ServerBootstrapExtended();
        bs.group(parentGroup, childGroup);
        bs.channel(NioServerSocketChannel.class);
        // apply the existing user config to the Netty TCP channel
        bs.applyConfiguration(config);
        ChannelInitializerWrapper tcpInitializer = new TCPChannelInitializerImpl(config);
        bs.setBaseInitializer(tcpInitializer);
        return bs;
    }

    @Override
    public Bootstrap createUDPBootstrap(Map<String, Object> options) throws NettyException {
        Bootstrap bs = new Bootstrap();
        bs.group(parentGroup);
        // TODO: parse any UDP options and set them as ChannelOptions
        bs.channel(NioDatagramChannel.class);
        return bs;
    }

    private ChannelFuture bindHelper(ServerBootstrapExtended bootstrap, String inetHost, int inetPort, ChannelFutureListener bindListener, final int retryCount) {
        ChannelFuture bindFuture = bootstrap.bind(inetHost, inetPort);
        if (bindListener != null) {
            bindFuture.addListener(bindListener);
        }
        bindFuture.addListener(future -> {
            if (future.isSuccess()) {
                activeChannels.add(bindFuture.channel());
                TCPConfigurationImpl config = (TCPConfigurationImpl) bootstrap.getConfiguration();
                String channelName = config.getExternalName();
                // set common channel attrs
                bindFuture.channel().attr(TCPConfigConstants.TCPNameKey).set(config.getExternalName());
                bindFuture.channel().attr(TCPConfigConstants.TCPHostKey).set(inetHost);
                bindFuture.channel().attr(TCPConfigConstants.TCPPortKey).set(inetPort);

                Tr.info(tc, TCPChannelMessageConstants.TCP_CHANNEL_STARTED, new Object[] { channelName, inetHost, String.valueOf(inetPort) });
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "bindHelper failed due to: " + future.cause().getMessage());
                }

                if (retryCount > 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "attempt to bind again after a wait of " +timeBetweenRetriesMsec +"ms; " + retryCount + " attempts remaining");
                    }
                    // recurse until we either complete successfully or run out of retries;
                    try {
                        Thread.sleep(timeBetweenRetriesMsec);
                    } catch (InterruptedException x) {
                        // do nothing but debug
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "sleep caught InterruptedException.  will proceed.");
                        }
                    }
                    bindHelper(bootstrap, inetHost, inetPort, bindListener, retryCount - 1);
                }
                String channelName = bindFuture.channel().attr(TCPConfigConstants.TCPNameKey).get();
                Tr.error(tc, TCPChannelMessageConstants.BIND_ERROR,
                        new Object[] { channelName, inetHost, String.valueOf(inetPort), 
                                bindFuture.cause().getMessage() });
            }
        });
        return bindFuture;
    }

    @Override
    public ChannelFuture start(ServerBootstrapExtended bootstrap, String inetHost, int inetPort, ChannelFutureListener bindListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "start (TCP): attempt to bind a channel at host " + inetHost + " port " + inetPort);
        }
        try {
            runWhenServerStarted(new Callable<ChannelFuture>() {
                @Override
                public ChannelFuture call() {
                    TCPConfigurationImpl config = (TCPConfigurationImpl) bootstrap.getConfiguration();
                    int bindRetryCount = config.getPortOpenRetries();
                    return bindHelper(bootstrap, inetHost, inetPort, bindListener, bindRetryCount);
                }
            });
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "caught exception performing late cycle server startup task: " + e);
            }
        }
        return null;
    }

    @Override
    public ChannelFuture start(Bootstrap bootstrap, String inetHost, int inetPort, ChannelFutureListener bindListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "start (UDP): attempt to bind a channel at host " + inetHost + " port " + inetPort);
        }
        ChannelFuture bindFuture = bootstrap.bind(inetHost, inetPort);
        if (bindListener != null) {
            bindFuture.addListener(bindListener);
        }
        return bindFuture;
    }

    @Override
    public ChannelFuture stop(Channel channel) {
        if (isActive && channel.isOpen()) {
            ChannelFuture closeFuture = channel.close();
            // TODO: should we wait until close actually completes to remove?
            activeChannels.remove(closeFuture.channel());
            closeFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    // TODO: error/warning?
                } else {
                    logChannelStopped(channel);
                }
            });
            return closeFuture;
        } else {
            return null;
        }
    }

    @Override
    public void stop(Channel channel, long timeout) {
        if (this.isActive && channel.isOpen()) {
            if (timeout == -1) {
                timeout = getDefaultChainQuiesceTimeout();
            }
            stop(channel).awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);
            activeChannels.remove(channel);
            logChannelStopped(channel);
        }
    }

    @Override
    public Set<Channel> getActiveChannels() {
        return activeChannels;
    }

    @Override
    public long getDefaultChainQuiesceTimeout() {
        // TODO: move this cfg out of the channelfw 
        if (chfw != null) {
            return chfw.getFramework().getDefaultChainQuiesceTimeout();
        } else {
            return 0;
        }
    }

    @Override
    public void destroy() {
        // destroy covered by serverStopping
    }

    private void logChannelStopped(String name, String host, Integer port) {
        Tr.info(tc, TCPChannelMessageConstants.TCP_CHANNEL_STOPPED, name, host, String.valueOf(port));
    }

    private void logChannelStopped(Channel channel) {
        String channelName = channel.attr(TCPConfigConstants.TCPNameKey).get();
        String host = channel.attr(TCPConfigConstants.TCPHostKey).get();
        Integer port = channel.attr(TCPConfigConstants.TCPPortKey).get();
        logChannelStopped(channelName, host, port);
    }
}
