/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.openliberty.netty.internal.BootstrapConfiguration;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;
import io.openliberty.netty.internal.impl.NettyConstants;


public class TCPUtils {

	private static final TraceComponent tc = Tr.register(TCPUtils.class, new String[]{TCPMessageConstants.TCP_TRACE_NAME,TCPMessageConstants.NETTY_TRACE_NAME},
			TCPMessageConstants.TCP_BUNDLE, TCPUtils.class.getName());
	private static final int timeBetweenRetriesMsec = 1000; // make this non-configurable

	/**
	 * Create a {@link ServerBootstrapExtended} for inbound TCP channels
	 * 
	 * @param framework
	 * @param tcpOptions
	 * @return
	 * @throws NettyException
	 */
	public static ServerBootstrapExtended createTCPBootstrap(NettyFrameworkImpl framework,
			Map<String, Object> tcpOptions) throws NettyException {
		BootstrapConfiguration config = new TCPConfigurationImpl(tcpOptions, true);
		ServerBootstrapExtended bs = new ServerBootstrapExtended();
		bs.group(framework.getParentGroup(), framework.getChildGroup());
		bs.channel(NioServerSocketChannel.class);
		// apply the existing user config to the Netty TCP channel
		bs.applyConfiguration(config);
		ChannelInitializerWrapper tcpInitializer = new TCPChannelInitializerImpl(config, framework);
		bs.setBaseInitializer(tcpInitializer);
		return bs;
	}

	/**
	 * Create a {@link BootstrapExtended} for outbound TCP channels
	 * 
	 * @param framework
	 * @param tcpOptions
	 * @return
	 * @throws NettyException
	 */
	public static BootstrapExtended createTCPBootstrapOutbound(NettyFrameworkImpl framework,
			Map<String, Object> tcpOptions) throws NettyException {
		BootstrapConfiguration config = new TCPConfigurationImpl(tcpOptions, false);
		BootstrapExtended bs = new BootstrapExtended();
		bs.group(framework.getChildGroup());
		bs.channel(NioSocketChannel.class);
		// apply the existing user config to the Netty TCP channel
		bs.applyConfiguration(config);
		ChannelInitializerWrapper tcpInitializer = new TCPChannelInitializerImpl(config, framework);
		bs.setBaseInitializer(tcpInitializer);
		return bs;
	}

