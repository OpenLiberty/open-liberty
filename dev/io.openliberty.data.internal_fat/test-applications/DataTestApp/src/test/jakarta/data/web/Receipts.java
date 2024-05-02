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
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 * Repository interface for the Receipt entity which is a record
 */
@Repository
public interface Receipts extends CrudRepository<Receipt, Long> {
    @Query("SELECT COUNT(this)")
    long count();

    boolean deleteByTotalLessThan(float max);

    Optional<Receipt> deleteByPurchaseId(long purchaseId);

    int deleteByPurchaseIdIn(Iterable<Long> ids);

    @Delete
    Collection<Receipt> discardFor(String customer);

    boolean existsByPurchaseId(long id);

    Stream<Receipt> findByPurchaseIdIn(Iterable<Long> ids);

    @Query("SELECT total FROM Receipt WHERE purchaseId=:id")
    float totalOf(long id);
}
