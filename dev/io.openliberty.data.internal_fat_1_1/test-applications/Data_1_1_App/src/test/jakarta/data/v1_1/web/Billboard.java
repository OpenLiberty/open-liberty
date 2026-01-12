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
package test.jakarta.data.v1_1.web;

import java.time.LocalDate;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Entity for testing inheritance from an abstract class.
 * This entity is directly referenced by life cycle methods of the
 * Advertisements repository.
 */
@DiscriminatorValue("billboard")
@Entity
public class Billboard extends Advertisement {

    public LocalDate firstDay;

    public LocalDate lastDay;

    public String location;

    public static Billboard create(int id,
                                   String sponsor,
                                   LocalDate firstDay,
                                   LocalDate lastDay,
                                   String location) {
        Billboard b = new Billboard();
        b.id = id;
        b.sponsor = sponsor;
        b.firstDay = firstDay;
        b.lastDay = lastDay;
        b.location = location;
        return b;
    }
}
