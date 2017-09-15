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
package com.ibm.ws.sib.jfapchannel;

/**
 * This interface should be implemented by classes who the JFap channel should dispatch work on.
 * For example, the JFap channel will dispatch data for the same conversation to the same 
 * dispatch queue (in most circumstances) and so the conversation should implement this interface.
 * <p>
 * The JFap channel will ask the conversation receive listener for a dispatchable object that the
 * data should be dispatched on. If the conversation receive listener returns null, the JFap channel
 * will queue data by conversation. 
 * 
 * @author Gareth Matthews
 */
public interface Dispatchable
{
   /**
    * This method should be called by JFap channel when it is allocating a new DispatchQueue for
    * this dispatchable object.
    * @param queue The queue to use.
    */
   public void setDispatchQueue(DispatchQueue queue);
   
   /**
    * Returns the current dispatch queue associated with this dispatchable object. If this returns
    * null, the JFap channel will allocate a new queue and associate it by calling the 
    * setDispatchQueue method.
    * @return Returns the dispatch queue or null if one has been associated.
    */
   public DispatchQueue getDispatchQueue();
   
   /**
    * @return Returns an object that should be synchronized on when modifying the reference count
    *         and queue.
    */
   public Object getDispatchLockObject();
   
   /**
    * Since a dispatch queue can hold multiple items of data from the same dispatcable, as well as
    * multiple items of data for other dispatchables, a dispatchable should maintain a reference 
    * count. This way, when the reference count is zero the queue can be dis-associated with this
    * dispatchable.
    * <p>
    * This method increments the use count and should be called when an item of data is added to
    * the associated queue.
    */
   public void incrementDispatchQueueRefCount();
   
   /**
    * Since a dispatch queue can hold multiple items of data from the same dispatcable, as well as
    * multiple items of data for other dispatchables, a dispatchable should maintain a reference 
    * count. This way, when the reference count is zero the queue can be dis-associated with this
    * dispatchable.
    * <p>
    * This method decrements the use count and should be called when an item of data is removed from
    * the associated queue.
    */   
   public void decrementDispatchQueueRefCount();
   
   /**
    * This method returns the reference count for this dispatchable. A reference count of zero 
    * indicates that it is safe to disassociate the dispatch queue with this dispatchable.
    * @return Returns the current reference count of this dispatchable on the dispatch queue.
    */
   public int getDispatchQueueRefCount();
}
