/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.utils;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A non re-enterent semaphore implementation.
 */
public class Semaphore
{
  private static final TraceComponent tc = SibTr.register(Semaphore.class, UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);

   private int count = 0;


  /**
   * Creates a semaphore which will block on the very first wait.
   */
  public Semaphore()
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "<init>");
    count = 0;
    if (tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
  }

  /**
   * Creates a semaphore which will require the specified number of waits (with no
   * corresponding posts) before blocking.
   * @param initialCount How many waitOn operations will be allowed before the
    * first blocks.
   */
  public Semaphore(int initialCount)
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "<init>", ""+initialCount);
    count = -initialCount;
    if (tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
  }

  /**
   * Wait for the semaphore to be posted
    * @see Semaphore#post()
    * @throws InterruptedException if the wait is interrupted rather than posted
   */
   public synchronized void waitOn() throws InterruptedException
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "waitOn", ""+count);

      ++count;
      if (count > 0)
      {
         try
         {
            wait();
         }
         catch(InterruptedException e)
         {
            // No FFDC code needed
            --count;
            throw e;
         }
      }

    if (tc.isEntryEnabled()) SibTr.exit(tc, "waitOn");
   }

   /**
    * Post the semaphore waking up at most one waiter.  If there are no
    * waiters, then the next thread issuing a waitOn call will not be
    * suspended.  In fact, if post is invoked 'n' times then the next
    * 'n' waitOn calls will not block.
    * @see Semaphore#waitOn()
    */
   public synchronized void post()
   {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "post", ""+count);
      --count;
      if (count >= 0)
         notify();
    if (tc.isEntryEnabled()) SibTr.exit(tc, "post");
   }

   /**
    * Wait on the semaphore ignoring any attempt to interrupt the thread.
    * @see Semaphore#waitOn()
    */
   public synchronized void waitOnIgnoringInterruptions()
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "waitOnIgnoringInterruptions");

      boolean interrupted;
      do
      {
         interrupted = false;
         try
         {
            waitOn();
         }
         catch (InterruptedException e)
         {
            // No FFDC code needed
            interrupted = true;
         }
      }
      while(interrupted);

      if (tc.isEntryEnabled()) SibTr.exit(tc, "waitOnIgnoringInterruptions");
   }

}
