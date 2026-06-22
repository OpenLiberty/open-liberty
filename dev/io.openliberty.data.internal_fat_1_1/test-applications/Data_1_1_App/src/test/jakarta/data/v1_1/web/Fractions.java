/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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
package test.jakarta.data.v1_1.web;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.Between;
import jakarta.data.constraint.EqualTo;
import jakarta.data.constraint.In;
import jakarta.data.constraint.LessThan;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotBetween;
import jakarta.data.constraint.NotEqualTo;
import jakarta.data.constraint.NotNull;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.First;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Is;
import jakarta.data.repository.JakartaQuery; // TODO replace with Persistence 4.0 anno once available
import jakarta.data.repository.NativeQuery; // TODO replace with Persistence 4.0 anno once available
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.QueryOptions; // TODO replace with Persistence 4.0 anno once available
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;
import jakarta.data.restrict.Restriction;
import jakarta.persistence.LockModeType;

/**
 * Repository for the Fraction entity
 */
@Repository(dataStore = "MyDataStore")
public interface Fractions {

    @NativeQuery("""
                    SELECT name
                      FROM Fraction
                     WHERE reduced = :isReduced
                     ORDER BY name ASC
                    """)
    List<String> alphabetized(@Param("isReduced") boolean reduced,
                              Limit limit);

    @Find
    @First(10)
    @Select(_Fraction.NUMERATOR)
    List<Integer> atMost10Numerators(int denominator,
                                     Restriction<Fraction> filter,
                                     Order<Fraction> sortBy);

    @NativeQuery("""
                    UPDATE Fraction
                       SET ceiling = ?,
                           truncated = ?
                     WHERE numerator = ?
                       AND denominator = ?
                    """)
    @QueryOptions(timeout = 12000) // query timeout = 12 seconds
    boolean change(BigDecimal ceiling,
                   BigDecimal truncated,
                   int numerator,
                   int denominator);

    Connection connect();

    Long count(Restriction<Fraction> filter);

    long countByDenominatorBetween(int min,
                                   int max,
                                   Restriction<Fraction> filter);

