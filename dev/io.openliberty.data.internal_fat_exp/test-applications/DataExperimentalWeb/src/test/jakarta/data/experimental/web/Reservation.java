/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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
package test.jakarta.data.experimental.web;

import java.util.Set;

/**
 * An unannotated entity that relies on an unannotated superclass.
 * This entity has fields of type OffsetDateTime, which is not one of the
 * temporal types that is currently supported by Jakarta Data.
 */
public class Reservation extends Timeslot {
    public String host;

    public Set<String> invitees;

    public String location;

    public long meetingID;

    // OffsetDateTime start is inherited from Timeslot

    // OffsetDateTime stop is inherited from Timeslot

    @Override
    public String toString() {
        return "Reservation[" + meetingID + "]@" + Integer.toHexString(hashCode());
    }
}
