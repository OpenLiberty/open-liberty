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
 * Record -> Class
 */
@Entity
public class Reciept {

    @Id
    public long purchaseId;

    public String customer;

    public float total;

    public static Reciept of(long purchaseId, String customer, float total) {
        Reciept inst = new Reciept();
        inst.purchaseId = purchaseId;
        inst.customer = customer;
        inst.total = total;
        return inst;
    }
}
