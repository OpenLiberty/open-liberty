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
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 *
 */
@Repository
public interface Cities {
    @Find
    Optional<Set<Integer>> areaCodes(String name, String stateName);

    @Find
    @OrderBy("name")
    Stream<AreaInfo> areaInfo(String stateName);

    @Query("SELECT VERSION(THIS) WHERE name = ?1 AND stateName = ?2")
    long currentVersion(String city, String state);
    // TODO: IdClass as query parameter
    //@Query("SELECT VERSION(THIS) WHERE ID(THIS) = ?1")
    //long currentVersion(CityId id);

    @Delete
    void delete(City city); // copied from BasicRepository

    CityId delete1ByStateName(String state, Limit limitOf1);

    // "IN" (which is needed for this) is not supported for composite IDs, but EclipseLink generates SQL
    // that leads to an SQLSyntaxErrorException rather than rejecting it outright
    @Delete
    void deleteAll(Iterable<City> list); // copied from BasicRepository

    Optional<CityId> deleteAtMost1ByStateName(String state,
                                              Limit limitOf1,
                                              Order<City> sorts);

    @Delete
    long deleteById(@By(ID) CityId id);

    LinkedList<CityId> deleteByStateName(String state);

    Iterable<CityId> deleteByStateName(String state, Limit limit, Order<City> sorts);

    @Delete
    List<CityId> deleteSome(@By("stateName") String state,
                            Limit limit);

    boolean existsByNameAndStateName(String name, String state);

    @Find
    Optional<City> findById(@By(ID) CityId id);

    @OrderBy("stateName")
    Stream<City> findByName(String name);

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

    @Query("SELECT " + ID)
    @OrderBy("stateName")
    @OrderBy("name")
    Stream<CityId> ids();

    @Update
    City[] modifyData(City... citiesToUpdate);

    @Update
    void modifyStats(City... citiesToUpdate);

    @Delete
    void remove(City city);

    List<City> removeByStateName(String state);

    List<City> removeByStateNameOrderByName(String state);

    @Save
    City save(City c);

    @Query("WHERE population < :maxPopulation OR name <> :nameToExclude")
    @OrderBy(ID)
    CursoredPage<City> smallerThanOrNotNamed(int maxPopulation,
                                             String nameToExclude,
                                             PageRequest pageReq);

    @Find
    @OrderBy("stateName")
    Stream<City> withNameOf(String name);

    @Find
    List<City> allSorted(Sort<?>... sorts);
}
