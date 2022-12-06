/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.Limit;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Page;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Sort;
import jakarta.data.repository.Streamable;
import jakarta.enterprise.concurrent.Asynchronous;

/**
 */
@Repository
public interface Primes {

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
    List<Prime> findByHexGreaterThanIgnoreCaseAndRomanNumeralLessThanEqualIgnoreCaseAndNumberLessThan(String hexAbove, String maxNumeral, long numBelow);

    List<Prime> findByNameBetweenIgnoreCaseAndNumberLessThanOrderByNumberDesc(String first, String last, long max);

    Prime findByNameIgnoreCase(String name);

    List<Prime> findByNameContainsIgnoreCaseAndNumberLessThanOrderByNumberDesc(String pattern, long max);

    List<Prime> findByNameLikeIgnoreCaseAndNumberLessThanOrderByNumberAsc(String pattern, long max);

    List<Prime> findByNameNotIgnoreCaseAndNumberLessThanOrderByNumberAsc(String name, long max);

    List<Prime> findByNameStartsWithIgnoreCaseAndNumberLessThanOrderByNumberAsc(String pattern, long max);

    Prime findByNumberBetween(long min, long max);

    @OrderBy("number")
    KeysetAwarePage<Prime> findByNumberBetween(long min, long max, Pageable pagination);

    List<Prime> findByNumberBetween(long min, long max, Sort... orderBy);

    KeysetAwarePage<Prime> findByNumberBetweenAndEvenFalse(long min, long max, Pageable pagination);

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

    Streamable<Prime> findByNumberLessThanEqualOrderByNumberAsc(long max, Pageable pagination);

    Streamable<Prime> findByNumberLessThanEqualOrderByNumberDesc(long max, Limit limit);

    Page<Prime> findByNumberLessThanEqualOrderByNumberDesc(long max, Pageable pagination);

    @Asynchronous
    CompletionStage<KeysetAwarePage<Prime>> findByNumberLessThanOrderByNumberDesc(long max, Pageable pagination);

    Stream<Prime> findFirst2147483648ByNumberGreaterThan(long min); // Exceeds Integer.MAX_VALUE by 1

    @OrderBy(value = "name", descending = true)
    Prime[] findFirst5ByNumberLessThanEqual(long maxNumber);

    Prime findFirstByNameLikeOrderByNumber(String namePattern);

    boolean existsByNumber(long number);

    Boolean existsNumberBetween(Long first, Long last);

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

    @Query("SELECT o.name, o.hex FROM Prime o WHERE o.number <= ?1")
    @OrderBy("number")
    Page<Object[]> namesWithHex(long maxNumber, Pageable pagination);

    @Query("SELECT DISTINCT LENGTH(p.romanNumeral) FROM Prime p WHERE p.number <= ?1 ORDER BY LENGTH(p.romanNumeral) DESC")
    Page<Integer> romanNumeralLengths(long maxNumber, Pageable pagination);

    void save(Prime... primes);
}
