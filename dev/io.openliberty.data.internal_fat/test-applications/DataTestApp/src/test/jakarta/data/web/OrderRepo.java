/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package test.jakarta.data.web;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 * Experiments with auto-generated keys.
 */
@Repository
public interface OrderRepo extends CrudRepository<Order, Long> {

    @Query("UPDATE Orders o SET o.total = o.total * :rate + :shipping WHERE o.id = :id")
    boolean addTaxAndShipping(@Param("id") long orderId,
                              @Param("rate") float taxRate,
                              @Param("shipping") float shippingCost);
}
