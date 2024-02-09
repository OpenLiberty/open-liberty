/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import jakarta.data.Streamable;
import jakarta.data.page.KeysetAwareSlice;
import jakarta.data.page.Pageable;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.comparison.GreaterThanEqual;
import io.openliberty.data.repository.comparison.LessThanEqual;
import io.openliberty.data.repository.comparison.StartsWith;
import io.openliberty.data.repository.function.AbsoluteValue;
import io.openliberty.data.repository.function.IgnoreCase;

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

    @Find
    @OrderBy(descending = true, ignoreCase = true, value = "name")
    Stream<Business> in(@By("location_address.city") String city,
                        @By("location.address_state") String state);

    @Find
    @OrderBy("name") // Business.name, not Business.Location.Address.Street.name
    @Select("name")
    List<String> onSouthSideOf(@By("locationAddressCity") String city,
                               @By("locationAddressState") String state,
                               @By("locationAddress.street_direction") @IgnoreCase @StartsWith String streetDirectionPrefix);

    // Save with a different entity type does not conflict with the primary entity type from BasicRepository
    @Save
    Streamable<Employee> save(Employee... e);

    @Update
    boolean update(Business b);

    @Query("UPDATE Business b SET b.location=?1, b.name=?2 WHERE b.id=?3")
    boolean updateWithJPQL(Location newLocation, String newName, long id);

    @Find
    List<Business> withLongitudeIgnoringSignWithin(@By("location.longitude") @AbsoluteValue @GreaterThanEqual float min,
                                                   @By("location.longitude") @AbsoluteValue @LessThanEqual float max);
}