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
package jakarta.data.expression;

import jakarta.annotation.Nonnull;
import jakarta.data.Sort;
import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.Between;
import jakarta.data.constraint.Constraint;
import jakarta.data.constraint.GreaterThan;
import jakarta.data.constraint.LessThan;
import jakarta.data.constraint.NotBetween;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.Restriction;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface ComparableExpression<T, V extends Comparable<?>> //
                extends Expression<T, V> {
    @Nonnull
    default Sort<T> asc() {
        return Sort.asc(this);
    }

    @Nonnull
    default <U extends ComparableExpression<? super T, V>> Restriction<T> //
                    between(@Nonnull U minExpression,
                            @Nonnull U maxExpression) {
        Constraint<V> constraint = Between.bounds(minExpression,
                                                  maxExpression);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> between(@Nonnull V min, @Nonnull V max) {
        Constraint<V> constraint = Between.bounds(min, max);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Sort<T> desc() {
        return Sort.desc(this);
    }

    @Nonnull
    default Restriction<T> greaterThan//
    (@Nonnull ComparableExpression<? super T, V> expression) {
        Constraint<V> constraint = GreaterThan.bound(expression);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> greaterThan(@Nonnull V value) {
        Constraint<V> constraint = GreaterThan.bound(value);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> greaterThanEqual//
    (@Nonnull ComparableExpression<? super T, V> expression) {
        Constraint<V> constraint = AtLeast.min(expression);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> greaterThanEqual(@Nonnull V value) {
        Constraint<V> constraint = AtLeast.min(value);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> lessThan(@Nonnull ComparableExpression<? super T, V> expression) {
        Constraint<V> constraint = LessThan.bound(expression);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> lessThan(@Nonnull V value) {
        Constraint<V> constraint = LessThan.bound(value);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> lessThanEqual//
    (@Nonnull ComparableExpression<? super T, V> expression) {
        Constraint<V> constraint = AtMost.max(expression);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> lessThanEqual(@Nonnull V value) {
        Constraint<V> constraint = AtMost.max(value);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default <U extends ComparableExpression<? super T, V>> Restriction<T> //
                    notBetween(@Nonnull U minExpression,
                               @Nonnull U maxExpression) {
        Constraint<V> constraint = NotBetween.bounds(minExpression,
                                                     maxExpression);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notBetween(@Nonnull V min, @Nonnull V max) {
        Constraint<V> constraint = NotBetween.bounds(min, max);
        return BasicRestriction.of(this, constraint);
    }
}
