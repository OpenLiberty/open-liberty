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

    default NumericExpression<T, N> abs() {
        return NumericFunctionExpression.of(NumericFunctionExpression.ABS, this);
    }

    default NumericExpression<T, BigDecimal> asBigDecimal() {
        return NumericCast.of(this, BigDecimal.class);
    }

    default NumericExpression<T, BigInteger> asBigInteger() {
        return NumericCast.of(this, BigInteger.class);
    }

    default NumericExpression<T, Double> asDouble() {
        return NumericCast.of(this, Double.class);
    }

    default NumericExpression<T, Long> asLong() {
        return NumericCast.of(this, Long.class);
    }

    default NumericExpression<T, N> dividedBy(N divisor) {
        return NumericOperatorExpression.of(Operator.DIVIDE,
                                            this,
                                            divisor);
    }

    default NumericExpression<T, N> //
                    dividedBy(NumericExpression<? super T, N> divisorExpression) {
        return NumericOperatorExpression.of(Operator.DIVIDE,
                                            this,
                                            divisorExpression);
    }

    default NumericExpression<T, N> dividedInto(N value) {
        return NumericOperatorExpression.of(Operator.DIVIDE,
                                            value,
                                            this);
    }

    default NumericExpression<T, N> minus(N value) {
        return NumericOperatorExpression.of(Operator.MINUS,
                                            this,
                                            value);
    }

    default NumericExpression<T, N> //
                    minus(NumericExpression<? super T, N> expression) {
        return NumericOperatorExpression.of(Operator.MINUS,
                                            this,
                                            expression);
    }

    default NumericExpression<T, N> negated() {
        return NumericFunctionExpression.of(NumericFunctionExpression.NEG,
                                            this);
    }

    default NumericExpression<T, N> plus(N value) {
        return NumericOperatorExpression.of(Operator.PLUS,
                                            this,
                                            value);
    }

    default NumericExpression<T, N> //
                    plus(NumericExpression<? super T, N> expression) {
        return NumericOperatorExpression.of(Operator.PLUS,
                                            this,
                                            expression);
    }

    default NumericExpression<T, N> subtractedFrom(N value) {
        return NumericOperatorExpression.of(Operator.MINUS,
                                            value,
                                            this);
    }

    default NumericExpression<T, N> times(N factor) {
        return NumericOperatorExpression.of(Operator.TIMES,
                                            this,
                                            factor);
    }

    default NumericExpression<T, N> //
                    times(NumericExpression<? super T, N> factorExpression) {
        return NumericOperatorExpression.of(Operator.TIMES,
                                            this,
                                            factorExpression);
    }

}
