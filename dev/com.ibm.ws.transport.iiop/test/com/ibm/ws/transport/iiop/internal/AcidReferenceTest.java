/*
 * =============================================================================
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
 * =============================================================================
 */
package com.ibm.ws.transport.iiop.internal;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AcidReferenceTest {
    private static ForkJoinPool pool;

    @BeforeClass
    public static void createThreadPool() {
        pool = new ForkJoinPool(8);
    }

    @AfterClass
    public static void destroyThreadPool() {
        pool.shutdownNow();
        pool = null;
    }

    @Test
    public void testNoArgsConstructor() {
        AcidReference<String> ref = new AcidReference<>();
        assertNull(ref.get());
        ref.update(s -> () -> "Hello");
        assertEquals("Hello", ref.get());
    }

    @Test
    public void testConstructWithValue() {
        AcidReference<String> ref = new AcidReference<>("Abair");
        assertEquals("Abair", ref.get());
        ref.update(s -> () -> "Bore da");
        assertEquals("Bore da", ref.get());
    }

	@Test
	public void testReentrantCallFails() {
		// If the update funcion passed in tries to retrieve the result of the update during processing, this is an unrecoverable error.
		// We should handle this by detecting and reporting the error as early as possible.
		try {
            AcidReference<String> ref = new AcidReference<>("Hej");
            ref.update(s -> () -> ref.get() + ", allesammen!"); // this should fail because ref.get() is called during the transition
            fail("Should never reach here");
        } catch (IllegalStateException expected) {
		}
	}

	@Test
    public void testUpdateThatThrows() {
        AcidReference<String> ref = new AcidReference<>("Salve");
        try {
            ref.update(s -> () -> Optional.<String>empty().get());
            fail("Should propagate the thrown exception");
        } catch (NoSuchElementException e) {
        }
        assertEquals("Salve", ref.get());
    }

	@Test
    public void testConcurrentNullUpdates() {
        AcidReference<Integer> ref = new AcidReference<>(0);
        int sum = runOneToAHundred(n -> ref.update(i -> null));
        assertEquals((Integer) 0, ref.get());
        assertEquals(0, sum); // all the updates should have "failed"
    }

    @Test
    public void testConcurrentAdditiveUpdates() {
        AcidReference<Integer> ref = new AcidReference<>(0);
        int sum = runOneToAHundred(n -> ref.update(i -> () -> i + n));
        assertEquals((Integer) 5050, ref.get());
        assertEquals(5050, sum);
    }

    @Test
    public void testExactlyOneUpdateHappens() {
        AcidReference<Integer> ref = new AcidReference<>(0);
        Integer winner = runOneToAHundred(n -> ref.update(i -> i == 0 ? n::intValue : null));
        assertEquals(winner, ref.get());
        assertTrue(winner <= 100);
    }

    @Test
    public void testOnlySomeUpdatesHappen() {
        AcidReference<Integer> ref = new AcidReference<>(0);
        int sum = runOneToAHundred(n -> ref.update(i -> n % 10 == 7 ? () -> i + n : null));
        assertEquals((Integer) 520, ref.get());
        assertEquals(520, sum);
    }

    private int runOneToAHundred(Function<Integer, Boolean> action) throws Error {
        IntStream aHundredTimes = IntStream.range(1, 101).parallel();
        try {
            return pool.submit(() -> aHundredTimes.filter(action::apply).sum()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new Error(e);
        }
    }
}
