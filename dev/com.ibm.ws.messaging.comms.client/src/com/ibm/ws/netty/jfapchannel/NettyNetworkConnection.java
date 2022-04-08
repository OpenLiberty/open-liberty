package com.ibm.ws.netty.jfapchannel;

import java.io.IOException;

import com.ibm.ejs.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.ConnectRequestListener;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionTarget;
import com.ibm.ws.sib.jfapchannel.impl.OutboundConnection;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWNetworkConnection;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWNetworkConnectionContext;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.openliberty.netty.internal.exception.NettyException;

public class NettyNetworkConnection implements NetworkConnection{
	
	/** Trace */
	   private static final TraceComponent tc = SibTr.register(NettyNetworkConnection.class,
	                                                           JFapChannelConstants.MSG_GROUP,
	                                                           JFapChannelConstants.MSG_BUNDLE);

	   /** Log class info on load */
	   static
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/jfapchannel/netty/NettyNetworkConnection.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
	   }

	   /** The virtual connection */
	   private Channel chan;
	   
	   private Bootstrap bootstrap;
	   
	   private String chainName;
	   
	   

	   /**
	    * @param vc
	 * @throws IOException 
	    */
	   public NettyNetworkConnection(Bootstrap bootstrap, String chainName) throws FrameworkException
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
	      
	      init(bootstrap);
	      this.bootstrap = bootstrap;
	      this.chainName = chainName;
	      
	      
	      
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
	   }
	   
	   public NettyNetworkConnection(Channel chan)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
	      
	      this.chan = chan;
	      
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
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
	      
	      // Bind established successfully
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
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#connectAsynch(com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionTarget, com.ibm.ws.sib.jfapchannel.framework.ConnectRequestListener)
	    */
	   @SuppressWarnings("unchecked")   // Channel FW implements the state map
	   public void connectAsynch(final NetworkConnectionTarget target, final ConnectRequestListener listener)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "connectAsynch", new Object[]{target, listener});

	      final NettyNetworkConnection readyConnection = this;
	      
	      
	      //TODO: Netty transformation
	      
	      ChannelFuture channelFuture = null;
	      if(chan == null) {
	    	  listener.connectRequestFailedNotification(new NettyException("Couldn't register channel to connect"));
	    	  return;
	      }
	    	  
	      try {
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
				e.printStackTrace();
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
