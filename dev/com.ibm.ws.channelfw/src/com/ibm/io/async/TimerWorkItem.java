/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

/**
 * The work item that will be queued from the requesting thread to the timer
 * thread. get/set methods are not used for accessing variables in order to help
 * performance, also no synchronization is done within this class.
 */
public class TimerWorkItem {

    // public final static long START_TIMER_REQUEST = 0;
    // public final static long CANCEL_TIMER_REQUEST = 1;

    /** State indicating this timer item is currently active */
    public final static long ENTRY_ACTIVE = 1L;
    /** State indicating this timer item is currently cancelled */
    public final static long ENTRY_CANCELLED = 2L;

    /** the time at which the timeout should trigger */
    long timeoutTime = 0L;

    /** Current state of this work item */
    public long state = ENTRY_ACTIVE;

    /** Callback used if/when this times out */
    TimerCallback callback = null;

    /** Attachment used during callback usage */
    Object attachment = null;

    /** ID used to protect against rapid timeout conflicts */
    int futureCount = 0;

    /**
     * Empty Constructor
     */
    public TimerWorkItem() {
        // nothing to do
    }

    /**
     * Constructor.
     * 
     * @param _timeoutTime
     * @param _callback
     * @param _attachment
     * @param _futureCount
     */
    public TimerWorkItem(long _timeoutTime,
                         TimerCallback _callback,
                         Object _attachment,
                         int _futureCount) {

        this.timeoutTime = _timeoutTime;
        this.callback = _callback;
        this.attachment = _attachment;
        this.futureCount = _futureCount;
    }

}
