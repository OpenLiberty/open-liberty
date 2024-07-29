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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Recreate from io.openliberty.data.internal_fat_exp
 */
@Entity
public class Item {
    public String description;

    public String name;

    @Id
    @GeneratedValue
    public UUID pk; // Do not add Id to this name. It should be detectable based on type alone.

    public float price;

    public long version;

    public static Item of(String description, String name, float price) {
        Item inst = new Item();
        inst.description = description;
        inst.name = name;
        inst.price = price;
        return inst;
    }
}
