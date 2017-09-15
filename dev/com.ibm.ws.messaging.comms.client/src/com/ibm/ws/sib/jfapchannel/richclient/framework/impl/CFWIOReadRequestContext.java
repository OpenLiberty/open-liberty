/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.framework.impl;

import java.io.IOException;
import java.util.Arrays;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool;
import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.richclient.buffer.impl.RichByteBufferPool;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext. It
 * basically wrappers the com.ibm.wsspi.tcp.channel.TCPReadRequestContext code in the
 * underlying TCP channel.
 *
 * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext
 * @see com.ibm.wsspi.tcp.channel.TCPReadRequestContext
 *
 * @author Gareth Matthews
 */
public class CFWIOReadRequestContext extends CFWIOBaseContext implements IOReadRequestContext
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(CFWIOReadRequestContext.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/framework/impl/CFWIOReadRequestContext.java, SIB.comms, WASX.SIB, uu1215.01 1.4");
   }

   /** The TCP Read Context */
   private TCPReadRequestContext readCtx = null;

   /**
    * @param readCtx
    */
   public CFWIOReadRequestContext(NetworkConnection conn, TCPReadRequestContext readCtx)
   {
      super(conn);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{conn, readCtx});
      this.readCtx = readCtx;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#setBuffer(com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer)
    */
   public void setBuffer(WsByteBuffer buffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBuffer", buffer);
      
      if(buffer != null)
      {
         readCtx.setBuffer((com.ibm.wsspi.bytebuffer.WsByteBuffer) buffer.getUnderlyingBuffer());
      }
      else
      {
         readCtx.setBuffer(null);
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBuffer");
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#setBuffers(com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer[])
    */
   public void setBuffers(WsByteBuffer[] buffers)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBuffers", Arrays.toString(buffers));

      if(buffers != null)
      {
         com.ibm.wsspi.bytebuffer.WsByteBuffer[] tcpBuffers = new com.ibm.wsspi.bytebuffer.WsByteBuffer[buffers.length];
         for (int x = 0; x < buffers.length; x++)
         {
            tcpBuffers[x] = (com.ibm.wsspi.bytebuffer.WsByteBuffer) buffers[x].getUnderlyingBuffer();
         }
         readCtx.setBuffers(tcpBuffers);
      }
      else
      {
         readCtx.setBuffers(null);
      }
         
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBuffers");
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#getBuffer()
    */
   public WsByteBuffer getBuffer()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBuffer");
      WsByteBuffer jfapByteBuffer = ((RichByteBufferPool) WsByteBufferPool.getInstance()).wrap(readCtx.getBuffer());
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBuffer", jfapByteBuffer);
      return jfapByteBuffer;
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext#getBuffers()
    */
   public WsByteBuffer[] getBuffers()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBuffers");

      WsByteBuffer[] jfapByteBuffers = null;
      com.ibm.wsspi.bytebuffer.WsByteBuffer[] tcpBuffers = readCtx.getBuffers();

      if (tcpBuffers != null) {
        jfapByteBuffers = new WsByteBuffer[tcpBuffers.length];

        int nonNullCount = 0;

        for (int x = 0; x < tcpBuffers.length; x++)
        {
           //Only wrap non-null buffers
           if(tcpBuffers[x] != null)
           {
              jfapByteBuffers[nonNullCount++] = ((RichByteBufferPool) WsByteBufferPool.getInstance()).wrap(tcpBuffers[x]);
           }
        }

        //tcpBuffers contained null entries, compact it
        if(nonNullCount < tcpBuffers.length)
        {
           WsByteBuffer[] compactedArray = new WsByteBuffer[nonNullCount];
           System.arraycopy(jfapByteBuffers, 0, compactedArray, 0, nonNullCount);
           jfapByteBuffers = compactedArray;
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBuffers", Arrays.toString(jfapByteBuffers));
      return jfapByteBuffers;
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

      NetworkConnection retConn = null;
      final IOReadRequestContext me = this;

      // Create a real TCP callback to use with this operation
      TCPReadCompletedCallback callback = new TCPReadCompletedCallback() {
         /**
          * Called when the read operation completes.
          *
          * @see com.ibm.wsspi.tcp.channel.TCPReadCompletedCallback#complete(com.ibm.websphere.channelfw.VirtualConnection, com.ibm.wsspi.tcp.channel.TCPReadRequestContext)
          */
         public void complete(VirtualConnection vc, TCPReadRequestContext ctx)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "complete", new Object[]{vc, ctx});

            completionCallback.complete(getNetworkConnectionInstance(vc), me);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "complete");
         }
         /**
          * Called when the read operation fails with an error.
          *
          * @see com.ibm.wsspi.tcp.channel.TCPReadCompletedCallback#error(com.ibm.websphere.channelfw.VirtualConnection, com.ibm.wsspi.tcp.channel.TCPReadRequestContext, java.io.IOException)
          */
         public void error(VirtualConnection vc, TCPReadRequestContext ctx, IOException ioException)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "error", new Object[]{vc, ctx, ioException});

            completionCallback.error(getNetworkConnectionInstance(vc),
                                     me,
                                     ioException);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "error");
         }
      };

      // Do the read
      if (timeout == NO_TIMEOUT) timeout = TCPReadRequestContext.NO_TIMEOUT;

      VirtualConnection vc = readCtx.read(amountToRead,
                                          callback,
                                          forceQueue,
                                          timeout);

      if (vc != null)
      {
         retConn = getNetworkConnectionInstance(vc);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "read", retConn);
      return retConn;
   }
}
