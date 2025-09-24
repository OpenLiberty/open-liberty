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
package jakarta.data.spi.expression.function;

import jakarta.data.expression.Expression;
import jakarta.data.expression.NumericExpression;
import jakarta.data.messages.Messages;
import jakarta.data.spi.expression.literal.NumericLiteral;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NumericOperatorExpression<T, N extends Number & Comparable<N>> //
                extends NumericExpression<T, N> {
    enum Operator {
        PLUS,
        MINUS,
        TIMES,
        DIVIDE
    }

    Expression<? super T, N> left();

    static <T, N extends Number & Comparable<N>> NumericOperatorExpression<T, N> //
                    of(Operator operator,
                       N left,
                       NumericExpression<T, N> right) {
        Messages.requireNonNull(left, "left");

        return new NumericOperatorExpressionRecord<>(operator, //
                        NumericLiteral.of(left), //
                        right);
    }

    static <T, N extends Number & Comparable<N>> NumericOperatorExpression<T, N> //
                    of(Operator operator,
                       NumericExpression<T, N> left,
                       N right) {
        Messages.requireNonNull(right, "right");

        return new NumericOperatorExpressionRecord<>(operator, //
                        left, //
                        NumericLiteral.of(right));
    }

    static <T, N extends Number & Comparable<N>> NumericOperatorExpression<T, N> //
                    of(Operator operator,
                       NumericExpression<T, N> left,
                       NumericExpression<? super T, N> right) {
        return new NumericOperatorExpressionRecord<>(operator, //
                        left, //
                        right);
    }

    Operator operator();

    Expression<? super T, N> right();
}
