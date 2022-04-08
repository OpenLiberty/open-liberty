package com.ibm.ws.netty.jfapchannel;

import java.io.IOException;
import java.util.Arrays;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
//import com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.impl.NettyConnectionWriteCompletedCallback;
//import com.ibm.ws.sib.jfapchannel.richclient.buffer.impl.RichByteBufferPool;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWIOWriteRequestContext;
import com.ibm.ws.sib.utils.ras.SibTr;
//import com.ibm.wsspi.channelfw.VirtualConnection;
//import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
//import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public class NettyIOWriteRequestContext extends NettyIOBaseContext implements IOWriteRequestContext{
	
	/** Trace */
	   private static final TraceComponent tc = SibTr.register(CFWIOWriteRequestContext.class,
	                                                           JFapChannelConstants.MSG_GROUP,
	                                                           JFapChannelConstants.MSG_BUNDLE);

	   /** Log class info on load */
	   static
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/framework/impl/CFWIOWriteRequestContext.java, SIB.comms, WASX.SIB, uu1215.01 1.5");
	   }


	   /**
	    * @param writeCtx
	    */
	   public NettyIOWriteRequestContext(NettyNetworkConnection conn)
	   {
	      super(conn);

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] { conn });
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext#setBuffer(com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer)
	    */
	   public void setBuffer(WsByteBuffer buffer)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBuffer", buffer);

	      //TODO: Figure out what to do here with buffers

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBuffer");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext#setBuffers(com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer[])
	    */
	   public void setBuffers(WsByteBuffer[] buffers)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBuffers", Arrays.toString(buffers));

	      //TODO: Figure out what to do here with buffers

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBuffers");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext#getBuffer()
	    */
	   public WsByteBuffer getBuffer()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBuffer");
//	      WsByteBuffer jfapByteBuffer = ((RichByteBufferPool) WsByteBufferPool.getInstance()).wrap(writeCtx.getBuffer());
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBuffer");
//	      return jfapByteBuffer;
	      return null;
	      //TODO: Figure out what to do here with buffers, probably just need to wrap the WsByteBuffer to a JFapByteBuffer. Not really used anywhere else?
	   }

	   /**
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext#getBuffers()
	    */
	   public WsByteBuffer[] getBuffers()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBuffers");

	      //TODO: Figure out what to do here when called

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBuffers");
	      return null;
	   }
	   
	   public NetworkConnection write(WsByteBuffer buffer, final NettyConnectionWriteCompletedCallback completionCallback) {
		   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "write",
                   new Object[]{buffer, completionCallback});
		   NetworkConnection retConn = null;
	       final IOWriteRequestContext me = this;
	       // Verify how to use the queueRequest and timeout
		      
	       Channel chan = this.conn.getVirtualConnection();
	       
		   ChannelFuture future = chan.writeAndFlush(buffer, chan.newPromise().addListener(f -> {
			   if (f.isDone() && f.isSuccess()) {
				   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Succesful write");
				   completionCallback.complete(getNetworkConnectionInstance(chan), me);
               } else {
            	   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "error", new Object[]{chan, f.cause()});
            	   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            		   SibTr.debug(this, tc, "Unsuccesful write");
            		   SibTr.error(tc, f.cause().getMessage(), f.cause());
            	   }
                   completionCallback.error(getNetworkConnectionInstance(chan), me, new IOException(f.cause()));
                   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "error");
               }
		   }));
		   
		   if(future.isDone()) {
			   retConn = getNetworkConnectionInstance(chan); 
		   }

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "write", retConn);
	      return retConn;
		   
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext#write(int, com.ibm.ws.sib.jfapchannel.framework.IOWriteCompletedCallback, boolean, int)
	    */
	   // TODO: Probably need to change this method to include the object itself not the amount to write
	   public NetworkConnection write(int amountToWrite, final IOWriteCompletedCallback completionCallback,
	                                  boolean queueRequest, int timeout)
	   {
		   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "write",
                   new Object[]{amountToWrite, completionCallback, queueRequest, timeout});

	       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "write", null);
		   throw new UnsupportedOperationException("Not currently supported.");

	   }


}
