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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Task that computes the minimum number within a list by breaking down the computation into smaller tasks.
 * This can be used to test a task that invokes other tasks in multiple layers.
 */
public class MinFinderTaskWithInvokeAll implements Callable<Integer> {
    private final int[] array;
    private final int begin, end;
    private final ExecutorService executor;

    public MinFinderTaskWithInvokeAll(int[] array, ExecutorService executor) {
        this.array = array;
        this.begin = 0;
        this.end = array.length - 1;
        this.executor = executor;
    }

    public MinFinderTaskWithInvokeAll(int[] array, int begin, int end, ExecutorService executor) {
        this.array = array;
        this.begin = begin;
        this.end = end;
        this.executor = executor;
    }

    @Override
    public Integer call() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("> call " + toString() + " for index " + begin + " to " + end);
        try {
            int min0, min1;
            if (end - begin > 1) {
                int mid = (begin + end) / 2;

                List<Future<Integer>> futures = executor.invokeAll(Arrays.asList(new MinFinderTaskWithInvokeAll(array, begin, mid, executor),
                                                                                 new MinFinderTaskWithInvokeAll(array, mid + 1, end, executor)));

                min0 = futures.get(0).get(0, TimeUnit.SECONDS);
                min1 = futures.get(1).get(0, TimeUnit.SECONDS);
            } else {
                min0 = array[begin];
                min1 = array[end];
            }
            int min = min0 < min1 ? min0 : min1;
            System.out.println("< call " + toString() + ' ' + min);
            return min;
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
