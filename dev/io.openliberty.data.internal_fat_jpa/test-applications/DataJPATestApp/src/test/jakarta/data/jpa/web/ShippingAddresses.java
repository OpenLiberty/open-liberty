/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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

import java.util.Set;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

/**
 * Repository for testing Inheritance and DiscriminatorColumn/Value.
 */
@Repository(dataStore = "java:app/env/data/DataStoreRef")
public interface ShippingAddresses {

    StreetAddress[] findByStreetAddress_houseNumberBetweenOrderByStreetAddress_streetNameAscStreetAddress_houseNumber(int minHouseNumber, int maxHouseNumber);

    WorkAddress[] findByStreetAddress_streetNameAndFloorNumber(String streetName, int floorNumber);

    ShippingAddress[] findByStreetAddress_streetNameOrderByStreetAddress_houseNumber(String streetName);

    @Query("SELECT o FROM WorkAddress o WHERE o.office=?1")
    WorkAddress forOffice(String officeNum);

    @Delete
    long removeAll();

    @Save
    void save(ShippingAddress entity);

    @Save
    Set<ShippingAddress> save(Set<ShippingAddress> addresses);
}
