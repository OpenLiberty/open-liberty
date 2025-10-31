/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.stream.Stream;

import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

/**
 * Repository for the Account entity, which has an EmbeddedId.
 */
@Repository(dataStore = "java:app/env/data/DataStoreRef")
public interface Accounts {

    @Query("UPDATE Account SET balance = balance + 15e-2 WHERE accountId = ?1")
    boolean addInterest(AccountId id);

    long countByOwnerAndBalanceBetween(String owner, double min, double max);

    @Insert
    void create(Account a);

    @Insert
    void createAll(Account... a);

    // "IN" (which is needed for this) is not supported for embeddables, but EclipseLink generates SQL
    // that leads to an SQLSyntaxErrorException rather than rejecting it outright
    @Delete
    void deleteAll(Iterable<Account> list); // copied from CrudRepository

    long deleteByOwnerEndsWith(String pattern);

    Account findByAccountId(AccountId id);

    @OrderBy("balance")
    Stream<Account> findByAccountIdNotAndOwner(AccountId idToExclude, String owner);

    @OrderBy("bankName")
    Stream<Account> findByAccountIdAccountNum(long accountNum);

    @OrderBy("owner")
    Stream<Account> findByAccountIdRoutingNum(long routingNum);

    @OrderBy("accountId")
    Stream<AccountId> findByBankName(String bank);

    @Find
    Account findById(@By(ID) AccountId id);

    // EclipseLink IllegalArgumentException:
    // Problem compiling [SELECT o FROM Account o WHERE (o.accountId BETWEEN ?1 AND ?2)].
    // [31, 42] The association field 'o.accountId' cannot be used as a state field path.
    List<Account> findByAccountIdBetween(AccountId minId, AccountId maxId);

    List<Account> findByAccountIdEmpty();

    // EclipseLink IllegalArgumentException:
    // Problem compiling [SELECT o FROM Account o WHERE (o.accountId>?1)].
    // The relationship mapping 'o.accountId' cannot be used in conjunction with the > operator
    Stream<Account> findByAccountIdGreaterThan(AccountId id);

    // EclipseLink org.eclipse.persistence.exceptions.DatabaseException
    // java.sql.SQLSyntaxErrorException: Syntax error: Encountered "," at line 1, column 102. Error Code: 30000
    // Call: SELECT BALANCE, BANKNAME, CHECKING, OWNER, ACCOUNTNUM, ROUTINGNUM FROM WLPAccount
    // WHERE (((ACCOUNTNUM, ROUTINGNUM) IN (AccountId:1004470:30372, AccountId:1006380:22158)) OR (OWNER = ...)) ORDER BY OWNER DESC
    // ** position 102 is ^ **
    @OrderBy(value = "owner", descending = true)
    Stream<Account> findByAccountIdInOrOwner(List<AccountId> id, String owner);

    @OrderBy("accountId")
    Stream<Account> findByAccountIdNotNull();

    // EclipseLink org.eclipse.persistence.exceptions.DescriptorException
    // No subclass matches this class [class java.lang.Boolean] for this Aggregate mapping with inheritance.
    // Mapping: org.eclipse.persistence.mappings.AggregateObjectMapping[accountId]
    // Descriptor: RelationalDescriptor(test.jakarta.data.jpa.web.Account --> [DatabaseTable(WLPAccount)])
    List<Account> findByAccountIdTrue();

    @Delete
    void remove(Account account);

    @Save
    void save(Account a);
}
