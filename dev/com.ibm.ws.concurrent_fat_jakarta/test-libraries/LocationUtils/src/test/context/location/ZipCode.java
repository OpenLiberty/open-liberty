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

/**
 * Example third-party thread context.
 */
public class ZipCode {
    public static final String CONTEXT_NAME = "ZipCode";

    static ThreadLocal<Integer> local = ThreadLocal.withInitial(() -> 0);

    // API methods:

    public static void clear() {
        local.remove();
    }

    public static int get() {
        return local.get();
    }

    public static void set(int zipCode) {
        if (zipCode > 9999 && zipCode < 100000)
            local.set(zipCode);
        else
            throw new IllegalArgumentException(Integer.toString(zipCode));
    }
}
