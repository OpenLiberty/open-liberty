/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.errpaths.web;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * Repository with a valid entity.
 * Some methods are valid.
 * Others have errors, as indicated.
 */
@Repository(dataStore = "java:app/jdbc/DerbyDataSource")
public interface Voters extends BasicRepository<Voter, Integer> {
    static record NameAndZipCode(String name, int zipCode) {
    }

    /**
     * Invalid method. A method with a life cycle annotation must have exactly
     * 1 parameter.
     */
    @Insert
    Voter[] addNothing();

    @Save
    void addOrUpdate(List<PollingLocation> list);

    @Insert
    void addPollingLocation(PollingLocation loc);

    /**
     * Invalid method. A method with a life cycle annotation must have exactly
     * 1 parameter, not multiple.
     */
    @Insert
    List<Voter> addSome(List<Voter> v, Limit limit);

    @Find
    Page<Voter> atAddress(@By("address") String homeAddress,
                          PageRequest pageReq,
                          Order<Voter> order);

    /**
     * This invalid method neglects to include the Param annotation for a
     * named parameter and is not running with -parameters enabled.
     */
    @Query("""
                    WHERE EXTRACT(YEAR FROM birthday) = :year
                    ORDER BY EXTRACT(MONTH FROM birthday) ASC,
                             EXTRACT(DAY FROM birthday) ASC,
                             ssn ASC
                    """)
    List<Voter> bornIn(int year); // missing @Param and -parameters not used

    /**
     * This invalid method has two Param annotations for the "month" named parameter.
     */
    @Query("""
                    WHERE EXTRACT(YEAR FROM birthday) = :year
                      AND EXTRACT(MONTH FROM birthday) = :month
                      AND EXTRACT(DAY FROM birthday) = :day
                    ORDER BY EXTRACT(YEAR FROM birthday) DESC,
                             EXTRACT(MONTH FROM birthday) ASC,
                             EXTRACT(DAY FROM birthday) ASC,
                             ssn ASC
                    """)
    List<Voter> bornOn(@Param("year") int yearBorn,
                       @Param("month") Month monthBorn,
                       @Param("month") int monthNum, // duplicate parameter name
                       @Param("day") int dayBorn);

    @Update
    List<Voter> changeAll(Stream<Voter> v);

    /**
     * Invalid method. A method with a life cycle annotation must have exactly
     * 1 parameter, not multiple.
     */
    @Update
    List<Voter> changeBoth(Voter v1, Voter v2);

    /**
     * Invalid method. A method with a life cycle annotation must have exactly
     * 1 parameter.
     */
    @Update
    void changeNothing();

    /**
     * This invalid method defines a limit on results of a delete operation
     * but has a return type that disallows returning results.
     */
    @Delete
    void discardLimited(@By("address") String mailingAddress, Limit limit);

    /**
     * This invalid method defines an ordering for results of a delete operation
     * but has a return type that disallows returning results.
     */
    @Delete
    void discardOrdered(@By("address") String mailingAddress, Order<Voter> order);

    /**
     * This invalid method attempts to delete a page of results.
     */
    @Delete
    void discardPage(@By("address") String mailingAddress, PageRequest pageReq);

    /**
     * This invalid method defines sorting of results of a delete operation
     * but has a return type that disallows returning results.
     */
    @Delete
    int discardSorted(@By("address") String mailingAddress, Sort<Voter> sort);

    /**
     * This invalid method attempts to return a true/false exists results as int.
     */
    int existsByAddress(String homeAddress);

    /**
     * This invalid method attempts to return a true/false exists results as a
     * Long value within a CompletableFuture. The CompletableFuture is fine, but
     * Long does not match the true/false result type.
     */
    CompletableFuture<Long> existsByName(String name);

    /**
     * This invalid method has a conflict between its OrderBy annotation and
     * method name keyword.
     */
    @OrderBy("ssn")
    List<Voter> findByAddressOrderByName(String address);

