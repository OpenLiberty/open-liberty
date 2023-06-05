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

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 *
 */
@Entity
public class Account {

    @EmbeddedId
    public AccountId accountId;

    public double balance;

    public String bankName;

    public boolean checking;

    public String owner;

    public Account() {
    }

    Account(long accountNum, long routingNum, String bankName, boolean checking, double balance, String owner) {
        this.accountId = AccountId.of(accountNum, routingNum);
        this.bankName = bankName;
        this.checking = checking;
        this.balance = balance;
        this.owner = owner;
    }

    @Override
    public String toString() {
        return bankName + ' ' + accountId + " $" + balance + " owned by " + owner + (checking ? " with checking" : "");
    }
}