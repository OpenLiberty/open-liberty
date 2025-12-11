/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
import jakarta.data.repository.Update;
import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;

/**
 * A repository for the Package entity.
 */
@ManagedExecutorDefinition(name = "java:comp/PackageRepositoryExecutor",
                           maxAsync = 1)
@Repository(dataStore = "java:module/env/data/DataStoreRef")
public interface Packages extends BasicRepository<Package, Integer> {

    @Update
    boolean adjust(Package p);

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

    @Query("SELECT id WHERE FLOOR(height) = :height")
    @OrderBy(ID)
    List<Integer> findIdByHeightRoundedDown(int height);

    @Query("SELECT id WHERE CEILING(length) = :length")
    @OrderBy(ID)
    List<Integer> findIdByLengthRoundedUp(int length);

    @Query("SELECT id WHERE ROUND(width, 0) = :width")
    @OrderBy(ID)
    List<Integer> findIdByWidthRounded(int width);

    @Query("""
                    UPDATE Package
                       SET height=height+?2,
                           length=length*?3,
                           width=width/?4
                     WHERE (id=?1)
                    """)
    boolean increaseHeightAndLengthReduceWidth(int id,
                                               float heightToAdd,
                                               float lengthMultiplier,
                                               float widthDivisor);

    @Query("""
                    UPDATE Package
                       SET length=length*?4,
                           width=width*?5,
                           height=?6
                     WHERE length<=?1 AND height BETWEEN ?2 AND ?3
                    """)
    long increaseLengthAndWidthAssignHeight(float maxLength,
                                            float minHeight,
                                            float maxHeight,
                                            float lengthMultiplier,
                                            float widthMultiplier,
                                            float newHeight);

    @Query("""
                    UPDATE Package o
                       SET o.length=o.length/?2,
                           o.width=o.width/?3,
                           o.height=o.height/?4
                     WHERE o.id=?1
                    """)
    void reduceDimensions(int id, float lengthDivisor, float widthDivisor, float heightDivisor);

    @Query("""
                    UPDATE Package
                       SET width=width/?2,
                           description=CONCAT(description,?3)
                     WHERE id=?1
                    """)
    boolean reduceWidthAppendDescription(int id, int widthDivisor, String additionalDescription);

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

    @Query("SELECT p FROM Package p WHERE (p.length * p.width * p.height >= ?1 AND p.length * p.width * p.height <= ?2)")
    @OrderBy(value = "width", descending = true)
    @OrderBy(value = "length")
    @OrderBy(value = "id")
    CursoredPage<Package> whereVolumeWithin(float minVolume, float maxVolume, PageRequest pagination);

    // The executor constrains concurrency to 1
    @Asynchronous(executor = "java:comp/PackageRepositoryExecutor")
    default CompletionStage<Boolean> widen(int id,
                                           float percentIncrease,
                                           float minAmount) {
        Optional<Package> found = findById(id);
        if (found.isPresent()) {
            Package p = found.get();

            float increase = p.width * percentIncrease / 100.0f;
            if (increase > minAmount)
                p.width += increase;
            else
                p.width += minAmount;

            return CompletableFuture.completedStage(adjust(p));
        } else {
            return CompletableFuture.completedStage(false);
        }
    }
}