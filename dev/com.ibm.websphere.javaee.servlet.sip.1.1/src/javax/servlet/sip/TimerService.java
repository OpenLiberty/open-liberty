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
 * Allows SIP servlet applications to set timers in order to receive
 * notifications on timer expiration. Applications receive such
 * notifications through an implementation of the
 * <code>TimerListener</code> interface. Applications using timers must
 * implement this interface and declare it as <code>listener</code>
 * in the SIP deployment descriptor.
 * 
 * <p>SIP servlet containers are requried to make a
 * <code>TimerService</code> instance available to applications
 * through a <code>ServletContext</code> attribute with name
 * <code>javax.servlet.sip.TimerService</code>.
 * 
 * @see TimerListener
 * @see SipApplicationSession#getTimers
 */
public interface TimerService {
    /** 
     * Creates a one-time <code>ServletTimer</code> and schedules it to
     * expire after the specified delay.
     * 
     * @param appSession the application session with which the new
     *     <code>ServletTimer</code> is to be associated
     * @param delay delay in milliseconds before timer is to expire
     * @param isPersistent if true, the <code>ServletTimer</code> will be
     *     reinstated after a shutdown be it due to complete failure
     *     or operator shutdown
     * @param info application information to be delivered along with the
     *     timer expiration notification. This may be null.
     * @return the newly created <code>ServletTimer</code>
     * @throws IllegalStateException if the application session is invalid
     */
    ServletTimer createTimer(SipApplicationSession appSession,
                             long delay,
                             boolean isPersistent,
                             Serializable info) throws IllegalStateException;
    
    /** 
     * Creates a repeating <code>ServletTimer</code> and schedules it to
     * expire after the specified delay and then again at approximately
     * regular intervals.
     * 
     * <p>The <code>ServletTimer</code> is rescheduled to expire in either
     * a <em>fixed-delay</em> or <em>fixed-rate</em> manner as specified
     * by the <code>fixedDelay</code> argument.
     * 
     * <p>The semantics are the same as for {@link java.util.Timer}:
     *
     * <blockquote>
     * In fixed-delay execution, each execution is scheduled relative
     * to the actual execution time of the previous execution. If an
     * execution is delayed for any reason (such as garbage collection
     * or other background activity), subsequent executions will be
     * delayed as well. In the long run, the frequency of execution will
     * generally be slightly lower than the reciprocal of the specified
     * period (assuming the system clock underlying
     * <code>Object.wait(long)</code> is
     * accurate).
     * 
     * <p>In fixed-rate execution, each execution is scheduled relative
     * to the scheduled execution time of the initial execution. If an
     * execution is delayed for any reason (such as garbage collection
     * or other background activity), two or more executions will occur
     * in rapid succession to "catch up." In the long run, the frequency
     * of execution will be exactly the reciprocal of the specified period
     * (assuming the system clock underlying <code>Object.wait(long)</code>
     * is accurate).
     * </blockquote> 
     * 
     * @param appSession the application session with which the new
     *     <code>ServletTimer</code> is to be associated
     * @param delay delay in milliseconds before timer is to expire
     * @param period time in milliseconds between successive timer expirations
     * @param fixedDelay if true, the repeating timer is scheduled in
     *     a fixed-delay mode, otherwise in a fixed-rate mode
     * @param isPersistent if true, the <code>ServletTimer</code> will be
     *     reinstated after a shutdown be it due to complete failure
     *     or operator shutdown
     * @param info application information to be delivered along with the
     *     timer expiration notification. This may be null.
     * @return the newly created <code>ServletTimer</code>
     * @throws IllegalStateException if the application session is invalid
     */
    ServletTimer createTimer(SipApplicationSession appSession,
                             long delay,
                             long period,
                             boolean fixedDelay,
                             boolean isPersistent,
                             Serializable info) throws IllegalStateException;
}
