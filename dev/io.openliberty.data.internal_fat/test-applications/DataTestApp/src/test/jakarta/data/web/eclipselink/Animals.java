/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
package test.jakarta.data.web.eclipselink;

import java.util.stream.Stream;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import test.jakarta.data.web.eclipselink.Animal.ScientificName;

/**
 * Repository interface for the Animal entity which is a record
 * with an embeddable ID that is also a record.
 */
@Repository(dataStore = "java:module/env/jdbc/DerbyDataSourceRef")
public interface Animals extends CrudRepository<Animal, ScientificName> {
    long countByIdNotNull();

    boolean existsById(ScientificName id);

    // Using @Find here would require @Select(ID), which isn't available
    // until Data 1.1
    @Query("SELECT id WHERE id.genus = ?1")
    @OrderBy("id.species")
    Stream<ScientificName> ofGenus(String id_genus);
}
