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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

/**
 * Entity that has one-to-many mapping with the CreditCard entity.
 */
@Entity
public class Customer {

    @OneToMany(cascade = CascadeType.ALL)
    public Set<CreditCard> cards;

    @Id
    public int customerId;

    @ManyToMany(cascade = CascadeType.ALL,
                fetch = FetchType.EAGER)
    public Set<DeliveryLocation> deliveryLocations;

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
        deliveryLocations = new LinkedHashSet<>();
    }

    public void addCard(CreditCard card) {
        card.debtor = this;
        cards.add(card);
    }

    public void addDeliveryLocation(DeliveryLocation loc) {
        deliveryLocations.add(loc);
        loc.customers.add(this);
    }

    public void removeCard(CreditCard card) {
        cards.remove(card);
        card.debtor = null;
    }

    public void removeDeliveryLocation(DeliveryLocation loc) {
        deliveryLocations.remove(loc);
        loc.customers.remove(this);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder().append("Customer #").append(customerId).append(' ').append(email).append(' ').append(phone);
        boolean first = true;
        for (CreditCard card : cards) {
            s.append(first ? " with " : ", ").append(card.issuer).append(" card #").append(card.number);
            first = false;
        }
        first = false;
        for (DeliveryLocation loc : deliveryLocations) {
            s.append(first ? " @ " : ", ").append(loc.houseNum).append(' ').append(loc.street);
            first = false;
        }
        return s.toString();
    }
}
