/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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
package test.jakarta.data.web;

import static jakarta.data.repository.By.ID;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.enterprise.concurrent.Asynchronous;

/**
 * Repository with data that is pre-populated.
 * This should be treated as read-only to avoid interference between with tests.
 */
@Repository
public interface Primes {
    @Find
    CursoredPage<Prime> all(Order<Prime> sorts, PageRequest req);

    @Query("")
    CursoredPage<Prime> all(PageRequest req, Order<Prime> sorts);

    @Query("FROM Prime")
    CursoredPage<Prime> all(PageRequest req, Sort<?>... sorts);

    @Query("SELECT (num.name) FROM Prime As num")
    Page<String> all(Sort<Prime> sort, PageRequest pagination);

    @Query("SELECT ID(THIS) WHERE ID(THIS) < ?1 ORDER BY ID(THIS) DESC")
    List<Long> below(long exclusiveMax);

    @Query("SELECT binaryDigits WHERE numberId <= :max")
    @OrderBy(ID)
    LongStream binaryDigitsAsDecimal(long max);

    @Query("SELECT name WHERE numberId < 35 AND romanNumeral || name LIKE :pattern")
    List<String> concatAndMatch(String pattern, Sort<?> sort);

    BigDecimal countAsBigDecimalByNumberIdLessThan(long number);

    BigInteger countAsBigIntegerByNumberIdLessThan(long number);

    // boolean return type is not allowed for count methods
    boolean countAsBooleanByNumberIdLessThan(long number);

    int countAsIntByNumberIdLessThan(long number);

    Integer countAsIntegerByNumberIdBetween(long first, long last);

    long countAsLongByNumberIdLessThan(long number);

    Long countAsLongWrapperByNumberIdLessThan(long number);

    Number countAsNumberByNumberIdLessThan(long number);

    short countAsShortByNumberIdLessThan(long number);

    Short countAsShortWrapperByNumberIdLessThan(long number);

    // The β symbol is used here because Byte starts with the By keyword
    byte countAsβyteByNumberIdLessThan(long number);

    // The β symbol is used here because Byte starts with the By keyword
    Byte countAsβyteWrapperByNumberIdLessThan(long number);

    @Asynchronous
    CompletableFuture<Short> countByNumberIdBetweenAndEvenNot(long first, long last, boolean isOdd);

    @Asynchronous
    @Find
    CompletableFuture<Page<Long>> divisibleByTwo(boolean even,
                                                 PageRequest req,
                                                 Order<Prime> order);

    @Asynchronous
    CompletionStage<Boolean> existsByNameIgnoreCase(String name);

    boolean existsByNumberId(long number);

    Boolean existsByNumberIdBetween(Long first, Long last);

    @Asynchronous
    CompletableFuture<Boolean> existsByRomanNumeralIgnoreCase(String romanNumeral);

    @Find
    Stream<Prime> find(boolean even, int sumOfBits, Limit limit, Sort<?>... sorts);

    @Query("SELECT p.numberId FROM Prime p WHERE p.numberId >= ?1 AND p.numberId <= ?2")
    long findAsLongBetween(long min, long max);

    @Find
    Optional<Prime> findByBinary(@By("binaryDigits") String binary);

    @OrderBy(ID)
    List<Prime> findByEvenFalseAndNumberIdLessThan(long max);

    List<Prime> findByEvenNotFalseAndNumberIdLessThan(long max);

    @OrderBy(value = ID, descending = true)
    List<Prime> findByEvenNotTrueAndNumberIdLessThan(long max);

    List<Prime> findByEvenTrueAndNumberIdLessThan(long max);

    @OrderBy(value = "romanNumeral", descending = true)
    List<Prime> findByHexIgnoreCaseGreaterThanAndRomanNumeralIgnoreCaseLessThanEqualAndNumberIdLessThan(String hexAbove, String maxNumeral, long numBelow);

    @OrderBy("name")
    Stream<Prime> findByNameCharCountBetween(int minLength, int maxLength);

    Prime findByNameIgnoreCase(String name);

    List<Prime> findByNameIgnoreCaseBetweenAndNumberIdLessThanOrderByNumberIdDesc(String first, String last, long max);