    /**
     * This invalid method has a conflict between its OrderBy annotation and
     * method name keyword. It also has a dynamic sort parameter.
     */
    @OrderBy("name")
    List<Voter> findByAddressOrderBySSN(int ssn, Sort<Voter> sort);

    /**
     * This invalid method attempts to return a CursoredPage of a non-entity type.
     */
    CursoredPage<Integer> findByBirthdayOrderBySSN(LocalDate birthday,
                                                   PageRequest pageReq);

    /**
     * This invalid method has both a First keyword and a Limit parameter.
     */
    @OrderBy("ssn")
    Voter[] findFirst2(Limit limit);

    /**
     * This invalid method has both a First keyword and a PageRequest parameter.
     */
    @OrderBy("ssn")
    Page<Voter> findFirst3(PageRequest pageRequest);

    /**
     * This invalid method places the Order special parameter ahead of
     * the query parameter.
     */
    List<Voter> findFirst5ByAddress(Order<Voter> order,
                                    String address);

    /**
     * This invalid method defines an ordering for results of a delete operation
     * but has a return type that disallows returning results.
     */
    void deleteByAddressOrderByName(String address);

    /**
     * This invalid method defines an ordering for results of a delete operation
     * but has a return type that disallows returning results.
     */
    @Delete
    @OrderBy("name")
    void discardInOrder(@By("address") String mailingAddress);

    /**
     * This invalid method has Limit and PageRequest parameters and returns a List.
     */
    @Find
    List<Voter> inhabiting(@By("address") String homeAddress,
                           Limit limit,
                           Order<Voter> order,
                           PageRequest pageReq);

    /**
     * This invalid method has 2 Limit parameters.
     */
    @Find
    List<Voter> livesAt(@By("address") String homeAddress,
                        Limit firstLimit,
                        Order<Voter> order,
                        Limit secondLimit);

    /**
     * This invalid method has a mixture of positional and named parameters.
     */
    @Query("""
                    WHERE LOWER(address) = LOWER(CONCAT(?1, ?2, :city, ?4, :zip))
                    ORDER BY LOWER(address) ASC,
                             ssn ASC
                    """)
    List<Voter> livingAt(int houseNum,
                         String streetName,
                         @Param("city") String city,
                         String stateCode,
                         @Param("zip") int zip);

    /**
     * This invalid method has a mismatch between one of the named parameter names
     * (:stateCode) and the Param annotation (state).
     */
    @Query("""
                    WHERE UPPER(address) LIKE CONCAT('% ', UPPER(:city), ', %')
                      AND UPPER(address) LIKE CONCAT('%, ', UPPER(:stateCode), ' %')
                    ORDER BY UPPER(address) ASC,
                             ssn ASC
                    """)
    List<Voter> livingIn(@Param("city") String city,
                         @Param("state") String stateCode); // Param does not match

    /**
     * This invalid method has extra Param annotations (city, state) that are not
     * used in the query.
     */
    @Query("""
                    WHERE UPPER(address) LIKE CONCAT('% ', UPPER(:street), ', %')
                    ORDER BY UPPER(address) ASC,
                             ssn ASC
                    """)
    List<Voter> livingOn(@Param("street") String street,
                         @Param("city") String city, // extra, unused Param
                         @Param("state") String stateCode); // extra, unused Param

    /**
     * Find method that returns a record instead of an entity,
     * but where the names of record components do not all match
     * entity attribute names.
     */
    @Find
    Optional<NameAndZipCode> nameAndZipCode(@By("ssn") int socialSecurityNumber);

    /**
     * This invalid method places the PageRequest and Order special parameters
     * before the query parameter.
     */
    @Find
    Page<Voter> occupantsOf(PageRequest pageReq,
                            Order<Voter> order,
                            @By("address") String homeAddress);

    /**
     * This invalid method has Limit and PageRequest parameters and returns a Page.
     */
    @Find
    Page<Voter> occupying(@By("address") String homeAddress,
                          PageRequest pageReq,
                          Order<Voter> order,
                          Limit limit);

