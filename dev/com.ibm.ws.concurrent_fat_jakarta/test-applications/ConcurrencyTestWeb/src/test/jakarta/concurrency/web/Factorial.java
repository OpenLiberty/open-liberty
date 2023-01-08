/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.concurrency.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.RecursiveTask;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A ForkJoinTask that computes a factorial and
 * asserts the availability of a JNDI name and
 * asserts the priority of the executing thread.
 * This is used to prove that ManagedThreadFactory
 * configuration is honored.
 */
public class Factorial extends RecursiveTask<Long> {
    private static final long serialVersionUID = 1L;

    private String expectedLookup;
    private int expectedPriority;
    private final long num;

    Factorial(long n) {
        num = n;
    }

    Factorial assertAvailable(String jndiName) {
        expectedLookup = jndiName;
        return this;
    }

    Factorial assertPriority(int priority) {
        expectedPriority = priority;
        return this;
    }

    @Override
    public Long compute() {
        if (num < 2)
            return 1L;
        Factorial f = new Factorial(num - 1).assertAvailable(expectedLookup).assertPriority(expectedPriority);
        f.fork();

        // Prove that the fork join thread has access to the application component's namespace
        if (expectedLookup != null)
            try {
                assertNotNull(InitialContext.doLookup(expectedLookup));
            } catch (NamingException x) {
                throw new AssertionError(x);
            }
        // and runs with the expected priority
        if (expectedPriority > 0)
            assertEquals(expectedPriority, Thread.currentThread().getPriority());

        return num * f.join();
    }
}