    List<Prime> findByNameIgnoreCaseContainsAndNumberIdLessThanOrderByNumberIdDesc(String pattern, long max);

    List<Prime> findByNameIgnoreCaseLikeAndNumberIdLessThanOrderByNumberIdAsc(String pattern, long max);

    List<Prime> findByNameIgnoreCaseNotAndNumberIdLessThanOrderByNumberIdAsc(String name, long max);

    List<Prime> findByNameIgnoreCaseStartsWithAndNumberIdLessThanOrderByNumberIdAsc(String pattern, long max);

    @OrderBy("even")
    @OrderBy("sumOfBits")
    @OrderBy(ID)
    Iterator<Prime> findByNameStartsWithAndNumberIdLessThanOrNameContainsAndNumberIdLessThan(String prefix, long max1, String contains, long max2,
                                                                                             PageRequest pagination);

    List<Prime> findByNameTrimmedCharCountAndNumberIdBetween(int length, long min, long max);

    Optional<Prime> findByNameTrimmedIgnoreCase(String name);

    Prime findByNumberIdBetween(long min, long max);

    // Unsupported pattern: lacks PageRequest parameter.
    @OrderBy("numberId")
    CursoredPage<Prime> findByNumberIdBetween(long min, long max, Limit limit);

    @OrderBy(ID)
    CursoredPage<Prime> findByNumberIdBetween(long min, long max, PageRequest pagination);

    List<Prime> findByNumberIdBetween(long min, long max, Sort<?>... orderBy);

    // Unsupported pattern: lacks PageRequest parameter.
    CursoredPage<Prime> findByNumberIdBetweenAndBinaryDigitsNotNull(long min, long max, Sort<?>... orderBy);

    CursoredPage<Prime> findByNumberIdBetweenAndEvenFalse(long min, long max, PageRequest pagination, Order<Prime> order);

    Page<Prime> findByNumberIdBetweenAndSumOfBitsNotNull(long min, long max, Order<Prime> order, PageRequest pagination);

    CursoredPage<Prime> findByNumberIdBetweenOrderByEvenDescSumOfBitsDescNumberIdAsc(long min, long max, PageRequest pagination);

    List<Prime> findByNumberIdBetweenOrderByNameIgnoreCaseDesc(long min, long max);

    @OrderBy("numberId")
    List<Prime> findByNumberIdInAndRomanNumeralEmpty(List<Long> nums);

    @OrderBy("numberId")
    List<Prime> findByNumberIdInAndRomanNumeralNotEmpty(List<Long> nums);

    @OrderBy(ID)
    List<Prime> findByNumberIdInAndRomanNumeralNull(Iterable<Long> nums);

    @OrderBy(ID)
    List<Prime> findByNumberIdInAndRomanNumeralNotNull(Set<Long> nums);

    @OrderBy("numberId")
    List<Prime> findByNumberIdInAndRomanNumeralSymbolsEmpty(Collection<Long> nums);

    @OrderBy(ID)
    List<Prime> findByNumberIdInAndRomanNumeralSymbolsNotEmpty(Stack<Long> nums);

    Stream<Prime> findByNumberIdLessThan(long max);

    @OrderBy("even")
    @OrderBy("sumOfBits")
    Page<Prime> findByNumberIdLessThan(long max, Sort<Prime> sort, PageRequest pagination);

    List<Prime> findByNumberIdLessThanEqualOrderByNumberIdAsc(long max, PageRequest pagination);

    List<Prime> findByNumberIdLessThanEqualOrderByNumberIdDesc(long max, Limit limit);

    Page<Prime> findByNumberIdLessThanEqualOrderByNumberIdDesc(long max, PageRequest pagination);

    Stream<Prime> findByNumberIdLessThanOrderByEven(long max, Sort<?>... sorts);

    CursoredPage<Prime> findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(long max, PageRequest pagination, Sort<Prime> sort);

    @Asynchronous
    CompletionStage<CursoredPage<Prime>> findByNumberIdLessThanOrderByNumberIdDesc(long max, PageRequest pagination);

    Iterator<Prime> findByNumberIdNotGreaterThan(long max, Sort<?>... order);

    Page<Prime> findByRomanNumeralEndsWithAndNumberIdLessThan(String ending, long max, Limit limit, Sort<?>... orderBy);

