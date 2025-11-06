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

import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotLike;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.Restriction;
import jakarta.data.spi.expression.function.NumericFunctionExpression;
import jakarta.data.spi.expression.function.TextFunctionExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface TextExpression<T> extends ComparableExpression<T, String> {

    default TextExpression<T> append(String suffix) {
        return TextFunctionExpression.of(TextFunctionExpression.CONCAT,
                                         this,
                                         suffix);
    }

    default TextExpression<T> append(TextExpression<? super T> suffixExpression) {
        return TextFunctionExpression.of(TextFunctionExpression.CONCAT,
                                         suffixExpression,
                                         this);
    }

    default Restriction<T> contains(String substring) {
        Like constraint = Like.substring(substring);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> endsWith(String suffix) {
        Like constraint = Like.suffix(suffix);
        return BasicRestriction.of(this, constraint);
    }

    default TextExpression<T> left(int length) {
        return TextFunctionExpression.of(TextFunctionExpression.LEFT,
                                         this,
                                         length);
    }

    default NumericExpression<T, Integer> length() {
        return NumericFunctionExpression.of(NumericFunctionExpression.LENGTH,
                                            this);
    }

    default Restriction<T> like(Like pattern) {
        return BasicRestriction.of(this, pattern);
    }

    default Restriction<T> like(String pattern) {
        Like constraint = Like.pattern(pattern);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> like(String pattern,
                                char charWildcard,
                                char stringWildcard) {
        Like constraint = Like.pattern(pattern,
                                       charWildcard,
                                       stringWildcard);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> like(String pattern,
                                char charWildcard,
                                char stringWildcard,
                                char escape) {
        Like constraint = Like.pattern(pattern,
                                       charWildcard,
                                       stringWildcard,
                                       escape);
        return BasicRestriction.of(this, constraint);
    }

    default TextExpression<T> lower() {
        return TextFunctionExpression.of(TextFunctionExpression.LOWER,
                                         this);
    }

    default Restriction<T> notContains(String substring) {
        NotLike constraint = NotLike.substring(substring);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notEndsWith(String suffix) {
        NotLike constraint = NotLike.suffix(suffix);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notLike(String pattern) {
        NotLike constraint = NotLike.pattern(pattern);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notLike(String pattern,
                                   char charWildcard,
                                   char stringWildcard) {
        NotLike constraint = NotLike.pattern(pattern,
                                             charWildcard,
                                             stringWildcard);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notLike(String pattern,
                                   char charWildcard,
                                   char stringWildcard,
                                   char escape) {
        NotLike constraint = NotLike.pattern(pattern,
                                             charWildcard,
                                             stringWildcard,
                                             escape);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notStartsWith(String prefix) {
        NotLike constraint = NotLike.prefix(prefix);
        return BasicRestriction.of(this, constraint);
    }

    default TextExpression<T> prepend(String prefix) {
        return TextFunctionExpression.of(TextFunctionExpression.CONCAT,
                                         prefix,
                                         this);
    }

    default TextExpression<T> prepend(TextExpression<? super T> prefixExpression) {
        return TextFunctionExpression.of(TextFunctionExpression.CONCAT,
                                         prefixExpression,
                                         this);
    }

    default TextExpression<T> right(int length) {
        return TextFunctionExpression.of(TextFunctionExpression.RIGHT,
                                         this,
                                         length);
    }

    default Restriction<T> startsWith(String prefix) {
        Like constraint = Like.prefix(prefix);
        return BasicRestriction.of(this, constraint);
    }

    default TextExpression<T> upper() {
        return TextFunctionExpression.of(TextFunctionExpression.UPPER,
                                         this);
    }

}
