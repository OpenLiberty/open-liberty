/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.buffer.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool;
import com.ibm.ws.sib.jfapchannel.impl.CommsClientServiceFacade;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;

/**
 * The JFap channel client interface package defines an interface called WsByteBuffer. This looks
 * a lot like a normal WsByteBuffer, but has no dependancies on the Channel Framework. This is
 * needed in the thin client case where we may be running with a Sun JRE (which doesn't come with
 * the IBM channel framework classes). The JFap channel never refers to anything but the local
 * interface of WsByteBuffer.
 * <p>
 * The JFap channel client also defines an abstract class called WsByteBufferPool of which this
 * class extends. When the getInstance() method is invoked on that class this class may be
 * instantiated if we are not running in the Portly client (which cannot guarentee to have access
 * to the channel framework).
 * <p>
 * Now the JFap channel has a reference to this class it will use it for allocating byte buffers.
 * This class must honour them by getting proper WsByteBuffers from the channel framework. It
 * holds a reference to a real WsByteBufferPoolManager but when a buffer is allocated this
 * must be wrapped in the local JFap WsByteBuffer interface (as this is what the JFap channel uses).
 *
 * @author Gareth Matthews
 */
public class RichByteBufferPool extends WsByteBufferPool
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(RichByteBufferPool.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/buffer/impl/RichByteBufferPool.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
   }

   /** The real pool manager */
   private WsByteBufferPoolManager actualPoolManager = null;

   /** The pool of byte buffer wrappers */
   private ObjectPool byteBufferWrapperPool = null;

   /**
    * Constructor.
    */
   public RichByteBufferPool()
   {
      actualPoolManager = CommsClientServiceFacade.getBufferPoolManager();
      byteBufferWrapperPool = new ObjectPool("WsByteBufferWrapperPool", 100);
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool#allocate(int)
    */
   @Override
   public WsByteBuffer allocate(int size)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "allocate", Integer.valueOf(size));

      // Get the real WsByteBuffer from the real Buffer pool
      com.ibm.wsspi.bytebuffer.WsByteBuffer actualBuffer = actualPoolManager.allocate(size);
      RichByteBufferImpl returnBuffer = getFromPool();
      returnBuffer.reset(actualBuffer, this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "allocate", returnBuffer);
      return returnBuffer;
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool#allocateDirect(int)
    */
   @Override
   public WsByteBuffer allocateDirect(int size)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "allocateDirect", Integer.valueOf(size));

      // Get the real WsByteBuffer from the real Buffer pool
      com.ibm.wsspi.bytebuffer.WsByteBuffer actualBuffer = actualPoolManager.allocateDirect(size);
      RichByteBufferImpl returnBuffer = getFromPool();
      returnBuffer.reset(actualBuffer, this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "allocateDirect", returnBuffer);
      return returnBuffer;
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool#wrap(byte[])
    */
   @Override
   public WsByteBuffer wrap(byte[] byteArray)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "wrap", byteArray);

      // Get the real WsByteBuffer from the real Buffer pool
      com.ibm.wsspi.bytebuffer.WsByteBuffer actualBuffer = actualPoolManager.wrap(byteArray);
      RichByteBufferImpl returnBuffer = getFromPool();
      returnBuffer.reset(actualBuffer, this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "wrap", returnBuffer);
      return returnBuffer;
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool#wrap(byte[], int, int)
    */
   @Override
   public WsByteBuffer wrap(byte[] byteArray, int offset, int length)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "wrap", new Object[]{byteArray, offset, length});

      // Get the real WsByteBuffer from the real Buffer pool
      com.ibm.wsspi.bytebuffer.WsByteBuffer actualBuffer = actualPoolManager.wrap(byteArray, offset, length);
      RichByteBufferImpl returnBuffer = getFromPool();
      returnBuffer.reset(actualBuffer, this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "wrap", returnBuffer);
      return returnBuffer;
   }

   /**
    * Wraps an already existing WsByteBuffer in the wrapper.
    *
    * @param realBuffer
    * @return
    */
   public WsByteBuffer wrap(com.ibm.wsspi.bytebuffer.WsByteBuffer realBuffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "wrap", realBuffer);

      RichByteBufferImpl returnBuffer = null;
      // Ensure here that if a null realBuffer was passed in we do not wrap that in a wrapper from
      // the pool - we must also simply return null
      if (realBuffer != null)
      {
         returnBuffer = getFromPool();
         returnBuffer.reset(realBuffer, this);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "wrap", returnBuffer);
      return returnBuffer;
   }

   /**
    * Actual creates the proxy which will handle the calls to the real WsByteBuffer.
    *
    * @param actualBuffer
    * @return
    */
   RichByteBufferImpl getFromPool()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getFromPool");

      RichByteBufferImpl buffer = (RichByteBufferImpl) byteBufferWrapperPool.remove();
      if (buffer == null)
      {
         buffer = new RichByteBufferImpl();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getFromPool", buffer);
      return buffer;
   }

   /**
    * Called when an allocated byte buffer (that has an invocation handler associated with it) is
    * released. At this point we can repool the invocation handler.
    *
    * @param handler
    */
   void release(RichByteBufferImpl handler)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "release", handler);
      byteBufferWrapperPool.add(handler);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "release");
   }
}
