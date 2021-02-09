/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.numeration.internal;

import java.util.concurrent.RejectedExecutionException;

import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * This a fake thread context that we made up for testing purposes.
 */
public class NumerationContext implements ThreadContext {
    /**  */
    private static final long serialVersionUID = 4646520179763123585L;

    /**
     * Radix for the numeration system used by this thread.
     */
    int radix;

    /**
     * Indicates whether digits (above 9) are displayed as upper or lower case
     */
    boolean upperCase;

    /**
     * Construct a default context
     */
    public NumerationContext() {
        this.radix = 10;
        this.upperCase = false;
    }

    @Override
    public NumerationContext clone() {
        NumerationContext n = new NumerationContext();
        n.radix = this.radix;
        n.upperCase = this.upperCase;
        return n;
    }

    /**
     * <p>Establishes context on the current thread.
     * When this method is used, expect that context will later be removed and restored
     * to its previous state via operationStopping.
     *
     * <p>This method should fail if the context cannot be established on the thread.
     * In the event of failure, any partially applied context must be removed before this method returns.
     *
     * @throws RejectedExecutionException if context properties are invalid.
     */
    @Override
    public void taskStarting() throws RejectedExecutionException {

        if (radix < 2 || radix > Character.MAX_RADIX)
            throw new RejectedExecutionException("Unsupported radix: " + radix);

        NumerationServiceImpl.threadlocal.get().push(this.clone());
    }

    /**
     * <p>Restore the thread to its previous state from before the most recently applied context.
     */
    @Override
    public void taskStopping() {

        // Remove most recent, which restores the previous
        NumerationServiceImpl.threadlocal.get().pop();
    }

    @Override
    public String toString() {
        return new StringBuilder().append('(').append(radix).append(' ').append(upperCase).append(')').toString();
    }
}