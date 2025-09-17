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
package test.jakarta.data.jpa.web;

import static jakarta.data.repository.By.ID;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

import test.jakarta.data.jpa.web.CreditCard.CardId;
import test.jakarta.data.jpa.web.CreditCard.Issuer;

/**
 * Repository for testing ManyToOne relationship between CreditCard and Customer entities.
 */
@Repository(dataStore = "java:module/jdbc/RepositoryDataStore")
public interface CreditCards extends DataRepository<CreditCard, CardId> {
    @OrderBy("debtor.phone")
    @OrderBy("issuer")
    Stream<CreditCard> findByDebtorEmailIgnoreCaseStartsWith(String beginning);

    @Query("SELECT DISTINCT c.debtor.email FROM CreditCard c WHERE c.expiresOn BETWEEN ?1 AND ?2")
    @OrderBy("debtorEmail")
    List<String> findByExpiresOnBetween(LocalDate expiresOnOrAfter, LocalDate expiresOnOrBefore);

    @Query("SELECT card FROM CreditCard card WHERE NOT (EXTRACT (QUARTER FROM card.expiresOn)) = ?1 ORDER BY card.number")
    Stream<CreditCard> expiresInQuarterOtherThan(int quarterToExclude);

    @Query("SELECT c.number FROM CreditCard c WHERE EXTRACT (YEAR FROM c.expiresOn) <= ?1 ORDER BY c.number")
    List<Long> expiringInOrBefore(int maxYearOfExpiry);

    @Query("SELECT o FROM CreditCard o WHERE (EXTRACT (WEEK FROM o.expiresOn)=?1)")
    @OrderBy("number")
    List<CreditCard> expiringInWeek(int weekNumber);

    @Query("WHERE EXTRACT (QUARTER FROM expiresOn) <> :quarterToExclude")
    @OrderBy("number")
    Stream<CreditCard> findByExpiresOnWithQuarterNot(int quarterToExclude);

    @Query("WHERE EXTRACT (WEEK FROM expiresOn) = :weekNumber")
    @OrderBy("number")
    List<CreditCard> findByExpiresOnWithWeek(int weekNumber);

    @Query("WHERE EXTRACT (MONTH FROM expiresOn) IN :months")
    @OrderBy("number")
    Stream<CreditCard> findByIssuedOnWithMonthIn(Iterable<Integer> months);

    @Query("WHERE EXTRACT (DAY FROM expiresOn) BETWEEN :minDayOfMonth AND :maxDayOfMonth")
    @OrderBy("number")
    Stream<CreditCard> findByIssuedOnWithDayBetween(int minDayOfMonth, int maxDayOfMonth);

    @OrderBy("debtor_email")
    Stream<CreditCard> findByIssuer(Issuer cardIssuer);

    @OrderBy(ID)
    Stream<CardId> findBySecurityCode(int code);

    @Query("SELECT number WHERE EXTRACT (YEAR FROM expiresOn) <= ?1")
    @OrderBy("number")
    List<Long> findNumberByExpiresOnWithYearLessThanEqual(int maxYearOfExpiry);

    @Query("SELECT o FROM CreditCard o WHERE EXTRACT (DAY FROM o.issuedOn) BETWEEN ?1 AND ?2")
    @OrderBy("number")
    Stream<CreditCard> issuedBetween(int minDayOfMonth, int maxDayOfMonth);

    @Query("SELECT o FROM CreditCard o WHERE EXTRACT (MONTH FROM o.issuedOn) IN ?1")
    @OrderBy("number")
    Stream<CreditCard> issuedInMonth(Iterable<Integer> months);

    @Update
    CreditCard replace(CreditCard newCard);

    @Update
    void revert(CreditCard previousCard);

    @Save
    void save(CreditCard... cards);

    @Save
    void save(Customer... customers);
}