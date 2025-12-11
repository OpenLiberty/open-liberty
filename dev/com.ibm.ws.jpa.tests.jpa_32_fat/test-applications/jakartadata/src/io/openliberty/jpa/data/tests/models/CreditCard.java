
/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

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

    public static CreditCard of(long number, int securityCode, LocalDate issuedOn, LocalDate expiresOn, Issuer issuer) {
        CreditCard inst = new CreditCard();

        inst.issuer = issuer;
        inst.number = number;
        inst.securityCode = securityCode;
        inst.issuedOn = issuedOn;
        inst.expiresOn = expiresOn;

        return inst;
    }

    @Override
    public String toString() {
        return issuer + " card #" + number + " (" + securityCode + ") " +
               " valid from " + issuedOn + " to " + expiresOn;
    }
}