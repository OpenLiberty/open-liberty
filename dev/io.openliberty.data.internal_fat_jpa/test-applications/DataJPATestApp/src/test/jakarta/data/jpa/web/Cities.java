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

import java.util.stream.Stream;

import jakarta.data.repository.Compare;
import jakarta.data.repository.Filter;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Param;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Cities {

    City findById(CityId id);

    @OrderBy("stateName")
    Stream<City> findByName(String name);

    @OrderBy("name")
    Stream<City> findByStateName(String state);

    KeysetAwarePage<City> findByStateNameGreaterThan(String stateNameBefore, Pageable pagination);

    @OrderBy(value = "id", descending = true, ignoreCase = true)
    Stream<City> findByStateNameNot(String exclude);

    @OrderBy("id")
    KeysetAwareSlice<City> findByStateNameNotEndsWith(String postfix, Pageable pagination);

    KeysetAwareSlice<City> findByStateNameNotNullOrderById(Pageable pagination);

    KeysetAwarePage<City> findByStateNameNotStartsWithOrderByIdDesc(String prefix, Pageable pagination);

    @Filter(by = "population", op = Compare.Between, param = { "minSize", "maxSize" })
    @OrderBy(value = "id", descending = true)
    KeysetAwarePage<City> sizedWithin(@Param("minSize") int minPopulation, @Param("maxSize") int maxPopulation, Pageable pagination);

    void save(City c);
}
