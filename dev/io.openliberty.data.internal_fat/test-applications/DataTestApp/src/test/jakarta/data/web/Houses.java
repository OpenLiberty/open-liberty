/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

/**
 * Repository for operations on the unannotated House entity,
 * which has multiple levels of unannotated embeddables.
 */
@Repository
public interface Houses {
    long deleteAll();

    long deleteById(String parcel);

    long deleteByKitchenWidthGreaterThan(int widthAbove);

    boolean existsById(String parcel);

    List<House> findByGarageTypeOrderByGarageDoorWidthDesc(Garage.Type type);

    House findById(String parcel);

    @OrderBy("purchasePrice")
    int[] findGarageAreaByGarageNotNull();

    Optional<Object[]> findGarageDoorAndKitchenLengthAndKitchenWidthById(String parcel);

    @OrderBy("lotSize")
    Stream<Object[]> findKitchenLengthAndKitchenWidthAndGarageAreaAndAreaByAreaLessThan(int maxArea);

    @OrderBy("area")
    DoubleStream findPurchasePriceByLotSizeGreaterThan(float minLotSize);

    List<House> save(House... h);

    boolean updateByIdSetGarageAddAreaAddKitchenLengthSetNumBedrooms(String parcel, Garage updatedGarage, int addedArea, int addedKitchenLength, int newNumBedrooms);
}
