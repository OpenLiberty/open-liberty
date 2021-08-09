/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

import java.io.Serializable;

/**
 * Created by the <code>TimerService</code> for servlet applications
 * wishing to schedule future tasks.
 * 
 * @see TimerService
 * @see TimerListener
 */
public interface ServletTimer {

    /**
     * Cancels this timer. If the task has been scheduled for one-time
     * execution and has not yet expired, or has not yet been scheduled, it
     * will never run. If the task has been scheduled for repeated
     * execution, it will never expire again.
     * 
     * <p>Note that calling this method on a repeating <code>ServletTimer</code>
     * from within the <code>timerFired</code> method of a
     * <code>TimerListener</code> absolutely guarantees that the timer
     * will not fire again (unless rescheduled).
     * 
     * <p>This method may be called repeatedly; the second and subsequent
     * calls have no effect.
     */
    void cancel();

    /**
     * Returns the application session associated with this
     * <code>ServletTimer</code>.
     * 
     * @return application session associated with this
     * <code>ServletTimer</code>
     */
    SipApplicationSession getApplicationSession();

    /**
     * Returns a string containing the unique identifier assigned to this 
     * timer task. The identifier is assigned by the servlet container and 
     * is implementation dependent. 
     * 
     * @return a string specifying the identifier assigned to this session
     */
    String getId();
    
    
    /**
     * Get the information associated with the timer at the time of
     * creation.
     * 
     * @return the <code>Serializable</code> object that was passed in
     *     at timer creation, or <code>null</code> if the info argument
     *     passed in at timer creation was <code>null</code>.
     */
    Serializable getInfo();
    
    /**
     * Get the number of milliseconds that will elapse before the next 
     * scheduled timer expiration. For a one-time timer that has already 
     * expired (i.e., current time > scheduled expiry time) this method 
     * will return the time remaining as a negative value.
     *  
     * @return the number of milliseconds that will elapse before the next 
     * 		   scheduled timer expiration.
     */
    long getTimeRemaining();    
    
    /**
     * Returns the scheduled expiration time of the most recent actual
     * expiration of this timer.
     * 
     * <p>This method is typically invoked from within
     * <code>TimerListener.timerFired</code> to determine whether the
     * timer callback was sufficiently timely to warrant performing the
     * scheduled activity:
     * 
     * <pre>
     *   public void run() {
     *       if (System.currentTimeMillis() - scheduledExecutionTime() >=
     *           MAX_TARDINESS)
     *               return;  // Too late; skip this execution.
     *       // Perform the task
     *   }
     * </pre>
     *
     * <p>This method is typically not used in conjunction with fixed-delay
     * execution repeating tasks, as their scheduled execution times are
     * allowed to drift over time, and so are not terribly significant.
     * 
     * @return the time at which the most recent expiration of this timer
     *     was scheduled to occur, in the format returned by
     *     <code>Date.getTime()</code>.
     *     The return value is undefined if the timer has yet to expire for
     *     the first time.
     */
    long scheduledExecutionTime();
}
