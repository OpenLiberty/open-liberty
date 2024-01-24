/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.compat.impl;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * CDI Extension for Jakarta Concurrency 3.1+ in Jakarta EE 11+, which corresponds to CDI 4.1+
 */
public class ContextualSubscriber<T> implements Subscriber<T>, ContextualAction<Subscriber<T>> {
    private final Subscriber<T> action;
    private final ThreadContextDescriptor threadContextDescriptor;

    public ContextualSubscriber(ThreadContextDescriptor threadContextDescriptor, Subscriber<T> action) {
        this.action = action;
        this.threadContextDescriptor = threadContextDescriptor;
    }

    @Override
    @Trivial
    public Subscriber<T> getAction() {
        return action;
    }

    @Override
    @Trivial
    public ThreadContextDescriptor getContextDescriptor() {
        return threadContextDescriptor;
    }

    @Override
    public void onComplete() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onError(Throwable throwable) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onNext(T item) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        // TODO Auto-generated method stub
    }
}