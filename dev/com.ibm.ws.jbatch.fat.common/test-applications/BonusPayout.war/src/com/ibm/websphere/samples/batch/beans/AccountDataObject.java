/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.samples.batch.beans;

import javax.inject.Named;

/**
 *
 */
@Named("AccountDataObject")
public class AccountDataObject {

    private int accountNumber;
    private int balance;
    private String accountCode;
    private AccountDataObject compareToDataObject;

    /**
     * @param accountNumber
     * @param balance
     * @param accountCode
     */
    public AccountDataObject(int accountNumber, int balance, String accountCode) {
        super();
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.accountCode = accountCode;
    }

    /**
     * @return the accountNumber
     */
    public int getAccountNumber() {
        return accountNumber;
    }

    /**
     * @param accountNumber the accountNumber to set
     */
    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }

    /**
     * @return the balance
     */
    public int getBalance() {
        return balance;
    }

    /**
     * @param balance the balance to set
     */
    public void setBalance(int balance) {
        this.balance = balance;
    }

    /**
     * @return the accountCode
     */
    public String getAccountCode() {
        return accountCode;
    }

    /**
     * @param accountCode the accountCode to set
     */
    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    /**
     * @return the compareToDataObject
     */
    public AccountDataObject getCompareToDataObject() {
        return compareToDataObject;
    }

    /**
     * @param compareToDataObject
     */
    public void setCompareToDataObject(AccountDataObject compareToDataObject) {
        this.compareToDataObject = compareToDataObject;
    }

}
