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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;

import javax.naming.NamingException;

import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 *
 */
public class MutinySubscriber implements MultiSubscriber<ContextCDL> {
    private Flow.Subscription subscription = null;
    private CompletableFuture<Object> result = new CompletableFuture<>();

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onFailure(Throwable e) {
        result.complete(e);
    }

    @Override
    public void onItem(ContextCDL latch) {
        try {
            latch.checkContext();
            latch.countDown();
            subscription.request(1);
            result.complete(latch);
        } catch (NamingException e) {
            result.complete(new AssertionError("Context unavailable in onItem").initCause(e));
        }

    }

    @Override
    public void onCompletion() {

    }

    public CompletableFuture<Object> getResult() {
        return result;
    }

}
