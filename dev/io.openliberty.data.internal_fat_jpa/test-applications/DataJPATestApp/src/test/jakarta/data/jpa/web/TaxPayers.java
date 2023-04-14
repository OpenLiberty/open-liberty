/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import java.util.Set;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;

import io.openliberty.data.repository.Select;

/**
 * Repository for the TaxPayer entity.
 */
@Repository
public interface TaxPayers extends DataRepository<TaxPayer, Long> {
    long delete();

    @Select("bankAccounts")
    Set<AccountId> findAccountsBySSN(long ssn);

    void save(TaxPayer... taxPayers);
}
