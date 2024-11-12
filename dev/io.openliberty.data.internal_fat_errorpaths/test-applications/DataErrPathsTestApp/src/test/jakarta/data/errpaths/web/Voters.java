/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import java.time.Month;
import java.util.List;
import java.util.stream.Stream;

import jakarta.data.Sort;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;

/**
 * Repository with a valid entity.
 * Some methods are valid.
 * Others have errors, as indicated.
 */
@Repository(dataStore = "java:app/jdbc/DerbyDataSource")
public interface Voters extends BasicRepository<Voter, Integer> {

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
     * For testing an error where the method parameter allows multiple entities,
     * but the return type only allows one.
     */
    @Insert
    Voter register(Voter... v);

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
}
