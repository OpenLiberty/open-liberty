/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * This example only references the entity class as a parameterized type.
 * Do not add methods or inheritance that would allow the entity class
 * to be discovered another way.
 */
@Repository
@Transactional(TxType.SUPPORTS)
public interface PersonRepo {
    @Query("WHERE lastName=?1")
    List<Person> find(String lastName);

    @Query("SELECT firstName WHERE lastName=:lastName")
    @OrderBy("firstName")
    List<String> findFirstNames(@Param("lastName") String surname);

    @Insert
    void insertAll(Iterable<Person> p);

    @Save
    void save(List<Person> people);

    @Find
    @Transactional(TxType.SUPPORTS)
    Person getPersonInCurrentOrNoTransaction(Long ssn_id);

    @Query("UPDATE Person SET firstName=?2 WHERE ID(THIS)=?1")
    @Transactional(TxType.REQUIRED)
    boolean setFirstNameInCurrentOrNewTransaction(Long ssn_id,
                                                  String firstName);

    @Query("UPDATE Person SET firstName=:newFirstName WHERE id(this)=:ssn")
    @Transactional(TxType.MANDATORY)
    boolean setFirstNameInCurrentTransaction(Long ssn,
                                             String newFirstName);

    @Query("UPDATE Person SET firstName=:firstName WHERE id(THIS)=:id")
    @Transactional(TxType.REQUIRES_NEW)
    boolean setFirstNameInNewTransaction(@Param("id") Long ssn,
                                         @Param("firstName") String newFirstName);

    @Query("UPDATE Person SET firstName=?2 WHERE ID(this)=?1")
    @Transactional(TxType.NEVER)
    boolean setFirstNameWhenNoTransactionIsPresent(Long id,
                                                   String newFirstName);

    @Query("UPDATE Person SET firstName=?2 WHERE Id(This)=?1")
    @Transactional(TxType.NOT_SUPPORTED)
    boolean setFirstNameWithCurrentTransactionSuspended(Long id,
                                                        String newFirstName);
}