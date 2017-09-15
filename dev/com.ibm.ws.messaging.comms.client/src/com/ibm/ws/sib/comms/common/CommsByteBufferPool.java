/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.common;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;

/**
 * The comms byte buffer pool.
 * 
 * @author Gareth Matthews
 */
public class CommsByteBufferPool
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(CommsByteBufferPool.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);
   
   /** The singleton instance of this class */
   private static CommsByteBufferPool instance = null;
   
   /**
    * @return Returns the byte buffer pool.
    */
   public static synchronized CommsByteBufferPool getInstance()
   {
      if (instance == null)
      {
         instance = new CommsByteBufferPool();
      }
      return instance;
   }

   
   /** Our object pool */
   private ObjectPool pool = null;

   /**
    * Constructs the initial pool.
    */
   protected CommsByteBufferPool()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
      pool = new ObjectPool(getPoolName(), 100);
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Gets a CommsByteBuffer from the pool.
    * 
    * @return CommsString
    */
   public synchronized CommsByteBuffer allocate()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "allocate");
   
      // Remove a CommsByteBuffer from the pool
      CommsByteBuffer buff = (CommsByteBuffer) pool.remove();
   
      // If the buffer is null then there was none available in the pool
      // So create a new one
      if (buff == null)
      {
         if (tc.isDebugEnabled()) SibTr.debug(this, tc, "No buffer available from pool - creating a new one");
      
         buff = createNew();
      }
   
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "allocate", buff);
      return buff;
   }
   
   /**
    * Creates a new buffer. This method may be overriden to create a new type of CommsByteBuffer.
    * 
    * @return Returns the new buffer.
    */
   protected CommsByteBuffer createNew()
   {
      return new CommsByteBuffer(this);
   }
   
   /**
    * @return Returns the name for the pool.
    */
   protected String getPoolName()
   {
      return "CommsByteBufferPool";
   }

   /**
    * Returns a buffer back to the pool so that it can be re-used.
    * 
    * @param buff
    */
   synchronized void release(CommsByteBuffer buff)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "release", buff);
      buff.reset();
      pool.add(buff);
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "release");
   }
}
