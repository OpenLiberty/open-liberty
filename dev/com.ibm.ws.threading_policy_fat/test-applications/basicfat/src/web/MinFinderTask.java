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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Task that computes the minimum number within a list by breaking down the computation into smaller tasks.
 * This can be used to test a task that submits other tasks in multiple layers.
 */
public class MinFinderTask implements Callable<Integer> {
    private final int[] array;
    private final int begin, end;
    private final ExecutorService executor;

    public MinFinderTask(int[] array, ExecutorService executor) {
        this.array = array;
        this.begin = 0;
        this.end = array.length - 1;
        this.executor = executor;
    }

    public MinFinderTask(int[] array, int begin, int end, ExecutorService executor) {
        this.array = array;
        this.begin = begin;
        this.end = end;
        this.executor = executor;
    }

    @Override
    public Integer call() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("> call " + toString() + " for index " + begin + " to " + end);
        try {
            int mid = (begin + end) / 2;
            Future<Integer> part1 = begin == mid ? null : executor.submit(new MinFinderTask(array, begin, mid, executor));
            Future<Integer> part2 = mid + 1 >= end ? null : executor.submit(new MinFinderTask(array, mid + 1, end, executor));
            int min1 = part1 == null ? array[begin] : part1.get(PolicyExecutorServlet.TIMEOUT_NS, TimeUnit.NANOSECONDS);
            int min2 = part2 == null ? array[end] : part2.get(PolicyExecutorServlet.TIMEOUT_NS, TimeUnit.NANOSECONDS);
            int min = min1 < min2 ? min1 : min2;
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
