/*******************************************************************************
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
 *******************************************************************************/
package test.context.locale;

import java.util.Locale;

/**
 * Example third-party thread context.
 * Associates a Locale with a thread.
 */
public class LocaleContext {
    public static final String CONTEXT_NAME = "Locale";

    /**
     * The null value indicates uninitialized and signals the
     * ThreadContextRestorer to remove the ThreadLocal value
     * so that it does not remain on virtual threads.
     */
    static ThreadLocal<Locale> local = ThreadLocal //
                    .withInitial(() -> null);

    // API methods:

    public static Locale get() {
        return local.get();
    }

    public static void remove() {
        local.remove();
    }

    public static void set(Locale locale) {
        local.set(locale);
    }
}
