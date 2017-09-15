/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

/**
 * A generic recursion detection facility.
 * 
 * Each thread has its own counter.
 * 
 * @author dbourne
 * 
 * 
 */
public class RecursionCounter {

    private final ThreadLocal<Integer> stackDepth = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue()
        {
            return Integer.valueOf(0);
        }
    };

    /**
     * Increments the recursion count for this thread.
     * 
     * @return new counter
     */
    public int incrementCount() {
        // get the logging recursion stack depth for the current thread
        int depth = stackDepth.get();
        depth = depth + 1;
        stackDepth.set(depth);
        return depth;
    }

    /**
     * Decrements the recursion count for this thread.
     */
    public void decrementCount() {
        int depth = stackDepth.get();
        depth = depth - 1;
        stackDepth.set(depth);
    }

}
