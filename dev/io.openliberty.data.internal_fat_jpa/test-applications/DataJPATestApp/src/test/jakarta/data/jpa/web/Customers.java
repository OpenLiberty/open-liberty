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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

import test.jakarta.data.jpa.web.CreditCard.Issuer;

/**
 * Repository for testing OneToMany relationship between Customer and CreditCard entities
 * and for testing the ManyToMany relationship between Customer and DeliveryLocation entities.
 */
@Repository(dataStore = "java:app/env/data/DataStoreRef")
public interface Customers extends DataRepository<Customer, Integer> {

    Optional<Customer> findByEmail(String emailAddress);

    @OrderBy("email")
    Stream<Customer> findByPhoneIn(List<Long> phoneNumbers);

    @OrderBy("debtor.phone")
    Stream<CreditCard> findCardsByDebtorEmailEndsWith(String ending);

    Set<CreditCard> findCardsByDebtorCustomerId(int customerId);

    @OrderBy("email")
    @Query("SELECT c.email FROM Customer c JOIN c.cards cc WHERE (cc.issuer=?1)")
    List<String> withCardIssuer(Issuer cardIssuer);

    @Query("SELECT dl.street FROM Customer c JOIN c.deliveryLocations dl WHERE (dl.type=?1) ORDER BY dl.street.name DESC, dl.street.direction")
    Stream<Street> withLocationType(DeliveryLocation.Type locationType);

    @Save
    void save(Customer... customers);
}