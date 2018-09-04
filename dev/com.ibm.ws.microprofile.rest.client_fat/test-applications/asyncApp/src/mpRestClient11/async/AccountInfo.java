/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient11.async;

public class AccountInfo {

    String customerName;
    String accountNumber;
    
    public AccountInfo() {}
    public AccountInfo(String customerName, String accountNumber) {
        this.customerName = customerName;
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    @Override
    public String toString() {
        return "AccountInfo(" + customerName + ", " + accountNumber + ")";
    }
    
}

