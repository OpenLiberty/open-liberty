/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotLike;
import jakarta.data.messages.Messages;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.Restriction;
import jakarta.data.spi.expression.function.NumericFunctionExpression;
import jakarta.data.spi.expression.function.TextFunctionExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface TextExpression<T> extends ComparableExpression<T, String> {

    @Nonnull
    default TextExpression<T> append(@Nonnull String suffix) {
        Messages.requireNonNull(suffix, "suffix");
        return TextFunctionExpression.of(TextFunctionExpression.CONCAT,
                                         this,
                                         suffix);
    }

    @Nonnull
    default TextExpression<T> append(@Nonnull TextExpression<? super T> suffixExpression) {
        Messages.requireNonNull(suffixExpression, "suffixExpression");
        return TextFunctionExpression.of(TextFunctionExpression.CONCAT,
                                         suffixExpression,
                                         this);
    }

    @Nonnull
    default Sort<T> ascIgnoreCase() {
        return Sort.ascIgnoreCase(this);
    }

    @Nonnull
    default Restriction<T> contains(@Nonnull String substring) {
        Like constraint = Like.substring(substring);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Sort<T> descIgnoreCase() {
        return Sort.descIgnoreCase(this);
    }

    @Nonnull
    default Restriction<T> endsWith(@Nonnull String suffix) {
        Like constraint = Like.suffix(suffix);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default TextExpression<T> left(int length) {
        return TextFunctionExpression.of(TextFunctionExpression.LEFT,
                                         this,
                                         length);
    }

    @Nonnull
    default NumericExpression<T, Integer> length() {
        return NumericFunctionExpression.of(NumericFunctionExpression.LENGTH,
                                            Integer.class,
                                            this);
    }

    @Nonnull
    default Restriction<T> like(@Nonnull Like pattern) {
        return BasicRestriction.of(this, pattern);
    }

    @Nonnull
    default Restriction<T> like(@Nonnull String pattern) {
        Like constraint = Like.pattern(pattern);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> like(@Nonnull String pattern,
                                char charWildcard,
                                char stringWildcard) {
        Like constraint = Like.pattern(pattern,
                                       charWildcard,
                                       stringWildcard);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> like(@Nonnull String pattern,
                                char charWildcard,
                                char stringWildcard,
                                char escape) {
        Like constraint = Like.pattern(pattern,
                                       charWildcard,
                                       stringWildcard,
                                       escape);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default TextExpression<T> lower() {
        return TextFunctionExpression.of(TextFunctionExpression.LOWER,
                                         this);
    }

    @Nonnull
    default Restriction<T> notContains(@Nonnull String substring) {
        NotLike constraint = NotLike.substring(substring);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notEndsWith(@Nonnull String suffix) {
        NotLike constraint = NotLike.suffix(suffix);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notLike(@Nonnull String pattern) {
        NotLike constraint = NotLike.pattern(pattern);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notLike(@Nonnull String pattern,
                                   char charWildcard,
                                   char stringWildcard) {
        NotLike constraint = NotLike.pattern(pattern,
                                             charWildcard,
                                             stringWildcard);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notLike(@Nonnull String pattern,
                                   char charWildcard,
                                   char stringWildcard,
                                   char escape) {
        NotLike constraint = NotLike.pattern(pattern,
                                             charWildcard,
                                             stringWildcard,
                                             escape);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notStartsWith(@Nonnull String prefix) {
        NotLike constraint = NotLike.prefix(prefix);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default TextExpression<T> prepend(@Nonnull String prefix) {
        Messages.requireNonNull(prefix, "prefix");
        return TextFunctionExpression.of(TextFunctionExpression.CONCAT,
                                         prefix,
                                         this);
    }

    @Nonnull
    default TextExpression<T> prepend(@Nonnull TextExpression<? super T> prefixExpression) {
        Messages.requireNonNull(prefixExpression, "prefixExpression");
        return TextFunctionExpression.of(TextFunctionExpression.CONCAT,
                                         prefixExpression,
                                         this);
    }

    @Nonnull
    default TextExpression<T> right(int length) {
        return TextFunctionExpression.of(TextFunctionExpression.RIGHT,
                                         this,
                                         length);
    }

    @Nonnull
    default Restriction<T> startsWith(@Nonnull String prefix) {
        Like constraint = Like.prefix(prefix);
        return BasicRestriction.of(this, constraint);
    }

    @Override
    @Nonnull
    default Class<String> type() {
        return String.class;
    }

    @Nonnull
    default TextExpression<T> upper() {
        return TextFunctionExpression.of(TextFunctionExpression.UPPER,
                                         this);
    }

}
