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

import java.time.LocalDateTime;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Entity for testing inheritance from an abstract class.
 * This entity is not referenced by life cycle methods of the
 * Advertisements repository (and only by @Find), even though
 * that repository is be used to persist, query, update, and
 * remove instances.
 */
@DiscriminatorValue("television")
@Entity
public class Commercial extends Advertisement {

    public String network;

    int numSeconds;

    public LocalDateTime showAt;

    public static Commercial create(int id,
                                    String sponsor,
                                    String network,
                                    int numSeconds,
                                    LocalDateTime showAt) {
        Commercial c = new Commercial();
        c.id = id;
        c.sponsor = sponsor;
        c.network = network;
        c.numSeconds = numSeconds;
        c.showAt = showAt;
        return c;
    }
}
