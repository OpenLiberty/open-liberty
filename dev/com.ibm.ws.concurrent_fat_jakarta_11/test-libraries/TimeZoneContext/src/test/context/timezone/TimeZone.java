/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.context.timezone;

import java.time.ZoneId;

/**
 * Example third-party thread context.
 * Associates a ZoneId with a thread.
 */
public class TimeZone {
    public static final String CONTEXT_NAME = "TimeZone";

    /**
     * The null value indicates uninitialized and signals the
     * ThreadContextRestorer to remove the ThreadLocal value
     * so that it does not remain on virtual threads.
     */
    static ThreadLocal<ZoneId> local = ThreadLocal //
                    .withInitial(() -> null);

    // API methods:

    public static ZoneId get() {
        return local.get();
    }

    public static void remove() {
        local.remove();
    }

    public static void set(ZoneId zoneId) {
        local.set(zoneId);
    }
}
