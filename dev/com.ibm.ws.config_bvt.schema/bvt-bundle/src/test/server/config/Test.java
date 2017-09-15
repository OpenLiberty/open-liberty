/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import test.server.BaseTest;

public abstract class Test extends BaseTest {

    protected CountDownLatch latch;
    protected Throwable exception;

    public Test(String name) {
        this(name, 1);
    }

    public Test(String name, int count) {
        super(name);
        this.latch = new CountDownLatch(count);
    }

    public Throwable getException() {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                return new RuntimeException("Timed out");
            }
        } catch (InterruptedException e) {
            return new RuntimeException("Interrupted");
        }
        return exception;
    }

}
