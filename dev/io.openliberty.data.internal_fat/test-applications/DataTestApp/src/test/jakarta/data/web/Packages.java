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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.Limit;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.PageableRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Sort;

import io.openliberty.data.repository.Compare;
import io.openliberty.data.repository.Delete;
import io.openliberty.data.repository.Filter;
import io.openliberty.data.repository.Function;
import io.openliberty.data.repository.Operation;
import io.openliberty.data.repository.Update;

/**
 *
 */
@Repository
public interface Packages extends PageableRepository<Package, Integer> {
    Object[] delete(Limit limit, Sort sort);

    Optional<Package> deleteByDescription(String description);

    Package[] deleteByDescriptionEndsWith(String ending, Sort... sorts);

    Optional<Integer> deleteFirstBy(Sort sort);

    int[] deleteFirst2By(Sort... sorts);

    LinkedList<?> deleteFirst2ByHeightLessThan(float maxHeight, Sort... sorts);

    long[] deleteFirst3(Sort sort); // invalid return type is not the entity or id

    List<String> deleteFirst4(Sort sort); // invalid return type is not the entity or id

    Collection<Number> deleteFirst5(Sort sort); // invalid return type is not the entity or id

    List<Package> findByHeightBetween(float minHeight, float maxHeight);

    @OrderBy(value = "width", descending = true)
    @OrderBy(value = "height")
    @OrderBy(value = "id", descending = true)
    KeysetAwareSlice<Package> findByHeightGreaterThan(float minHeight, Pageable pagination);

    KeysetAwareSlice<Package> findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(float minHeight, Pageable pagination);

    @OrderBy(value = "id")
    List<Integer> findIdByHeightRoundedDown(int height);

    @OrderBy(value = "id")
    List<Integer> findIdByLengthRoundedUp(int length);

    @OrderBy(value = "id")
    List<Integer> findIdByWidthRounded(int width);

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

    @Delete
    @Filter(by = "id")
    Package take(int id);

    @Delete
    @Filter(by = "length", op = Compare.Between)
    List<Package> takeWithin(float minLength, float maxLength);

    @Delete
    @Filter(by = "length", op = Compare.Between)
    @OrderBy("id")
    List<Package> takeWithinOrdered(float minLength, float maxLength);

    boolean updateByIdAddHeightMultiplyLengthDivideWidth(int id, float heightToAdd, float lengthMultiplier, float widthDivisor);

    void updateByIdDivideLengthDivideWidthDivideHeight(int id, float lengthDivisor, float widthDivisor, float heightDivisor);

    boolean updateByIdDivideWidthAddDescription(int id, int widthDivisor, String additionalDescription);

    long updateByLengthLessThanEqualAndHeightBetweenMultiplyLengthMultiplyWidthSetHeight(float maxLength, float minHeight, float maxHeight,
                                                                                         float lengthMultiplier, float widthMultiplier, float newHeight);

    @Filter(by = "height", op = Compare.LessThan, param = "min")
    @Filter(as = Filter.Type.Or, by = "height", op = Compare.GreaterThan, param = "max")
    KeysetAwarePage<Package> whereHeightNotWithin(@Param("min") float minToExclude,
                                                  @Param("max") float maxToExclude,
                                                  Pageable pagination);

    @Query("SELECT p FROM Package p WHERE (p.length * p.width * p.height >= ?1 AND p.length * p.width * p.height <= ?2)")
    @OrderBy(value = "width", descending = true)
    @OrderBy(value = "length")
    @OrderBy(value = "id")
    KeysetAwarePage<Package> whereVolumeWithin(float minVolume, float maxVolume, Pageable pagination);

    @Filter(by = "height", fn = Function.Rounded)
    @OrderBy(value = "id")
    List<Integer> withHeightAbout(float height);

    @Filter(by = "length", fn = Function.RoundedDown)
    @OrderBy(value = "id")
    List<Integer> withLengthFloored(float length);

    @Filter(by = "width", fn = Function.RoundedUp)
    @OrderBy(value = "id")
    List<Integer> withWidthCeiling(float width);
}