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
package test.context.location;

import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a zip code with a thread.
 */
public class ZipCodeContextRestorer implements ThreadContextRestorer {
    private boolean restored;
    private final int zipCode;

    ZipCodeContextRestorer(int zipCode) {
        this.zipCode = zipCode;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        if (zipCode == 0)
            ZipCode.local.remove();
        else
            ZipCode.local.set(zipCode);
        restored = true;
    }

    @Override
    public String toString() {
        return "ZipCodeContextRestorer@" + Integer.toHexString(hashCode()) + ":" + zipCode;
    }
}
