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
package com.ibm.ws.tcpchannel.internal;

/**
 * Simple sync block object.
 */
public class SimpleSync {

    boolean notifyOn = false;

    protected void simpleWait() {
        synchronized (this) {
            if (notifyOn) {
                // return right away if notify is outstanding
                notifyOn = false;
                return;
            }
            // else wait
            try {
                this.wait();
                notifyOn = false;
            } catch (InterruptedException x) {
                // do nothing
            }
        } // end-sync
    }

    protected void simpleNotify() {
        synchronized (this) {
            notifyOn = true;
            this.notify();
        }
    }

}
