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
package test.jakarta.data.jpa.web;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * An entity with an attribute that requires a Converter that is specified
 * on an attribute of the MappedSuperclass.
 */
@Entity
public class Purchase extends PurchaseInfo {

    @Id
    public short identifier;

    public String itemName;

    public float total;

    static Purchase of(short identifier,
                       String itemName,
                       PurchaseTime timeOfPurchase,
                       float total) {
        Purchase p = new Purchase();
        p.identifier = identifier;
        p.itemName = itemName;
        p.timeOfPurchase = timeOfPurchase;
        p.total = total;
        return p;
    }
}
