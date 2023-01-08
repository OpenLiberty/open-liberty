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
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a zip code with a thread.
 */
public class ZipCodeContextSnapshot implements ThreadContextSnapshot {
    private final int hashCode;
    private final int zipCode;

    ZipCodeContextSnapshot(int zipCode) {
        this.hashCode = Integer.valueOf(Integer.toString(zipCode), 16);
        this.zipCode = zipCode;
    }

    @Override
    public ThreadContextRestorer begin() {
        ThreadContextRestorer restorer = new ZipCodeContextRestorer(ZipCode.local.get());
        ZipCode.local.set(zipCode);
        return restorer;
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "ZipCodeContextSnapshot@" + Integer.toHexString(hashCode());
    }
}
