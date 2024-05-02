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
package test.jakarta.data.experimental.web;

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

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
public interface PrimeNumbers {

    @Exists
    boolean anyLessThanEndingWithBitPattern(@By("numberId") @LessThan long upperLimit,
                                            @By("binaryDigits") @EndsWith String pattern);

    @Count
    long howManyIn(@By(ID) @GreaterThanEqual long min,
                   @By(ID) @LessThanEqual long max);

    @Count
    Long howManyBetweenExclusive(@By("NumberId") @GreaterThan long exclusiveMin,
                                 @By("NumberId") @LessThan long exclusiveMax);

    @Find
    @OrderBy(value = ID, descending = true)
    List<Long> inRangeHavingNumeralLikeAndSubstringOfName(@By(ID) @GreaterThanEqual long min,
                                                          @By(ID) @LessThanEqual long max,
                                                          @By("romanNumeral") @IgnoreCase @Like String pattern,
                                                          @By("name") @Contains String nameSuffix);

    @Exists
    boolean isFoundWith(long numberId, String hex);

    @Find
    @OrderBy(value = "numberId", descending = true)
    Stream<PrimeNum> lessThanWithSuffixOrBetweenWithSuffix(@By(ID) @LessThan long numLessThan,
                                                           @By("name") @EndsWith String firstSuffix,
                                                           @Or @By(ID) @GreaterThanEqual long lowerLimit,
                                                           @By(ID) @LessThanEqual long upperLimit,
                                                           @By("name") @EndsWith String secondSuffix);

    @Find
    @OrderBy(ID)
    List<Long> notWithinButBelow(@By(ID) @LessThan int rangeMin,
                                 @Or @By(ID) @GreaterThan int rangeMax,
                                 @By(ID) @LessThan int below);

    @Count
    int numEvenWithSumOfBits(int sumOfBits, boolean even);

    @Find
    @OrderBy("name")
    Stream<PrimeNum> whereNameLengthWithin(@By("name") @CharCount @GreaterThanEqual int minLength,
                                           @By("name") @CharCount @LessThanEqual int maxLength);

    @Find
    Optional<PrimeNum> withAnyCaseName(@By("name") @Trimmed @IgnoreCase String name);

    @Find
    List<PrimeNum> withNameLengthAndWithin(@By("name") @Trimmed @CharCount int length,
                                           @By(ID) @GreaterThanEqual long min,
                                           @By(ID) @LessThanEqual long max);

    @Find
    @Select("name")
    List<String> withRomanNumeralSuffixAndWithoutNameSuffix(@By("romanNumeral") @EndsWith String numeralSuffix,
                                                            @By("name") @Not @EndsWith String nameSuffixToExclude,
                                                            @By(ID) @LessThanEqual long max);

    @Insert
    void write(PrimeNum... primes);
}
