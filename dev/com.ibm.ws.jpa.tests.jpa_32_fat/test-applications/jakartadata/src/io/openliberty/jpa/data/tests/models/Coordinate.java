/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import java.util.UUID;

/**
 * Recreate from Jakarta Data TCK
 */
@jakarta.persistence.Entity
public class Coordinate {
    @jakarta.persistence.Id
    public UUID id;

    public double x;

    public float y;

    public static Coordinate of(String id, double x, float y) {
        Coordinate c = new Coordinate();
        c.id = UUID.nameUUIDFromBytes(id.getBytes());
        c.x = x;
        c.y = y;
        return c;
    }

    @Override
    public String toString() {
        return "Coordinate@" + Integer.toHexString(hashCode()) + "(" + x + "," + y + ")" + ":" + id;
    }
}