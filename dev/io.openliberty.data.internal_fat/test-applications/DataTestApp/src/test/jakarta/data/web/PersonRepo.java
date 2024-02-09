/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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
package test.jakarta.data.web;

import java.util.List;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.update.Assign;

/**
 * This example only references the entity class as a parameterized type.
 * Do not add methods or inheritance that would allow the entity class
 * to be discovered another way.
 */
@Repository
@Transactional(TxType.SUPPORTS)
public interface PersonRepo {
    @Query("SELECT o FROM Person o WHERE o.lastName=?1")
    List<Person> find(String lastName);

    @Find
    @OrderBy("firstName")
    @Select("firstName")
    List<String> findFirstNames(@By("lastName") String surname);

    @Insert
    void insert(Person p);

    @Insert
    void insertAll(Person... p);

    @Insert
    void insertAll(Iterable<Person> p);

    @Save
    void save(List<Person> people);

    @Find
    @Select("firstName")
    @Transactional(TxType.SUPPORTS)
    String getFirstNameInCurrentOrNoTransaction(Long ssn_id);

    @Update
    @Transactional(TxType.REQUIRED)
    boolean setFirstNameInCurrentOrNewTransaction(Long ssn_id,
                                                  @Assign String firstName);

    @Update
    @Transactional(TxType.MANDATORY)
    boolean setFirstNameInCurrentTransaction(@By("id") Long ssn,
                                             @Assign("firstName") String newFirstName);

    @Update
    @Transactional(TxType.REQUIRES_NEW)
    boolean setFirstNameInNewTransaction(Long ssn_id,
                                         @Assign("FirstName") String newFirstName);

    @Update
    @Transactional(TxType.NEVER)
    boolean setFirstNameWhenNoTransactionIsPresent(Long id,
                                                   @Assign("FIRSTNAME") String newFirstName);

    @Update
    @Transactional(TxType.NOT_SUPPORTED)
    boolean setFirstNameWithCurrentTransactionSuspended(Long id,
                                                        @Assign("firstname") String newFirstName);

    @Update
    boolean updateOne(Person person);

    @Update
    long updateSome(Person... people);
}