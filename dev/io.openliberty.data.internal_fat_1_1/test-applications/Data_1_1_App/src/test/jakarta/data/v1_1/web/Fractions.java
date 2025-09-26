/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import java.util.Collection;
import java.util.List;
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
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Is;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;

/**
 * Repository for the Fraction entity
 */
@Repository
public interface Fractions {
    @Find
    Stream<Fraction> denominatoredUpTo //
    (@By(_Fraction.DENOMINATOR) NotNull<Integer> notNull,
     @By(_Fraction.DENOMINATOR) AtMost<Integer> max,
     Sort<?>... sorts);

    @Find
    @OrderBy(_Fraction.NUMERATOR)
    @OrderBy(_Fraction.DENOMINATOR)
    Stream<Fraction> havingDenominatorWithin//
    (@By(_Fraction.DENOMINATOR) @Is(AtLeast.class) long min,
     @By(_Fraction.DENOMINATOR) @Is(AtMost.class) long max);

    @Find
    @Select(_Fraction.NAME)
    List<String> named(@By(_Fraction.NAME) Like pattern,
                       Order<Fraction> order,
                       Limit limit);

    @Query("SELECT numerator, denominator - numerator" +
           " ORDER BY denominator - numerator DESC, numerator ASC")
    Page<Ratio> pageOfRatios(PageRequest pageReq);

    @Query("SELECT numerator, denominator - numerator")
    Stream<Ratio> streamOfRatios();

    @Find
    @OrderBy(_Fraction.DENOMINATOR)
    CursoredPage<Fraction> namedLike //
    (@By(_Fraction.NAME) @Is(Like.class) String pattern,
     Order<Fraction> additionalSorting,
     PageRequest pageReq);

    @Insert
    void supply(Collection<Fraction> list);

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
    @Select(_Fraction.NAME)
    List<String> withNumeratorsAndDenominator //
    (@By(_Fraction.NUMERATOR) In<Integer> numerators,
     @Is int denominator,
     Sort<Fraction> sort);
}
