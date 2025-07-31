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
package jakarta.data.spi.expression.literal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;

import jakarta.data.expression.ComparableExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface ComparableLiteral<V extends Comparable<?>> extends //
                ComparableExpression<Object, V>, //
                Literal<V> {

    @SuppressWarnings("unchecked")
    static <V extends Comparable<?>> ComparableLiteral<V> of(V value) {
        if (value instanceof String s)
            return (ComparableLiteral<V>) StringLiteral.of(s);
        else if (value instanceof Long n)
            return (ComparableLiteral<V>) NumericLiteral.of(n);
        else if (value instanceof Integer n)
            return (ComparableLiteral<V>) NumericLiteral.of(n);
        else if (value instanceof Double n)
            return (ComparableLiteral<V>) NumericLiteral.of(n);
        else if (value instanceof Float n)
            return (ComparableLiteral<V>) NumericLiteral.of(n);
        else if (value instanceof BigDecimal n)
            return (ComparableLiteral<V>) NumericLiteral.of(n);
        else if (value instanceof BigInteger n)
            return (ComparableLiteral<V>) NumericLiteral.of(n);
        else if (value instanceof Byte n)
            return (ComparableLiteral<V>) NumericLiteral.of(n);
        else if (value instanceof Short n)
            return (ComparableLiteral<V>) NumericLiteral.of(n);
        else if (value instanceof Year y)
            return (ComparableLiteral<V>) TemporalLiteral.of(y);
        else if (value instanceof LocalDate l)
            return (ComparableLiteral<V>) TemporalLiteral.of(l);
        else if (value instanceof LocalDateTime l)
            return (ComparableLiteral<V>) TemporalLiteral.of(l);
        else if (value instanceof LocalTime l)
            return (ComparableLiteral<V>) TemporalLiteral.of(l);
        else if (value instanceof Instant i)
            return (ComparableLiteral<V>) TemporalLiteral.of(i);
        else
            return new ComparableLiteralRecord<>(value);
    }
}
