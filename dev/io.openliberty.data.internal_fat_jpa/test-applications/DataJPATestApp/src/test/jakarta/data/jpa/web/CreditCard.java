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

import java.time.LocalDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

/**
 * Entity that has many-to-one mapping with the Customer entity.
 */
@Entity
@IdClass(CreditCard.CardId.class)
public class CreditCard {
    public static enum Issuer {
        AmericanExtravagance, Discrooger, MonsterCard, Feesa
    }

    public static record CardId(Issuer issuer, long number) {
        @Override
        public String toString() {
            return issuer + " card #" + number;
        }
    }

    @ManyToOne(cascade = CascadeType.ALL)
    public Customer debtor;

    @Column
    public LocalDate expiresOn;

    @Column
    public LocalDate issuedOn;

    @Id
    public Issuer issuer;

    @Id
    public long number;

    @Column
    public int securityCode;

    public CreditCard() {
    }

    public CreditCard(Customer debtor, long number, int securityCode, LocalDate issuedOn, LocalDate expiresOn, Issuer issuer) {
        this.issuer = issuer;
        this.number = number;
        this.securityCode = securityCode;
        this.issuedOn = issuedOn;
        this.expiresOn = expiresOn;
        this.debtor = debtor;
        debtor.addCard(this);
    }

    @Override
    public String toString() {
        return issuer + " card #" + number + " (" + securityCode + ") for " + (debtor == null ? null : debtor.email) +
               " valid from " + issuedOn + " to " + expiresOn;
    }
}
