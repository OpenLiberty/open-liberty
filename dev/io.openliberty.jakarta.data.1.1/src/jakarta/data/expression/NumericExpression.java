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
import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.data.spi.expression.function.NumericCast;
import jakarta.data.spi.expression.function.NumericFunctionExpression;
import jakarta.data.spi.expression.function.NumericOperatorExpression;
import jakarta.data.spi.expression.function.NumericOperatorExpression.Operator;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NumericExpression<T, N extends Number & Comparable<N>> //
                extends ComparableExpression<T, N> {

    @Nonnull
    default NumericExpression<T, N> abs() {
        return NumericFunctionExpression.of(NumericFunctionExpression.ABS,
                                            type(),
                                            this);
    }

    @Nonnull
    default NumericExpression<T, BigDecimal> asBigDecimal() {
        return NumericCast.of(this, BigDecimal.class);
    }

    @Nonnull
    default NumericExpression<T, BigInteger> asBigInteger() {
        return NumericCast.of(this, BigInteger.class);
    }

    @Nonnull
    default NumericExpression<T, Double> asDouble() {
        return NumericCast.of(this, Double.class);
    }

    @Nonnull
    default NumericExpression<T, Long> asLong() {
        return NumericCast.of(this, Long.class);
    }

    @Nonnull
    default NumericExpression<T, N> dividedBy(@Nonnull N divisor) {
        return NumericOperatorExpression.of(Operator.DIVIDE,
                                            this,
                                            divisor);
    }

    @Nonnull
    default NumericExpression<T, N> //
                    dividedBy(@Nonnull NumericExpression<? super T, N> divisorExpression) {
        return NumericOperatorExpression.of(Operator.DIVIDE,
                                            this,
                                            divisorExpression);
    }

    @Nonnull
    default NumericExpression<T, N> dividedInto(@Nonnull N value) {
        return NumericOperatorExpression.of(Operator.DIVIDE,
                                            value,
                                            this);
    }

    @Nonnull
    default NumericExpression<T, N> minus(@Nonnull N value) {
        return NumericOperatorExpression.of(Operator.MINUS,
                                            this,
                                            value);
    }

    @Nonnull
    default NumericExpression<T, N> //
                    minus(@Nonnull NumericExpression<? super T, N> expression) {
        return NumericOperatorExpression.of(Operator.MINUS,
                                            this,
                                            expression);
    }

    @Nonnull
    default NumericExpression<T, N> negated() {
        return NumericFunctionExpression.of(NumericFunctionExpression.NEG,
                                            type(),
                                            this);
    }

    @Nonnull
    default NumericExpression<T, N> plus(@Nonnull N value) {
        return NumericOperatorExpression.of(Operator.PLUS,
                                            this,
                                            value);
    }

    @Nonnull
    default NumericExpression<T, N> //
                    plus(@Nonnull NumericExpression<? super T, N> expression) {
        return NumericOperatorExpression.of(Operator.PLUS,
                                            this,
                                            expression);
    }

    @Nonnull
    default NumericExpression<T, N> subtractedFrom(@Nonnull N value) {
        return NumericOperatorExpression.of(Operator.MINUS,
                                            value,
                                            this);
    }

    @Nonnull
    default NumericExpression<T, N> times(@Nonnull N factor) {
        return NumericOperatorExpression.of(Operator.TIMES,
                                            this,
                                            factor);
    }

    @Nonnull
    default NumericExpression<T, N> //
                    times(@Nonnull NumericExpression<? super T, N> factorExpression) {
        return NumericOperatorExpression.of(Operator.TIMES,
                                            this,
                                            factorExpression);
    }

}
