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

import java.util.List;

import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

/**
 * Repository for the Purchase entity that has a Converter that is specified
 * on an attribute of the MappedSuperclass.
 */
@Repository
public interface Purchases {

    @OrderBy("timeOfPurchase")
    List<Purchase> findByTimeOfPurchaseBetween(PurchaseTime min,
                                               PurchaseTime max);

    @Insert
    void make(Purchase purchase);

    int removeByTimeOfPurchaseBetween(PurchaseTime min,
                                      PurchaseTime max);
}
