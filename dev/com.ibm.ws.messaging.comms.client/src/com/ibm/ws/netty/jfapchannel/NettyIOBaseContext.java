package com.ibm.ws.netty.jfapchannel;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWIOBaseContext;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWNetworkConnection;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.VirtualConnection;

import io.netty.channel.Channel;

public class NettyIOBaseContext {

	/** Trace */
	   private static final TraceComponent tc = SibTr.register(NettyIOBaseContext.class, 
	                                                           JFapChannelConstants.MSG_GROUP, 
	                                                           JFapChannelConstants.MSG_BUNDLE);
	   
	   /** Log class info on load */
	   static
	   {
	      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/jfapchannel/netty/NettyIOBaseContext.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
	   }

	   /** The connection reference */
	   protected NettyNetworkConnection conn = null;
	   
	   /**
	    * Constructor.
	    * 
	    * @param conn
	    */
	   public NettyIOBaseContext(NettyNetworkConnection conn)
	   {
	      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", conn);
	      this.conn = conn;
	      if (tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
	   }
	   
	   /**
	    * This method tries to avoid creating a new instance of a CFWNetworkConnection object by seeing
	    * if the specified virtual connection is the one that we are wrapping in the 
	    * CFWNetworkConnection instance that created this context. If it is, we simply return that.
	    * Otherwise we must create a new instance.
	    * 
	    * @param vc The virtual connection.
	    * 
	    * @return Returns a NetworkConnection instance that wraps the virtual connection.
	    */
	   protected NetworkConnection getNetworkConnectionInstance(Channel chan)
	   {
	      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getNetworkConnectionInstance", chan);
	      
	      NetworkConnection retConn = null;
	      if (chan != null)
	      {
	         // Default to the connection that we were created from
	         retConn = conn;
	         
	         if (chan != ((NettyNetworkConnection) conn).getVirtualConnection())
	         {
	            // The connection is different - nothing else to do but create a new instance
	            retConn = new NettyNetworkConnection(chan);
	         }
	      }
	      
	      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getNetworkConnectionInstance", retConn);
	      return retConn;
	   }
	
	
}
