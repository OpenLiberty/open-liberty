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
package test.jakarta.data.jpa.web;

import static jakarta.data.repository.By.ID;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * Experiments with auto-generated keys on records.
 */
@Repository
public interface Rebates { // Do not allow this interface to inherit from other repositories, so that it tests inferring a primary entity class
    @Insert
    Rebate add(Rebate r);

    @Insert
    Rebate[] addAll(Rebate... r);

    @Insert
    Iterable<Rebate> addMultiple(Iterable<Rebate> r);

    @Query("WHERE customerId=?1")
    @OrderBy("amount")
    List<Double> amounts(String customerId);

    // It should be acceptable for the return type to be more general
    // when using @Query
    @Query("SELECT purchaseMadeOn WHERE ID(THIS) = :id")
    Optional<Temporal> dayOfPurchase(int id);

    List<LocalDate> findByCustomerIdOrderByPurchaseMadeOnDesc(String customer);

    @OrderBy("purchaseMadeOn")
    @OrderBy("purchaseMadeAt")
    PurchaseTime[] findTimeOfPurchaseByCustomerId(String customer);

    @Find
    Optional<LocalDateTime> lastUpdated(int id);

    @Update
    Rebate modify(Rebate r);

    @Update
    Rebate[] modifyAll(Rebate... r);

    @Update
    List<Rebate> modifyMultiple(List<Rebate> r);

    @Query("SELECT ID(THIS)" +
           "  FROM Rebate" +
           " WHERE updatedAt <> LOCAL DATETIME AND customerId LIKE ?1")
    @OrderBy(value = "amount", descending = true)
    List<Integer> notRecentlyUpdated(String customerIdPattern);

    @Query("WHERE customerId=?1 AND status=test.jakarta.data.jpa.web.Rebate.Status.PAID ORDER BY amount DESC, id ASC")
    List<Rebate> paidTo(String customerId);

    @Save
    Rebate process(Rebate r);

    @Save
    Rebate[] processAll(Rebate... r);

    @Query("SELECT id " +
           " WHERE customerId LIKE ?1" +
           "   AND (purchaseMadeAt < LOCAL TIME AND purchaseMadeOn = LOCAL DATE" +
           "     OR purchaseMadeOn < LOCAL DATE)")
    @OrderBy("updatedAt")
    @OrderBy(ID)
    List<Integer> purchasedInThePast(String customerIdPattern);

    @Find
    Optional<PurchaseTime> purchaseTime(int id);

    @Save
    Collection<Rebate> processMultiple(Collection<Rebate> r);

    @Delete
    void remove(Rebate r);

    @Delete
    void removeAll(Rebate... r);

    @Delete
    void removeMultiple(ArrayList<Rebate> r);

    @Find
    Optional<Rebate.Status> status(int id);
}
