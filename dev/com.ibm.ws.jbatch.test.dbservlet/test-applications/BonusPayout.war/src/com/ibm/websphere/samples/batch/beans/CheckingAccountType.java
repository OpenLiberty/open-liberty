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

@Named("CheckingAccountType")
public class CheckingAccountType implements AccountType {

    @Override
    public String getAccountCode() {
        return "CHK";
    }

}
