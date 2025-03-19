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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Packages extends BasicRepository<Package, Integer> {

    @Query("SELECT COUNT(o) FROM Package o")
    long countAll();

    @Delete
    long deleteAll();

    Optional<Package> deleteByDescription(String description);

    Package[] deleteByDescriptionEndsWith(String ending, Sort<?>... sorts);

    List<Integer> deleteByDescriptionOrderByWidthAsc(String desc, Limit limit);

    Package[] deleteByDescriptionOrderByWidthDesc(String desc, Limit limit);

    void deleteByIdIn(Iterable<Integer> ids);

    @Query("DELETE FROM Package")
    int deleteEverything();

    Optional<Integer> delete1(Limit limit, Sort<Package> sort);

    int[] delete2(Limit limit, Sort<?>... sorts);

    LinkedList<?> delete2ByHeightLessThan(float maxHeight, Limit limit, Sort<?>... sorts);

    List<Package> deleteFirst2(); // 'first2' should be ignored and this should delete all entities

    Package deleteFirst5ByWidthLessThan(float maxWidth); // 'first5' should be ignored and the number of results should be limited by the condition

    @Delete
    Object[] destroy(Limit limit, Sort<Package> sort);

    List<Package> findByHeightBetween(float minHeight, float maxHeight);

    @OrderBy(value = "width", descending = true)
    @OrderBy(value = "height")
    @OrderBy(value = "id", descending = true)
    CursoredPage<Package> findByHeightGreaterThan(float minHeight, PageRequest pagination);

    CursoredPage<Package> findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(float minHeight, PageRequest pagination);

    CursoredPage<Package> findByHeightLessThanOrHeightGreaterThan(float minToExclude,
                                                                  float maxToExclude,
                                                                  Order<Package> order,
                                                                  PageRequest pagination);

    @OrderBy(value = "id")
    List<Integer> findIdByHeightRoundedDown(int height);

    @OrderBy(value = "id")
    List<Integer> findIdByLengthRoundedUp(int length);

    @OrderBy(value = "id")
    List<Integer> findIdByWidthRounded(int width);

    @Delete
    @OrderBy(value = "length", descending = true)
    List<Integer> removeIfDescriptionMatches(String description, Limit limit);

    @Delete
    Package take(@By("id") int packageNum);

    @Delete
    List<Package> take(@By("description") String desc);

    @Delete
    @OrderBy("width")
    List<Package> takeOrdered(String description);

    boolean updateByIdAddHeightMultiplyLengthDivideWidth(int id, float heightToAdd, float lengthMultiplier, float widthDivisor);

    void updateByIdDivideLengthDivideWidthDivideHeight(int id, float lengthDivisor, float widthDivisor, float heightDivisor);

    boolean updateByIdDivideWidthAddDescription(int id, int widthDivisor, String additionalDescription);

    long updateByLengthLessThanEqualAndHeightBetweenMultiplyLengthMultiplyWidthSetHeight(float maxLength, float minHeight, float maxHeight,
                                                                                         float lengthMultiplier, float widthMultiplier, float newHeight);

    @Query("SELECT p FROM Package p WHERE (p.length * p.width * p.height >= ?1 AND p.length * p.width * p.height <= ?2)")
    @OrderBy(value = "width", descending = true)
    @OrderBy(value = "length")
    @OrderBy(value = "id")
    CursoredPage<Package> whereVolumeWithin(float minVolume, float maxVolume, PageRequest pagination);
}