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

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
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
import com.ibm.ws.channelfw.internal.chains.EndPointMgrImpl;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tcp.TCPConfigurationImpl;
import io.openliberty.netty.internal.tcp.TCPUtils;
import io.openliberty.netty.internal.udp.UDPUtils;
import com.ibm.websphere.channelfw.EndPointMgr;

/**
 * Liberty NettyFramework implementation bundle
 */
@Component(configurationPid = "io.openliberty.netty.internal", immediate = true, service = { NettyFramework.class,
        ServerQuiesceListener.class }, property = { "service.vendor=IBM" })
public class NettyFrameworkImpl implements ServerQuiesceListener, NettyFramework {

    private static final TraceComponent tc = Tr.register(NettyFrameworkImpl.class, NettyConstants.NETTY_TRACE_NAME,
            NettyConstants.BASE_BUNDLE);

    /** Reference to the executor service -- required */
    private ExecutorService executorService = null;

    /** server started logic borrowed from CHFWBundle */
    private static AtomicBoolean serverCompletelyStarted = new AtomicBoolean(false);
    private static Queue<FutureTask<ChannelFuture>> serverStartedTasks = new LinkedBlockingQueue<>();
    private static Object syncStarted = new Object() {
    }; // use brackets/inner class to make lock appear in dumps using class name

    private Map<Channel, ChannelGroup> activeChannelMap = new ConcurrentHashMap<Channel, ChannelGroup>();
        
    // TODO: Should we use this or maybe the event loop on activate?
    private ChannelGroup outboundConnections = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private EventLoopGroup parentGroup;
    private EventLoopGroup childGroup;

    private CHFWBundle chfw;
    private volatile boolean isActive = false;

	private ScheduledExecutorService scheduledExecutorService = null;


	@Activate
	protected void activate(ComponentContext context, Map<String, Object> config) {
		// Ideally use the executor service provided by Liberty
		// Compared to channelfw, quiesce is hit every time because
		// connections are lazy cleaned on deactivate
		parentGroup = new NioEventLoopGroup(1);
		// specify 0 for the "default" number of threads,
		// (java.lang.Runtime.availableProcessors() * 2)
		childGroup = new NioEventLoopGroup(0);
	}

