/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.concurrent.spec.app;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Create a Runnable from a Callable
 */
class RunnableFromCallable<T> implements Runnable {
    private final Callable<T> callable;
    private final LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

    RunnableFromCallable(Callable<T> callable) {
        this.callable = callable;
    }

    @SuppressWarnings("unchecked")
    T poll(long timeout, TimeUnit units) throws ExecutionException, InterruptedException {
        Object result = results.poll(timeout, units);
        if (result instanceof Throwable)
            throw new ExecutionException((Throwable) result);
        else
            return (T) result;
    }

    @Override
    public void run() {
        try {
            results.add(callable.call());
        } catch (Throwable x) {
            results.add(x);
        }
    }
}
