/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;

import test.jakarta.data.jpa.web.CreditCard.CardId;
import test.jakarta.data.jpa.web.CreditCard.Issuer;

/**
 * Repository for testing ManyToOne relationship between CreditCard and Customer entities.
 */
@Repository
public interface CreditCards extends DataRepository<CreditCard, CardId> {
    @OrderBy("debtor.phone")
    @OrderBy("issuer")
    Stream<CreditCard> findByDebtorEmailIgnoreCaseStartsWith(String beginning);

    @OrderBy("debtorEmail")
    @Select(distinct = true, value = "debtorEmail")
    List<String> findByExpiresOnBetween(LocalDate expiresOnOrAfter, LocalDate expiresOnOrBefore);

    @OrderBy("debtor_email")
    Stream<Customer> findByIssuer(Issuer cardIssuer);

    @OrderBy("id")
    Stream<CardId> findBySecurityCode(int code);

    void save(CreditCard... cards);

    void save(Customer... customers);
}