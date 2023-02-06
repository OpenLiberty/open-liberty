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
import jakarta.data.repository.Exists;
import jakarta.data.repository.Filter;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Param;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Sort;

/**
 *
 */
@Repository
public interface Cities {
    @Exists
    @Filter(by = "stateName")
    boolean areFoundIn(String state);

    boolean existsByNameAndStateName(String name, String state);

    City findById(CityId id);

    @OrderBy("stateName")
    Stream<City> findByName(String name);

    @OrderBy(value = "stateName", descending = true)
    Stream<CityId> findByNameStartsWith(String prefix);

    @OrderBy("name")
    Stream<City> findByStateName(String state);

    @OrderBy("stateName")
    CityId[] findByStateNameEndsWith(String ending);

    KeysetAwarePage<City> findByStateNameGreaterThan(String stateNameAfter, Pageable pagination);

    Stream<City> findByStateNameLessThan(String stateNameBefore, Sort... sorts);

    @OrderBy(value = "id", descending = true, ignoreCase = true)
    Stream<City> findByStateNameNot(String exclude);

    @OrderBy("id")
    KeysetAwareSlice<City> findByStateNameNotEndsWith(String postfix, Pageable pagination);

    KeysetAwareSlice<City> findByStateNameNotNullOrderById(Pageable pagination);

    KeysetAwarePage<City> findByStateNameNotStartsWithOrderByIdDesc(String prefix, Pageable pagination);

    CityId findFirstByNameOrderByPopulationDesc(String name);

    @Filter(by = "population", op = Compare.Between, param = { "minSize", "maxSize" })
    @OrderBy(value = "id", descending = true)
    KeysetAwarePage<City> sizedWithin(@Param("minSize") int minPopulation, @Param("maxSize") int maxPopulation, Pageable pagination);

    void save(City c);
}
