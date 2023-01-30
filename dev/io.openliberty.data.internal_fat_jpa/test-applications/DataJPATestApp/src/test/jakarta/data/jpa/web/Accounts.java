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

import java.util.List;
import java.util.stream.Stream;

import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Accounts {

    long deleteByOwnerEndsWith(String pattern);

    Account findByAccountId(AccountId id);

    @OrderBy("balance")
    Stream<Account> findByAccountIdNotAndOwner(AccountId idToExclude, String owner);

    @OrderBy("bankName")
    Stream<Account> findByAccountIdAccountNum(long routingNum);

    @OrderBy("owner")
    Stream<Account> findByAccountIdRoutingNum(long routingNum);

    Account findById(AccountId id);

    @OrderBy(value = "owner", descending = true)
    Stream<Account> findByIdInOrOwner(List<AccountId> id, String owner);

    // TODO OrderBy on embeddable?

    void save(Account a);
}
