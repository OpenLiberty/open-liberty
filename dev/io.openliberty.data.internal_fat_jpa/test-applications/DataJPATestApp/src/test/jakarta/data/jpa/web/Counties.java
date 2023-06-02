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

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Page;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Repository;

/**
 * Repository for the County entity.
 */
@Repository
public interface Counties {

    boolean deleteByNameAndLastUpdated(String name, Timestamp version);

    int deleteByNameIn(List<String> names);

    Optional<County> findByName(String name);

    @OrderBy("population")
    List<County> findByPopulationLessThanEqual(int maxPopulation);

    Optional<County> findByZipCodes(int... zipcodes);

    @OrderBy("name")
    List<Set<CityId>> findCitiesByNameStartsWith(String beginning);

    Timestamp findLastUpdatedByName(String name);

    int[] findZipCodesById(String name);

    Optional<int[]> findZipCodesByName(String name);

    @OrderBy("population")
    Stream<int[]> findZipCodesByNameEndsWith(String ending);

    @OrderBy("name")
    List<int[]> findZipCodesByNameNotStartsWith(String beginning);

    @OrderBy("population")
    @OrderBy("name")
    Page<int[]> findZipCodesByNameStartsWith(String beginning, Pageable pagination);

    @OrderBy("population")
    Optional<Iterator<int[]>> findZipCodesByPopulationLessThanEqual(int maxPopulation);

    boolean remove(County c);

    void save(County... c);

    boolean updateByNameSetZipCodes(String name, int... zipcodes);
}
