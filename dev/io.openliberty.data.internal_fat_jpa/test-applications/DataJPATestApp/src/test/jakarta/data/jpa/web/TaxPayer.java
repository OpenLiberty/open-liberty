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

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * Entity with an collection of embeddables.
 */
@Entity
public class TaxPayer {
    public enum FilingStatus {
        Single, MarriedFilingJointly, MarriedFilingSeparately, HeadOfHousehold
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public Set<AccountId> bankAccounts;

    @Basic(optional = false)
    public FilingStatus filingStatus;

    public float income;

    public int numDependents;

    @Id
    public long ssn;

    public TaxPayer() {
    }

    TaxPayer(long ssn, FilingStatus status, int numDependents, float income, AccountId... bankAccounts) {
        this.ssn = ssn;
        this.filingStatus = status;
        this.numDependents = numDependents;
        this.income = income;
        this.bankAccounts = new LinkedHashSet<AccountId>();
        for (AccountId account : bankAccounts)
            this.bankAccounts.add(account);
    }

    @Override
    public String toString() {
        return "TaxPayer #" + ssn + " " + filingStatus + " with " + numDependents + " dependents and $" + income + " " + bankAccounts;
    }
}