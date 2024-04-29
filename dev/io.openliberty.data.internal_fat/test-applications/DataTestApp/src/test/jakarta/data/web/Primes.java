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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;
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
    @Query("SELECT (num.name) FROM Prime As num")
    Page<String> all(Sort<Prime> sort, PageRequest pagination);

    Integer countByNumberIdBetween(long first, long last);

    @Asynchronous
    CompletableFuture<Short> countByNumberIdBetweenAndEvenNot(long first, long last, boolean isOdd);

    long countByNumberIdLessThan(long number);

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

    @OrderBy("numberId")
    CursoredPage<Prime> findByNumberIdBetween(long min, long max, Limit limit);

    @OrderBy(ID)
    CursoredPage<Prime> findByNumberIdBetween(long min, long max, PageRequest pagination);

    List<Prime> findByNumberIdBetween(long min, long max, Sort<?>... orderBy);

    CursoredPage<Prime> findByNumberIdBetweenAndBinaryDigitsNotNull(long min, long max, Sort<?>... orderBy); // Lacks PageRequest

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

    @OrderBy(value = "sumOfBits", descending = true)
    @OrderBy("name")
    Page<Prime> findByRomanNumeralStartsWithAndNumberIdLessThan(String prefix, long max, PageRequest pagination);

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

    boolean existsByNumberId(long number);

    Boolean existsByNumberIdBetween(Long first, Long last);

    // TODO after JDQL SELECT is added: "Select name Where length(romanNumeral) * 2 >= length(name) Order By name Asc",
    @Query(value = "Where numberId < 50 and romanNumeral is not null and length(romanNumeral) * 2 >= length(name) Order By name Desc")
    Page<Prime> lengthBasedQuery(PageRequest pageRequest);

    @OrderBy(ID)
    @Query("SELECT ID(THIS) FROM Prime o WHERE (o.name = :numberName OR :numeral=o.romanNumeral OR o.hex =:hex OR ID(THIS)=:num)")
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

    @Query("SELECT o.numberId FROM Prime o WHERE (o.name = ?1 OR o.numberId=:num)")
    Collection<Long> matchAnyWithMixedUsageOfPositionalAndNamed(String name, long num);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), COUNT(o.numberId), AVG(o.numberId) FROM Prime o WHERE o.numberId < ?1")
    Deque<Double> minMaxSumCountAverageDeque(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), COUNT(o.numberId), AVG(o.numberId) FROM Prime o WHERE o.numberId < ?1")
    float[] minMaxSumCountAverageFloat(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), COUNT(o.numberId), AVG(o.numberId) FROM Prime o WHERE o.numberId < ?1")
    int[] minMaxSumCountAverageInt(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), COUNT(o.numberId), AVG(o.numberId) FROM Prime o WHERE o.numberId < ?1")
    Iterable<Integer> minMaxSumCountAverageIterable(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), COUNT(o.numberId), AVG(o.numberId) FROM Prime o WHERE o.numberId < ?1")
    List<Long> minMaxSumCountAverageList(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), COUNT(o.numberId), AVG(o.numberId) FROM Prime o WHERE o.numberId < ?1")
    Long[] minMaxSumCountAverageLong(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), COUNT(o.numberId), AVG(o.numberId) FROM Prime o WHERE o.numberId < ?1")
    Number[] minMaxSumCountAverageNumber(long numBelow);

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), COUNT(o.numberId), AVG(o.numberId) FROM Prime o WHERE o.numberId < ?1")
    Object[] minMaxSumCountAverageObject(long numBelow); // TODO List<Number>?, List<Object>?

    @Query("SELECT MIN(o.numberId), MAX(o.numberId), SUM(o.numberId), COUNT(o.numberId), AVG(o.numberId) FROM Prime o WHERE o.numberId < ?1")
    Stack<String> minMaxSumCountAverageStack(long numBelow);

    @Query("SELECT o.name FROM Prime o WHERE o.numberId < ?1")
    Page<String> namesBelow(long numBelow, Sort<Prime> sort, PageRequest pageRequest);

    @Query(value = "SELECT NEW java.util.AbstractMap.SimpleImmutableEntry(p.numberId, p.name) FROM Prime p WHERE p.numberId <= ?1 ORDER BY p.name")
    Page<Map.Entry<Long, String>> namesByNumber(long maxNumber, PageRequest pagination);

    @Query("SELECT prime.name, prime.hex FROM  Prime  prime  WHERE prime.numberId <= ?1")
    @OrderBy("numberId")
    Page<Object[]> namesWithHex(long maxNumber, PageRequest pagination);

    @Insert
    void persist(Prime... primes);

    @Query("SELECT DISTINCT LENGTH(p.romanNumeral) FROM Prime p WHERE p.numberId <= ?1 ORDER BY LENGTH(p.romanNumeral) DESC")
    Page<Integer> romanNumeralLengths(long maxNumber, PageRequest pagination);

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
    @Query(" WHERE( numberId<=:max AND UPPER(romanNumeral) NOT LIKE '%VII' AND (numberId-(numberId/10)* 10)<>3 AND\tnumberId\t>= :min)")
    CursoredPage<Prime> withinButNotEndingIn7or3(@Param("min") long minimum,
                                                 @Param("max") long maximum,
                                                 PageRequest pageRequest);

}
