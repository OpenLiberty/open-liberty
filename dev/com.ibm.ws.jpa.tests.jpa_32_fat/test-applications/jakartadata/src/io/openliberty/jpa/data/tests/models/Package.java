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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Recreate from io.openliberty.data.internal_fat
 */
@Entity
public class Package {

    public String description;

    @Id
    public int id;

    public float height;

    public float length;

    public float width;

    public static Package of(int id, float length, float width, float height, String description) {
        Package inst = new Package();
        inst.id = id;
        inst.length = length;
        inst.width = width;
        inst.height = height;
        inst.description = description;
        return inst;
    }

    @Override
    public String toString() {
        return "Package id=" + id + "; L=" + length + "; W=" + width + "; H=" + height + " " + description;
    }
}