/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package reactiveapp.web;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;

import javax.naming.NamingException;

/**
 *
 */
public class ThreadSubscriber implements Flow.Subscriber<ContextCDL> {

    private Flow.Subscription subscription = null;
    private Throwable closedException = null;

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(ContextCDL latch) {
        System.out.println("subscriber onNext");

        try {
            latch.checkContext();
            latch.countDown();
            subscription.request(1);
        } catch (NamingException e) {
            closeExceptionally(e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        closeExceptionally(throwable);
        throwable.printStackTrace(System.out);
    }

    @Override
    public void onComplete() {
    }

    public void closeExceptionally(Throwable t) {
        closedException = t;
    }

    /**
     * Returns the exception associated with closeExceptionally, or null if not closed or if closed normally.
     *
     * @return the exception, or null if none
     */
    public Throwable getClosedException() {
        return closedException;
    }

}
