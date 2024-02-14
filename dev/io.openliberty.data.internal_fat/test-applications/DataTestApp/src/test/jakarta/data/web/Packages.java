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
package test.jakarta.data.web;

import static io.openliberty.data.repository.function.Rounded.Direction.DOWN;
import static io.openliberty.data.repository.function.Rounded.Direction.UP;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import jakarta.data.Limit;
import jakarta.data.Sort;
import jakarta.data.page.KeysetAwarePage;
import jakarta.data.page.KeysetAwareSlice;
import jakarta.data.page.Pageable;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.PageableRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;

import io.openliberty.data.repository.Or;
import io.openliberty.data.repository.comparison.GreaterThan;
import io.openliberty.data.repository.comparison.GreaterThanEqual;
import io.openliberty.data.repository.comparison.LessThan;
import io.openliberty.data.repository.comparison.LessThanEqual;
import io.openliberty.data.repository.function.Rounded;
import io.openliberty.data.repository.update.Add;
import io.openliberty.data.repository.update.Divide;
import io.openliberty.data.repository.update.SubtractFrom;

/**
 *
 */
@Repository
public interface Packages extends PageableRepository<Package, Integer> {

    Optional<Package> deleteByDescription(String description);

    Package[] deleteByDescriptionEndsWith(String ending, Sort<?>... sorts);

    Optional<Integer> deleteFirstBy(Sort<Package> sort);

    int[] deleteFirst2By(Sort<?>... sorts);

    LinkedList<?> deleteFirst2ByHeightLessThan(float maxHeight, Sort<?>... sorts);

    long[] deleteFirst3By(Sort<Package> sort); // invalid return type is not the entity or id

    List<String> deleteFirst4By(Sort<Package> sort); // invalid return type is not the entity or id

    Collection<Number> deleteFirst5By(Sort<Package> sort); // invalid return type is not the entity or id

    @Delete
    Object[] destroy(Limit limit, Sort<Package> sort);

    List<Package> findByHeightBetween(float minHeight, float maxHeight);

    @OrderBy(value = "width", descending = true)
    @OrderBy(value = "height")
    @OrderBy(value = "id", descending = true)
    KeysetAwareSlice<Package> findByHeightGreaterThan(float minHeight, Pageable<?> pagination);

    KeysetAwareSlice<Package> findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(float minHeight, Pageable<?> pagination);

    @OrderBy(value = "id")
    List<Integer> findIdByHeightRoundedDown(int height);

    @OrderBy(value = "id")
    List<Integer> findIdByLengthRoundedUp(int length);

    @OrderBy(value = "id")
    List<Integer> findIdByWidthRounded(int width);

    @Update
    int reduceBy(int id,
                 @Divide("height") float heightDivisor,
                 @Add("description") String additionalDescription);

    @Update
    boolean shorten(int id,
                    @SubtractFrom float height,
                    @Add String description);

    @Update
    void shortenBy(@SubtractFrom("height") int reduction,
                   @Add("description") String moreDescription,
                   int id);

    @Delete
    Package take(@By("id") int packageNum);

    @Delete
    List<Package> takeWithin(@By("length") @GreaterThanEqual float minLength,
                             @By("length") @LessThanEqual float maxLength);

    @Delete
    @OrderBy("id")
    List<Package> takeWithinOrdered(@By("length") @GreaterThanEqual float minLength,
                                    @By("length") @LessThanEqual float maxLength);

    boolean updateByIdAddHeightMultiplyLengthDivideWidth(int id, float heightToAdd, float lengthMultiplier, float widthDivisor);

    void updateByIdDivideLengthDivideWidthDivideHeight(int id, float lengthDivisor, float widthDivisor, float heightDivisor);

    boolean updateByIdDivideWidthAddDescription(int id, int widthDivisor, String additionalDescription);

    long updateByLengthLessThanEqualAndHeightBetweenMultiplyLengthMultiplyWidthSetHeight(float maxLength, float minHeight, float maxHeight,
                                                                                         float lengthMultiplier, float widthMultiplier, float newHeight);

    @Find
    KeysetAwarePage<Package> whereHeightNotWithin(@By("height") @LessThan float minToExclude,
                                                  @Or @By("height") @GreaterThan float maxToExclude,
                                                  Pageable<?> pagination);

    @Query("SELECT p FROM Package p WHERE (p.length * p.width * p.height >= ?1 AND p.length * p.width * p.height <= ?2)")
    @OrderBy(value = "width", descending = true)
    @OrderBy(value = "length")
    @OrderBy(value = "id")
    KeysetAwarePage<Package> whereVolumeWithin(float minVolume, float maxVolume, Pageable<?> pagination);

    @Find
    @OrderBy(value = "id")
    List<Integer> withHeightAbout(@Rounded float height);

    @Find
    @OrderBy(value = "id")
    List<Integer> withLengthFloored(@Rounded(DOWN) float length);

    @Find
    @OrderBy(value = "id")
    List<Integer> withWidthCeiling(@Rounded(UP) float width);
}