/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 *
 */
@SuppressWarnings("serial")
public class ControlledAccessBlockingQueue extends ArrayBlockingQueue<Runnable> {

    Semaphore semaphore = new Semaphore(1);

    /**
     * @param capacity
     * @throws InterruptedException
     */
    public ControlledAccessBlockingQueue(int capacity) {
        super(capacity);
        try {
            semaphore.acquire(); // acquire the first one, so all access requires a call to allowAccess()
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Runnable take() throws InterruptedException {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        return super.take();
    }

    void allowAccess() throws InterruptedException {
        semaphore.release();
    }
}
