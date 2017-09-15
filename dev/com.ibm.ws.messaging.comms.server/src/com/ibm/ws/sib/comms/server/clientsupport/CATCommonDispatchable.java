/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.ws.sib.jfapchannel.DispatchQueue;
import com.ibm.ws.sib.jfapchannel.Dispatchable;

/**
 * In the Comms layer we usually want the JFap channel to dispatch work to our receive listener
 * by Conversation - ie so that all data for a single conversation is executed serially. 
 * However, to increase throughput, we can allow the JFap channel to dispatch work more efficiently
 * than that. For example, work done under a transaction could be done on a different thread to that
 * of non-transacted work, regardless of the Conversation.
 * <p>
 * This class provides the base functionality in allowing an object to be dispatchable. It should be
 * used by for example the CATTransaction class, which can then extend this class and be returned
 * when the JFap channel calls the getThreadContext method on the receive listener.
 * 
 * @author Gareth Matthews
 */
public abstract class CATCommonDispatchable implements Dispatchable
{
   /** The dispatch queue this object is using */
   private DispatchQueue dispatchQueue = null;

   /** The lock object */
   private Object lock = new Object();
   
   /** The dispatch queue reference count */
   private int refCount = 0;

   /**
    * Sets the dispatch queue for this object.
    * 
    * @param queue
    */
   public void setDispatchQueue(DispatchQueue queue)
   {
      this.dispatchQueue = queue;
   }

   /**
    * @return Returns the dispatch queue for this object.
    */
   public DispatchQueue getDispatchQueue()
   {
      return dispatchQueue;
   }

   /**
    * @return Returns an object that can be synchronized on before doing anything with this object.
    */
   public Object getDispatchLockObject()
   {
      return lock;
   }

   /**
    * Increments the dispatch queue ref count.
    */
   public void incrementDispatchQueueRefCount()
   {
      ++refCount;
   }

   /**
    * Decrements the dispatch queue ref count.
    */
   public void decrementDispatchQueueRefCount()
   {
      --refCount;
   }

   /**
    * @return Returns the dispatch queue ref count.
    */
   public int getDispatchQueueRefCount()
   {
      return refCount;
   }

}
