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

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * Repository for an entity with multiple levels of embeddables.
 */
@Repository
public interface Businesses extends BasicRepository<Business, Integer> {

    @Query("WHERE name >= :beginAtName AND name <= :endAtName")
    @OrderBy("name")
    @OrderBy(ID)
    CursoredPage<?> findAsCursoredPage(String beginAtName,
                                       String endAtName,
                                       PageRequest pageReq);

    @OrderBy("name")
    List<?> findAsListByNameBetween(String beginAtName, String endAtName);

    @OrderBy("location.address.houseNum")
    Object[] findAsObjectArrayByNameBetween(String beginAtName, String endAtName);

    @Find
    Optional<?> findAsOptional(String name);

    @OrderBy(value = "location.address.houseNum", descending = true)
    @OrderBy(value = ID)
    Page<Object> findAsPageByNameBetween(String beginAtName,
                                         String endAtName,
                                         PageRequest pageReq);

    @Find
    @OrderBy("name")
    @OrderBy(ID)
    CompletableFuture<Stream<?>> findAsStreamByCity(String locationAddressCity,
                                                    String locationAddressState);

    // embeddable 1 level deep
    List<Business> findByLocationLatitudeBetweenOrderByLocationLongitudeDesc(float min, float max);

    // embeddable 3 levels deep where @Column resolves name conflict
    Business[] findByLocation_Address_Street_NameIgnoreCaseEndsWithOrderByLocation_Address_Street_DirectionIgnoreCaseAscNameAsc(String streetName);

    List<Business> findByLocationLongitudeAbsoluteValueBetween(float min, float max);

    // embeddable as result type
    @OrderBy("location.address.street")
    @OrderBy("location.address.houseNum")
    Stream<Location> findByLocationAddressZip(int zipCode);

    // embeddable 2 levels deep
    @OrderBy(value = "location.address.city", descending = true)
    @OrderBy("location.address.zip")
    @OrderBy("location.address.houseNum")
    @OrderBy("id")
    CursoredPage<Business> findByLocationAddressZipIn(Iterable<Integer> zipCodes, PageRequest pagination);

    // embeddable 3 levels deep as result type
    @OrderBy("location.address.street")
    @OrderBy("location.address.houseNum")
    Stream<Street> findByLocationAddressZipNotAndLocationAddressCity(int excludeZipCode, String city);

    @OrderBy("id")
    Business findFirstByName(String name);

    @Find
    @OrderBy(descending = true, ignoreCase = true, value = "name")
    Stream<Business> in(@By("location_address.city") String city,
                        @By("location.address_state") String state);

    @Query("SELECT CASE WHEN COUNT(THIS) > 0 THEN TRUE ELSE FALSE END" +
           " WHERE location.address.houseNum = :houseNum" +
           "   AND location.address.street.name = :streetName" +
           "   AND location.address.street.direction = :streetDir" +
           "   AND name = :businessName")
    boolean isLocatedAt(int houseNum,
                        String streetName,
                        String streetDir,
                        String businessName);

    @Find
    @OrderBy("name") // Business.name, not Business.Location.Address.Street.name
    List<Business> onSouthSideOf(@By("locationAddressCity") String city,
                                 @By("locationAddressState") String state,
                                 @By("locationAddress.street_direction") String streetDirection);

    // Save with a different entity type does not conflict with the primary entity type from BasicRepository
    @Save
    Stream<Employee> save(Employee... e);

    @Query("FROM Business" +
           " ORDER BY location.address.state ASC," +
           "          location.address.zip ASC," +
           "          location.address.street.name || ' ' || " +
           "              location.address.street.direction ASC," +
           "          location.address.houseNum ASC," +
           "          name ASC, " +
           "          id ASC")
    Page<Business> sorted(PageRequest pageReq);

    @Update
    boolean update(Business b);

    @Query("UPDATE Business b SET b.location=?1, b.name=?2 WHERE b.id=?3")
    boolean updateWithJPQL(Location newLocation, String newName, long id);

    @Query("WHERE ABS(location.longitude) >= :min AND ABS(location.longitude) <= :max")
    List<Business> withLongitudeIgnoringSignWithin(float min, float max);

    @Query("FROM Business" +
           " WHERE location.address.street.direction = :dir" +
           " ORDER BY location.address.houseNum DESC, id ASC")
    Page<Business> withStreetDirection(String dir, PageRequest pageReq);
}