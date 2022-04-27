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

import java.io.IOException;
import java.util.Arrays;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.impl.NettyConnectionWriteCompletedCallback;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.CFWIOWriteRequestContext;
import com.ibm.ws.sib.utils.ras.SibTr;

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
	      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/netty/jfapchannel/NettyIOWriteRequestContext.java, SIB.comms, WASX.SIB, uu1215.01 1.5");
	   }


	   /**
	    * @param conn
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

	      // TODO: Figure out what to do here when called. Think its safe to remove but will cause issues with interface and compatibility

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBuffer");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext#setBuffers(com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer[])
	    */
	   public void setBuffers(WsByteBuffer[] buffers)
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBuffers", Arrays.toString(buffers));

	      // TODO: Figure out what to do here when called. Think its safe to remove but will cause issues with interface and compatibility

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBuffers");
	   }

	   /**
	    *
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext#getBuffer()
	    */
	   public WsByteBuffer getBuffer()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBuffer");
	      // TODO: Figure out what to do here when called. Think its safe to remove but will cause issues with interface and compatibility
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBuffer");
	      return null;
	   }

	   /**
	    * @see com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext#getBuffers()
	    */
	   public WsByteBuffer[] getBuffers()
	   {
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBuffers");

	      // TODO: Figure out what to do here when called. Think its safe to remove but will cause issues with interface and compatibility

	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBuffers");
	      return null;
	   }
	   
	   public NetworkConnection write(WsByteBuffer buffer, final NettyConnectionWriteCompletedCallback completionCallback) {
		   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "write",
                   new Object[]{buffer, completionCallback});
		   NetworkConnection retConn = null;
	       final IOWriteRequestContext me = this;
	       // TODO: Verify how to use the queueRequest and timeout
	       // Only called from write completed callback
	       // queueRequest passed is always false
	       // timeout is always no timeout
		      
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
	   // TODO: Probably need to change this method to include the object itself not the amount to write. See other write method
	   public NetworkConnection write(int amountToWrite, final IOWriteCompletedCallback completionCallback,
	                                  boolean queueRequest, int timeout)
	   {
		   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "write",
                   new Object[]{amountToWrite, completionCallback, queueRequest, timeout});

	       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "write: Not supported");
		   throw new UnsupportedOperationException("Not currently supported for Netty. Please use other write method.");

	   }


}
