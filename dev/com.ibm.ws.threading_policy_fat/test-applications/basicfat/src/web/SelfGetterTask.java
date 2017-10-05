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
package web;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.ws.threading.PolicyTaskCallback;

/**
 * Task which obtains its own Future from a callback and later invokes get() on that Future when it runs.
 * This is clearly an error path, as a task cannot successfully await its own completion.
 * The executor should detect this and immediately interrupt the get() operation to prevent hangs.
 */
public class SelfGetterTask extends PolicyTaskCallback implements Callable<Object> {
    private Future<?> future;
    private final long timeout;
    private final TimeUnit unit;

    public SelfGetterTask() {
        this.timeout = -1;
        this.unit = null;
    }

    public SelfGetterTask(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public Object call() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("> call " + toString());
        try {
            Object result;
            if (timeout == -1 || unit == null)
                result = future.get();
            else
                result = future.get(timeout, unit);
            System.out.println("< call " + toString() + " " + result);
            return result;
        } catch (ExecutionException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        } catch (InterruptedException x) {
            System.out.println("< call " + toString() + " " + x);
            return x; // expected, so we return as a result rather than an exception
        } catch (RuntimeException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        }
    }

    @Override
    public void onSubmit(Object task, Future<?> future) {
        this.future = future;
    }
}
