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
package test.jakarta.data.web;

import java.util.List;
import java.util.Optional;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * Repository with multiple entity classes,
 * and where this is no primary enitty class.
 */
@Repository
public interface MultiRepository {

    @Insert
    List<Person> add(Person... people);

    @Insert
    Product create(Product prod);

    Person[] deleteByIdIn(Iterable<Long> ids);

    Optional<Package> findById(Number id);

    @Update
    int modify(Product prod);

    @Delete
    boolean remove(Person person);

    @Save
    Package upsert(Package p);
}
