/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

/**
 * Repository for operations on the unannotated House entity,
 * which has multiple levels of unannotated embeddables.
 */
@Repository(dataStore = "java:module/env/data/DataStoreRef")
public interface Houses {

    @Delete
    long deleteById(@By(ID) String parcel);

    long deleteByKitchenWidthGreaterThan(int widthAbove);

    @Delete
    int discardBasedOnGarage(Garage.Type garage_type, int garage_door_height);

    @Delete
    long dropAll();

    boolean existsByParcelId(String parcel);

    Stream<House> findByAreaGreaterThan(int minArea, Order<House> sorts);

    List<House> findByGarageTypeOrderByGarageDoorWidthDesc(Garage.Type type);

    List<House> findByGarage_door_heightOrderByGarage_door_heightDesc(int garageDoorHeight);

    List<House> findByArea(int area);

    @Find
    House findById(@By(ID) String parcel);

    @Query("SELECT garage.area WHERE garage IS NOT NULL")
    @OrderBy("purchasePrice")
    int[] findGarageAreas();

    @Query("SELECT garage.door, kitchen.length, kitchen.width WHERE parcelId = ?1")
    Optional<Object[]> findGarageDoorAndKitchenLengthAndKitchenWidthByParcelId(String parcel);

    @Query("SELECT kitchen.length, kitchen.width, garage.area, area WHERE area < :maxArea")
    @OrderBy("lotSize")
    Stream<Object[]> findKitchenLengthAndKitchenWidthAndGarageAreaAndAreaByAreaLessThan(int maxArea);

    @Query("SELECT purchasePrice WHERE lotSize > ?1")
    @OrderBy("area")
    DoubleStream findPurchasePriceByLotSizeGreaterThan(float minLotSize);

    @Find
    List<House> findWithGarageDoorDimensions(int garage_door_width, int garage_door_height);

    @Insert
    void insert(House h);

    @Delete
    Optional<House> remove(String parcelId);

    @Save
    List<House> save(House... h);

    @Query("""
                    UPDATE House
                       SET garage=?2,
                           area=area+?3,
                           kitchen.length=kitchen.length+?4,
                           numBedrooms=?5
                     WHERE parcelId=?1
                    """)
    boolean updateHomeInfo(String parcel,
                           Garage updatedGarage,
                           int addedArea,
                           int addedKitchenLength,
                           int newNumBedrooms);
}
