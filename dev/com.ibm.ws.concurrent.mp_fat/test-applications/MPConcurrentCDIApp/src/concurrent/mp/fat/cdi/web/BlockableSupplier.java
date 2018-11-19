/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.mp.fat.cdi.web;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Supplier that can block for a period of time when it runs.
 * Latches are provided to the constructor for the test case which uses this function
 * to know when it has started and control when it is allowed to complete.
 */
public class BlockableSupplier<T> implements Supplier<T> {
    /**
     * This latch, if supplied, is counted down when the function begins.
     */
    private final CountDownLatch beginLatch;

    /**
     * This latch, if supplied, is awaited before the function returns a value.
     */
    private final CountDownLatch continueLatch;

    /**
     * Thread upon which the supplier is running.
     */
    volatile Thread executionThread;

    /**
     * Value that is supplied by this supplier.
     */
    private final T value;

    /**
     * Constructor for BlockableIncrementFunction
     *
     * @param value the value for this supplier to supply.
     * @param beginLatch if not null, this latch is counted down when the function begins.
     * @param continueLatch if not null, this latch is awaited before the function returns a value.
     */
    public BlockableSupplier(T value, CountDownLatch beginLatch, CountDownLatch continueLatch) {
        this.beginLatch = beginLatch;
        this.continueLatch = continueLatch;
        this.value = value;
    }

    @Override
    public T get() {
        executionThread = Thread.currentThread();
        System.out.println("BlockableSupplier > get: supplies the value " + value);
        if (beginLatch != null)
            beginLatch.countDown();
        try {
            if (continueLatch != null && !continueLatch.await(MPConcurrentCDITestServlet.TIMEOUT_MIN * 3, TimeUnit.MINUTES))
                throw new TimeoutException();

            System.out.println("BlockableSupplier < get: " + value);
            return value;
        } catch (InterruptedException | TimeoutException x) {
            System.out.println("BlockableSupplier < get: " + x);
            throw new CompletionException(x);
        } finally {
            executionThread = null;
        }
    }
}
