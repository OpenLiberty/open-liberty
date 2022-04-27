/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.netty.jfapchannel;

import java.util.Map;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.ConnectRequestListener;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionTarget;
import com.ibm.ws.sib.jfapchannel.impl.CommsOutboundChain;
import com.ibm.ws.sib.jfapchannel.impl.OutboundConnection;
import com.ibm.ws.sib.utils.ras.SibTr;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tls.NettyTlsProvider;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

public class NettyNetworkConnection implements NetworkConnection{
	
	/** Trace */
	   private static final TraceComponent tc = SibTr.register(NettyNetworkConnection.class,
	                                                           JFapChannelConstants.MSG_GROUP,
	                                                           JFapChannelConstants.MSG_BUNDLE);

	   /** Log class info on load */
	   static
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/netty/jfapchannel/NettyNetworkConnection.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
	   }

	   /** The virtual connection */
	   
	   private Channel chan;
	   	   
	   private String chainName;
	   	   
	   private CommsOutboundChain chain;
	   
	   

	   /**
	    * @param bootstrap The Netty bootstrap object to create a channel from
	    * @param chainName The chain name to which the channel belongs to
	    * @throws FrameworkException 
	    */
	   public NettyNetworkConnection(Bootstrap bootstrap, String chainName) throws FrameworkException
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>" ,new Object[] {bootstrap, chainName});
	      
	      init(bootstrap);
	      this.chainName = chainName;
	      //TODO: Check if this is the best way to link a chain with the channel
	      this.chain = CommsOutboundChain.getChainDetails(chainName);
	      
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>", new Object[] {bootstrap, chainName, chain});
	   }
	   
	   public NettyNetworkConnection(Channel chan)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", chan);
	      
	      this.chan = chan;
	      // TODO: Check if this is fired and adapt to Netty accordingly
	      this.chain = CommsOutboundChain.getChainDetails(chan.attr(JMSClientInboundHandler.CHAIN_ATTR_KEY).get());
	      
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>", new Object[] {chan, chain});
	   }
	   
	   public void linkOutboundConnection(OutboundConnection conn) throws NettyException {
		   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "linkOutboundConnection");
		   if(this.chan == null) {
			   throw new NettyException("Error linking connection appropriately");
		   }
		   if(chan.attr(JMSClientInboundHandler.CONNECTION_KEY).get() != null) {
			   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "linkOutboundConnection","Connection was already set: "+chan.attr(JMSClientInboundHandler.CONNECTION_KEY).get());
		   }
		   chan.attr(JMSClientInboundHandler.CONNECTION_KEY).set(conn);
		   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "linkOutboundConnection");
	   }
	   
	   
	   
	   private void init(Bootstrap bootstrap) throws FrameworkException {
		  ChannelFuture channelFuture = null;
		      
		  try {
			  channelFuture = bootstrap.register().sync();
		  } catch (Exception e) {
			  SibTr.error(tc, "init", e);
			  throw new FrameworkException(e);
		  }
		  
		  if (channelFuture.isCancelled() || !channelFuture.isSuccess()) {
			  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "init", "bind can't create channel: " + channelFuture.cause());
			  }
			  throw new FrameworkException((Exception) channelFuture.cause());
		  }
	      
	      // Register established successfully
	      this.chan = channelFuture.channel();
	   }

	   /**
	    * @return Returns the channel connection.
	    */
	   Channel getVirtualConnection()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getVirtualConnection");
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getVirtualConnection", chan);
	      return this.chan;
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#requestPermissionToClose(long)
	    */
	   public boolean requestPermissionToClose(long timeout)
	   {
		  // TODO Figure out the netty equivalent for this. Only used in connection
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "requestPermissionToClose", Long.valueOf(timeout));
//	      boolean canProcess = vc.requestPermissionToClose(timeout);
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "requestPermissionToClose", Boolean.valueOf(true));
	      return true;
	   }
	   
	   /**
	    * Sets the timeout to the values passed
	    * @param timeout in seconds
	    * @throws NettyException 
	    */
	   public void setHearbeatInterval(int timeout) throws NettyException {
		   if(chan == null || chain == null) {
		    	  throw new NettyException("Haven't registered channel to set timeout");
		   }
		   ChannelPipeline pipeline = this.chan.pipeline();
		   if(getHearbeatInterval() != timeout * 1000)
			   pipeline.replace(
					   NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, 
					   NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, 
					   new JMSHeartbeatHandler(timeout));
	   }
	   
	   
	   private long getHearbeatInterval() throws NettyException {
		   if(chan == null || chain == null) {
		    	  throw new NettyException("Haven't registered channel to get timeout");
		   }
		   ChannelPipeline pipeline = this.chan.pipeline();
		   return ((JMSHeartbeatHandler)pipeline.get(NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY)).getReaderIdleTimeInMillis();
		   
	   }
	   
	   protected SSLSession getSSLSession() {
		   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSSLSession", new Object[] {chan, chain});
		   SSLSession session = null;
		   if(this.chain.isSSL() && CommsOutboundChain.getChainDetails(chainName).getSslOptions() != null) {
			   ChannelHandler handler = this.chan.pipeline().get(NettyNetworkConnectionFactory.SSL_HANDLER_KEY);
			   if(handler == null || !(handler instanceof SslHandler)) {
				   if (tc.isWarningEnabled())
		                  SibTr.warning(tc, "getSSLSession: Found SSL turned on but no valid SSL Handler found. This shouldn't happen.", new Object[] {this.chan, this.chain});
			   }else {
				   session = ((SslHandler)handler).engine().getSession();
			   }
		   }else {
			   if(tc.isDebugEnabled()) SibTr.debug(tc, "getSSLSession: No SSL Found for session.", new Object[] {this.chan, this.chain});
		   }
		   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSSLSession", session);
		   return session;
	   }
	   

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#connectAsynch(com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionTarget, com.ibm.ws.sib.jfapchannel.framework.ConnectRequestListener)
	    */
	   @SuppressWarnings("unchecked")   // Channel FW implements the state map
	   public void connectAsynch(final NetworkConnectionTarget target, final ConnectRequestListener listener)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "connectAsynch", new Object[]{target, listener});

	      final NettyNetworkConnection readyConnection = this;
	      
	      	      
	      
	      ChannelFuture channelFuture = null;
	      if(chan == null || chain == null) {
	    	  listener.connectRequestFailedNotification(new NettyException("Couldn't register channel to connect"));
	    	  return;
	      }
	    	  
	      try {

	    	  // Check if SSL is needed and construct Handler accordingly
				if(this.chain.isSSL()) {
		          	if (tc.isDebugEnabled())
		                  SibTr.debug(this, tc, "initChannel","Adding SSL Support");
		          	if(CommsOutboundChain.getChainDetails(chainName).getSslOptions() == null) {
		          		if (tc.isDebugEnabled())
			                  SibTr.debug(this, tc, "initChannel","Found SSL Set but no SSLOptions to use. Throwing error.");
		          		throw new NettyException("Invalid SSL options found for " + chainName);
		          	}
		          	NettyTlsProvider tlsProvider = this.chain.getTlsProvider();
		          	String host = target.getRemoteAddress().getAddress().getHostAddress();
		          	String port = Integer.toString(target.getRemoteAddress().getPort());
		          	Map<String, Object> sslOptions = this.chain.getSslOptions();
		          	if (tc.isDebugEnabled()) SibTr.debug(this, tc, "Create SSL", new Object[] {tlsProvider, host, port, sslOptions});
		          	SslContext context = tlsProvider.getOutboundSSLContext(sslOptions, host, port);
		          	if(context == null) {
						if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initChannel","Error adding TLS Support");
			            listener.connectRequestFailedNotification(new NettyException("Problems creating SSL context"));
			            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initChannel");
			            return;
		          	}
		          	SSLEngine engine = context.newEngine(chan.alloc());
		          	this.chan.pipeline().addFirst(NettyNetworkConnectionFactory.SSL_HANDLER_KEY, new SslHandler(engine, false));
		          }
	    	  
	    	  
				channelFuture = chan.connect(target.getRemoteAddress());
				NettyNetworkConnection parent = this;
				
				
				channelFuture.addListener(new ChannelFutureListener() {
					
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (future.isCancelled() || !future.isSuccess()) {
							SibTr.debug(this, tc, "Channel exception during connect: " + future.cause().getMessage());
							if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "destroy", (Exception) future.cause());
				            listener.connectRequestFailedNotification((Exception) future.cause());
				            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "destroy");
						}else {
							if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "ready", future);
					        listener.connectRequestSucceededNotification(readyConnection);
					        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "ready");
						}
					}
				});
				
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "destroy", e);
	            listener.connectRequestFailedNotification(e);
	            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "destroy");
				
			}
	      
	      
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "connectAsynch");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#getNetworkConnectionContext()
	    */
	   public NetworkConnectionContext getNetworkConnectionContext()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getNetworkConnectionContext");
	      
	      NetworkConnectionContext context = new NettyNetworkConnectionContext(this);

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getNetworkConnectionContext", context);
	      return context;
	   }
	   
	   public String getChainName() {
		   return chainName;
	   }

}
