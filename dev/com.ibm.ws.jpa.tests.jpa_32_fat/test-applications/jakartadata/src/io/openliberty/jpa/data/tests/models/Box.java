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

/**
 * Recreate from Jakarta Data TCK
 */
@jakarta.persistence.Entity
public class Box {
    @jakarta.persistence.Id
    public String boxIdentifier;

    public int length;

    public int width;

    public int height;

    public static Box of(String id, int length, int width, int height) {
        Box box = new Box();
        box.boxIdentifier = id;
        box.length = length;
        box.width = width;
        box.height = height;
        return box;
    }

    @Override
    public String toString() {
        return "Box@" + Integer.toHexString(hashCode()) + ":" + length + "x" + width + "x" + height + ":" + boxIdentifier;
    }
}
