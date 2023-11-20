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
package test.jakarta.data.jpa.web;

import java.util.LinkedList;
import java.util.stream.Stream;

import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

/**
 * Repository that has no primary entity type and allows queries for different entity classes.
 * Do not add any lifecycle methods.
 */
@Repository
public interface MixedRepository { // Do not inherit from a supertype

    @OrderBy("name")
    Business[] findByLocationAddressCity(String cityName);

    @OrderBy("stateName")
    Stream<City> findByName(String name);

    LinkedList<Unpopulated> findBySomethingStartsWith(String prefix);
}
