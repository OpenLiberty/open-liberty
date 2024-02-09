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
import jakarta.data.Sort;
import jakarta.data.Streamable;
import jakarta.data.page.KeysetAwarePage;
import jakarta.data.page.KeysetAwareSlice;
import jakarta.data.page.Page;
import jakarta.data.page.Pageable;
import jakarta.data.page.Slice;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.enterprise.concurrent.Asynchronous;

import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Or;
import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.comparison.Contains;
import io.openliberty.data.repository.comparison.EndsWith;
import io.openliberty.data.repository.comparison.GreaterThan;
import io.openliberty.data.repository.comparison.GreaterThanEqual;
import io.openliberty.data.repository.comparison.LessThan;
import io.openliberty.data.repository.comparison.LessThanEqual;
import io.openliberty.data.repository.comparison.Like;
import io.openliberty.data.repository.function.CharCount;
import io.openliberty.data.repository.function.IgnoreCase;
import io.openliberty.data.repository.function.Not;
import io.openliberty.data.repository.function.Trimmed;

/**
 * Repository with data that is pre-populated.
 * This should be treated as read-only to avoid interference between with tests.
 */
@Repository
public interface Primes {
    @Query("SELECT (num.name) FROM Prime As num")
    Slice<String> all(Pageable<?> pagination);

    @Exists
    boolean anyLessThanEndingWithBitPattern(@By("numberId") @LessThan long upperLimit,
                                            @By("binaryDigits") @EndsWith String pattern);

    long countByIdLessThan(long number);

    @Asynchronous
    CompletableFuture<Short> countByIdBetweenAndEvenNot(long first, long last, boolean isOdd);

    Integer countByNumberIdBetween(long first, long last);

    @Find
    Stream<Prime> find(boolean even, int sumOfBits, Limit limit, Sort<?>... sorts);

    @Query("SELECT p.numberId FROM Prime p WHERE p.numberId >= ?1 AND p.numberId <= ?2")
    long findAsLongBetween(long min, long max);

    @Find
    Optional<Prime> findByBinary(@By("binaryDigits") String binary);

    @OrderBy("id")
    List<Prime> findByEvenFalseAndIdLessThan(long max);

    List<Prime> findByEvenNotFalseAndIdLessThan(long max);

    @OrderBy(value = "id", descending = true)
    List<Prime> findByEvenNotTrueAndIdLessThan(long max);

    List<Prime> findByEvenTrueAndIdLessThan(long max);

    @OrderBy(value = "romanNumeral", descending = true)
    List<Prime> findByHexIgnoreCaseGreaterThanAndRomanNumeralIgnoreCaseLessThanEqualAndIdLessThan(String hexAbove, String maxNumeral, long numBelow);

    @OrderBy("name")
    Stream<Prime> findByNameCharCountBetween(int minLength, int maxLength);

    Prime findByNameIgnoreCase(String name);

    List<Prime> findByNameIgnoreCaseBetweenAndIdLessThanOrderByIdDesc(String first, String last, long max);

    List<Prime> findByNameIgnoreCaseContainsAndIdLessThanOrderByIdDesc(String pattern, long max);

    List<Prime> findByNameIgnoreCaseLikeAndIdLessThanOrderByIdAsc(String pattern, long max);

    List<Prime> findByNameIgnoreCaseNotAndIdLessThanOrderByIdAsc(String name, long max);

    List<Prime> findByNameIgnoreCaseStartsWithAndIdLessThanOrderByIdAsc(String pattern, long max);

    @OrderBy("even")
    @OrderBy("sumOfBits")
    @OrderBy("id")
    Iterator<Prime> findByNameStartsWithAndIdLessThanOrNameContainsAndIdLessThan(String prefix, long max1, String contains, long max2,
                                                                                 Pageable<?> pagination);

    List<Prime> findByNameTrimmedCharCountAndIdBetween(int length, long min, long max);

    Optional<Prime> findByNameTrimmedIgnoreCase(String name);

    Prime findByNumberIdBetween(long min, long max);

    @OrderBy("numberId")
    KeysetAwarePage<Prime> findByNumberIdBetween(long min, long max, Limit limit);

    @OrderBy("id")
    KeysetAwarePage<Prime> findByNumberIdBetween(long min, long max, Pageable<?> pagination);

    List<Prime> findByNumberIdBetween(long min, long max, Sort<?>... orderBy);

