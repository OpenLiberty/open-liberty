/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.concurrency32cdi.web;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.concurrent.Lock;
import jakarta.enterprise.context.RequestScoped;

/**
 * A bean with some methods that are annotated Lock, some of which invoke
 * other methods of the bean that are annotated Lock.
 */
@RequestScoped
public class LoopbackBean {
    /**
     * Maximum amount of time for an operation to remain delayed.
     */
    static final long MAX_DELAY_MS = TimeUnit.MINUTES.toMillis(5);

    private long number;

    // For WRITE to WRITE
    @Lock(type = Lock.Type.WRITE,
          accessTimeout = 100) // seconds
    public void clear(LoopbackBean bean) {
        bean.setNumber(0);
    }

    // for WRITE to unannotated (implicit WRITE)
    @Lock(type = Lock.Type.WRITE)
    public long decreaseBy(long amount, LoopbackBean bean) {
        long decremented = Long.MIN_VALUE; // uninitialized
        for (long l = 0; l < amount; l++)
            decremented = bean.decrementAndGet(bean);
        return decremented;
    }

    // defaults to @Lock, which defaults to WRITE 60 SECONDS
    public long decrementAndGet(LoopbackBean bean) {
        return --number;
    }

    // for READ to unannotated (implicit WRITE): error
    @Lock(type = Lock.Type.READ)
    public void doubleDecrement(LoopbackBean bean) {
        bean.decrementAndGet(bean);
        bean.decrementAndGet(bean);
    }

    @Lock(type = Lock.Type.READ,
          accessTimeout = Lock.UNLIMITED)
    public long getNumber() {
        return number;
    }

    // For WRITE to READ
    @Lock(type = Lock.Type.WRITE,
          accessTimeout = Lock.IMMEDIATE)
    public void increment(LoopbackBean bean) {
        number = bean.getNumber() + 1;
    }

    // For READ to READ to READ to ...
    @Lock(type = Lock.Type.READ,
          accessTimeout = Lock.IMMEDIATE)
    public long power(long exponent, LoopbackBean bean) {
        if (exponent < 1)
            return 1;
        else
            return number * bean.power(exponent - 1, bean);
    }

    @Lock(type = Lock.Type.WRITE,
          accessTimeout = Lock.IMMEDIATE)
    public long setNumber(long number) {
        long previous = this.number;
        this.number = number;
        return previous;
    }

    // For READ to WRITE: error
    @Lock(type = Lock.Type.READ,
          accessTimeout = Lock.IMMEDIATE)
    public void square(LoopbackBean bean) {
        bean.setNumber(number * number);
    }
}