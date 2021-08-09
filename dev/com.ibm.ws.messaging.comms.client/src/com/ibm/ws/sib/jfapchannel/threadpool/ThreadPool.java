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
package com.ibm.ws.sib.jfapchannel.threadpool;

/**
 * An interface which wrappers the thin and rich implementations of a thread pool.
 * 
 * @author Gareth Matthews
 */
public interface ThreadPool
{
   /**
    * Specifies that a dispatch should throw an exception if
    * the request queue is already full.
    *
    * @see com.ibm.ws.util.ThreadPool
    */
   public static final int ERROR_WHEN_QUEUE_IS_FULL = 1;
   
   /**
    * Initialises the thread pool wrapper with the specified name and sizes. This method must be
    * called before attempting to use the thread pool.
    * 
    * @param name The name of the thread pool.
    * @param minSize The minimum size of the thread pool.
    * @param maxSize The maximum size of the thread pool.
    */
   public void initialise(String name, int minSize, int maxSize);
   
   /**
    * Sets whether the thread pool is allowed to grow as needed.
    * 
    * @param growAsNeeded
    */
   public void setGrowAsNeeded(boolean growAsNeeded);

   /**
    * Executes the specified runnable using a thread from the pool.
    * 
    * @param runnable
    * @param was_thread_mode
    * 
    * @throws InterruptedException
    * @throws IllegalStateException
    * @throws ThreadPoolFullException
    */
   public void execute(Runnable runnable, int was_thread_mode) 
      throws InterruptedException, IllegalStateException, ThreadPoolFullException;

   /**
    * Executes the specified runnable using a thread from the pool.
    * 
    * @param runnable
    * 
    * @throws InterruptedException
    * @throws IllegalStateException
    */
   public void execute(Runnable runnable)
      throws InterruptedException, IllegalStateException;

   /**
    * Sets the keep-alive time.
    * 
    * @param msecs
    */
   public void setKeepAliveTime(long msecs);
   
   /**
    * Sets the size of the request buffer.
    * 
    * @param size
    */
   public void setRequestBufferSize(int size);
}
