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

import static jakarta.data.repository.By.ID;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Or;
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

    @Count
    long countByStateButNotCity_Or_NotCityButWithCityName(@By("stateName") String state, @By(ID) @Not CityId exceptForInState,
                                                          @Or @By(ID) @Not CityId exceptForCity, @By("name") String city);

    @Delete
    void delete(City city); // copied from BasicRepository

    // "IN" (which is needed for this) is not supported for composite IDs, but EclipseLink generates SQL
    // that leads to an SQLSyntaxErrorException rather than rejecting it outright
    @Delete
    void deleteAll(Iterable<City> list); // copied from BasicRepository

    @Delete
    long deleteById(@By(ID) CityId id);

    LinkedList<CityId> deleteByStateName(String state);

    CityId deleteByStateName(String state, Limit limitOf1);

    Optional<CityId> deleteFirstByStateName(String state, Order<City> sorts);

    Iterable<CityId> deleteFirst3ByStateName(String state, Order<City> sorts);

    @Delete
    List<CityId> deleteSome(@By("stateName") String state,
                            Limit limit);

    @Delete
    CityId[] deleteWithinPopulationRange(@By("population") @GreaterThanEqual int min,
                                         @By("population") @LessThanEqual int max);

    @Exists
    boolean existsById(@By(ID) CityId id);

    boolean existsByNameAndStateName(String name, String state);

    @Find
    Optional<City> findById(@By(ID) CityId id);

    @Find
    @OrderBy("name")
    Stream<City> findByIdIsOneOf(@By(ID) CityId id1,
                                 @Or @By(ID) @IgnoreCase CityId id2,
                                 @Or @By(ID) CityId id3);

    @OrderBy("stateName")
    Stream<City> findByName(String name);

    @Find
    @OrderBy("stateName")
    Stream<City> findByNameButNotId(String name,
                                    @By(ID) @Not CityId exceptFor);

    @OrderBy(value = "stateName", descending = true)
    Stream<CityId> findByNameStartsWith(String prefix);

    @OrderBy("name")
    Stream<City> findByStateName(String state);

    @OrderBy("stateName")
    CityId[] findByStateNameEndsWith(String ending);

    CursoredPage<City> findByStateNameGreaterThan(String stateNameAfter, PageRequest pagination, Order<City> order);

    Stream<City> findByStateNameLessThan(String stateNameBefore, Sort<?>... sorts);

    @OrderBy(value = ID, descending = true, ignoreCase = true)
    Stream<City> findByStateNameNot(String exclude);

    @OrderBy(ID)
    CursoredPage<City> findByStateNameNotEndsWith(String postfix, PageRequest pagination);

    @OrderBy(ID)
    CursoredPage<City> findByStateNameNotNull(PageRequest pagination, Order<City> order);

    @OrderBy(value = ID, descending = true)
    CursoredPage<City> findByStateNameNotStartsWith(String prefix, PageRequest pagination);

    CityId findFirstByNameOrderByPopulationDesc(String name);

    @Exists
    boolean isBiggerThan(@By("population") @GreaterThan int minPopulation,
                         CityId id);

    @Find
    @OrderBy("stateName")
    @OrderBy("name")
    Stream<City> largerThan(@By("population") @GreaterThan int minPopulation,
                            @By(ID) @IgnoreCase @Not CityId exceptFor,
                            @By("stateName") @StartsWith String statePattern);

    @Delete
    void remove(City city);

    List<City> removeByStateName(String state);

    List<City> removeByStateNameOrderByName(String state);

    @Update
    int replace(@By(ID) CityId id,
                @Assign("name") String newCityName,
                @Assign("stateName") String newStateName,
                // TODO switch the above to the following once IdClass is supported for updates
                //@Assign(ID) CityId newId,
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
    @OrderBy(value = ID, descending = true)
    CursoredPage<City> sizedWithin(@By("population") @GreaterThanEqual int minPopulation,
                                   @By("population") @LessThanEqual int maxPopulation,
                                   PageRequest pagination);

    @Save
    City save(City c);

    @Update
    int updateIdPopulationAndAreaCodes(@By(ID) CityId oldId,
                                       @By("population") int oldPopulation,
                                       @Assign(ID) CityId newId,
                                       @Assign("population") int newPopulation,
                                       @Assign("areaCodes") Set<Integer> newAreaCodes);

    @Find
    @OrderBy("stateName")
    Stream<City> withNameOf(String name);
}
