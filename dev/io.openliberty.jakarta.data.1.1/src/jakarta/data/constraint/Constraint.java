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
package jakarta.data.constraint;

import java.util.Set;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface Constraint<V> {

    static <V extends Comparable<?>> Between<V> between(V minimum,
                                                        V maximum) {
        return Between.bounds(minimum, maximum);
    }

    static <V> EqualTo<V> equalTo(V value) {
        return EqualTo.value(value);
    }

    static <V extends Comparable<?>> GreaterThan<V> greaterThan(V bound) {
        return GreaterThan.bound(bound);
    }

    static <V extends Comparable<?>> AtLeast<V> greaterThanEqual(V minimum) {
        return AtLeast.min(minimum);
    }

    static <V> In<V> in(Set<V> values) {
        return In.values(values);
    }

    @SafeVarargs
    static <V> In<V> in(V... values) {
        return In.values(values);
    }

    static <V> Null<V> isNull() {
        return Null.instance();
    }

    static <V extends Comparable<?>> LessThan<V> lessThan(V bound) {
        return LessThan.bound(bound);
    }

    static <V extends Comparable<?>> AtMost<V> lessThanEqual(V maximum) {
        return AtMost.max(maximum);
    }

    static Like like(String pattern) {
        return Like.pattern(pattern);
    }

    static Like like(String pattern,
                     char charWildcard,
                     char stringWildcard) {
        return Like.pattern(pattern, charWildcard, stringWildcard);
    }

    static Like like(String pattern,
                     char charWildcard,
                     char stringWildcard,
                     char escape) {
        return Like.pattern(pattern, charWildcard, stringWildcard, escape);
    }

    Constraint<V> negate();

    static <V extends Comparable<?>> NotBetween<V> notBetween(V lowerBound,
                                                              V upperBound) {
        return NotBetween.bounds(lowerBound, upperBound);
    }

    static <V> NotEqualTo<V> notEqualTo(V value) {
        return NotEqualTo.value(value);
    }

    static <V> NotIn<V> notIn(Set<V> values) {
        return NotIn.values(values);
    }

    @SafeVarargs
    static <V> NotIn<V> notIn(V... values) {
        return NotIn.values(values);
    }

    static NotLike notLike(String pattern) {
        return NotLike.pattern(pattern);
    }

    static NotLike notLike(String pattern,
                           char charWildcard,
                           char stringWildcard) {
        return NotLike.pattern(pattern, charWildcard, stringWildcard);
    }

    static NotLike notLike(String pattern,
                           char charWildcard,
                           char stringWildcard,
                           char escape) {
        return NotLike.pattern(pattern, charWildcard, stringWildcard, escape);
    }

    static <V> NotNull<V> notNull() {
        return NotNull.instance();
    }

}