    KeysetAwarePage<Prime> findByNumberIdBetweenAndBinaryDigitsNotNull(long min, long max, Sort<?>... orderBy); // Lacks Pageable

    KeysetAwareSlice<Prime> findByNumberIdBetweenAndEvenFalse(long min, long max, Pageable<?> pagination);

    Page<Prime> findByNumberIdBetweenAndSumOfBitsNotNull(long min, long max, Pageable<?> pagination);

    KeysetAwarePage<Prime> findByNumberIdBetweenOrderByEvenDescSumOfBitsDescIdAsc(long min, long max, Pageable<?> pagination);

    List<Prime> findByNumberIdBetweenOrderByNameIgnoreCaseDesc(long min, long max);

    @OrderBy("numberId")
    List<Prime> findByNumberIdInAndRomanNumeralEmpty(List<Long> nums);

    @OrderBy("numberId")
    List<Prime> findByNumberIdInAndRomanNumeralNotEmpty(List<Long> nums);

    @OrderBy("id")
    List<Prime> findByNumberIdInAndRomanNumeralNull(Iterable<Long> nums);

    @OrderBy("id")
    List<Prime> findByNumberIdInAndRomanNumeralNotNull(Set<Long> nums);

    @OrderBy("numberId")
    List<Prime> findByNumberIdInAndRomanNumeralSymbolsEmpty(Collection<Long> nums);

    @OrderBy("id")
    List<Prime> findByNumberIdInAndRomanNumeralSymbolsNotEmpty(Stack<Long> nums);

    Stream<Prime> findByNumberIdLessThan(long max);

    @OrderBy("even")
    @OrderBy("sumOfBits")
    Page<Prime> findByNumberIdLessThan(long max, Pageable<?> pagination);

    Streamable<Prime> findByNumberIdLessThanEqualOrderByIdAsc(long max, Pageable<?> pagination);

    Streamable<Prime> findByNumberIdLessThanEqualOrderByIdDesc(long max, Limit limit);

    Page<Prime> findByNumberIdLessThanEqualOrderByNumberIdDesc(long max, Pageable<?> pagination);

    Stream<Prime> findByNumberIdLessThanOrderByEven(long max, Sort<?>... sorts);

    KeysetAwareSlice<Prime> findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(long max, Pageable<?> pagination);

    @Asynchronous
    CompletionStage<KeysetAwarePage<Prime>> findByNumberIdLessThanOrderByIdDesc(long max, Pageable<?> pagination);

    Iterator<Prime> findByNumberIdNotGreaterThan(long max, Pageable<?> pagination);

    Iterator<Prime> findByNumberIdNotGreaterThan(long max, Sort<?>... order);

    Slice<Prime> findByRomanNumeralEndsWithAndIdLessThan(String ending, long max, Limit limit, Sort<?>... orderBy);

    Slice<Prime> findByRomanNumeralEndsWithAndIdLessThan(String ending, long max, Pageable<?> pagination, Sort<?>... orderBy);

    @OrderBy(value = "sumOfBits", descending = true)
    @OrderBy("name")
    Slice<Prime> findByRomanNumeralStartsWithAndIdLessThan(String prefix, long max, Pageable<?> pagination);

    @Find
    Prime findFirst(Sort<Prime> sort, Limit limitOf1);

    Stream<Prime> findFirst2147483648ByIdGreaterThan(long min); // Exceeds Integer.MAX_VALUE by 1

    @OrderBy(value = "name", descending = true)
    Prime[] findFirst5ByIdLessThanEqual(long maxNumber);

    Prime findFirstByNameLikeOrderByNumberId(String namePattern);

    @Find
    Optional<Prime> findHexadecimal(String hex);

    List<Object[]> findIdAndNameBy(Sort<?>... sort);

    @OrderBy(value = "id", descending = true)
    Set<Long> findIdByIdBetween(long min, long max);

    @OrderBy(value = "id", descending = true)
    IntStream findSumOfBitsByIdBetween(long min, long max);

    boolean existsByNumberId(long number);

    Boolean existsByIdBetween(Long first, Long last);

    @Count
    long howManyIn(@By("id") @GreaterThanEqual long min,
                   @By("id") @LessThanEqual long max);

    @Count
    Long howManyBetweenExclusive(@By("NumberId") @GreaterThan long exclusiveMin,
                                 @By("NumberId") @LessThan long exclusiveMax);

