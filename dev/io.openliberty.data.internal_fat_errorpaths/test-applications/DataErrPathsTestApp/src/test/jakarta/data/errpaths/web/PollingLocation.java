/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.errpaths.web;

import java.time.LocalTime;

/**
 * A valid record entity.
 */
public record PollingLocation(
                long id,
                String address,
                LocalTime closesAt,
                LocalTime opensAt,
                int precinct,
                int ward) {

    public static PollingLocation of(long id,
                                     String address,
                                     int ward,
                                     int precinct,
                                     LocalTime opensAt,
                                     LocalTime closesAt) {
        return new PollingLocation(id, address, closesAt, opensAt, precinct, ward);
    }
}
