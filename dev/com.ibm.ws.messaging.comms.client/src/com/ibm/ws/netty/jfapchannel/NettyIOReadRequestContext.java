package com.ibm.ws.netty.jfapchannel;

import java.util.Arrays;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.utils.ras.SibTr;

public class NettyIOReadRequestContext extends NettyIOBaseContext implements IOReadRequestContext{

	
	/** Trace */
	   private static final TraceComponent tc = SibTr.register(NettyIOReadRequestContext.class,
	                                                           JFapChannelConstants.MSG_GROUP,
	                                                           JFapChannelConstants.MSG_BUNDLE);

	   /** Log class info on load */
	   static
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/jfapchannel/netty/NettyIOReadRequestContext.java, SIB.comms, WASX.SIB, uu1215.01 1.4");
	   }


	   /**
	    * @param readCtx
	    */
	   public NettyIOReadRequestContext(NettyNetworkConnection conn)
	   {
	      super(conn);

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{conn});
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#setBuffer(com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer)
	    */
	   public void setBuffer(WsByteBuffer buffer)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBuffer", buffer);
	      
	      // TODO: Figure out what to do here when called. Think its safe to remove but will cause issues with interface and compatibility
	      
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBuffer");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#setBuffers(com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer[])
	    */
	   public void setBuffers(WsByteBuffer[] buffers)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBuffers", Arrays.toString(buffers));

	      // TODO: Figure out what to do here when called. Think its safe to remove but will cause issues with interface and compatibility
	         
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBuffers");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#getBuffer()
	    */
	   public WsByteBuffer getBuffer()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBuffer");
	      // TODO: Figure out what to do here when called. Think its safe to remove but will cause issues with interface and compatibility
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBuffer");
	      return null;
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#getBuffers()
	    */
	   public WsByteBuffer[] getBuffers()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBuffers");

	      // TODO: Figure out what to do here when called. Think its safe to remove but will cause issues with interface and compatibility

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBuffers");
	      return null;
	   }
	   

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#read(int, com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback, boolean, int)
	    */
	   public NetworkConnection read(int amountToRead, final IOReadCompletedCallback completionCallback,
	                                 boolean forceQueue, int timeout)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "read",
	                                           new Object[]{Integer.valueOf(amountToRead), completionCallback, Boolean.valueOf(forceQueue), Integer.valueOf(timeout)});
	      // TODO Just return null here for now? Everything is async and originally it was expecting null if async
	      this.conn.getVirtualConnection().read();
	      return null;
	   }
	
	
	
}