	@Deactivate
	protected void deactivate(ComponentContext context, Map<String, Object> properties) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(this, tc, "Deactivate called", new Object[] {context, properties});
		}
		EndPointMgrImpl.destroyEndpoints();
		stopEventLoops();
	}

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Processing config", config);
        }
        // update any framework-specific config
    }
    
    /**
     * DS method for setting the required channel framework service. For now
     * this reference is needed for access to EndPointMgr. That code will be split
     * out.
     *
     * @param bundle
     */
    @Reference(name = "chfwBundle")
    protected void setChfwBundle(CHFWBundle bundle) {
        chfw = bundle;
    }

    /**
     * This is a required static reference, this won't be called until the component
     * has been deactivated
     *
     * @param bundle CHFWBundle instance to unset
     */
    protected void unsetChfwBundle(CHFWBundle bundle) {
    }

    /**
     * DS method for setting the executor service reference.
     *
     * @param executorService the {@link java.util.concurrent.ExecutorService} to
     *                        queue work to.
     */
    @Reference(service = ExecutorService.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setExecutorService(ExecutorService executorService) {
    	this.executorService = executorService;
    }

    /**
     * DS method for clearing the executor service reference. This is a required
     * reference, will be called after deactivate.
     *
     * @param executorService the service instance to clear
     */
    protected void unsetExecutorService(ExecutorService executorService) {
    	this.executorService = null;
    }

    /**
     * DS method for setting the scheduled executor service reference.
     *
     * @param scheduledExecutorService the {@link java.util.concurrent.ScheduledExecutorService} to
     *                        queue work to.
     */
    @Reference(service = ScheduledExecutorService.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService  = scheduledExecutorService;
    }

    /**
     * DS method for clearing the executor service reference. This is a required
     * reference, will be called after deactivate.
     *
     * @param executorService the service instance to clear
     */
    protected void unsetScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = null;
    }
    
    /**
     * Returns whether the framework is active
     * 
     */
    public boolean isActive(){
    	return this.isActive;
    }
    
    /**
     * Returns whether the framework has been issued to stop
     * 
     */
    public boolean isStopping(){
    	return isServerCompletelyStarted() && !this.isActive();
    } 

    /**
     * When notified that the server is going to stop, pre-quiesce all chains in the
     * runtime. This will be called before services start getting torn down..
     *
     * @see com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener#serverStopping()
     */
    @Override
    public void serverStopping() {
    	if (isActive) {
    		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
    			Tr.event(this, tc, "Destroying all endpoints (closing all channels): " + activeChannelMap.keySet());
    		}
    		isActive = false;
    		// If the system is configured to quiesce connections..
    		long timeout = getDefaultChainQuiesceTimeout();

    		if(timeout > 0) {
                if(activeChannelMap.isEmpty() && outboundConnections.isEmpty()){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    					Tr.debug(tc, "No connections to clean up, skipping quiesce creation.");
    				}
                    return;
                }
    			NettyQuiesceListener quiesce = new NettyQuiesceListener(this, scheduledExecutorService, timeout);
    			try {
    				// Go through active endpoints and stop accepting connections
    				for (Channel channel : activeChannelMap.keySet()) {
    					// Fire custom user event to let know that the endpoint is being stopped
    					channel.pipeline().fireUserEventTriggered(QuiesceHandler.QUIESCE_EVENT);
    				}
    				// Schedule quiesce tasks
    				quiesce.startTasks();
    			} catch (Exception e) {
    				if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
    					//TODO: change to same log used in traditional channel.
    					Tr.event(this, tc, "Exception occurred on quiesce", e);
    				}
    			}
    		}
    	}
    }
    

    private void stopEventLoops() {
    	Future<?> parent = null;
    	Future<?> child = null;
    	Future<?> global = null;
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Gracefully shutting down parentGroup Event Loop " + parentGroup);
        }
    	if (parentGroup != null) {
    		parent = parentGroup.shutdownGracefully();
    	}
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Gracefully shutting down childGroup Event Loop " + childGroup);
        }
    	if (childGroup != null) {
    		child = childGroup.shutdownGracefully();
    	}

    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Gracefully shutting down GlobalEventExecutor " + GlobalEventExecutor.INSTANCE);
        }
    	global = GlobalEventExecutor.INSTANCE.shutdownGracefully();
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Waiting for parentGroup Event Loop shutdown...");
        }
    	if (parent != null) {
    		parent.awaitUninterruptibly();
    	}
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Waiting for childGroup Event Loop shutdown...");
        }
    	if (child != null) {
    		child.awaitUninterruptibly();
    	}
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Waiting for GlobalEventExecutor shutdown...");
        }
    	if (global != null) {
    		global.awaitUninterruptibly();
    	}
    	
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Event loops finished clean up!");
        }
    }

    

    /**
     * Declarative services method that is invoked once the server is started. Only
     * after this method is invoked is the initial polling for persistent tasks
     * performed.
     * 
     * {@See CHFWBundle}
     *
     * @param ref reference to the ServerStarted service
     */
    @Reference(service = ServerStarted.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    protected void setServerStarted(ServiceReference<ServerStarted> ref) {
        // set will be called when the ServerStarted service has been registered (by the
        // FeatureManager as of 9/2015). This is a signal that
        // the server is fully started, but before the "smarter planet" message has been
        // output. Use this signal to run tasks, mostly likely tasks that will
        // finish the port listening logic, that need to run at the end of server
        // startup
    	System.out.println("Started setServerStarted");
        FutureTask<ChannelFuture> task;
        CountDownLatch latch = new CountDownLatch(serverStartedTasks.size());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Netty Framework signaled- Server Completely Started signal received");
        }
        synchronized (syncStarted) {
	        while ((task = serverStartedTasks.poll()) != null) {
	            try {
	            	if(!task.isCancelled()) {
	            		executorService.submit(new StartTaskRunnable(task, latch));
	            	}else
	            		latch.countDown();
	            } catch (Exception e) {
	                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	                    Tr.debug(tc, "caught exception performing late cycle server startup task: " + e);
	                }
	            }
	        }
	        
	        try {
	        	latch.await();
			} catch (InterruptedException e) {
				// TODO: handle exception
				System.out.println("Got interrupted exception");
				throw new RuntimeException(e);
			}
        
            serverCompletelyStarted.set(true);
            isActive = true;
            syncStarted.notifyAll();
        }
        System.out.println("Finished setServerStarted");
    }
    
    private class StartTaskRunnable implements Runnable{
    	
    	private FutureTask<ChannelFuture> task;
		private CountDownLatch latch;

		public StartTaskRunnable(FutureTask<ChannelFuture> task, CountDownLatch latch) {
			// TODO Auto-generated constructor stub
    		this.task = task;
    		this.latch = latch;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			task.run();
			try {
				task.get(getDefaultChainQuiesceTimeout(), TimeUnit.MILLISECONDS);
			}catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "caught exception performing startup task: " + e);
                }
            }
			latch.countDown();
		}
    	
    }
    
    /**
     * Method is called to run a task if the server has already started, if the
     * server has not started that task is queued to be run when the server start
     * signal has been received.
     *
     * @param callable - task to run
     * @return Callable return null if the task was not ran, but queued, else return
     *         the task to denote it has ran.
     * @throws Exception
     */
    public FutureTask<ChannelFuture> runWhenServerStarted(Callable<ChannelFuture> callable) throws Exception {
        synchronized (syncStarted) {
        	FutureTask<ChannelFuture> future = new FutureTask<ChannelFuture>(callable);
            if (!serverCompletelyStarted.get()) {
                serverStartedTasks.add(future);
            }else {
            	this.executorService.submit(future);
            }
            return future;
        }
    }

    /*
     * If the server has not completely started, then wait until it has been. The
     * server will be "completely" started when the server start signal has been
     * received and any tasks waiting on that signal before running have now been
     * run.
     */
    @FFDCIgnore({ InterruptedException.class })
    public static void waitServerCompletelyStarted() {
        synchronized (syncStarted) {
            if (serverCompletelyStarted.get() == false) {
                try {
                    syncStarted.wait();
                } catch (InterruptedException x) {
                    // assume we can go on then
                }
            }
        }
        return;
    }

    /**
     * non-blocking method to return the state of server startup with respect to the
     * server being completely started. The server will be "completely" started when
     * the server start signal has been received and any tasks waiting on that
     * signal before running have now been run.
     *
     * @return
     */
    public static boolean isServerCompletelyStarted() {
        return serverCompletelyStarted.get();
    }
    
    @Override
    public void registerEndpointQuiesce(Channel chan, Callable quiesce) {
    	if(chan != null && getActiveChannelsMap().containsKey(chan)) {
    		ChannelHandler quiesceHandler = new QuiesceHandler(quiesce);
        	chan.pipeline().addLast(quiesceHandler);
    	}else {
    		if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "Attempted to add a Quiesce Task to a channel which is not an endpoint. Quiesce will not be added and will be ignored.");
            }
    	}
    }

    /**
     * Declarative Services method for unsetting the ServerStarted service
     *
     * @param ref reference to the service
     */
    protected synchronized void unsetServerStarted(ServiceReference<ServerStarted> ref) {
        // server is shutting down
        serverCompletelyStarted.set(false);
    }

    @Override
    public ServerBootstrapExtended createTCPBootstrap(Map<String, Object> tcpOptions) throws NettyException {
        return TCPUtils.createTCPBootstrap(this, tcpOptions);
    }

    @Override
    public BootstrapExtended createTCPBootstrapOutbound(Map<String, Object> tcpOptions) throws NettyException {
        return TCPUtils.createTCPBootstrapOutbound(this, tcpOptions);
    }

    @Override
    public BootstrapExtended createUDPBootstrap(Map<String, Object> options) throws NettyException {
        return UDPUtils.createUDPBootstrap(this, options);
    }

    @Override
    public BootstrapExtended createUDPBootstrapOutbound(Map<String, Object> options) throws NettyException {
        return UDPUtils.createUDPBootstrapOutbound(this, options);
    }

    @Override
    public FutureTask<ChannelFuture> start(ServerBootstrapExtended bootstrap, String inetHost, int inetPort,
            ChannelFutureListener bindListener) throws NettyException {
        return TCPUtils.start(this, bootstrap, inetHost, inetPort, bindListener);
    }
    
    public FutureTask<ChannelFuture> start(ServerBootstrapExtended bootstrap, String inetHost, int inetPort,
            ChannelFutureListener bindListener, AtomicBoolean cancelToken) throws NettyException {
    	
    	System.out.println("Starting channel with cancel token set to: " + cancelToken.get());
    	
        return TCPUtils.start(this, bootstrap, inetHost, inetPort, bindListener, cancelToken);
    }

    @Override
    public FutureTask<ChannelFuture> start(BootstrapExtended bootstrap, String inetHost, int inetPort,
            ChannelFutureListener bindListener) throws NettyException {
        return UDPUtils.start(this, bootstrap, inetHost, inetPort, bindListener);
    }

    @Override
    public FutureTask<ChannelFuture> startOutbound(BootstrapExtended bootstrap, String inetHost, int inetPort,
    		ChannelFutureListener bindListener) throws NettyException {
    	if (bootstrap.getConfiguration() instanceof TCPConfigurationImpl) {
    		return TCPUtils.startOutbound(this, bootstrap, inetHost, inetPort, bindListener);
    	} else {
    		return UDPUtils.startOutbound(this, bootstrap, inetHost, inetPort, bindListener);
    	}
    }

    @Override
    public ChannelFuture stop(Channel channel) {
    	ChannelGroup group = activeChannelMap.get(channel);
    	if(group != null) {
    		group.close().addListener(innerFuture -> {
    			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "channel group" + group + " has closed...");
                }
    		});
    		activeChannelMap.remove(channel);
    	}
    	return channel.close();
    }

    @Override
    public void stop(Channel channel, long timeout) {
    	if (timeout == -1) {
    		timeout = getDefaultChainQuiesceTimeout();
    	}
    	ChannelFuture future = stop(channel);
    	if (future != null) {
    		future.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);
    	}
    }
    

    @Override
    public Set<Channel> getActiveChannels() {
        return activeChannelMap.keySet();
    }
    

    public Map<Channel, ChannelGroup> getActiveChannelsMap() {
        return activeChannelMap;
    }
    
    public ChannelGroup getOutboundConnections() {
        return outboundConnections;
    }

    @Override
    public long getDefaultChainQuiesceTimeout() {
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
    
    @Override
    public String toString() {
    	StringBuffer buf = new StringBuffer();
    	buf.append("NettyFrameworkImpl@").append(Integer.toHexString(System.identityHashCode(this)));
    	buf.append(": {");
    	buf.append("Parent Group: ");
    	buf.append(getParentGroup());
    	if(getParentGroup() != null) {
    		buf.append(" isShuttingDown? ");
    		buf.append(getParentGroup().isShuttingDown());
    		buf.append(" isShutDown? ");
    		buf.append(getParentGroup().isShutdown());
    		buf.append(" isTerminated? ");
    		buf.append(getParentGroup().isTerminated());
    	}
    	buf.append(", Child Group: ");
    	buf.append(getChildGroup());
    	if(getChildGroup() != null) {
    		buf.append(" isShuttingDown? ");
    		buf.append(getChildGroup().isShuttingDown());
    		buf.append(" isShutDown? ");
    		buf.append(getChildGroup().isShutdown());
    		buf.append(" isTerminated? ");
    		buf.append(getChildGroup().isTerminated());
    	}
    	buf.append(", EndpointManager: ");
    	buf.append(getEndpointManager());
    	buf.append(", Default Chain Quiesce Timeout: ");
    	buf.append(getDefaultChainQuiesceTimeout());
    	buf.append(", Outbound Connections: ");
    	buf.append(getOutboundConnections());
    	buf.append(", Active Endpoints: ");
    	buf.append(getActiveChannels());
    	buf.append(", Active endpoint maps: ");
    	buf.append(getActiveChannelsMap());
    	buf.append(", Is Active: ");
    	buf.append(isActive());
    	buf.append(", Is Stopping: ");
    	buf.append(isStopping());
    	buf.append("}");
    	return buf.toString();
    }

    public EventLoopGroup getParentGroup() {
        return this.parentGroup;
    }

    public EventLoopGroup getChildGroup() {
        return this.childGroup;
    }

    private void logChannelStopped(Channel channel) {
        if (channel instanceof NioServerSocketChannel || channel instanceof NioSocketChannel) {
            TCPUtils.logChannelStopped(channel);
        } else if (channel instanceof NioDatagramChannel) {
            UDPUtils.logChannelStopped(channel);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unexpected channel type: " + channel);
            }
        }
    }

    @Override
    public EndPointMgr getEndpointManager() {
        return EndPointMgrImpl.getRef();
    }
}
