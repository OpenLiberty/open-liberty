/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.client.internal;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import io.openliberty.restfulWS.client.ClientAsyncTaskWrapper;

public abstract class AsyncClientExecutorHelper {

    static AtomicReference<AsyncClientExecutorHelper> instance = new AtomicReference<>();

    public static Runnable wrap(Runnable r) {
        AsyncClientExecutorHelper instance = AsyncClientExecutorHelper.instance.get();
        if (instance != null) {
            for (ClientAsyncTaskWrapper wrapper : instance.getTaskWrappers()) {
                try {
                    Runnable wrapped = wrapper.wrap(r);
                    if (wrapped == null) {
                        throw new NullPointerException("ClientAsyncTaskWrapper " + wrapper + " returned null");
                    }
                    r = wrapped;
                } catch (Exception e) {
                    // FFDC
                }
            }
            // The ThreadContext wrapper needs to be last so that it is run first before any other wrappers.
            r = instance.getThreadContextWrapper().wrap(r);
        }
        return r;
    }

    public static <T> Callable<T> wrap(Callable<T> c) {
        AsyncClientExecutorHelper instance = AsyncClientExecutorHelper.instance.get();
        if (instance != null) {
            for (ClientAsyncTaskWrapper wrapper : instance.getTaskWrappers()) {
                try {
                    Callable<T> wrapped = wrapper.wrap(c);
                    if (wrapped == null) {
                        throw new NullPointerException("ClientAsyncTaskWrapper " + wrapper + " returned null");
                    }
                    c = wrapped;
                } catch (Exception e) {
                    // FFDC
                }
            }
            // The ThreadContext wrapper needs to be last so that it is run first before any other wrappers.
            c = instance.getThreadContextWrapper().wrap(c);
        }
        return c;
    }

    abstract public ExecutorService getExecutorService();

    abstract List<ClientAsyncTaskWrapper> getTaskWrappers();

    abstract ClientAsyncTaskWrapper getThreadContextWrapper();
}
