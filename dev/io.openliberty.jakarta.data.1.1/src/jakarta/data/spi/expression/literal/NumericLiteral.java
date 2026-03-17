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
package jakarta.data.spi.expression.literal;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.data.expression.NumericExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NumericLiteral<N extends Number & Comparable<N>> extends //
                ComparableLiteral<N>, //
                NumericExpression<Object, N> {

    static NumericLiteral<BigDecimal> of(BigDecimal value) {
        return new NumericLiteralRecord<>(BigDecimal.class, value);
    }

    static NumericLiteral<BigInteger> of(BigInteger value) {
        return new NumericLiteralRecord<>(BigInteger.class, value);
    }

    static NumericLiteral<Byte> of(byte value) {
        return new NumericLiteralRecord<>(Byte.class, value);
    }

    static NumericLiteral<Double> of(double value) {
        return new NumericLiteralRecord<>(Double.class, value);
    }

    static NumericLiteral<Float> of(float value) {
        return new NumericLiteralRecord<>(Float.class, value);
    }

    static NumericLiteral<Long> of(long value) {
        return new NumericLiteralRecord<>(Long.class, value);
    }

    static NumericLiteral<Integer> of(int value) {
        return new NumericLiteralRecord<>(Integer.class, value);
    }

    static NumericLiteral<Short> of(short value) {
        return new NumericLiteralRecord<>(Short.class, value);
    }

    static <N extends Number & Comparable<N>> NumericLiteral<N> of//
    (Class<? extends N> type,
     N value) {
        return new NumericLiteralRecord<>(type, value);
    }

}
