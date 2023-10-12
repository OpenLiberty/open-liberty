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

import static org.junit.Assert.fail;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 */
public class ThreadSubscriber implements Flow.Subscriber<ThreadSnapshot> {

    Flow.Subscription subscription = null;

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(ThreadSnapshot ts) {
        try {
            new InitialContext().lookup("java:comp/env/entry1");
        } catch (NamingException e) {
            fail("Could not lookup context on subscriber thread");
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        fail("onError should not have occured, see Log for Stack Trace");
        throwable.printStackTrace(System.out);
    }

    @Override
    public void onComplete() {
    }

}
