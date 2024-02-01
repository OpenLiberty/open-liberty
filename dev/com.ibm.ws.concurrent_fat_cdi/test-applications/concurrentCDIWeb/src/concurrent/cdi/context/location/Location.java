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
package concurrent.cdi.context.location;

/**
 * Example third-party thread context.
 */
public class Location {
    public static final String CONTEXT_NAME = "Location";

    static ThreadLocal<String> local = new ThreadLocal<>();

    // API methods:

    public static final void clear() {
        local.remove();
    }

    public static final String get() {
        return local.get();
    }

    public static final void set(String newLocation) {
        if (newLocation == null || newLocation.length() == 0)
            throw new IllegalArgumentException(newLocation);
        else
            local.set(newLocation);
    }
}
