/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.data.web;

import java.time.OffsetDateTime;

/**
 * Simulates a Java record, but usable on Java 11.
 */
public class ReservedTimeSlot {
    public ReservedTimeSlot(OffsetDateTime start,
                            OffsetDateTime stop) {

// TODO The remainder of the class would be unnecessary if it were actually a record
        this.start = start;
        this.stop = stop;
    }

    private final OffsetDateTime start;

    private final OffsetDateTime stop;

    public OffsetDateTime start() {
        return start;
    }

    public OffsetDateTime stop() {
        return stop;
    }

    @Override
    public String toString() {
        return "ReservedTimeSlot[start=" + start + ", stop=" + stop + "]";
    }
}
