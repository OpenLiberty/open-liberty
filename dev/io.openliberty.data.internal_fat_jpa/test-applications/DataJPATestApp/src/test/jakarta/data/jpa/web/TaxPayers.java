/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

import io.openliberty.data.repository.Select;

/**
 * Repository for the TaxPayer entity.
 */
@Repository
public interface TaxPayers extends DataRepository<TaxPayer, Long> {
    @Delete
    long delete();

    @Select("bankAccounts")
    Set<AccountId> findAccountsBySSN(long ssn);

    Stream<AccountId> findBankAccountsByFilingStatus(TaxPayer.FilingStatus status);

    @OrderBy("income")
    @OrderBy("ssn")
    Stream<TaxPayer> findByBankAccountsContains(AccountId account);

    @OrderBy("ssn")
    Stream<TaxPayer> findByBankAccountsNotEmpty();

    @Save
    Iterable<TaxPayer> save(Iterable<TaxPayer> taxPayers);

    @Save
    Iterator<TaxPayer> save(TaxPayer... taxPayers);
}
