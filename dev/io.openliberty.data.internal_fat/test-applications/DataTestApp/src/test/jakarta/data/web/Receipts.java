/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.enterprise.concurrent.Asynchronous;

/**
 * Repository interface for the Receipt entity which is a record
 */
@Repository
public interface Receipts extends CrudRepository<Receipt, Long> {
    @Query("UPDATE Receipt SET total = total * (1.0 + :taxRate) WHERE purchaseId = :id")
    boolean addTax(long id, float taxRate);

    @Query(" ")
    Page<Receipt> all(PageRequest req, Order<Receipt> sorts);

    @Query("FROM Receipt")
    Page<Receipt> all(PageRequest req, Sort<?>... sorts);

    @Query("SELECT COUNT(this)")
    long count();

    boolean deleteByTotalLessThan(float max);

    Optional<Receipt> deleteByPurchaseId(long purchaseId);

    int deleteByPurchaseIdIn(Iterable<Long> ids);

    @Delete
    Collection<Receipt> discardFor(String customer);

    boolean existsByPurchaseId(long id);

    @Asynchronous
    CompletableFuture<Receipt> findByPurchaseId(long purchaseId);

    Stream<Receipt> findByPurchaseIdIn(Iterable<Long> ids);

    @Asynchronous
    CompletionStage<Optional<Receipt>> findIfPresentByPurchaseId(long purchaseId);

    @Find
    @OrderBy("purchaseId")
    Receipt[] forCustomer(String customer);

    @Asynchronous
    @Find
    CompletableFuture<List<Receipt>> forCustomer(String customer, Order<Receipt> sorts);

    @Find
    Page<Receipt> forCustomer(String customer, PageRequest req, Order<Receipt> sorts);

    @Find
    CursoredPage<Receipt> forCustomer(String customer, PageRequest req, Sort<?>... sorts);

    @Query("""
                    UPDATE Receipt rct
                       SET rct.total = rct.total * :ReceiptTotalRateOfIncrease
                     WHERE rct.total > (SELECT AVG(rt.total) FROM Receipt rt)""")
    long increaseIfAboveAverageTotal(@Param("ReceiptTotalRateOfIncrease") float rate);

    @Query("""
                    DELETE FROM Receipt r
                     WHERE r.total < (SELECT AVG(t.total) FROM Receipt t)""")
    long removeByBelowAverageTotal();

    long removeByPurchaseId(long purchaseId);

    Set<Long> removeByTotalBetween(float min, float max);

    @Query("DELETE FROM Receipt WHERE total < :max")
    int removeIfTotalUnder(float max);

    @Query("ORDER BY total ASC")
    Page<Receipt> sortedByTotalIncreasing(PageRequest req);

    @Query("SELECT total FROM Receipt WHERE purchaseId=:id")
    float totalOf(long id);

    @Query("SELECT total")
    Page<Float> totals(PageRequest req, Sort<?>... sorts);

    @Query("SELECT total ORDER BY total DESC")
    Page<Float> totalsDecreasing(PageRequest req);

    @Find
    Receipt withPurchaseNum(long purchaseId);
}
