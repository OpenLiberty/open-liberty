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

import static io.openliberty.data.repository.Is.Op.GreaterThan;
import static io.openliberty.data.repository.Is.Op.GreaterThanEqual;
import static io.openliberty.data.repository.Is.Op.IgnoreCase;
import static io.openliberty.data.repository.Is.Op.LessThan;
import static io.openliberty.data.repository.Is.Op.LessThanEqual;
import static io.openliberty.data.repository.Is.Op.LikeIgnoreCase;
import static io.openliberty.data.repository.Is.Op.NotSuffixed;
import static io.openliberty.data.repository.Is.Op.Substringed;
import static io.openliberty.data.repository.Is.Op.Suffixed;
import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;

import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Is;
import io.openliberty.data.repository.Or;
import io.openliberty.data.repository.function.CharCount;
import io.openliberty.data.repository.function.Trimmed;

/**
 * Repository with data that is pre-populated.
 * This should be treated as read-only to avoid interference between with tests.
 */
@Repository
public interface PrimeNumbers {

    @Exists
    boolean anyLessThanEndingWithBitPattern(@By("numberId") @Is(LessThan) long upperLimit,
                                            @By("binaryDigits") @Is(Suffixed) String pattern);

    @Count
    long howManyIn(@By(ID) @Is(GreaterThanEqual) long min,
                   @By(ID) @Is(LessThanEqual) long max);

    @Count
    Long howManyBetweenExclusive(@By("NumberId") @Is(GreaterThan) long exclusiveMin,
                                 @By("NumberId") @Is(LessThan) long exclusiveMax);

    @Find
    @OrderBy(value = ID, descending = true)
    List<Long> inRangeHavingNumeralLikeAndSubstringOfName(@By(ID) @Is(GreaterThanEqual) long min,
                                                          @By(ID) @Is(LessThanEqual) long max,
                                                          @By("romanNumeral") @Is(LikeIgnoreCase) String pattern,
                                                          @By("name") @Is(Substringed) String nameSubstring);

    @Exists
    boolean isFoundWith(long numberId, String hex);

    @Find
    @OrderBy(value = "numberId", descending = true)
    Stream<PrimeNum> lessThanWithSuffixOrBetweenWithSuffix(@By(ID) @Is(LessThan) long numLessThan,
                                                           @By("name") @Is(Suffixed) String firstSuffix,
                                                           @Or @By(ID) @Is(GreaterThanEqual) long lowerLimit,
                                                           @By(ID) @Is(LessThanEqual) long upperLimit,
                                                           @By("name") @Is(Suffixed) String secondSuffix);

    @Find
    @OrderBy(ID)
    List<Long> notWithinButBelow(@By(ID) @Is(LessThan) int rangeMin,
                                 @Or @By(ID) @Is(GreaterThan) int rangeMax,
                                 @By(ID) @Is(LessThan) int below);

    @Count
    int numEvenWithSumOfBits(int sumOfBits, boolean even);

    @Query("SELECT hex, numberId WHERE numberId = ?1")
    Optional<Hexadecimal> toHexadecimal(long decimalValue);

    @Find
    @OrderBy("name")
    Stream<PrimeNum> whereNameLengthWithin(@By("name") @CharCount @Is(GreaterThanEqual) int minLength,
                                           @By("name") @CharCount @Is(LessThanEqual) int maxLength);

    @Find
    Optional<PrimeNum> withAnyCaseName(@By("name") @Trimmed @Is(IgnoreCase) String name);

    @Find
    List<PrimeNum> withNameLengthAndWithin(@By("name") @Trimmed @CharCount int length,
                                           @By(ID) @Is(GreaterThanEqual) long min,
                                           @By(ID) @Is(LessThanEqual) long max);

    @Find
    @Select("name")
    List<String> withRomanNumeralSuffixAndWithoutNameSuffix(@By("romanNumeral") @Is(Suffixed) String numeralSuffix,
                                                            @By("name") @Is(NotSuffixed) String nameSuffixToExclude,
                                                            @By(ID) @Is(LessThanEqual) long max);

    @Insert
    void write(PrimeNum... primes);
}
