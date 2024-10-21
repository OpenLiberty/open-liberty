package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * The EventSemaphore interface provides operations that wait for and post an
 * event semaphore.
 * <p>
 * This is specifically to handle the situation where the event may have been
 * posted before the wait method is called. This behaviour is not supported by
 * the existing wait and notify methods.
 */
public final class EventSemaphore {
    boolean _posted;

    public synchronized void waitEvent() throws InterruptedException {
        while (!_posted) wait();
    }

    public synchronized void post() {
        _posted = true;
        notifyAll();
    }

    public synchronized void clear() {
        _posted = false;
    }
}