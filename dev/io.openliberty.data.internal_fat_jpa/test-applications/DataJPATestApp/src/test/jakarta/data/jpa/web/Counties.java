/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Delete;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;

/**
 * Repository for the County entity.
 */
@Repository
public interface Counties {

    boolean deleteByNameAndLastUpdated(String name, LocalDateTime version);

    int deleteByNameIn(List<String> names);

    Optional<County> findByName(String name);

    @OrderBy("population")
    List<County> findByPopulationLessThanEqual(int maxPopulation);

    Optional<County> findByZipCodes(int... zipcodes);

    @Query("SELECT cities WHERE name LIKE CONCAT(?1, '%')")
    @OrderBy("name")
    List<Set<CityId>> findCitiesByNameStartsWith(String beginning);

    LocalDateTime findLastUpdatedByName(String name);

    @Query("SELECT zipcodes WHERE name = ?1")
    Optional<int[]> findZipCodesByName(String name);

    @Query("SELECT zipcodes WHERE name LIKE CONCAT('%', ?1, '%')")
    int[] findZipCodesByNameContains(String substring);

    @Query("SELECT zipcodes WHERE name LIKE CONCAT('%', ?1)")
    @OrderBy("population")
    Stream<int[]> findZipCodesByNameEndsWith(String ending);

    @Query("SELECT zipcodes WHERE name NOT LIKE CONCAT(?1, '%')")
    @OrderBy("name")
    List<int[]> findZipCodesByNameNotStartsWith(String beginning);

    @Query("SELECT zipcodes WHERE name LIKE CONCAT(?1, '%')")
    @OrderBy("population")
    @OrderBy("name")
    Page<int[]> findZipCodesByNameStartsWith(String beginning, PageRequest pagination);

    @Query("SELECT zipcodes WHERE population <= ?1")
    @OrderBy("population")
    Iterator<int[]> findZipCodesByPopulationLessThanEqual(int maxPopulation);

    default EntityManager getAutoClosedEntityManager() {
        return getEntityManager(); // must be automatically closed after getAutoClosedEntityManager ends
    }

    EntityManager getEntityManager();

    default void insert(County c) throws Exception {
        UserTransaction tx = InitialContext.doLookup("java:comp/UserTransaction");
        tx.begin();
        try (EntityManager em = getEntityManager()) {
            em.persist(c);
            em.flush();
        } finally {
            if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tx.rollback();
            else
                tx.commit();
        }
    }

    @Query("SELECT o.population FROM County o WHERE LOWER(id(o)) = ?1")
    Optional<Integer> populationOf(String lowerCaseName);

    @Delete
    void remove(County c);

    @Save
    Stream<County> save(County... c);

    default Object[] topLevelDefaultMethod() {
        EntityManager emOuter1 = getEntityManager();
        EntityManager emInner = getAutoClosedEntityManager();
        EntityManager emOuter2 = getEntityManager();
        return new Object[] { emOuter1, emOuter2, emOuter1.isOpen(), emOuter2.isOpen(), emInner.isOpen() };
    }

    boolean updateByNameSetZipCodes(String name, int... zipcodes);
}
