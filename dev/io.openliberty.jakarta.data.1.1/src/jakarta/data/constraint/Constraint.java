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

import jakarta.annotation.Nonnull;
import java.util.Set;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface Constraint<V> {

    @Nonnull
    static <V extends Comparable<?>> Between<V> between(@Nonnull V minimum,
                                                        @Nonnull V maximum) {
        return Between.bounds(minimum, maximum);
    }

    @Nonnull
    static <V> EqualTo<V> equalTo(@Nonnull V value) {
        return EqualTo.value(value);
    }

    @Nonnull
    static <V extends Comparable<?>> GreaterThan<V> greaterThan(@Nonnull V bound) {
        return GreaterThan.bound(bound);
    }

    @Nonnull
    static <V extends Comparable<?>> AtLeast<V> greaterThanEqual(@Nonnull V minimum) {
        return AtLeast.min(minimum);
    }

    @Nonnull
    static <V> In<V> in(@Nonnull Set<V> values) {
        return In.values(values);
    }

    @SafeVarargs
    @Nonnull
    static <V> In<V> in(@Nonnull V... values) {
        return In.values(values);
    }

    @Nonnull
    static <V> Null<V> isNull() {
        return Null.instance();
    }

    @Nonnull
    static <V extends Comparable<?>> LessThan<V> lessThan(@Nonnull V bound) {
        return LessThan.bound(bound);
    }

    @Nonnull
    static <V extends Comparable<?>> AtMost<V> lessThanEqual(@Nonnull V maximum) {
        return AtMost.max(maximum);
    }

    @Nonnull
    static Like like(@Nonnull String pattern) {
        return Like.pattern(pattern);
    }

    @Nonnull
    static Like like(@Nonnull String pattern,
                     char charWildcard,
                     char stringWildcard) {
        return Like.pattern(pattern, charWildcard, stringWildcard);
    }

    @Nonnull
    static Like like(@Nonnull String pattern,
                     char charWildcard,
                     char stringWildcard,
                     char escape) {
        return Like.pattern(pattern, charWildcard, stringWildcard, escape);
    }

    @Nonnull
    Constraint<V> negate();

    @Nonnull
    static <V extends Comparable<?>> NotBetween<V> notBetween(@Nonnull V lowerBound,
                                                              @Nonnull V upperBound) {
        return NotBetween.bounds(lowerBound, upperBound);
    }

    @Nonnull
    static <V> NotEqualTo<V> notEqualTo(@Nonnull V value) {
        return NotEqualTo.value(value);
    }

    @Nonnull
    static <V> NotIn<V> notIn(@Nonnull Set<V> values) {
        return NotIn.values(values);
    }

    @SafeVarargs
    @Nonnull
    static <V> NotIn<V> notIn(@Nonnull V... values) {
        return NotIn.values(values);
    }

    @Nonnull
    static NotLike notLike(@Nonnull String pattern) {
        return NotLike.pattern(pattern);
    }

    @Nonnull
    static NotLike notLike(@Nonnull String pattern,
                           char charWildcard,
                           char stringWildcard) {
        return NotLike.pattern(pattern, charWildcard, stringWildcard);
    }

    @Nonnull
    static NotLike notLike(@Nonnull String pattern,
                           char charWildcard,
                           char stringWildcard,
                           char escape) {
        return NotLike.pattern(pattern, charWildcard, stringWildcard, escape);
    }

    @Nonnull
    static <V> NotNull<V> notNull() {
        return NotNull.instance();
    }

}
