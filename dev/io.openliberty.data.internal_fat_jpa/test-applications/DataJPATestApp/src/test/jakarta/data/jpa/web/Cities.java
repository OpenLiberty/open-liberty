/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Cities {

    long deleteByStateNameIn(Set<String> states);

    City findById(CityId id);

    @OrderBy("stateName")
    Stream<City> findByName(String name);

    @OrderBy("name")
    Stream<City> findByStateName(String state);

    void save(City c);
}