    Page<Prime> findByRomanNumeralEndsWithAndNumberIdLessThan(String ending, long max,
                                                              PageRequest pagination,
                                                              Order<Prime> order,
                                                              Sort<?>... orderBy);

    CursoredPage<Prime> findByRomanNumeralIgnoreCaseEndsWith(String prefix,
                                                             PageRequest pageReq);

    Page<Prime> findByRomanNumeralIgnoreCaseStartsWith(String prefix,
                                                       PageRequest pageReq);

    @OrderBy(value = "sumOfBits", descending = true)
    @OrderBy("name")
    Page<Prime> findByRomanNumeralStartsWithAndNumberIdLessThan(String prefix, long max, PageRequest pagination);

    @OrderBy(ID)
    Stream<Prime> findByRomanNumeralSymbolsContainsAndNumberIdLessThan(String symbol,
                                                                       long max);

    @Find
    Prime findFirst(Sort<Prime> sort, Limit limitOf1);

    Stream<Prime> findFirst2147483648ByNumberIdGreaterThan(long min); // Exceeds Integer.MAX_VALUE by 1

    @OrderBy(value = "name", descending = true)
    Prime[] findFirst5ByNumberIdLessThanEqual(long maxNumber);

    Prime findFirstByNameLikeOrderByNumberId(String namePattern);

    @Find
    Optional<Prime> findHexadecimal(String hex);

    @Query("SELECT o.numberId, o.name FROM Prime o")
    List<Object[]> findNumberIdAndName(Sort<?>... sort);

    @OrderBy(value = ID, descending = true)
    Set<Long> findNumberIdByNumberIdBetween(long min, long max);

    @OrderBy(value = ID, descending = true)
    IntStream findSumOfBitsByNumberIdBetween(long min, long max);

    @Query(value = "Select name" +
                   " Where numberId < 50 and" +
                   "       romanNumeral is not null and" +
                   "       length(romanNumeral) * 2 >= length(name)" +
                   " Order By name Desc")
    Page<String> lengthBasedQuery(PageRequest pageRequest);

    @OrderBy(ID)
    @Query("SELECT ID(THIS)" +
           "  FROM Prime" +
           " WHERE (name = :numberName" +
           "     OR :numeral=romanNumeral" +
           "     OR hex =:hex" +
           "     OR ID(THIS)=:num)")
    long[] matchAny(long num, String numeral, String hex, String numberName);

    @OrderBy(ID)
    @Query("SELECT o.name FROM Prime o WHERE (o.name <> ':name' AND (o.numberId=?1 OR o.name=?2))")
    List<String> matchAnyExceptLiteralValueThatLooksLikeANamedParameter(long num, String name);

    @OrderBy("name")
    @Query("SELECT o.name FROM Prime o WHERE ((o.name=?1 OR o.numberId=?2) AND o.name <> ':name')")
    ArrayList<String> matchAnyExceptLiteralValueThatLooksLikeANamedParameter(String name, long num);

    @Query("SELECT o.numberId FROM Prime o WHERE (o.name = :numName OR o.romanNumeral=:numeral OR o.hex =:hexadecimal OR o.numberId=:num)")
    Stream<Long> matchAnyWithMixedUsageOfParamAnnotation(long num,
                                                         @Param("numName") String numberName,
                                                         String numeral,
                                                         @Param("hexadecimal") String hex);

    @Query("SELECT name WHERE numberId < 50 AND LEFT(name, LENGTH(:s)) = :s")
    @OrderBy("name")
    List<String> matchLeftSideOfName(@Param("s") String searchFor);

