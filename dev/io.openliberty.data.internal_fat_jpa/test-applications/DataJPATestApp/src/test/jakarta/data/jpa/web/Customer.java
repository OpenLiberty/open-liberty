/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * Entity that has one-to-many mapping with the CreditCard entity.
 */
@Entity
public class Customer {

    @OneToMany
    public Set<CreditCard> cards;

    @Id
    public int customerId;

    @Column
    public String email;

    @Column
    public long phone;

    public Customer() {
    }

    public Customer(int id, String emailAddress, long phoneNumber, CreditCard... creditCards) {
        customerId = id;
        email = emailAddress;
        phone = phoneNumber;
        cards = new LinkedHashSet<>();
        for (CreditCard card : creditCards) {
            card.debtor = this;
            cards.add(card);
        }
    }

    public void addCard(CreditCard card) {
        card.debtor = this;
        cards.add(card);
    }

    public void removeCard(CreditCard card) {
        cards.remove(card);
        card.debtor = null;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder().append("Customer #").append(customerId).append(' ').append(email).append(' ').append(phone);
        boolean first = true;
        for (CreditCard card : cards) {
            s.append(first ? " with " : ", ").append(card.issuer).append(" card #").append(card.number);
            first = false;
        }
        return s.toString();
    }
}
