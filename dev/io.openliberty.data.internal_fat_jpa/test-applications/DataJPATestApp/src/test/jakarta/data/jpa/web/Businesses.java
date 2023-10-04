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
package test.jakarta.data.jpa.web;

import java.util.List;
import java.util.stream.Stream;

import jakarta.data.page.KeysetAwareSlice;
import jakarta.data.page.Pageable;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import io.openliberty.data.repository.Compare;
import io.openliberty.data.repository.Filter;
import io.openliberty.data.repository.Function;
import io.openliberty.data.repository.Select;

/**
 *
 */
@Repository
public interface Businesses extends BasicRepository<Business, Integer> {

    // embeddable 1 level deep
    List<Business> findByLatitudeBetweenOrderByLongitudeDesc(float min, float max);

    // embeddable 3 levels deep where @Column resolves name conflict
    Business[] findByLocation_Address_Street_NameIgnoreCaseEndsWithOrderByLocation_Address_Street_DirectionIgnoreCaseAscNameAsc(String streetName);

    List<Business> findByLocationLongitudeAbsoluteValueBetween(float min, float max);

    // embeddable as result type
    @OrderBy("street")
    @OrderBy("houseNum")
    Stream<Location> findByZip(int zipCode);

    // embeddable 2 levels deep
    @OrderBy(value = "city", descending = true)
    @OrderBy("location.address.zip")
    @OrderBy("houseNum")
    @OrderBy("id")
    KeysetAwareSlice<Business> findByZipIn(Iterable<Integer> zipCodes, Pageable pagination);

    // embeddable 3 levels deep as result type
    @OrderBy("street")
    @OrderBy("houseNum")
    Stream<Street> findByZipNotAndCity(int excludeZipCode, String city);

    @OrderBy("id")
    Business findFirstByName(String name);

    @Filter(by = "location_address.city")
    @Filter(by = "location.address_state")
    @OrderBy(descending = true, ignoreCase = true, value = "name")
    Stream<Business> in(String city, String state);

    @Filter(by = "locationAddressCity", value = "Rochester")
    @Filter(by = "locationAddressState", value = "MN")
    @Filter(by = "locationAddress.street_direction", fn = Function.IgnoreCase, op = Compare.StartsWith, value = "s")
    @OrderBy("name") // Business.name, not Business.Location.Address.Street.name
    @Select("name")
    List<String> onSouthSide();

    boolean update(Business b);

    @Query("UPDATE Business b SET b.location=?1, b.name=?2 WHERE b.id=?3")
    boolean updateWithJPQL(Location newLocation, String newName, long id);

    @Filter(by = "location.longitude", fn = Function.AbsoluteValue, op = Compare.Between)
    List<Business> withLongitudeIgnoringSignWithin(float min, float max);
}