    /**
     * For testing an error where the method parameter allows multiple entities,
     * but the return type only allows one.
     */
    @Insert
    Voter register(Voter... v);

    /**
     * This invalid method attempts to return a CursoredPage of a non-entity type.
     */
    @Find
    @OrderBy("ssn")
    CursoredPage<VoterRegistration> registrations(@By("birthday") LocalDate birthday,
                                                  PageRequest pageReq);

    /**
     * Delete method that attempts to return a record instead of an entity.
     */
    @Delete
    Optional<NameAndZipCode> removeBySSN(@By("ssn") int socialSecurityNumber);

    /**
     * This invalid method has 2 PageRequest parameters.
     */
    @Find
    Page<Voter> residesAt(@By("address") String homeAddress,
                          PageRequest pageReq1,
                          Order<Voter> order,
                          PageRequest pageReq2);

    /**
     * This invalid method has matching named parameters and Param annotation,
     * but also has extra parameters (city, stateCode) that are not used in the
     * query.
     */
    @Query("""
                    WHERE address LIKE CONCAT(:houseNum, ' ', :street, ', %')
                    ORDER BY address ASC,
                             ssn ASC
                    """)
    List<Voter> residingAt(@Param("houseNum") int houseNum,
                           @Param("street") String street,
                           String city, // extra, unused parameter
                           String stateCode); // extra, unused parameter

    @Find
    Page<Voter> selectAll(PageRequest req,
                          Sort<?>... sorts);

    @Find
    CursoredPage<Voter> selectByAddress(@By("address") String homeAddress,
                                        PageRequest pageReq,
                                        Sort<?>... sorts);

    @Find
    CursoredPage<Voter> selectByBirthday(@By("birthday") LocalDate date,
                                         PageRequest pageReq,
                                         Order<Voter> order);

    /**
     * This invalid method includes an ORDER BY clause with cursor pagination.
     */
    @Query("WHERE name LIKE (:fname || ' %') ORDER BY name ASC, ssn ASC")
    CursoredPage<Voter> selectByFirstName(@Param("fname") String lastName,
                                          PageRequest pageReq,
                                          Order<Voter> order);

    /**
     * This invalid method lacks an OrderBy annotation or any sort parameters.
     */
    @Query("WHERE name LIKE ('% ' || :lname)")
    CursoredPage<Voter> selectByLastName(@Param("lname") String lastName,
                                         PageRequest pageReq);

    @Find
    CursoredPage<Voter> selectByName(@By("name") String name,
                                     PageRequest pageReq,
                                     Sort<Voter> sort);

    /**
     * Invalid method. A method with a life cycle annotation must have exactly
     * 1 parameter.
     */
    @Save
    void storeNothing();

    /**
     * Invalid method. A method with a life cycle annotation must have exactly
     * 1 parameter, not multiple.
     */
    @Save
    Voter storeInDatabase(Voter voter, PageRequest pageReq);

    /**
     * This invalid method has a query that requires a single positional parameter,
     * but the method supplies 3 parameters.
     */
    @Query("WHERE LENGTH(address) > ?1 ORDER BY ssn ASC")
    List<Voter> withAddressLongerThan(int min1, int min2, int min3);

    /**
     * This invalid method has a query that requires a positional parameter,
     * but the method uses the Param annotation to defined a named parameter
     * instead.
     */
    @Query("WHERE LENGTH(address) < ?1 ORDER BY ssn ASC")
    List<Voter> withAddressShorterThan(@Param("maxLength") int maxAddressLength);

    /**
     * This invalid method places the Limit special parameter ahead of
     * the query parameter.
     */
    @Query("WHERE LENGTH(name) > :min ORDER BY ssn ASC")
    List<Voter> withNameLongerThan(Limit limit, @Param("min") int minLength);

    /**
     * This invalid method places the Sort special parameter ahead of
     * the query parameter.
     */
    @Query("WHERE LENGTH(name) < ?1")
    List<Voter> withNameShorterThan(Sort<Voter> sort, int maxLength);
}
