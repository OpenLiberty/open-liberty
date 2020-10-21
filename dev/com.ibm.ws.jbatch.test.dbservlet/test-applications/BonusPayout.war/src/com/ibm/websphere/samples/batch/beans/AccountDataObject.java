/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
