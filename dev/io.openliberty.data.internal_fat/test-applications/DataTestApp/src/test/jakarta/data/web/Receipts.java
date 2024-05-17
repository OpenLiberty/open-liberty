/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import jakarta.data.repository.OrderBy;
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

    @Asynchronous
    CompletionStage<Optional<Receipt>> findByPurchaseIdIfPresent(long purchaseId);

    Stream<Receipt> findByPurchaseIdIn(Iterable<Long> ids);

    @OrderBy("purchaseId")
    Receipt[] forCustomer(String customer);

    @Asynchronous
    CompletableFuture<List<Receipt>> forCustomer(String customer, Order<Receipt> sorts);

    Page<Receipt> forCustomer(String customer, PageRequest req, Order<Receipt> sorts);

    CursoredPage<Receipt> forCustomer(String customer, PageRequest req, Sort<?>... sorts);

    long removeByPurchaseId(long purchaseId);

    @OrderBy("purchaseId")
    List<Long> removeByTotalBetween(float min, float max);

    @Query("DELETE FROM Receipt WHERE total < :max")
    int removeIfTotalUnder(float max);

    @Query("SELECT total FROM Receipt WHERE purchaseId=:id")
    float totalOf(long id);

    Receipt withPurchaseNum(long purchaseId);
}
