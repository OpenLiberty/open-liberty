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

import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a Locale with a thread.
 */
public class LocaleContextRestorer implements ThreadContextRestorer {
    private boolean restored;
    private final Locale locale;

    LocaleContextRestorer(Locale locale) {
        this.locale = locale;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        if (locale == null)
            LocaleContext.local.remove();
        else
            LocaleContext.local.set(locale);
        restored = true;
    }

    @Override
    public String toString() {
        return "LocaleContextRestorer@" +
               Integer.toHexString(hashCode()) + ": " +
               (locale == null ? null : locale.getDisplayName());
    }
}
