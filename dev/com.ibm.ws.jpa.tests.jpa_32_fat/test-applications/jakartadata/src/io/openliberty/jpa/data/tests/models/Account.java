/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * Recreate from io.openliberty.data.internal_fat_jpa
 */
@Entity
public class Account {

    @EmbeddedId
    public AccountId accountId;

    public double balance;

    public String bankName;

    public boolean checking;

    public String owner;

    public static Account of(long accountNum, long routingNum, String bankName, boolean checking, double balance, String owner) {
        Account inst = new Account();
        inst.accountId = AccountId.of(accountNum, routingNum);
        inst.bankName = bankName;
        inst.checking = checking;
        inst.balance = balance;
        inst.owner = owner;
        return inst;
    }

    @Override
    public String toString() {
        return bankName + ' ' + accountId + " $" + balance + " owned by " + owner + (checking ? " with checking" : "");
    }
}