/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee9.internal.tests.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Parallel executor.
 *
 * Perform an action on supplied parameter values.
 * Accumulate any errors into a shared errors collection.
 *
 * Use threads
 */
public class FATExecutor {
    public static interface FATAction {
        void run(Queue<String> parms, List<String> errors);
    }

    /**
     * Compute the number of threads which are to be used.
     * Do not use more threads than a specified maximum.
     *
     * Default the number of available processors, minus one,
     * bounded to a minimum value of 2 and a maximum value
     * of 4.
     *
     * See {@link Runtime#availableProcessors()}.
     *
     * @return The number of threads which are to be used.
     */
    private static int getThreadCount(int max) {
        // P:     1  2  3  4  5  6
        // M:  0  0  0  0  0  0  0
        //     1  1  1  1  1  1  1
        //     2  2  2  2  2  2  2
        //     3  2  2  2  3  3  3
        //     4  2  2  2  3  4  4
        //     5  2  2  2  3  4  4
        //     6  2  2  2  3  4  4

        if ((max == 0) || (max == 1) || (max == 2)) {
            return max;
        }

        // 'max' must be >= 3.

        int processors = Runtime.getRuntime().availableProcessors();
        if (processors <= 3) {
            return 2; // Use at least 2.
        }

        processors--; // Try to use one less less than available.

        if (processors > 4) {
            processors = 4; // Use no more than 4.
        }

        if (processors > max) {
            processors = max; // Use no more than the maximum.
        }

        return processors;
    }

    public static <T> T any(Collection<? extends T> storage) {
        Iterator<? extends T> iterator = storage.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            throw new IndexOutOfBoundsException("Element requested from empty collection");
        }
    }

    /**
     * Run an action in parallel against a collection
     * of parameters. Use a number of threads according to
     * the system configuration and according to the count of
     * available parameters.
     *
     * @param action Action which accepts a queue of parameters
     *                   and performs processing, polling parameters, and adding
     *                   to a shared errors list.
     * @param parms  Parameters which are to be processed one at a time.
     *
     * @return Errors collection from the action applied to the parameters.
     *         Errors will not usually be in the same order as the parameters.
     *
     * @throws InterruptedException Thrown if the threaded processing is
     *                                  interrupted.
     */
    public static List<String> run(FATAction action, Collection<String> parms) throws InterruptedException {
        final List<String> errors = new CopyOnWriteArrayList<>();

        int threadCount = getThreadCount(parms.size());

        if (threadCount == 0) {
            // Nothing to do!

        } else if (threadCount == 1) {
            // No need for multi-threading.
            action.run(new ArrayDeque<String>(parms), errors);

        } else {
            // Launch the indicated number of threads.  Each is expected
            // to loop, polling the parameters and performing processing
            // on each polled value, until no values are available.

            final Queue<String> parmsQueue = new ConcurrentLinkedQueue<>(parms);

            Thread[] threads = new Thread[threadCount];
            for (int threadNo = 0; threadNo < threadCount; ++threadNo) {
                threads[threadNo] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        action.run(parmsQueue, errors);
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join(); // throws InterruptedException
            }
        }

        return errors;
    }
}
