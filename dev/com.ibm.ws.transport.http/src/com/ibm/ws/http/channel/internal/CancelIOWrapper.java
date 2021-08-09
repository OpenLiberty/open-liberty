/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

/**
 * Wrapper to handle synchronizing and controlling all the logic surrounding
 * attempts to cancel an IO request below the HTTP channel.
 * 
 */
public class CancelIOWrapper {

    /** Flag on whether a cancel attempt is active or not */
    private boolean active = false;
    /** Flag on whether the last cancel attempt failed to get canceled */
    private boolean failed = false;
    /** Sync lock around state changes */
    private Object lock = new Object()
    {
                    };

    /**
     * Constructor with default values.
     */
    public CancelIOWrapper() {
        // nothing to do
    }

    /**
     * Clear the cancel flags back to the default values.
     */
    public void clear() {
        this.active = false;
        this.failed = false;
    }

    /**
     * Initialize a cancel attempt.
     */
    public void init() {
        this.active = true;
        this.failed = false;
    }

    /**
     * A cancel attempt is first a call to init(), then the TCP level immediate
     * timeout call, and then this API to block waiting for success or failure
     * on that timeout attempt.
     * 
     * @param time
     *            - amount of time in milliseconds to wait for completion
     * @return boolean - returns flag on whether the cancel attempt worked or not
     */
    public boolean block(long time) {
        synchronized (this.lock) {
            // see if we've already canceled the IO
            if (!this.active) {
                return true;
            }
            // see if the cancel missed and the IO complete path was triggered
            if (this.failed) {
                this.active = false;
                return false;
            }
            try {
                this.lock.wait(time);
            } catch (InterruptedException ie) {
                // no FFDC required
            }
            // now check whether it worked, failed, or timed out
            if (this.active) {
                this.active = false;
                return false;
            }
        } // end-sync
        return true;
    }

    /**
     * If the cancel attempt failed to complete in time, this API will record
     * that failure and wake any thread that was waiting for a status change.
     * 
     */
    public void failure() {
        if (!this.active) {
            // no active cancel attempt to mark as failed
            return;
        }
        synchronized (this.lock) {
            if (this.active) {
                this.failed = true;
                this.lock.notify();
            }
        }
    }

    /**
     * Attempt to mark this cancel IO path as a success. This still may fail
     * if the block delay timed out waiting for a status change.
     * 
     * @return boolean on whether this happened in time
     */
    public boolean success() {
        synchronized (this.lock) {
            if (this.active) {
                // successfully canceled the IO request, wake up the blocker
                this.active = false;
                this.lock.notify();
                return true;
            }
            // too late to complete, no longer active
            return false;
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("active=").append(this.active);
        sb.append(" failed=").append(this.failed);
        sb.append(' ').append(super.toString());
        return sb.toString();
    }
}
