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

import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a location string with a thread.
 */
public class LocationContextRestorer implements ThreadContextRestorer {
    private boolean restored;
    private final String location;

    LocationContextRestorer(String location) {
        this.location = location;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        if (location == null)
            Location.local.remove();
        else
            Location.local.set(location);
        restored = true;
    }

    @Override
    public String toString() {
        return "LocationContextRestorer@" + Integer.toHexString(hashCode()) + ":" + location;
    }
}
