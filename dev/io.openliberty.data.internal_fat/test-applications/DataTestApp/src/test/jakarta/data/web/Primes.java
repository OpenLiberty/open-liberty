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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.Limit;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Page;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
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

    @OrderBy("number")
    KeysetAwarePage<Prime> findByNumberBetween(long min, long max, Pageable pagination);

    KeysetAwarePage<Prime> findByNumberBetweenOrderByEvenDescSumOfBitsDescNumberAsc(long min, long max, Pageable pagination);

    Stream<Prime> findByNumberLessThan(long max);

    Streamable<Prime> findByNumberLessThanEqualOrderByNumberAsc(long max, Pageable pagination);

    Streamable<Prime> findByNumberLessThanEqualOrderByNumberDesc(long max, Limit limit);

    Page<Prime> findByNumberLessThanEqualOrderByNumberDesc(long max, Pageable pagination);

    @OrderBy(value = "name", descending = true)
    Prime[] findFirst5ByNumberLessThanEqual(long maxNumber);

    Prime findFirstByNameLikeOrderByNumber(String namePattern);

    boolean existsByNumber(long number);

    Boolean existsNumberBetween(Long first, Long last);

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
