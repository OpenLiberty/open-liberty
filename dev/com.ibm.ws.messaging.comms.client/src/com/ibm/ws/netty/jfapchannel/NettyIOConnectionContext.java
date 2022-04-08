package com.ibm.ws.netty.jfapchannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWIOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWIOWriteRequestContext;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

public class NettyIOConnectionContext implements IOConnectionContext{
	
	
	/** Trace */
	   private static final TraceComponent tc = SibTr.register(NettyIOConnectionContext.class,
	                                                           JFapChannelConstants.MSG_GROUP,
	                                                           JFapChannelConstants.MSG_BUNDLE);

	   /** Log class info on load */
	   static
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/jfapchannel/netty/NettyIOConnectionContext.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
	   }


	   /** The connection reference */
	   private NettyNetworkConnection conn = null;

	   /**
	    * @param tcpCtx
	    */
	   public NettyIOConnectionContext(NettyNetworkConnection conn)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{conn});
	      this.conn = conn;
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getLocalAddress()
	    */
	   public InetAddress getLocalAddress()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLocalAddress");
	      InetAddress localAddress = ((InetSocketAddress) this.conn.getVirtualConnection().localAddress()).getAddress();
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLocalAddress", localAddress);
	      return localAddress;
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getRemoteAddress()
	    */
	   public InetAddress getRemoteAddress()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemoteAddress");
	      InetAddress remoteAddress = ((InetSocketAddress) this.conn.getVirtualConnection().remoteAddress()).getAddress();
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemoteAddress", remoteAddress);
	      return remoteAddress;
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getLocalPort()
	    */
	   public int getLocalPort()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLocalPort");
	      int localPort = ((InetSocketAddress) this.conn.getVirtualConnection().localAddress()).getPort();
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLocalPort", Integer.valueOf(localPort));
	      return localPort;
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getRemotePort()
	    */
	   public int getRemotePort()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemotePort");
	      int remotePort = ((InetSocketAddress) this.conn.getVirtualConnection().remoteAddress()).getPort();
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemotePort", Integer.valueOf(remotePort));
	      return remotePort;
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getReadInterface()
	    */
	   public IOReadRequestContext getReadInterface()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getReadInterface");
	      IOReadRequestContext readCtx = new NettyIOReadRequestContext(conn);
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getReadInterface", readCtx);
	      return readCtx;
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getWriteInterface()
	    */
	   public IOWriteRequestContext getWriteInterface()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getWriteInterface");
	      IOWriteRequestContext writeCtx = new NettyIOWriteRequestContext(conn);
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getWriteInterface", writeCtx);
	      return writeCtx;
	   }
	

}
