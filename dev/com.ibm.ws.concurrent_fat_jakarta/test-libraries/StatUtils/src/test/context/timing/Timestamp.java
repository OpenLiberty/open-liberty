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
package test.context.timing;

/**
 * Example third-party thread context.
 */
public class Timestamp {
    public static final String CONTEXT_NAME = "Timestamp";

    static ThreadLocal<Long> local = ThreadLocal.withInitial(() -> null);

    // API methods:

    public static void clear() {
        local.remove();
    }

    public static long elapsed() {
        Long timestamp = local.get();
        if (timestamp == null)
            throw new IllegalStateException();
        else
            return System.nanoTime() - timestamp;
    }

    public static Long get() {
        return local.get();
    }

    public static void set() {
        local.set(System.nanoTime());
    }
}