    @Query("SELECT name" +
           " WHERE numberId < 40" +
           "   AND RIGHT(name, LENGTH(:searchFor)) = :searchFor" +
           " ORDER BY name DESC")
    List<String> matchRightSideOfName(String searchFor);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), " +
           "       COUNT(o.numberId), AVG(o.numberId) " +
           "  FROM Prime o WHERE o.numberId < ?1")
    Deque<Double> minMaxSumCountAverageDeque(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId)," +
           "       COUNT(o.numberId), CAST(AVG(o.numberId) AS FLOAT)" +
           "  FROM Prime o WHERE o.numberId < ?1")
    float[] minMaxSumCountAverageFloat(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId)," +
           "       COUNT(o.numberId), CAST(AVG(o.numberId) AS INTEGER)" +
           "  FROM Prime o WHERE o.numberId < ?1")
    int[] minMaxSumCountAverageInt(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId)," +
           "       COUNT(o.numberId), CAST(AVG(o.numberId) AS INTEGER)" +
           "  FROM Prime o WHERE o.numberId < ?1")
    Iterable<Integer> minMaxSumCountAverageIterable(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId)," +
           "       COUNT(o.numberId), CAST(AVG(o.numberId) AS INTEGER)" +
           "  FROM Prime o WHERE o.numberId < ?1")
    List<Long> minMaxSumCountAverageList(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId)," +
           "       COUNT(o.numberId), CAST(AVG(o.numberId) AS INTEGER)" +
           "  FROM Prime o WHERE o.numberId < ?1")
    Long[] minMaxSumCountAverageLong(long numBelow);

    @Query("""
                    SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId),
                           COUNT(o.numberId), AVG(o.numberId)
                    FROM Prime o WHERE o.numberId < ?1
                    """)
    Number[] minMaxSumCountAverageNumberArray(long numBelow);

    @Query("""
                    SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId),
                           COUNT(o.numberId), AVG(o.numberId)
                    FROM Prime o WHERE o.numberId < ?1
                    """)
    List<Number> minMaxSumCountAverageNumberList(long numBelow);

    @Query("""
                    SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId),
                           COUNT(o.numberId), AVG(o.numberId)
                    FROM Prime o WHERE o.numberId < ?1
                    """)
    Object[] minMaxSumCountAverageObjectArray(long numBelow);

    @Query("""
                    SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId),
                           COUNT(o.numberId), AVG(o.numberId)
                    FROM Prime o WHERE o.numberId < ?1
                    """)
    List<Object> minMaxSumCountAverageObjectList(long numBelow);

    @Query("""
                    SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId),
                           COUNT(o.numberId), AVG(o.numberId)
                    FROM Prime o WHERE o.numberId < ?1
                    """)
    Stack<String> minMaxSumCountAverageStack(long numBelow);

    @Query("SELECT o.name FROM Prime o WHERE o.numberId < ?1")
    Page<String> namesBelow(long numBelow, Sort<Prime> sort, PageRequest pageRequest);

    @Query(value = "SELECT NEW java.util.AbstractMap.SimpleImmutableEntry(p.numberId, p.name) FROM Prime p WHERE p.numberId <= ?1 ORDER BY p.name")
    Page<Map.Entry<Long, String>> namesByNumber(long maxNumber, PageRequest pagination);

    @Query("SELECT prime.name, prime.hex FROM  Prime  prime  WHERE prime.numberId <= ?1")
    @OrderBy("numberId")
    Page<Object[]> namesWithHex(long maxNumber, PageRequest pagination);

    @Query("SELECT numberId WHERE numberId=?1")
    BigDecimal numberAsBigDecimal(long num);

    @Query("SELECT numberId WHERE numberId=:num")
    Optional<BigInteger> numberAsBigInteger(long num);

    @Query("SELECT numberId WHERE numberId=?1")
    byte numberAsByte(long num);

    @Query("SELECT numberId WHERE numberId=:n")
    Optional<Byte> numberAsByteWrapper(@Param("n") long num);

    @Query("SELECT numberId WHERE ID(this)=?1")
    double numberAsDouble(long num);

    @Asynchronous
    @Query("SELECT numberId WHERE id(THIS)=?1")
    CompletableFuture<Optional<Float>> numberAsFloatWrapper(long num);

    @Query("SELECT numberId WHERE Id(This)=?1")
    int numberAsInt(long num);

    @Query("SELECT numberId WHERE Id(This)=:num")
    Optional<Integer> numberAsInteger(long num);

    @Query("SELECT numberId WHERE id(this)=?1")
    long numberAsLong(long num);

    @Query("SELECT numberId WHERE id(this)=:num")
    Optional<Long> numberAsLongWrapper(long num);

    @Query("SELECT numberId WHERE ID(THIS)=?1")
    short numberAsShort(long num);

    @Query("SELECT numberId WHERE ID(THIS)=:num")
    Optional<Short> numberAsShortWrapper(long num);

    // discouraged usage, but testing what happens
    @Query("SELECT COUNT(THIS) WHERE ID(THIS) < :max")
    Page<Long> pageOfCountUpTo(long max, PageRequest pageReq);

    // discouraged usage, but testing what happens
    @Query("SELECT CASE WHEN COUNT(THIS) > 0 THEN TRUE ELSE FALSE END")
    Page<Boolean> pageOfExists(PageRequest pageReq);

    @Insert
    void persist(Prime... primes);

    @Query("SELECT DISTINCT(romanNumeral)" +
           " WHERE (numberId BETWEEN ?1 AND ?2)" +
           "    OR (numberId BETWEEN ?3 AND ?4)" +
           " ORDER BY numberId ASC")
    Page<String> romanNumeralsDistinct(long min1, long max1,
                                       long min2, long max2,
                                       PageRequest pageRequest);

    @Query("SELECT DISTINCT LENGTH(p.romanNumeral) FROM Prime p WHERE p.numberId <= ?1 ORDER BY LENGTH(p.romanNumeral) DESC")
    Page<Integer> romanNumeralLengths(long maxNumber, PageRequest pagination);

    @Query("WHERE numberId <= ?1")
    @OrderBy("name")
    List<RomanNumeral> romanNumeralsLessThanEq(long max);

    @Query("SELECT romanNumeral" +
           " WHERE (numberId BETWEEN ?1 AND ?2)" +
           "    OR (numberId BETWEEN ?3 AND ?4)" +
           " ORDER BY numberId ASC")
    Page<String> romanNumeralsWithin(long min1, long max1,
                                     long min2, long max2,
                                     PageRequest pageRequest);

    @Query("SELECT romanNumeralSymbols WHERE numberId = ?1")
    Optional<ArrayList<String>> romanNumeralSymbolsAsArrayList(long num);

    @Query("SELECT romanNumeralSymbols WHERE numberId = ?1")
    Optional<Collection<String>> romanNumeralSymbolsAsCollection(long num);

    @Query("SELECT romanNumeralSymbols WHERE romanNumeral LIKE ?1")
    @OrderBy(ID)
    List<ArrayList<String>> romanNumeralSymbolsAsListOfArrayList(String pattern);

    @Query("SELECT romanNumeralSymbols WHERE romanNumeral LIKE ?1")
    @OrderBy(ID)
    LinkedHashSet<ArrayList<String>> romanNumeralSymbolsAsSetOfArrayList(String pattern);

    @Query("SELECT hex WHERE numberId=:id")
    Optional<Character> singleHexDigit(long id);

    @Query("SELECT hex WHERE numberId=?1")
    Optional<String> toHexadecimal(long num);

    @Query("SELECT prime_ FROM Prime AS prime_ WHERE (prime_.numberId <= ?1)")
    @OrderBy(value = "even", descending = true)
    @OrderBy(value = "sumOfBits", descending = true)
    CursoredPage<Prime> upTo(long maxNumber, PageRequest pagination, Order<Prime> order);

    @Query("where numberId <= ?2 and numberId>=?1")
    Page<Prime> within(long minimum, long maximum, PageRequest pageRequest, Sort<Prime> sort);

    @Query("where (numberId <= :maximum) and numberId>=10 order by name asc")
    Page<Prime> within10toXAndSortedByName(long maximum, PageRequest pageRequest);

    @OrderBy(value = "even", descending = true)
    @OrderBy(value = "name", descending = false)
    // Query used to contain (numberId-(numberId/10)* 10)<>3
    // Long values in oracle are stored as a NUMBER which includes a
    // floating point, therefore integer based arithmetic is not supported.
    @Query(" WHERE( numberId<=:max AND UPPER(romanNumeral) NOT LIKE '%VII' AND MOD(numberId,10)<>3 AND\tnumberId\t>= :min)")
    CursoredPage<Prime> withinButNotEndingIn7or3(@Param("min") long minimum,
                                                 @Param("max") long maximum,
                                                 PageRequest pageRequest);

}