    @Find
    @OrderBy(value = "id", descending = true)
    List<Long> inRangeHavingNumeralLikeAndSubstringOfName(@By("id") @GreaterThanEqual long min,
                                                          @By("id") @LessThanEqual long max,
                                                          @By("romanNumeral") @IgnoreCase @Like String pattern,
                                                          @By("name") @Contains String nameSuffix);

    @Exists
    boolean isFoundWith(long id, String hex);

    @Find
    @OrderBy(value = "numberId", descending = true)
    Stream<Prime> lessThanWithSuffixOrBetweenWithSuffix(@By("id") @LessThan long numLessThan,
                                                        @By("name") @EndsWith String firstSuffix,
                                                        @Or @By("id") @GreaterThanEqual long lowerLimit,
                                                        @By("id") @LessThanEqual long upperLimit,
                                                        @By("name") @EndsWith String secondSuffix);

    @OrderBy("id")
    @Query("SELECT o.numberId FROM Prime o WHERE (o.name = :numberName OR :numeral=o.romanNumeral OR o.hex =:hex OR o.numberId=:num)")
    long[] matchAny(long num, String numeral, String hex, String numberName);

    @OrderBy("id")
    @Query("SELECT o.name FROM Prime o WHERE (o.name <> ':name' AND (o.numberId=?1 OR o.name=?2))")
    List<String> matchAnyExceptLiteralValueThatLooksLikeANamedParameter(long num, String name);

    @OrderBy("name")
    @Query("SELECT o.name FROM Prime o WHERE ((o.name=?1 OR o.numberId=?2) AND o.name <> ':name')")
    ArrayList<String> matchAnyExceptLiteralValueThatLooksLikeANamedParameter(String name, long num);

    @Query("SELECT o.numberId FROM Prime o WHERE (o.name = :numName OR o.romanNumeral=:numeral OR o.hex =:hexadecimal OR o.numberId=:num)")
    Streamable<Long> matchAnyWithMixedUsageOfParamAnnotation(long num,
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

    @Query(value = "SELECT NEW java.util.AbstractMap.SimpleImmutableEntry(p.numberId, p.name) FROM Prime p WHERE p.numberId <= ?1 ORDER BY p.name",
           count = "SELECT COUNT(p) FROM Prime p WHERE p.numberId <= ?1")
    Page<Map.Entry<Long, String>> namesByNumber(long maxNumber, Pageable<?> pagination);

    @Query("SELECT prime.name, prime.hex FROM  Prime  prime  WHERE prime.numberId <= ?1")
    @OrderBy("numberId")
    Page<Object[]> namesWithHex(long maxNumber, Pageable<?> pagination);

    @Find
    @OrderBy("id")
    List<Long> notWithinButBelow(@By("id") @LessThan int rangeMin,
                                 @Or @By("id") @GreaterThan int rangeMax,
                                 @By("id") @LessThan int below);

    @Count
    int numEvenWithSumOfBits(int sumOfBits, boolean even);

    @Insert
    void persist(Prime... primes);

    @Query("SELECT DISTINCT LENGTH(p.romanNumeral) FROM Prime p WHERE p.numberId <= ?1 ORDER BY LENGTH(p.romanNumeral) DESC")
    Page<Integer> romanNumeralLengths(long maxNumber, Pageable<?> pagination);

    @Query("SELECT prime_ FROM Prime AS prime_ WHERE (prime_.numberId <= ?1)")
    @OrderBy(value = "even", descending = true)
    @OrderBy(value = "sumOfBits", descending = true)
    KeysetAwarePage<Prime> upTo(long maxNumber, Pageable<?> pagination);

    @Find
    @OrderBy("name")
    Stream<Prime> whereNameLengthWithin(@By("name") @CharCount @GreaterThanEqual int minLength,
                                        @By("name") @CharCount @LessThanEqual int maxLength);

    @Find
    Optional<Prime> withAnyCaseName(@By("name") @Trimmed @IgnoreCase String name);

    @Find
    List<Prime> withNameLengthAndWithin(@By("name") @Trimmed @CharCount int length,
                                        @By("id") @GreaterThanEqual long min,
                                        @By("id") @LessThanEqual long max);

    @Find
    @Select("name")
    List<String> withRomanNumeralSuffixAndWithoutNameSuffix(@By("romanNumeral") @EndsWith String numeralSuffix,
                                                            @By("name") @Not @EndsWith String nameSuffixToExclude,
                                                            @By("id") @LessThanEqual long max);
}
