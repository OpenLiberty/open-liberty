/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.models;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Recreate from io.openliberty.data.internal_fat
 */
@Entity
public class Product {
    public String description;

    public String name;

    @Id
    @GeneratedValue
    public UUID pk;

    public float price;

    @Version
    public long version;

    public static Product of(String description, String name, float price) {
        Product inst = new Product();
        inst.name = name;
        inst.description = description;
        inst.price = price;
        return inst;
    }
}
