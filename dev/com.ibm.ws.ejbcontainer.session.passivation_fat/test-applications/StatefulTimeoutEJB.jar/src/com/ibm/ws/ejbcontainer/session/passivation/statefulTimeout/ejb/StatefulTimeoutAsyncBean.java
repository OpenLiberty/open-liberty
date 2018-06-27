/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.StatefulTimeout;

@Stateful
@StatefulTimeout(value = 15, unit = TimeUnit.SECONDS)
public class StatefulTimeoutAsyncBean {
    private long ivLastInvoked = -1;

    public long sync() {
        ivLastInvoked = System.currentTimeMillis();
        return ivLastInvoked;
    }

    @Asynchronous
    public Future<Long> async(long sleepTime) {
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ex) {
            }
        }
        return new AsyncResult<Long>(sync());
    }

    @Asynchronous
    public void fireAndForget() {
        System.out.println("fireAndForget method executed");
    }

    @Remove(retainIfException = true)
    public void remove(boolean throwException) throws MyAppException {
        if (throwException) {
            throw new MyAppException("Exception during remove method");
        }
    }

    @Remove(retainIfException = true)
    @Asynchronous
    public Future<String> removeAsync(boolean throwException) throws MyAppException {
        remove(throwException);
        return new AsyncResult<String>("removeAsync");
    }

    @Asynchronous
    public void throwUncheckedExceptionInFAFAsyncMethod() {
        throw new RuntimeException("now see if you can access this bean again");
    }

    @Asynchronous
    public Future<String> throwUncheckedExceptionInFARAsyncMethod() {
        throw new RuntimeException("now see if you can access this bean again");
    }

    @Asynchronous
    @Remove
    public void throwUncheckedExceptionInFAFAsyncRemoveMethod() {
        throw new RuntimeException("now see if you can access this bean again");
    }

    @Asynchronous
    @Remove(retainIfException = true)
    public void throwUncheckedExceptionInFAFAsyncRemoveWithRetainMethod() {
        throw new RuntimeException("now see if you can access this bean again");
    }
}