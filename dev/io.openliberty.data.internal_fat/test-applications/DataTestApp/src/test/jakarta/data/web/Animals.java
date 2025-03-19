/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import java.util.stream.Stream;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import test.jakarta.data.web.Animal.ScientificName;

/**
 * Repository interface for the Animal entity which is a record
 * with an embeddable ID that is also a record.
 */
@Repository
public interface Animals extends CrudRepository<Animal, ScientificName> {
    long countByIdNotNull();

    boolean existsById(ScientificName id);

    // Using @Find here would require @Select(ID),
    // which is not available in Data 1.0 // TODO move to 1.1 tests
    @Query("SELECT id WHERE id.genus = ?1")
    @OrderBy("id.species")
    Stream<ScientificName> ofGenus(String id_genus);
}
