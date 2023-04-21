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

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Page;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Repository;

import io.openliberty.data.repository.Select;

/**
 * Repository for the County entity.
 */
@Repository
public interface Counties {

    int deleteByNameIn(List<String> names);

    Optional<County> findByName(String name);

    @OrderBy("population")
    List<County> findByPopulationLessThanEqual(int maxPopulation);

    Optional<County> findByZipCodes(int... zipcodes);

    @Select("cities")
    @OrderBy("name")
    List<Set<CityId>> findCityListByNameStartsWith(String beginning);

    @Select("zipcodes")
    int[] findZipCodesById(String name);

    @Select("zipcodes")
    Optional<int[]> findZipCodesByName(String name);

    @Select("zipcodes")
    @OrderBy("population")
    Stream<int[]> findZipCodesByNameEndsWith(String ending);

    @Select("zipcodes")
    @OrderBy("name")
    List<int[]> findZipCodesByNameNotStartsWith(String beginning);

    @Select("zipcodes")
    @OrderBy("population")
    @OrderBy("name")
    Page<int[]> findZipCodesByNameStartsWith(String beginning, Pageable pagination);

    @Select("zipcodes")
    @OrderBy("population")
    Optional<Iterator<int[]>> findZipCodesByPopulationLessThanEqual(int maxPopuluation);

    void save(County... c);

    boolean updateByNameSetZipCodes(String name, int... zipcodes);
}
