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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Task that very inefficiently computes the factorial of a number by recursively submitting itself.
 */
public class FactorialTask implements Callable<Long> {
    volatile int num;
    private final ExecutorService executor;

    public FactorialTask(int num, ExecutorService executor) {
        this.num = num;
        this.executor = executor;
    }

    @Override
    public Long call() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("> call " + toString() + " for " + num);
        try {
            long factorial = num < 3 ? num-- : (num-- * executor.submit(this).get(PolicyExecutorServlet.TIMEOUT_NS, TimeUnit.NANOSECONDS));
            System.out.println("< call " + toString() + ' ' + factorial);
            return factorial;
        } catch (ExecutionException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        } catch (InterruptedException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        } catch (TimeoutException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        }
    }
}
