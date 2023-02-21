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

import java.util.List;

import jakarta.data.repository.Compare;
import jakarta.data.repository.Filter;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.Operation;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.PageableRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;

/**
 *
 */
@Repository
public interface Packages extends PageableRepository<Package, Integer> {
    List<Package> findByHeightBetween(float minHeight, float maxHeight);

    @OrderBy(value = "width", descending = true)
    @OrderBy(value = "height")
    @OrderBy(value = "id", descending = true)
    KeysetAwareSlice<Package> findByHeightGreaterThan(float minHeight, Pageable pagination);

    KeysetAwareSlice<Package> findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(float minHeight, Pageable pagination);

    @Filter(by = "id")
    @Update(attr = "height", op = Operation.Divide)
    @Update(attr = "description", op = Operation.Add)
    int reduceBy(int id, float heightDivisor, String additionalDescription);

    @Filter(by = "id")
    @Update(attr = "height", op = Operation.Subtract, value = "1")
    @Update(attr = "description", op = Operation.Add, value = " and shortened 1 cm")
    boolean shorten(int id);

    @Filter(by = "id", param = "id")
    @Update(attr = "height", op = Operation.Subtract, param = "reduction")
    @Update(attr = "description", op = Operation.Add, param = "moreDesc")
    void shortenBy(@Param("reduction") int amount, @Param("moreDesc") String moreDescription, @Param("id") int id);

    boolean updateByIdAddHeightMultiplyLengthDivideWidth(int id, float heightToAdd, float lengthMultiplier, float widthDivisor);

    void updateByIdDivideLengthDivideWidthDivideHeight(int id, float lengthDivisor, float widthDivisor, float heightDivisor);

    boolean updateByIdDivideWidthAddDescription(int id, int widthDivisor, String additionalDescription);

    long updateByLengthLessThanEqualAndHeightBetweenMultiplyLengthMultiplyWidthSetHeight(float maxLength, float minHeight, float maxHeight,
                                                                                         float lengthMultiplier, float widthMultiplier, float newHeight);

    @Filter(by = "height", op = Compare.LessThan, param = "min")
    @Filter(as = Filter.Type.OR, by = "height", op = Compare.GreaterThan, param = "max")
    KeysetAwarePage<Package> whereHeightNotWithin(@Param("min") float minToExclude,
                                                  @Param("max") float maxToExclude,
                                                  Pageable pagination);

    @Query("SELECT p FROM Package p WHERE (p.length * p.width * p.height >= ?1 AND p.length * p.width * p.height <= ?2)")
    @OrderBy(value = "width", descending = true)
    @OrderBy(value = "length")
    @OrderBy(value = "id")
    KeysetAwarePage<Package> whereVolumeWithin(float minVolume, float maxVolume, Pageable pagination);
}