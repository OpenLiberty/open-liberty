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
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Recreate from io.openliberty.data.internal_fat_jpa
 */
@Entity(name = "Orders") // overrides the default name PurchaseOrder
public class PurchaseOrder {

    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    public UUID id;

    public String purchasedBy;

    public float total;

    @Version
    public int versionNum;

    public static PurchaseOrder of(String purchasedBy, float total) {
        PurchaseOrder inst = new PurchaseOrder();
        inst.purchasedBy = purchasedBy;
        inst.total = total;
        return inst;
    }
}
