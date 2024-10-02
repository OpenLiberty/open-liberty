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
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a ZoneId with a thread.
 */
public class TimeZoneContextSnapshot implements ThreadContextSnapshot {
    private final int hashCode;
    private final ZoneId zoneId;

    TimeZoneContextSnapshot(ZoneId zoneId) {
        this.hashCode = zoneId == null ? 0 : zoneId.getId().hashCode();
        this.zoneId = zoneId;
    }

    @Override
    public ThreadContextRestorer begin() {
        ThreadContextRestorer restorer = //
                        new TimeZoneContextRestorer(TimeZone.local.get());
        TimeZone.local.set(zoneId);
        return restorer;
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "TimeZoneContextSnapshot@" +
               Integer.toHexString(hashCode()) + ": " +
               (zoneId == null ? null : zoneId.getId());
    }
}
