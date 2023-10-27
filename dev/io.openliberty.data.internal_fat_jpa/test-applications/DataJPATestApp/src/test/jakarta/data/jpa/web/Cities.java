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

import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Sort;
import jakarta.data.Streamable;
import jakarta.data.page.KeysetAwarePage;
import jakarta.data.page.KeysetAwareSlice;
import jakarta.data.page.Pageable;
import jakarta.data.repository.Delete;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Repository;

import io.openliberty.data.repository.Compare;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Filter;
import io.openliberty.data.repository.Function;
import io.openliberty.data.repository.update.Assign;

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

    LinkedList<CityId> deleteByStateName(String state);

    CityId deleteByStateName(String state, Limit limitOf1);

    Optional<CityId> deleteFirstByStateName(String state, Sort... sorts);

    Iterable<CityId> deleteFirst3ByStateName(String state, Sort... sorts);

    @Delete
    @Filter(by = "stateName")
    Streamable<CityId> deleteSome(String state, Limit limit);

    @Delete
    @Filter(by = "population", op = Compare.Between)
    CityId[] deleteWithinPopulationRange(int min, int max);

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
    @Filter(by = "id", fn = Function.IgnoreCase, op = Compare.Not)
    @Filter(by = "stateName", op = Compare.StartsWith)
    @OrderBy("stateName")
    @OrderBy("name")
    Stream<City> largerThan(int minPopulation, CityId exceptFor, String statePattern);

    boolean remove(City city);

    Streamable<City> removeByStateName(String state);

    Streamable<City> removeByStateNameOrderByName(String state);

    int replace(String name,
                String stateName,
                //TODO switch the above to the following once IdClass is supported for query conditions
                //CityId id,
                @Assign("name") String newCityName,
                @Assign("stateName") String newStateName,
                // TODO switch the above to the following once IdClass is supported for updates
                //@Assign("id") CityId newId,
                @Assign("population") int newPopulation,
                @Assign("areaCodes") Set<Integer> newAreaCodes);

    int replace(String name,
                String stateName,
                @Assign("name") String newCityName,
                @Assign("stateName") String newStateName,
                @Assign("areaCodes") Set<Integer> newAreaCodes,
                @Assign("population") int newPopulation);

    @Filter(by = "population", op = Compare.Between, param = { "minSize", "maxSize" })
    @OrderBy(value = "id", descending = true)
    KeysetAwarePage<City> sizedWithin(@Param("minSize") int minPopulation, @Param("maxSize") int maxPopulation, Pageable pagination);

    City save(City c);

    int updateByIdAndPopulationSetIdSetPopulationSetAreaCodes(CityId oldId, int oldPopulation,
                                                              CityId newId, int newPopulation, Set<Integer> newAreaCodes);

    @Filter(by = "id", op = Compare.NotNull)
    @Filter(by = "name")
    @OrderBy("stateName")
    Stream<City> withNameOf(String name);
}
