/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import io.openliberty.data.Data;
import io.openliberty.data.Param;
import io.openliberty.data.Query;
import io.openliberty.data.Repository;

/**
 * Experiments with auto-generated keys.
 */
@Data(Order.class)
public interface OrderRepo extends Repository<Order, Long> {

    @Query("UPDATE Order o SET o.total = o.total * :rate + :shipping WHERE o.id = :id")
    boolean addTaxAndShipping(@Param("id") long orderId,
                              @Param("rate") float taxRate,
                              @Param("shipping") float shippingCost);
}
