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

    default <U extends ComparableExpression<? super T, V>> Restriction<T> //
                    between(U minExpression,
                            U maxExpression) {
        Constraint<V> constraint = Between.bounds(minExpression,
                                                  maxExpression);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> between(V min, V max) {
        Constraint<V> constraint = Between.bounds(min, max);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> greaterThan//
    (ComparableExpression<? super T, V> expression) {
        Constraint<V> constraint = GreaterThan.bound(expression);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> greaterThan(V value) {
        Constraint<V> constraint = GreaterThan.bound(value);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> greaterThanEqual//
    (ComparableExpression<? super T, V> expression) {
        Constraint<V> constraint = AtLeast.min(expression);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> greaterThanEqual(V value) {
        Constraint<V> constraint = AtLeast.min(value);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> lessThan(ComparableExpression<? super T, V> expression) {
        Constraint<V> constraint = LessThan.bound(expression);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> lessThan(V value) {
        Constraint<V> constraint = LessThan.bound(value);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> lessThanEqual//
    (ComparableExpression<? super T, V> expression) {
        Constraint<V> constraint = AtMost.max(expression);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> lessThanEqual(V value) {
        Constraint<V> constraint = AtMost.max(value);
        return BasicRestriction.of(this, constraint);
    }

    default <U extends ComparableExpression<? super T, V>> Restriction<T> //
                    notBetween(U minExpression,
                               U maxExpression) {
        Constraint<V> constraint = NotBetween.bounds(minExpression,
                                                     maxExpression);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notBetween(V min, V max) {
        Constraint<V> constraint = NotBetween.bounds(min, max);
        return BasicRestriction.of(this, constraint);
    }
}
