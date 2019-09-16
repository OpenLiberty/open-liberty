/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.util.Iterator;
import java.util.Random;

/**
 * Runnable used to simulate creating a connection, creating requests and then destroying the connection.
 * Ensure clean up is correct.
 */
public class ConcurrentServiceReferenceSetTestWorker implements Runnable {

    private final Random rand = new Random();
    private final String name;
    private final ConcurrentServiceReferenceSetTest owner;

    public ConcurrentServiceReferenceSetTestWorker(String name, ConcurrentServiceReferenceSetTest owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public void run() {
        try {
            Iterator<String> iterator = owner.getServices();
            while (iterator.hasNext()) {
                String s = iterator.next();
                if (s.equals(name)) {
                    owner.removeReference(name);
                } else {
                    owner.addReference(name);
                }
                try {
                    Thread.sleep(rand.nextInt(2));
                } catch (InterruptedException e) {
                }
            }
            owner.addReference(name);
        } catch (Exception e) {
            owner.setException(e);
            e.printStackTrace(System.out);
        }

        // indicate that this connection thread is finished
        owner.finishThread();
    }

    public void setException(Exception e) {
        owner.setException(e);
    }
}
