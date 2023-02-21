/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import jakarta.data.repository.Compare;
import jakarta.data.repository.Count;
import jakarta.data.repository.Exists;
import jakarta.data.repository.Filter;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.Limit;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Page;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;
import jakarta.data.repository.Slice;
import jakarta.data.repository.Sort;
import jakarta.data.repository.Streamable;
import jakarta.enterprise.concurrent.Asynchronous;

/**
 */
@Repository
public interface Primes {
    @Query("SELECT (num.name) FROM Prime As num")
    Slice<String> all(Pageable pagination);

    @Exists
    @Filter(by = "binary", op = Compare.EndsWith, param = "bits")
    @Filter(by = "number", op = Compare.LessThan, param = "max")
    boolean anyLessThanEndingWithBitPattern(@Param("max") long upperLimit, @Param("bits") String pattern);

    long countByNumberLessThan(long number);

    @Asynchronous
    CompletableFuture<Short> countByNumberBetweenAndEvenNot(long first, long last, boolean isOdd);

    Integer countNumberBetween(long first, long last);

    @Query("SELECT p.number FROM Prime p WHERE p.number >= ?1 AND p.number <= ?2")
    long findAsLongBetween(long min, long max);

    @OrderBy("number")
    List<Prime> findByEvenFalseAndNumberLessThan(long max);

    List<Prime> findByEvenNotFalseAndNumberLessThan(long max);

    @OrderBy(value = "number", descending = true)
    List<Prime> findByEvenNotTrueAndNumberLessThan(long max);

    List<Prime> findByEvenTrueAndNumberLessThan(long max);

    @OrderBy(value = "romanNumeral", descending = true)
    List<Prime> findByHexIgnoreCaseGreaterThanAndRomanNumeralIgnoreCaseLessThanEqualAndNumberLessThan(String hexAbove, String maxNumeral, long numBelow);

    Prime findByNameIgnoreCase(String name);

    List<Prime> findByNameIgnoreCaseBetweenAndNumberLessThanOrderByNumberDesc(String first, String last, long max);

    List<Prime> findByNameIgnoreCaseContainsAndNumberLessThanOrderByNumberDesc(String pattern, long max);

    List<Prime> findByNameIgnoreCaseLikeAndNumberLessThanOrderByNumberAsc(String pattern, long max);

    List<Prime> findByNameIgnoreCaseNotAndNumberLessThanOrderByNumberAsc(String name, long max);

    List<Prime> findByNameIgnoreCaseStartsWithAndNumberLessThanOrderByNumberAsc(String pattern, long max);

    @OrderBy("even")
    @OrderBy("sumOfBits")
    @OrderBy("number")
    Iterator<Prime> findByNameStartsWithAndNumberLessThanOrNameContainsAndNumberLessThan(String prefix, long max1, String contains, long max2, Pageable pagination);

    Prime findByNumberBetween(long min, long max);

    @OrderBy("number")
    KeysetAwarePage<Prime> findByNumberBetween(long min, long max, Limit limit);

    @OrderBy("number")
    KeysetAwarePage<Prime> findByNumberBetween(long min, long max, Pageable pagination);

    List<Prime> findByNumberBetween(long min, long max, Sort... orderBy);

    KeysetAwarePage<Prime> findByNumberBetweenAndBinaryNotNull(long min, long max, Sort... orderBy); // Lacks Pageable

    KeysetAwareSlice<Prime> findByNumberBetweenAndEvenFalse(long min, long max, Pageable pagination);

    Page<Prime> findByNumberBetweenAndSumOfBitsNotNull(long min, long max, Pageable pagination);

    KeysetAwarePage<Prime> findByNumberBetweenOrderByEvenDescSumOfBitsDescNumberAsc(long min, long max, Pageable pagination);

    List<Prime> findByNumberBetweenOrderByNameIgnoreCaseDesc(long min, long max);