    @NativeQuery("""
                    INSERT INTO Fraction
                         (numerator, denominator, name, reduced, VAL, inverse,
                          ceiling, truncated, type, nonrepeating, repeating )
                         values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)
    void create(int numerator,
                int denominator,
                String name,
                boolean reduced,
                double value,
                double inverse,
                BigDecimal ceiling,
                BigDecimal truncated,
                int decimalType, // ordinal value of Fraction.Decimal.Type enum
                String nonrepeating,
                String repeating);

    int deleteByNameStartsWith(String prefix,
                               Restriction<Fraction> filter);

    @Find
    Stream<Fraction> denominatoredUpTo //
    (@By(_Fraction.DENOMINATOR) NotNull<Integer> notNull,
     @By(_Fraction.DENOMINATOR) AtMost<Integer> max,
     Sort<?>... sorts);

    @NativeQuery("""
                    DELETE FROM Fraction
                     WHERE numerator = ?
                       AND name LIKE CONCAT('% ', ?)
                    """)
    long destroy(int numerator,
                 String denominatorName);

    @Delete
    long discard(@By("denominator") AtLeast<Integer> minDenominator,
                 @By("denominator") AtMost<Integer> maxDenominator,
                 Restriction<Fraction> filter);

    boolean exists(Restriction<Fraction> filter);

    Boolean existsByDenominatorGreaterThanAndDenominatorLessThan//
    (int exclusiveMin,
     int exclusiveMax,
     Restriction<Fraction> filter);

    @First
    @NativeQuery("""
                    SELECT *
                      FROM Fraction
                     WHERE val >= ? AND val <= ?
                     ORDER BY val
                    """)
    Optional<Fraction> firstValueWithin(double minValue, double maxValue);

    @Query("WHERE denominator = ?1 AND numerator < denominator")
    @First
    @OrderBy(value = _Fraction.NUMERATOR, descending = true)
    Optional<Fraction> greatestLessThan1(int denominator);

    @Find
    @OrderBy(_Fraction.NUMERATOR)
    @OrderBy(_Fraction.DENOMINATOR)
    Stream<Fraction> havingDenominatorWithin//
    (@By(_Fraction.DENOMINATOR) @Is(AtLeast.class) long min,
     @By(_Fraction.DENOMINATOR) @Is(AtMost.class) long max);

    @NativeQuery("""
                    SELECT *
                      FROM Fraction
                     WHERE reduced = :reduced
                       AND numerator = :numerator
                       AND denominator = :denominator
                    """)
    Optional<Fraction> ifReduced(boolean reduced,
                                 int numerator,
                                 int denominator);

    @Find
    @Select(_Fraction.NAME)
    List<String> named(@By(_Fraction.NAME) Like pattern,
                       Order<Fraction> order,
                       Limit limit);

    @Find
    @OrderBy(_Fraction.DENOMINATOR)
    CursoredPage<Fraction> namedLike //
    (@By(_Fraction.NAME) @Is(Like.class) String pattern,
     Order<Fraction> additionalSorting,
     PageRequest pageReq);

    @NativeQuery("""
                    SELECT *
                      FROM Fraction
                     WHERE numerator <= CAST(FLOOR(SQRT(denominator)) AS INT)
                     ORDER BY denominator, numerator
                    """)
    List<Fraction> numeratorLTESquareRootOfDenominator(Limit limit);

    @NativeQuery("""
                    SELECT COUNT(*)
                      FROM Fraction
                     WHERE denominator = ? AND reduced = ?
                    """)
    long numReducedWithDenominatorOf(int denominator,
                                     boolean isReduced);

    @Find
    @QueryOptions(entityGraph = "EagerlyLoadRoundedValues")
    Optional<Fraction> of(int numerator, int denominator);

    @Query("SELECT numerator, denominator - numerator" +
           " ORDER BY denominator - numerator DESC, numerator ASC")
    Page<Ratio> pageOfRatios(PageRequest pageReq);

    @NativeQuery("""
                    SELECT numerator, denominator - numerator
                      FROM Fraction
                     WHERE denominator = ?
                     ORDER BY numerator
                    """)
    // TODO Java record results?
    default Ratio[] ratioArrayWithDenominator(int sum) {
        return new Ratio[0];
    }

    @NativeQuery("""
                    SELECT numerator, denominator
                      FROM Fraction
                     WHERE ABS(numerator - denominator) = ?
                     ORDER BY numerator
                    """)
    // TODO Java record results?
    default List<Ratio> ratioListWithDifferenceOfTerms(int difference) {
        return List.of();
    }

    @NativeQuery("""
                    SELECT numerator, denominator
                      FROM Fraction
                     WHERE numerator + denominator = ?
                     ORDER BY numerator
                    """)
    // TODO Java record results?
    default Stream<Ratio> ratioStreamWithSumOfTerms(int sum) {
        return Stream.of();
    }

    @Delete
    List<Fraction> remove(Like name,
                          Restriction<Fraction> filter);

    @Find
    @Select(_Fraction.DECIMAL_CEILING)
    Optional<BigDecimal> roundedUp(@By(_Fraction.NUMERATOR) int numerator,
                                   @By(_Fraction.DENOMINATOR) int denominator);

    /**
     * This is a workaround for Derby, which ignores query timeout
     * and eventually the lock timeout (default 60s) applies instead.
     * Tests can use this method to set the lock timeout to the desired
     * query timeout value to make a query that involves a lock appear
     * to time out as expected if the query timeout were honored.
     */
    default void setLockTimeout(int seconds) throws SQLException {
        String sql = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(?, ?)";
        try (Connection con = connect()) {
            CallableStatement cs = con.prepareCall(sql);
            cs.setString(1, "derby.locks.waitTimeout");
            cs.setInt(2, seconds);
            cs.execute();
        }
    }

    @Query("SELECT NEW test.jakarta.data.v1_1.web.Ratio(" +
           "\t\tnumerator, denominator - numerator)" +
           "\tWHERE numerator=?1 AND denominator=?2")
    Optional<Ratio> singleRatio(int numerator, int denominator);

    @JakartaQuery("""
                     FROM Fraction f
                    WHERE SQRT(f.decimal.value) BETWEEN ?1 AND ?2
                    """)
    List<Fraction> squareRootBetween(double min,
                                     double max,
                                     Restriction<Fraction> filter,
                                     Order<Fraction> order);

    @Query("SELECT numerator, denominator - numerator")
    Stream<Ratio> streamOfRatios();

    @Insert
    void supply(Collection<Fraction> list);

    @Find
    @OrderBy(_Fraction.DENOMINATOR)
    @OrderBy(value = _Fraction.NUMERATOR, descending = true)
    Stream<Fraction> where(Restriction<Fraction> filter);

    @Find
    @OrderBy(_Fraction.DENOMINATOR)
    @OrderBy(_Fraction.NUMERATOR)
    Stream<Fraction> withDenominatorBetweenNamedBeforeAndNumeratorNotBetween //
    (@By(_Fraction.DENOMINATOR) Between<Integer> denominatorRange,
     @By(_Fraction.NAME) @Is(LessThan.class) String exclusiveMaxName,
     @By(_Fraction.NUMERATOR) NotBetween<Integer> excludedNumerators,
     @By(_Fraction.REDUCED) @Is boolean reduced);

    @Find
    Stream<Fraction> withDenominatorButNotNumerator //
    (@By(_Fraction.DENOMINATOR) @Is(EqualTo.class) long denominator,
     @By(_Fraction.NUMERATOR) @Is(NotEqualTo.class) long excludeNumerator,
     Order<Fraction> order);

    @Find
    Stream<Fraction> withNameLike //
    (@By(_Fraction.NAME) @Is(Like.class) String pattern,
     Restriction<Fraction> filter,
     Order<Fraction> order);

    @NativeQuery("""
                    SELECT *
                      FROM Fraction
                     WHERE name LIKE CONCAT('%', :nameSuffix)
                       AND name LIKE CONCAT(:namePrefix, '%')
                     ORDER BY name
                    """)
    Fraction[] withNamePattern(@Param("namePrefix") String prefix,
                               @Param("nameSuffix") String suffix);

    @Find
    @Select(_Fraction.NAME)
    List<String> withNumeratorsAndDenominator //
    (@By(_Fraction.NUMERATOR) In<Integer> numerators,
     @Is int denominator,
     Sort<Fraction> sort);

    @JakartaQuery("WHERE name = :name")
    @QueryOptions(lockMode = LockModeType.PESSIMISTIC_WRITE,
                  timeout = 10000) // query timeout = 10 seconds
    Optional<Fraction> withWriteLock(String name);

}
