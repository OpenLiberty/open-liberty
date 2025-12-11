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
import java.util.Set;
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
     * This invalid method attempts to return a long count result as Page of Long.
     */
    Page<Long> countByBirthday(LocalDate birthday);

    /**
     * Invalid method. A method with a life cycle annotation must have exactly
     * 1 parameter.
     */
    @Update
    void changeNothing();

    /**
     * Boolean return type is not allowed for count methods.
     */
    boolean countAsBooleanBySSNLessThan(long ssnBelow);

    /**
     * Fails if more than one match.
     */
    Optional<Voter> deleteByNameStartsWith(String namePrefix);

    /**
     * 'first' should be ignored and this should delete all entities
     * or cause failure if when the result would be non-unique
     */
    Optional<Voter> deleteFirst();

    /**
     * Invalid return type Boolean is not the entity or Id.
     */
    Page<Boolean> deleteReturnBooleanByAddress(String address,
                                               Limit limit,
                                               Sort<Voter> sort);

    /**
     * Invalid return type char is not the entity or id.
     */
    char[] deleteReturnCharByAddress(String address,
                                     Limit limit,
                                     Sort<Voter> sort);

    /**
     * Invalid return type String is not the entity or Id.
     */
    List<String> deleteReturnStringByAddress(String address,
                                             Limit limit,
                                             Sort<Voter> sort);

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
     * This invalid method attempts to return a true/false exist result as int.
     */
    int existsByAddress(String homeAddress);

    /**
     * This invalid method attempts to return a true/false exists result as Page
     * of Boolean.
     */
    Page<Boolean> existsByBirthday(LocalDate birthday, PageRequest pageReq);

    /**
     * This invalid method attempts to return a true/false exist result as a
     * Long value within a CompletableFuture. The CompletableFuture is fine, but
     * Long does not match the true/false result type.
     */
    CompletableFuture<Long> existsByName(String name);

    /**
     * This invalid method omits the entity attribute name from OrderBy in
     * the method name.
     */
    List<Voter> findByAddressContainsOrderByAsc(String addressSubstring);

    /**
     * This invalid method attempts to combine the IgnoreCase and In keywords.
     */
    List<Voter> findByAddressIgnoreCaseIn(Set<String> addresses);

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
     * This invalid method tries to apply the GreaterThanEqual keyword to a
     * collection of values.
     */
    List<Voter> findByEmailAddressesGreaterThanEqual(int minEmailAddresses);

    /**
     * This invalid method tries to apply the IgnoreCase keyword to a collection.
     */
    List<Voter> findByEmailAddressesIgnoreCaseContains(String email);

    /**
     * This invalid method omits the entity attribute name from findBy in the
     * method name.
     */
    List<Voter> findByIgnoreCaseContains(String address);

    /**
     * This invalid method name contains an entity attribute name "Description"
     * that contains a reserved keyword, "Desc".
     */
    List<Voter> findByNameNotNullOrderByDescriptionAsc();

    /**
     * Unsupported pattern: lacks PageRequest parameter.
     */
    @OrderBy("ssn")
    CursoredPage<Voter> findBySsnBetweenAndAddressNotNull(int min,
                                                          int max,
                                                          Limit limit);

    /**
     * Unsupported pattern: lacks PageRequest parameter.
     */
    @OrderBy("ssn")
    CursoredPage<Voter> findBySsnBetweenAndBirthdayNotNull(int min,
                                                           int max,
                                                           Sort<?>... orderBy);

    /**
     * Only valid when the range has exactly 1 result.
     */
    Voter findBySSNBetweenAndNameNotNull(long min, long max);

    List<Voter> findBySsnLessThanEqualOrderBySsnDesc(int max, Limit limit);

    /**
     * This invalid method has both a First keyword and a Limit parameter.
     */
    @OrderBy("ssn")
    Voter[] findFirst2(Limit limit);

    /**
     * This invalid method attempts to retrieve a number of results that
     * exceeds Integer.MAX_VALUE by 1
     */
    Stream<Voter> findFirst2147483648BySsnGreaterThan(int min);

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
     * This method is invalid for all names with more than 1 character.
     */
    @Query("SELECT name WHERE ssn=?1")
    Optional<Character> firstLetterOfName(int ssn);

    /**
     * Only valid when the range has exactly 1 result.
     */
    @Query("SELECT v.ssn FROM Voter v WHERE v.ssn >= ?1 AND v.ssn <= ?2")
    long findSSNAsLongBetween(long min, long max);

    /**
     * This invalid method includes GROUP BY in a query that is used for
     * cursor-based pagination.
     */
    @Query("FROM Voter v GROUP BY v.address")
    @OrderBy("ssn")
    CursoredPage<Voter> groupedByAddress(PageRequest pageReq);

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
     * This invalid method has a By annotation where the value is an empty string
     * instead of a valid entity attribute name.
     */
    @Find
    List<Voter> inPrecinct(@By("") int precinct);

    /**
     * This invalid method has an OrderBy annotation where the value is an
     * empty string instead of a valid entity attribute name.
     */
    @Query("WHERE address LIKE CONCAT('%;TOWNSHIP:', ?1, ';%')")
    @OrderBy("address")
    @OrderBy("")
    @OrderBy("ssn")
    List<Voter> inTownship(String name);

    /**
     * This invalid method has a Param annotation where the value is an empty string
     * instead of a valid named parameter name.
     */
    @Query("WHERE address IS NOT NULL")
    List<Voter> inWard(@Param("") int ward);

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
     * This invalid method returns values that cannot all be converted to float.
     */
    @Query("""
                    SELECT MIN(o.ssn), MAX(o.ssn), SUM(o.ssn),
                           COUNT(o.ssn), CAST(AVG(o.ssn) AS FLOAT)
                      FROM Voter o WHERE o.ssn < ?1
                    """)
    float[] minMaxSumCountAverageFloat(long numBelow);

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
     * Invalid method. A function that does not exist cannot be used within the
     * sort criteria.
     */
    @Find
    @OrderBy("last5DigitsOf(address)")
    List<Voter> sortedByEndOfAddress();

    /**
     * Invalid method. The entity has no zipcode attribute upon which to sort.
     */
    @Find
    @OrderBy("birthday")
    @OrderBy("zipcode")
    List<Voter> sortedByZipCode();

    /**
     * Invalid method for 9 digit SSN.
     */
    @Query("SELECT ssn WHERE ssn=?1")
    byte ssnAsByte(long ssn);

    /**
     * Invalid method for 9 digit SSN.
     */
    @Query("SELECT ssn WHERE ssn=:s")
    Optional<Byte> ssnAsByteWrapper(@Param("s") long ssn);

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
     * This invalid method includes UNION in a query that is used for
     * cursor-based pagination.
     */
    @Query("""
                    SELECT v1 FROM Voter v1 WHERE v1.address = ?1
                     UNION
                    SELECT v2 FROM Voter v2 WHERE v2.address = ?2""")
    @OrderBy("ssn")
    CursoredPage<Voter> unionOfAddresses(String address1,
                                         String address2,
                                         PageRequest pageReq);

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
     * This invalid method includes INTERSECT in a query that is used for
     * cursor-based pagination.
     */
    @Query("""
                    SELECT v FROM Voter v WHERE v.name = ?1
                     INTERSECT
                    SELECT v FROM Voter v WHERE v.address = ?2""")
    @OrderBy("ssn")
    CursoredPage<Voter> withNameAndAddress(String name,
                                           String address,
                                           PageRequest pageReq);

    /**
     * This invalid method includes EXCEPT in a query that is used for
     * cursor-based pagination.
     */
    @Query("""
                    SELECT this FROM Voter WHERE name = ?1
                     EXCEPT
                    SELECT this FROM Voter WHERE address = ?2""")
    @OrderBy("ssn")
    CursoredPage<Voter> withNameNotAddress(String name,
                                           String address,
                                           PageRequest pageReq);

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
