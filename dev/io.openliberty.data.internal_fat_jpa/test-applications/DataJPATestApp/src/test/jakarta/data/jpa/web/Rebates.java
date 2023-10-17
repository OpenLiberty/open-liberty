/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * Experiments with auto-generated keys on records.
 * TODO remove DataRepository and have the entity type be inferred from the methods, as the spec claims is possible.
 */
@Repository
public interface Rebates extends DataRepository<Rebate, Integer> {
    @Insert
    Rebate add(Rebate r);

    @Insert
    Rebate[] addAll(Rebate... r);

    @Insert
    Iterable<Rebate> addMultiple(Iterable<Rebate> r);

    @Update
    Rebate modify(Rebate r);

    @Update
    Rebate[] modifyAll(Rebate... r);

    @Update
    List<Rebate> modifyMultple(List<Rebate> r);

    @Save
    Rebate process(Rebate r);

    @Save
    Rebate[] processAll(Rebate... r);

    @Save
    Collection<Rebate> processMultiple(Collection<Rebate> r);

    // TODO allow entity return types for Delete?
    @Delete
    boolean remove(Rebate r);

    @Delete
    int removeAll(Rebate... r);

    @Delete
    int removeMultiple(ArrayList<Rebate> r);
}
