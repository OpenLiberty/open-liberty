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

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import test.jakarta.data.jpa.web.CreditCard.Issuer;

/**
 * Repository for testing OneToMany relationship between Customer and CreditCard entities.
 */
@Repository(dataStore = "java:module/jdbc/RepositoryDataStore")
public interface Customers extends DataRepository<Customer, Integer> {

    @OrderBy("phone")
    Stream<CreditCard> findCardsByEmailEndsWith(String ending);

    Set<CreditCard> findCardsById(int customerId);

    @OrderBy("email")
    @Query("SELECT c.email FROM Customer c JOIN c.cards cc WHERE (cc.issuer=?1)")
    List<String> withCardIssuer(Issuer cardIssuer);

    void save(Customer... customers);
}