	private static ChannelFuture open(NettyFrameworkImpl framework, final Channel channel,
			final TCPConfigurationImpl config, String inetHost, int inetPort, ChannelFutureListener openListener,
			final int retryCount) {
		if(!channel.isOpen()) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Channel not started because it was closed: " + channel);
			}
			return null;
		}
		ChannelFuture oFuture = null;
		if (inetHost.equals("*")) {
			inetHost = NettyConstants.INADDR_ANY;
		}
		if (config.isInbound()) {
			oFuture = channel.bind(new InetSocketAddress(inetHost, inetPort));
		} else {
			oFuture = channel.connect(new InetSocketAddress(inetHost, inetPort));
		}
		final ChannelFuture openFuture = oFuture;
		
		final String newHost = inetHost;

		openFuture.addListener(future -> {
			if (future.isSuccess()) {

				// add new channel to set of active channels, and set a close future to
				// remove it
				// Get parent and increment active connections

				// set common channel attrs
				channel.attr(ConfigConstants.NAME_KEY).set(config.getExternalName());
				channel.attr(ConfigConstants.HOST_KEY).set(newHost);
				channel.attr(ConfigConstants.PORT_KEY).set(inetPort);
				channel.attr(ConfigConstants.IS_INBOUND_KEY).set(config.isInbound());

				// Listener to stop channel on close
				// This should just log that the channel stopped
				channel.closeFuture().addListener(innerFuture -> logChannelStopped(channel));

                if(config.isInbound()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding new channel group for " + channel);
                    }
                    synchronized (framework.getActiveChannelsMap()) {
                    	framework.getActiveChannelsMap().put(channel, new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
                    }
                }else {
                	synchronized (framework.getOutboundConnections()) {
                		framework.getOutboundConnections().add(channel);
                	}
				}
				// set up a helpful log message
				String hostLogString = newHost;
				SocketAddress addr = channel.localAddress();
				InetSocketAddress inetAddr = (InetSocketAddress)addr;
				String IPvType = "IPv4";
				if (inetAddr.getAddress() instanceof Inet6Address) {
					IPvType = "IPv6";
				}
				if (newHost == NettyConstants.INADDR_ANY) {
					hostLogString = "*  (" + IPvType + ")";
				} else {
					hostLogString = config.getHostname() + "  (" + IPvType + ": "
							+ inetAddr.getAddress().getHostAddress() + ")";
				}

				if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "serverSocket getInetAddress is: " + inetAddr);
					Tr.debug(tc, "serverSocket getLocalSocketAddress is: " + channel.localAddress());
					Tr.debug(tc, "serverSocket getInetAddress hostname is: " + inetAddr.getAddress().getHostName());
					Tr.debug(tc, "serverSocket getInetAddress address is: " + inetAddr.getAddress().getHostAddress());
					Tr.debug(tc, "channelConfig.getHostname() is: " + config.getHostname());
					Tr.debug(tc, "channelConfig.getPort() is: " + config.getPort());
				}

				if (config.isInbound()) {
					Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
							new Object[] { config.getExternalName(), hostLogString, String.valueOf(inetPort) });
				} else {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
								new Object[] { config.getExternalName(), hostLogString, String.valueOf(inetPort) });
					}
				}
			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc,
							"open failed for " + config.getExternalName() + " due to: " + future.cause().getMessage());
				}

				if (retryCount > 0) {
					if(!channel.isOpen()) {
						if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
							Tr.debug(tc, "Channel not open so it must have been cancelled. Returning...");
						}						
						return;
					}
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, "attempt to bind again after a wait of " + timeBetweenRetriesMsec + "ms; "
								+ retryCount + " attempts remaining" + " for " + config.getExternalName());
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
					open(framework, channel, config, newHost, inetPort, openListener, retryCount - 1);
				} else {
					if(!channel.isOpen()) {
						if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
							Tr.debug(tc, "No retries left and channel is not open so not printing any logs. Returning...");
						}
						return;
					}

					// Check if the exception is or was caused by UnresolvedAddressException
					Throwable cause = future.cause();
					boolean unresolvedAddress = false;
	
					while (cause != null) {
						if (cause instanceof java.nio.channels.UnresolvedAddressException) {
							unresolvedAddress = true;
							break;
						}
						cause = cause.getCause();
					}	
					if (unresolvedAddress) {
						// Log the specific error message
						Tr.error(tc, TCPMessageConstants.LOCAL_HOST_UNRESOLVED,
							new Object[] { config.getExternalName(), newHost, String.valueOf(inetPort) });
					} else {
						if (config.isInbound()) {
							Tr.error(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(), newHost,
									String.valueOf(inetPort), openFuture.cause().getMessage() });
						} else {
							if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
								Tr.debug(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(),
										newHost, String.valueOf(inetPort), openFuture.cause().getMessage() });
							}
						}
					}
				}
			}
		});

		if (openListener != null) {
			openFuture.addListener(generateOpenListenerWrapper(framework, openListener));
		}		
		return openFuture;
	}
	
	private static ChannelFutureListener generateOpenListenerWrapper(NettyFrameworkImpl framework, ChannelFutureListener listener) {
		return new ChannelFutureListener() {
		    @Override
		    public void operationComplete(ChannelFuture future) throws Exception {
		        framework.getExecutorService().execute(new Runnable() {
					
					@Override
					public void run() {
						try {
							listener.operationComplete(future);
						} catch (Exception e) {
							if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
								Tr.debug(tc, "Exception caught running open listener!! Closing channel just in case");
							}
							future.channel().close();
						}
					}
				});
		    }
		};
	}

	private static Channel startHelper(NettyFrameworkImpl framework, AbstractBootstrap bootstrap,
			TCPConfigurationImpl config, String inetHost, int inetPort, ChannelFutureListener openListener)
					throws NettyException {
		if(framework.isStopping()){ // Framework already stopping and is no longer active
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "server is stopping, channel will not be started");
			}
			return null;
		}else{
			try {
				Channel channel = bootstrap.register().channel();
				framework.runWhenServerStarted(new Callable<ChannelFuture>() {
					@Override
					public ChannelFuture call() {
						return open(framework, channel, config, inetHost, inetPort, openListener,
								config.getPortOpenRetries());
					}
				});
				return channel;
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "NettyFramework signaled- caught exception: " + e.getMessage());
				}
			}
		}
		return null;
	}

	/**
	 * Start an inbound TCP channel
	 * 
	 * @param framework
	 * @param bootstrap
	 * @param inetHost
	 * @param inetPort
	 * @param openListener
	 * @return
	 * @throws NettyException
	 */
	public static Channel start(NettyFrameworkImpl framework, ServerBootstrapExtended bootstrap, String inetHost,
			int inetPort, ChannelFutureListener openListener) throws NettyException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "start (TCP): attempt to bind a channel at host " + inetHost + " port " + inetPort);
		}
		TCPConfigurationImpl config = (TCPConfigurationImpl) bootstrap.getConfiguration();
		return startHelper(framework, bootstrap, config, inetHost, inetPort, openListener);
	}

	/**
	 * Start an outbound TCP channel
	 * 
	 * @param framework
	 * @param bootstrap
	 * @param inetHost
	 * @param inetPort
	 * @param openListener
	 * @return
	 * @throws NettyException
	 */
	public static Channel startOutbound(NettyFrameworkImpl framework, BootstrapExtended bootstrap,
			String inetHost, int inetPort, ChannelFutureListener openListener) throws NettyException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "startOutbound (TCP): attempt to connect to host " + inetHost + " port " + inetPort);
		}
		TCPConfigurationImpl config = (TCPConfigurationImpl) bootstrap.getConfiguration();
		return startHelper(framework, bootstrap, config, inetHost, inetPort, openListener);
	}

	/**
	 * Log a TCP channel stopped message. Inbound channels will log a INFO message,
	 * and outbound channels will log DEBUG
	 * 
	 * @param channel
	 */
	public static void logChannelStopped(Channel channel) {
		String channelName = channel.attr(ConfigConstants.NAME_KEY).get();
		String host = channel.attr(ConfigConstants.HOST_KEY).get();
		Integer port = channel.attr(ConfigConstants.PORT_KEY).get();
		Boolean inbound = channel.attr(ConfigConstants.IS_INBOUND_KEY).get();
		if (inbound != null && inbound) {
			Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
		} else if (inbound != null && !inbound) {
			if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				Tr.debug(tc, TCPMessageConstants.TCP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
		} else {
			if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				Tr.debug(tc, "Socket channel closed, local: " + channel.localAddress() + " remote: " + channel.remoteAddress());
		}
	}

	/**
	 * Log a TCP channel started message. Inbound channels will log a INFO message,
	 * and outbound channels will log DEBUG
	 * 
	 * @param channel
	 */
	public static void logChannelStarted(Channel channel) {
		String channelName = channel.attr(ConfigConstants.NAME_KEY).get();
		String host = channel.attr(ConfigConstants.HOST_KEY).get();
		Integer port = channel.attr(ConfigConstants.PORT_KEY).get();
		if (channel.attr(ConfigConstants.IS_INBOUND_KEY).get()) {
			Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
					new Object[] { channelName, host, String.valueOf(port) });
		} else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
					new Object[] { channelName, host, String.valueOf(port) });
		}
	}

}
