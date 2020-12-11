package com.ibm.websphere.samples.batch.beans;

import java.io.Serializable;

@PriorityAccount
public class PreferredAccountType implements AccountType, Serializable {

	private static final long serialVersionUID = 1L;

	@Override
    public String getAccountCode() {
        return "PREF";
    }
}
