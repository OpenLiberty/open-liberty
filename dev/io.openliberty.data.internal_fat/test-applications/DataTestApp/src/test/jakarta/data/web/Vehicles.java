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

import java.util.Optional;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

/**
 * Repository for operations on the unannotated Vehicle entity.
 * The entity type for this repository only appears as a type parameter.
 * Do not add methods that would allow it to be discovered any other way.
 */
@Repository(dataStore = "java:module/jdbc/env/DerbyDataSourceRef")
public interface Vehicles {

    Optional<Vehicle> findById(String vin);

    @Delete
    long removeAll();

    boolean removeById(String vin);

    @Save
    Iterable<Vehicle> save(Iterable<Vehicle> v);

    boolean updateByIdAddPrice(String vin, float priceIncrease);
}
