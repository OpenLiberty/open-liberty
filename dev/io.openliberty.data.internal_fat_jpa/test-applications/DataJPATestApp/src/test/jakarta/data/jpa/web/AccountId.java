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

import jakarta.persistence.Embeddable;

/**
 * Id class for Account entity.
 */
@Embeddable
public class AccountId {

    public long accountNum;
    public long routingNum;

    public AccountId() {
    }

    public AccountId(long accountNum, long routingNum) {
        this.accountNum = accountNum;
        this.routingNum = routingNum;
    }

    public static AccountId of(long accountNum, long routingNum) {
        return new AccountId(accountNum, routingNum);
    }

    @Override
    public String toString() {
        return "AccountId:" + accountNum + ":" + routingNum;
    }
}