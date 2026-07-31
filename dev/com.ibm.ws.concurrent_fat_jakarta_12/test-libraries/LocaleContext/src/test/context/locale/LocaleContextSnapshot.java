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
import java.util.Objects;

import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a Locale with a thread.
 */
public class LocaleContextSnapshot implements ThreadContextSnapshot {
    private final int hashCode;
    private final Locale locale;

    LocaleContextSnapshot(Locale locale) {
        this.hashCode = Objects.hashCode(locale);
        this.locale = locale;
    }

    @Override
    public ThreadContextRestorer begin() {
        ThreadContextRestorer restorer = //
                        new LocaleContextRestorer(LocaleContext.local.get());
        LocaleContext.local.set(locale);
        return restorer;
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "LocaleContextSnapshot@" +
               Integer.toHexString(hashCode()) + ": " +
               (locale == null ? null : locale.getDisplayName());
    }
}
