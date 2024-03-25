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

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 */
public class ThreadSubscriber implements Flow.Subscriber<ContextCDL> {

    private Flow.Subscription subscription = null;
    public Object onCompleteResult = null;
    public Object onErrorResult = null;

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(ContextCDL latch) {

        try {
            latch.checkContext();
            latch.countDown();
            subscription.request(1);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        try {
            onErrorResult = new InitialContext().lookup("java:comp/env/entry1");
        } catch (NamingException e) {
            onErrorResult = e;
        }
    }

    @Override
    public void onComplete() {
        try {
            onCompleteResult = new InitialContext().lookup("java:comp/env/entry1");
        } catch (NamingException e) {
            onCompleteResult = e;
        }

    }

}
