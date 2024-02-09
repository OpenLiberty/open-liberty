/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.Streamable;
import jakarta.data.page.KeysetAwarePage;
import jakarta.data.page.KeysetAwareSlice;
import jakarta.data.page.Pageable;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.comparison.GreaterThan;
import io.openliberty.data.repository.comparison.GreaterThanEqual;
import io.openliberty.data.repository.comparison.LessThanEqual;
import io.openliberty.data.repository.comparison.StartsWith;
import io.openliberty.data.repository.function.IgnoreCase;
import io.openliberty.data.repository.function.Not;
import io.openliberty.data.repository.update.Assign;

/**
 *
 */
@Repository
public interface Cities {
    @Exists
    boolean areFoundIn(@By("stateName") String state);

    long countByStateNameAndIdNotOrIdNotAndName(String state, CityId exceptForInState, CityId exceptForCity, String city);

    @Delete
    void delete(City city); // copied from BasicRepository

    // "IN" (which is needed for this) is not supported for composite IDs, but EclipseLink generates SQL
    // that leads to an SQLSyntaxErrorException rather than rejecting it outright
    @Delete
    void deleteAll(Iterable<City> list); // copied from BasicRepository

    long deleteByIdOrId(CityId id1, CityId id2);

    LinkedList<CityId> deleteByStateName(String state);

    CityId deleteByStateName(String state, Limit limitOf1);

    Optional<CityId> deleteFirstByStateName(String state, Order<City> sorts);

    Iterable<CityId> deleteFirst3ByStateName(String state, Order<City> sorts);

    @Delete
    Streamable<CityId> deleteSome(@By("stateName") String state,
                                  Limit limit);

    @Delete
    CityId[] deleteWithinPopulationRange(@By("population") @GreaterThanEqual int min,
                                         @By("population") @LessThanEqual int max);

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

    KeysetAwarePage<City> findByStateNameGreaterThan(String stateNameAfter, Pageable<City> pagination);

    Stream<City> findByStateNameLessThan(String stateNameBefore, Sort<?>... sorts);

    @OrderBy(value = "id", descending = true, ignoreCase = true)
    Stream<City> findByStateNameNot(String exclude);

    @OrderBy("id")
    KeysetAwareSlice<City> findByStateNameNotEndsWith(String postfix, Pageable<?> pagination);

    KeysetAwareSlice<City> findByStateNameNotNullOrderById(Pageable<City> pagination);

    KeysetAwarePage<City> findByStateNameNotStartsWithOrderByIdDesc(String prefix, Pageable<?> pagination);

    CityId findFirstByNameOrderByPopulationDesc(String name);

    @Exists
    boolean isBiggerThan(@By("population") @GreaterThan int minPopulation,
                         CityId id);

    @Find
    @OrderBy("stateName")
    @OrderBy("name")
    Stream<City> largerThan(@By("population") @GreaterThan int minPopulation,
                            @By("id") @IgnoreCase @Not CityId exceptFor,
                            @By("stateName") @StartsWith String statePattern);

    @Delete
    boolean remove(City city);

    Streamable<City> removeByStateName(String state);

    Streamable<City> removeByStateNameOrderByName(String state);

    @Update
    int replace(CityId id,
                @Assign("name") String newCityName,
                @Assign("stateName") String newStateName,
                // TODO switch the above to the following once IdClass is supported for updates
                //@Assign("id") CityId newId,
                @Assign("population") int newPopulation,
                @Assign("areaCodes") Set<Integer> newAreaCodes);

    @Update
    int replace(String name,
                String stateName,
                @Assign("name") String newCityName,
                @Assign("stateName") String newStateName,
                @Assign("areaCodes") Set<Integer> newAreaCodes,
                @Assign("population") int newPopulation);

    @Find
    @OrderBy(value = "id", descending = true)
    KeysetAwarePage<City> sizedWithin(@By("population") @GreaterThanEqual int minPopulation,
                                      @By("population") @LessThanEqual int maxPopulation,
                                      Pageable<City> pagination);

    @Save
    City save(City c);

    int updateByIdAndPopulationSetIdSetPopulationSetAreaCodes(CityId oldId, int oldPopulation,
                                                              CityId newId, int newPopulation, Set<Integer> newAreaCodes);

    @Find
    @OrderBy("stateName")
    Stream<City> withNameOf(String name);
}
