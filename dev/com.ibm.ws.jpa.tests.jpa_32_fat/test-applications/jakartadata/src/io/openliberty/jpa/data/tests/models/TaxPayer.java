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

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TaxPayer {
    public enum FilingStatus {
        Single, MarriedFilingJointly, MarriedFilingSeparately, HeadOfHousehold
    }

    @ElementCollection
    public Set<AccountId> bankAccounts;

    @Basic(optional = false)
    public FilingStatus filingStatus;

    public float income;

    public int numDependents;

    @Id
    public long ssn;

    public TaxPayer() {
    }

    public TaxPayer(long ssn, FilingStatus status, int numDependents, float income, AccountId... bankAccounts) {
        this.ssn = ssn;
        this.filingStatus = status;
        this.numDependents = numDependents;
        this.income = income;
        this.bankAccounts = new LinkedHashSet<AccountId>();
        for (AccountId account : bankAccounts)
            this.bankAccounts.add(account);
    }
}