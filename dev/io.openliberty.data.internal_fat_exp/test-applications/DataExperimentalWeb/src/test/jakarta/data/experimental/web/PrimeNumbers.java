/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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
package test.jakarta.data.experimental.web;

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.GreaterThan;
import jakarta.data.constraint.LessThan;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotLike;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Is;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;

import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.IgnoreCase;
import io.openliberty.data.repository.function.CharCount;
import io.openliberty.data.repository.function.Trimmed;

/**
 * Repository with data that is pre-populated.
 * This should be treated as read-only to avoid interference between with tests.
 */
@Repository
public interface PrimeNumbers {

    @Exists
    boolean anyLessThanWithBitPattern(@By("numberId") @Is(LessThan.class) long upperLimit,
                                      @By("binaryDigits") @Is(Like.class) String pattern);

    @Count
    long howManyIn(@By(ID) @Is(AtLeast.class) long min,
                   @By(ID) @Is(AtMost.class) long max);

    @Count
    Long howManyBetweenExclusive(@By("NumberId") @Is(GreaterThan.class) long exclusiveMin,
                                 @By("NumberId") @Is(LessThan.class) long exclusiveMax);

    @Find
    @OrderBy(value = ID, descending = true)
    List<Long> inRangeHavingNumeralLikeAndNamePattern(@By(ID) @Is(AtLeast.class) long min,
                                                      @By(ID) @Is(AtMost.class) long max,
                                                      @By("romanNumeral") @Is(Like.class) @IgnoreCase String pattern,
                                                      @By("name") Like namePattern);

    @Exists
    boolean isFoundWith(long numberId, String hex);

    @Count
    int numEvenWithSumOfBits(int sumOfBits, boolean even);

    @Query("SELECT hex, numberId WHERE numberId = ?1")
    Optional<Hexadecimal> toHexadecimal(long decimalValue);

    @Find
    @OrderBy("name")
    Stream<PrimeNum> whereNameLengthWithin(@By("name") @CharCount @Is(AtLeast.class) int minLength,
                                           @By("name") @CharCount @Is(AtMost.class) int maxLength);

    @Find
    Optional<PrimeNum> withAnyCaseName(@By("name") @Trimmed @IgnoreCase String name);

    @Find
    List<PrimeNum> withNameLengthAndWithin(@By("name") @Trimmed @CharCount int length,
                                           @By(ID) @Is(AtLeast.class) long min,
                                           @By(ID) @Is(AtMost.class) long max);

    @Find
    @Select("name")
    List<String> withRomanNumeralAndWithoutName(@By("romanNumeral") @Is(Like.class) String numeralPattern,
                                                @By("name") @Is(NotLike.class) String namePatternToExclude,
                                                @By(ID) @Is(AtMost.class) long max);

    @Insert
    void write(PrimeNum... primes);
}
