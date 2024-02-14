/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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

import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

/**
 * Repository for operations on the unannotated House entity,
 * which has multiple levels of unannotated embeddables.
 */
@Repository
public interface Houses {

    long deleteById(String parcel);

    long deleteByKitchenWidthGreaterThan(int widthAbove);

    @Delete
    int discardBasedOnGarage(Garage.Type garage_type, int garage_door_height);

    @Delete
    long dropAll();

    boolean existsById(String parcel);

    Stream<House> findByAreaGreaterThan(int minArea, Order<House> sorts);

    List<House> findByGarageTypeOrderByGarageDoorWidthDesc(Garage.Type type);

    House findById(String parcel);

    @OrderBy("purchasePrice")
    int[] findGarageAreaByGarageNotNull();

    Optional<Object[]> findGarageDoorAndKitchenLengthAndKitchenWidthById(String parcel);

    @OrderBy("lotSize")
    Stream<Object[]> findKitchenLengthAndKitchenWidthAndGarageAreaAndAreaByAreaLessThan(int maxArea);

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

    boolean updateByIdSetGarageAddAreaAddKitchenLengthSetNumBedrooms(String parcel, Garage updatedGarage, int addedArea, int addedKitchenLength, int newNumBedrooms);
}
