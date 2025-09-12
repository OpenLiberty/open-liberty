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

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.data.expression.NumericExpression;
import jakarta.data.messages.Messages;
import jakarta.data.spi.expression.literal.NumericLiteral;

record NumericOperatorExpressionRecord<T, N extends Number & Comparable<N>>(
                Operator operator,
                NumericExpression<? super T, N> left,
                NumericExpression<? super T, N> right)
                implements NumericOperatorExpression<T, N> {

    NumericOperatorExpressionRecord {
        Messages.requireNonNull(operator, "operator");
        Messages.requireNonNull(left, "left");
        Messages.requireNonNull(right, "right");

        if (operator == Operator.DIVIDE &&
            right instanceof NumericLiteral lit &&
            zero(lit.value())) {
            throw new IllegalArgumentException(Messages.get("005.zero.not.allowed",
                                                            "divisor"));
        }
    }

    @Override
    public String toString() {
        char op = switch (operator) {
            case PLUS -> '+';
            case TIMES -> '*';
            case MINUS -> '-';
            case DIVIDE -> '/';
            default -> throw new IllegalStateException();
        };

        String rightString = right.toString();
        String leftString = left.toString();

        StringBuilder expression = new StringBuilder(rightString.length() +
                                                     leftString.length() +
                                                     7);

        if (left instanceof NumericOperatorExpression) {
            expression.append('(').append(leftString).append(')');
        } else {
            expression.append(leftString);
        }

        expression.append(' ').append(op).append(' ');

        if (right instanceof NumericOperatorExpression) {
            expression.append('(').append(rightString).append(')');
        } else {
            expression.append(rightString);
        }

        return expression.toString();
    }

    private static boolean zero(Object number) {
        if (number instanceof Integer i)
            return i == 0;
        if (number instanceof Long l)
            return l == 0L;
        if (number instanceof Double d)
            return 0 == Double.compare(d, 0.0d);
        if (number instanceof Float f)
            return 0 == Float.compare(f, 0.0f);
        if (number instanceof BigDecimal bd)
            return 0 == bd.compareTo(BigDecimal.ZERO);
        if (number instanceof BigInteger bi)
            return 0 == bi.compareTo(BigInteger.ZERO);
        if (number instanceof Short s)
            return s == (short) 0;
        if (number instanceof Byte b)
            return b == (byte) 0;

        throw new IllegalArgumentException(Messages.get("009.unknown.number.type",
                                                        number.getClass().getName()));
    }
}
