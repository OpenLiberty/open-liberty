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

import java.util.Optional;
import java.util.Set;
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
import jakarta.data.repository.Update;

/**
 *
 */
@Repository
public interface Cities {
    @Exists
    @Filter(by = "stateName")
    boolean areFoundIn(String state);

    long countByStateNameAndIdNotOrIdNotAndName(String state, CityId exceptForInState, CityId exceptForCity, String city);

    void delete(City city); // copied from CrudRepository

    // "IN" (which is needed for this) is not supported for composite IDs, but EclipseLink generates SQL
    // that leads to an SQLSyntaxErrorException rather than rejecting it outright
    void deleteAll(Iterable<City> list); // copied from CrudRepository

    long deleteByIdOrId(CityId id1, CityId id2);

    boolean existsById(CityId id);

    boolean existsByNameAndStateName(String name, String state);

    Optional<City> findById(CityId id);

    @OrderBy("name")
    Stream<City> findByIdOrIdIgnoreCaseOrId(CityId id1, CityId id2, CityId id3);

    @OrderBy("stateName")
    Stream<City> findByName(String name);

    @OrderBy("stateName")
    Stream<City> findByNameAndIdNot(String state, CityId exceptFor);

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

    @Exists
    @Filter(by = "id", param = "name")
    @Filter(by = "population", op = Compare.GreaterThan, param = "size")
    boolean isBiggerThan(@Param("size") int minPopulation, @Param("name") CityId id);

    @Filter(by = "population", op = Compare.GreaterThan)
    @Filter(by = "id", ignoreCase = true, op = Compare.Not)
    @Filter(by = "stateName", op = Compare.StartsWith)
    @OrderBy("stateName")
    @OrderBy("name")
    Stream<City> largerThan(int minPopulation, CityId exceptFor, String statePattern);

    @Filter(by = "id")
    @Update(attr = "id")
    @Update(attr = "population")
    @Update(attr = "areaCodes")
    int replace(CityId oldId, CityId newId, int newPopulation, Set<Integer> newAreaCodes);

    @Filter(by = "id", param = "oldName")
    @Update(attr = "id", param = "newName")
    @Update(attr = "population", param = "newSize")
    @Update(attr = "areaCodes", param = "newAreaCodes")
    int replace(@Param("oldName") CityId oldId,
                @Param("newName") CityId newId,
                @Param("newAreaCodes") Set<Integer> newAreaCodes,
                @Param("newSize") int newPopulation);

    @Filter(by = "population", op = Compare.Between, param = { "minSize", "maxSize" })
    @OrderBy(value = "id", descending = true)
    KeysetAwarePage<City> sizedWithin(@Param("minSize") int minPopulation, @Param("maxSize") int maxPopulation, Pageable pagination);

    void save(City c);

    int updateByIdAndPopulationSetIdSetPopulationSetAreaCodes(CityId oldId, int oldPopulation,
                                                              CityId newId, int newPopulation, Set<Integer> newAreaCodes);

    @Filter(by = "id", op = Compare.NotNull)
    @Filter(by = "name")
    @OrderBy("stateName")
    Stream<City> withNameOf(String name);
}
