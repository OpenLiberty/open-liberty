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
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a location string with a thread.
 */
public class LocationContextSnapshot implements ThreadContextSnapshot {
    private final String location;

    LocationContextSnapshot(String location) {
        this.location = location;
    }

    @Override
    public ThreadContextRestorer begin() {
        ThreadContextRestorer restorer = new LocationContextRestorer(Location.local.get());
        Location.local.set(location);
        return restorer;
    }

    @Override
    public final int hashCode() {
        return location == null ? 0 : location.hashCode();
    }

    @Override
    public String toString() {
        return "LocationContextSnapshot@" + Integer.toHexString(hashCode()) + ":" + location;
    }
}
