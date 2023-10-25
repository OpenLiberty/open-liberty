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

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import javax.naming.NamingException;

/**
 *
 */
public class ThreadProcessor extends SubmissionPublisher<ContextCDL> implements Flow.Processor<ContextCDL, ContextCDL> {

    Flow.Subscription subscription = null;

    //Subscriber methods
    public ThreadProcessor(Executor ex, BiConsumer<? super Subscriber<? super ContextCDL>, ? super Throwable> handler) {
        super(ex, 3, handler);
    }

    @Override
    public int offer(ContextCDL latch, BiPredicate<Subscriber<? super ContextCDL>, ? super ContextCDL> onDrop) {
        try {
            latch.checkContext();
            return super.offer(latch, onDrop);
        } catch (NamingException e) {
            closeExceptionally(e);
        }
        return -1;
    }

    //Publisher methods
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
            offer(latch, null);
            subscription.request(1);
        } catch (NamingException e) {
            closeExceptionally(e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
    }

}
