/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.service.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class ThreadLocalStackTest {

    // -----------------------------------------------------------------------
    // newStack() — null default
    // -----------------------------------------------------------------------

    @Test
    public void newStack_getReturnsNullBeforeAnyPush() {
        ThreadLocalStack<String> stack = ThreadLocalStack.newStack();
        assertNull(stack.get());
    }

    @Test
    public void newStack_pushAndGetReturnsValue() {
        ThreadLocalStack<String> stack = ThreadLocalStack.newStack();
        try (ThreadLocalStack<String>.Popper p = stack.push("a")) {
            assertEquals("a", stack.get());
        }
    }

    @Test
    public void newStack_closeRestoresToNull() {
        ThreadLocalStack<String> stack = ThreadLocalStack.newStack();
        try (ThreadLocalStack<String>.Popper p = stack.push("a")) {
            // inside
        }
        assertNull(stack.get());
    }

    // -----------------------------------------------------------------------
    // withDefault() — non-null default
    // -----------------------------------------------------------------------

    @Test
    public void withDefault_getReturnsDefaultBeforeAnyPush() {
        ThreadLocalStack<String> stack = ThreadLocalStack.withDefault("default");
        assertEquals("default", stack.get());
    }

    @Test
    public void withDefault_closeRestoresToDefault() {
        ThreadLocalStack<String> stack = ThreadLocalStack.withDefault("default");
        try (ThreadLocalStack<String>.Popper p = stack.push("pushed")) {
            assertEquals("pushed", stack.get());
        }
        assertEquals("default", stack.get());
    }

    // -----------------------------------------------------------------------
    // Nesting
    // -----------------------------------------------------------------------

    @Test
    public void nestedPushesAndPopsRestoreInOrder() {
        ThreadLocalStack<String> stack = ThreadLocalStack.newStack();
        try (ThreadLocalStack<String>.Popper p1 = stack.push("first")) {
            assertEquals("first", stack.get());
            try (ThreadLocalStack<String>.Popper p2 = stack.push("second")) {
                assertEquals("second", stack.get());
            }
            assertEquals("first", stack.get());
        }
        assertNull(stack.get());
    }

    // -----------------------------------------------------------------------
    // Out-of-order close
    // -----------------------------------------------------------------------

    @Test(expected = IllegalStateException.class)
    public void outOfOrderCloseThrowsIllegalStateException() {
        ThreadLocalStack<String> stack = ThreadLocalStack.newStack();
        ThreadLocalStack<String>.Popper p1 = stack.push("first");
        ThreadLocalStack<String>.Popper p2 = stack.push("second");
        p1.close(); // wrong order — p2 is still on top
    }

    // -----------------------------------------------------------------------
    // Per-thread isolation
    // -----------------------------------------------------------------------

    /**
     * While the main thread has "main" on the stack, a second thread reading
     * the same stack instance must see null — it has never pushed anything.
     */
    @Test
    public void otherThreadSeesNullWhileMainThreadHasValuePushed() throws InterruptedException {
        ThreadLocalStack<String> stack = ThreadLocalStack.newStack();
        AtomicReference<String> otherThreadValue = new AtomicReference<>();

        try (ThreadLocalStack<String>.Popper p = stack.push("main")) {
            assertEquals("main", stack.get());

            Thread t = new Thread(() -> otherThreadValue.set(stack.get()));
            t.start();
            t.join(); // wait for the other thread to read while "main" is still pushed
        }

        assertNull(otherThreadValue.get());
    }

    /**
     * Two threads push different values onto the same stack instance concurrently.
     * Each thread must see only its own value, not the other thread's.
     */
    @Test
    public void twoThreadsSeeTheirOwnValuesIndependently() throws InterruptedException {
        ThreadLocalStack<String> stack = ThreadLocalStack.newStack();
        AtomicReference<String> otherThreadValue = new AtomicReference<>();

        Thread t = new Thread(() -> {
            try (ThreadLocalStack<String>.Popper p = stack.push("other")) {
                otherThreadValue.set(stack.get());
            }
        });

        try (ThreadLocalStack<String>.Popper p = stack.push("main")) {
            t.start();
            t.join(); // other thread reads its own value while main's push is also live
            assertEquals("main", stack.get());
        }

        assertEquals("other", otherThreadValue.get());
    }
}
