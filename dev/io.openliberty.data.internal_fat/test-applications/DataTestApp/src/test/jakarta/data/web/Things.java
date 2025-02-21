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

import java.util.stream.Stream;

import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 * For testing entity attribute names with reserved keywords in them.
 */
@Repository
public interface Things {

    Stream<Thing> findByALike(String aPattern);

    Stream<Thing> findByAlike(boolean alikeValue);

    /**
     * The "And" in "Android" should not be treated as a keyword because it immediately follows "findBy".
     */
    Stream<Thing> findByAndroid(boolean isAndroid);

    /**
     * The "and" in "Brand" should not be treated as a keyword due to case difference.
     * The "And" in "Android" should not be treated as a keyword because it immediately follows "Or".
     */
    Stream<Thing> findByBrandOrNotesContainsOrAndroid(String brand, String searchTerm, boolean isAndroid);

    /**
     * The "or" in "Floor" should not be treated as a keyword due to case difference.
     * The "Or" in "OrderNumber" should not be treated as a keyword because it immediately follows "And".
     */
    Stream<Thing> findByFloorNotAndInfoLikeAndOrderNumberLessThan(Integer floor, String infoPattern, long orderNumBelow);

    @Find
    Thing findById(@By(ID) long id);

    /**
     * The "Or" in "OrderNumber" should not be treated as a keyword because it immediately follows "findBy".
     */
    Stream<Thing> findByOrderNumber(long orderNum);

    /**
     * "Desc" within OrderByDescription would be treated as a keyword,
     * but @OrderBy can be used instead.
     */
    @OrderBy("Description")
    Stream<Thing> findByThingIdGreaterThan(long exclusiveMin);

    /**
     * "Or" within findBy...PurchaseOrder... would be treated as a keyword,
     * but @Query with JPQL can be used instead.
     */
    @Query("SELECT o FROM Thing o WHERE o.purchaseOrder=?1")
    Stream<Thing> forPurchaseOrder(int num);

    // Matches signature from CrudRepository
    @Delete
    void deleteAll();

    @Insert // intentionally does not match the method name because the spec permits this, although hopefully no one will write code this way
    void save(Thing thing);
}
