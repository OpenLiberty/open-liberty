/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * A repository that is annotated with Transactional and sets the REQUIRES_NEW
 * transaction type so that by default, all repository operations will run in a
 * transaction of their own instead of a transaction that was already on the thread.
 */
@Repository
@Transactional(TxType.REQUIRES_NEW)
public interface Persons {

    @Insert
    @Transactional(TxType.SUPPORTS)
    void insert(Person person);

    @Insert
    void insertAll(Person... people);

    @Update
    boolean updateOne(Person person);

    @Update
    long updateSome(Person... people);
}