    @OrderBy("number")
    List<Prime> findByNumberInAndRomanNumeralEmpty(List<Long> nums);

    @OrderBy("number")
    List<Prime> findByNumberInAndRomanNumeralNotEmpty(List<Long> nums);

    @OrderBy("number")
    List<Prime> findByNumberInAndRomanNumeralNull(Iterable<Long> nums);

    @OrderBy("number")
    List<Prime> findByNumberInAndRomanNumeralNotNull(Set<Long> nums);

    @OrderBy("number")
    List<Prime> findByNumberInAndRomanNumeralSymbolsEmpty(Collection<Long> nums);

    @OrderBy("number")
    List<Prime> findByNumberInAndRomanNumeralSymbolsNotEmpty(Stack<Long> nums);

    Stream<Prime> findByNumberLessThan(long max);

    @OrderBy("even")
    @OrderBy("sumOfBits")
    Page<Prime> findByNumberLessThan(long max, Pageable pagination);

    Streamable<Prime> findByNumberLessThanEqualOrderByNumberAsc(long max, Pageable pagination);

    Streamable<Prime> findByNumberLessThanEqualOrderByNumberDesc(long max, Limit limit);

    Page<Prime> findByNumberLessThanEqualOrderByNumberDesc(long max, Pageable pagination);

    Stream<Prime> findByNumberLessThanOrderByEven(long max, Sort... sorts);

    KeysetAwareSlice<Prime> findByNumberLessThanOrderByEvenAscSumOfBitsAsc(long max, Pageable pagination);

    @Asynchronous
    CompletionStage<KeysetAwarePage<Prime>> findByNumberLessThanOrderByNumberDesc(long max, Pageable pagination);

    Iterator<Prime> findByNumberNotGreaterThan(long max, Pageable pagination);

    Iterator<Prime> findByNumberNotGreaterThan(long max, Sort... order);

    Slice<Prime> findByRomanNumeralEndsWithAndNumberLessThan(String ending, long max, Limit limit, Sort... orderBy);

    Slice<Prime> findByRomanNumeralEndsWithAndNumberLessThan(String ending, long max, Pageable pagination, Sort... orderBy);

    @OrderBy(value = "sumOfBits", descending = true)
    @OrderBy("name")
    Slice<Prime> findByRomanNumeralStartsWithAndNumberLessThan(String prefix, long max, Pageable pagination);

    Stream<Prime> findFirst2147483648ByNumberGreaterThan(long min); // Exceeds Integer.MAX_VALUE by 1

    @OrderBy(value = "name", descending = true)
    Prime[] findFirst5ByNumberLessThanEqual(long maxNumber);

    Prime findFirstByNameLikeOrderByNumber(String namePattern);

    boolean existsByNumber(long number);

    Boolean existsNumberBetween(Long first, Long last);

    @Count
    @Filter(by = "number", op = Compare.GreaterThanEqual)
    @Filter(by = "number", op = Compare.LessThanEqual)
    long howManyIn(long min, long max);

    @Count
    @Filter(by = "number", op = Compare.GreaterThan)
    @Filter(by = "number", op = Compare.LessThan, value = "20")
    Long howManyLessThan20StartingAfter(long min);

    @Filter(by = "number", op = Compare.Between)
    @Filter(by = "romanNumeral", ignoreCase = true, op = Compare.Like, value = "%v%")
    @Filter(by = "name", op = Compare.Contains)
    @OrderBy(value = "number", descending = true)
    @Select("number")
    List<Long> inRangeHavingVNumeralAndSubstringOfName(long min, long max, String nameSuffix);

    @Filter(by = "number", op = Compare.LessThan)
    @Filter(by = "name", op = Compare.EndsWith)
    @Filter(as = Filter.Type.OR, by = "number", op = Compare.Between)
    @Filter(by = "name", op = Compare.EndsWith)
    @OrderBy(value = "number", descending = true)
    Stream<Prime> lessThanWithSuffixOrBetweenWithSuffix(long numLessThan, String firstSuffix,
                                                        long lowerLimit, long upperLimit, String secondSuffix);

