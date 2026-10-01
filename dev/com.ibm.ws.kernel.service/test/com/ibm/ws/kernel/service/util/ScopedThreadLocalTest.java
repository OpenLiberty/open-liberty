/* *****************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * *****************************************************************************/
package com.ibm.ws.kernel.service.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class ScopedThreadLocalTest {

    // -----------------------------------------------------------------------
    // create() — null default
    // -----------------------------------------------------------------------

    @Test
    public void create_getReturnsNullBeforeAnyScope() {
        ScopedThreadLocal<String> context = ScopedThreadLocal.create();
        assertNull(context.get());
    }

    @Test
    public void create_withAndGetReturnsValue() {
        ScopedThreadLocal<String> context = ScopedThreadLocal.create();
        try (ScopedThreadLocal<String>.Scope s = context.with("a")) {
            assertEquals("a", context.get());
        }
    }

    @Test
    public void create_closeRestoresToNull() {
        ScopedThreadLocal<String> context = ScopedThreadLocal.create();
        try (ScopedThreadLocal<String>.Scope s = context.with("a")) {
            // inside
        }
        assertNull(context.get());
    }

    // -----------------------------------------------------------------------
    // withDefault() — non-null default
    // -----------------------------------------------------------------------

    @Test
    public void withDefault_getReturnsDefaultBeforeAnyScope() {
        ScopedThreadLocal<String> context = ScopedThreadLocal.withDefault("default");
        assertEquals("default", context.get());
    }

    @Test
    public void withDefault_closeRestoresToDefault() {
        ScopedThreadLocal<String> context = ScopedThreadLocal.withDefault("default");
        try (ScopedThreadLocal<String>.Scope s = context.with("pushed")) {
            assertEquals("pushed", context.get());
        }
        assertEquals("default", context.get());
    }

    // -----------------------------------------------------------------------
    // Nesting
    // -----------------------------------------------------------------------

    @Test
    public void nestedScopesRestoreInOrder() {
        ScopedThreadLocal<String> context = ScopedThreadLocal.create();
        try (ScopedThreadLocal<String>.Scope s1 = context.with("first")) {
            assertEquals("first", context.get());
            try (ScopedThreadLocal<String>.Scope s2 = context.with("second")) {
                assertEquals("second", context.get());
            }
            assertEquals("first", context.get());
        }
        assertNull(context.get());
    }

    // -----------------------------------------------------------------------
    // Out-of-order close
    // -----------------------------------------------------------------------

    @Test(expected = IllegalStateException.class)
    public void outOfOrderCloseThrowsIllegalStateException() {
        ScopedThreadLocal<String> context = ScopedThreadLocal.create();
        ScopedThreadLocal<String>.Scope s1 = context.with("first");
        ScopedThreadLocal<String>.Scope s2 = context.with("second");
        s1.close(); // wrong order — s2 is still active
    }

    // -----------------------------------------------------------------------
    // Per-thread isolation
    // -----------------------------------------------------------------------

    /**
     * While the main thread has "main" in scope, a second thread reading
     * the same ScopedThreadLocal instance must see null — it has never entered a scope.
     */
    @Test
    public void otherThreadSeesNullWhileMainThreadHasValueInScope() throws InterruptedException {
        ScopedThreadLocal<String> context = ScopedThreadLocal.create();
        AtomicReference<String> otherThreadValue = new AtomicReference<>();

        try (ScopedThreadLocal<String>.Scope s = context.with("main")) {
            assertEquals("main", context.get());

            Thread t = new Thread(() -> otherThreadValue.set(context.get()));
            t.start();
            t.join(); // wait for the other thread to read while "main" is still in scope
        }

        assertNull(otherThreadValue.get());
    }

    /**
     * Two threads enter scope with different values on the same ScopedThreadLocal instance concurrently.
     * Each thread must see only its own value, not the other thread's.
     */
    @Test
    public void twoThreadsSeeTheirOwnValuesIndependently() throws InterruptedException {
        ScopedThreadLocal<String> context = ScopedThreadLocal.create();
        AtomicReference<String> otherThreadValue = new AtomicReference<>();

        Thread t = new Thread(() -> {
            try (ScopedThreadLocal<String>.Scope s = context.with("other")) {
                otherThreadValue.set(context.get());
            }
        });

        try (ScopedThreadLocal<String>.Scope s = context.with("main")) {
            t.start();
            t.join(); // other thread reads its own value while main's scope is also live
            assertEquals("main", context.get());
        }

        assertEquals("other", otherThreadValue.get());
    }
}
