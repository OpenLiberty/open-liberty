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
import io.openliberty.data.Delete;
import io.openliberty.data.Query;
import io.openliberty.data.Select;

/**
 *
 */
@Data
public interface ShippingAddresses {
    @Select({ "houseNumber", "streetName" })
    StreetAddress[] findByHouseNumberBetweenOrderByStreetNameOrderByHouseNumber(int minHouseNumber, int maxHouseNumber);

    WorkAddress[] findByStreetNameAndFloorNumber(String streetName, int floorNumber);

    ShippingAddress[] findByStreetNameOrderByHouseNumber(String streetName);

    @Query("SELECT o FROM WorkAddress o WHERE o.office=?1")
    WorkAddress forOffice(String officeNum);

    @Delete
    long removeAll();

    void save(ShippingAddress entity);
}