    @Query("SELECT MIN(o.number), MAX(o.number), SUM(o.number), COUNT(o.number), AVG(o.number) FROM Prime o WHERE o.number < ?1")
    Deque<Double> minMaxSumCountAverageDeque(long numBelow);

    @Query("SELECT MIN(o.number), MAX(o.number), SUM(o.number), COUNT(o.number), AVG(o.number) FROM Prime o WHERE o.number < ?1")
    float[] minMaxSumCountAverageFloat(long numBelow);

    @Query("SELECT MIN(o.number), MAX(o.number), SUM(o.number), COUNT(o.number), AVG(o.number) FROM Prime o WHERE o.number < ?1")
    int[] minMaxSumCountAverageInt(long numBelow);

    @Query("SELECT MIN(o.number), MAX(o.number), SUM(o.number), COUNT(o.number), AVG(o.number) FROM Prime o WHERE o.number < ?1")
    Iterable<Integer> minMaxSumCountAverageIterable(long numBelow);

    @Query("SELECT MIN(o.number), MAX(o.number), SUM(o.number), COUNT(o.number), AVG(o.number) FROM Prime o WHERE o.number < ?1")
    List<Long> minMaxSumCountAverageList(long numBelow);

    @Query("SELECT MIN(o.number), MAX(o.number), SUM(o.number), COUNT(o.number), AVG(o.number) FROM Prime o WHERE o.number < ?1")
    Long[] minMaxSumCountAverageLong(long numBelow);

    @Query("SELECT MIN(o.number), MAX(o.number), SUM(o.number), COUNT(o.number), AVG(o.number) FROM Prime o WHERE o.number < ?1")
    Number[] minMaxSumCountAverageNumber(long numBelow);

    @Query("SELECT MIN(o.number), MAX(o.number), SUM(o.number), COUNT(o.number), AVG(o.number) FROM Prime o WHERE o.number < ?1")
    Object[] minMaxSumCountAverageObject(long numBelow); // TODO List<Number>?, List<Object>?

    @Query("SELECT MIN(o.number), MAX(o.number), SUM(o.number), COUNT(o.number), AVG(o.number) FROM Prime o WHERE o.number < ?1")
    Stack<String> minMaxSumCountAverageStack(long numBelow);

    @Query(value = "SELECT NEW java.util.AbstractMap.SimpleImmutableEntry(p.number, p.name) FROM Prime p WHERE p.number <= ?1 ORDER BY p.name",
           count = "SELECT COUNT(p) FROM Prime p WHERE p.number <= ?1")
    Page<Map.Entry<Long, String>> namesByNumber(long maxNumber, Pageable pagination);

    @Query("SELECT prime.name, prime.hex FROM  Prime  prime  WHERE prime.number <= ?1")
    @OrderBy("number")
    Page<Object[]> namesWithHex(long maxNumber, Pageable pagination);

    @Filter(by = "number", op = Compare.NotBetween)
    @Filter(by = "number", op = Compare.LessThan)
    @OrderBy("number")
    @Select("number")
    List<Long> notWithinButBelow(int rangeMin, int rangeMax, int below);

    @Query("SELECT DISTINCT LENGTH(p.romanNumeral) FROM Prime p WHERE p.number <= ?1 ORDER BY LENGTH(p.romanNumeral) DESC")
    Page<Integer> romanNumeralLengths(long maxNumber, Pageable pagination);

    void save(Prime... primes);

    @Query("SELECT prime_ FROM Prime AS prime_ WHERE (prime_.number <= ?1)")
    @OrderBy(value = "even", descending = true)
    @OrderBy(value = "sumOfBits", descending = true)
    KeysetAwarePage<Prime> upTo(long maxNumber, Pageable pagination);
}
