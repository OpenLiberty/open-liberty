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

import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a ZoneId with a thread.
 */
public class TimeZoneContextRestorer implements ThreadContextRestorer {
    private boolean restored;
    private final ZoneId zoneId;

    TimeZoneContextRestorer(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        if (zoneId == null)
            TimeZone.local.remove();
        else
            TimeZone.local.set(zoneId);
        restored = true;
    }

    @Override
    public String toString() {
        return "TimeZoneContextRestorer@" +
               Integer.toHexString(hashCode()) + ": " +
               (zoneId == null ? null : zoneId.getId());
    }
}
