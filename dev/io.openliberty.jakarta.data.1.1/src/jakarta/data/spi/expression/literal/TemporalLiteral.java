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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.temporal.Temporal;

import jakarta.annotation.Nonnull;
import jakarta.data.expression.TemporalExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface TemporalLiteral<V extends Temporal & Comparable<? extends Temporal>> //
                extends //
                ComparableLiteral<V>, //
                TemporalExpression<Object, V> {

    @Nonnull
    static <V extends Temporal & Comparable<? extends Temporal>> TemporalLiteral<V> //
                    of(@Nonnull Class<V> type, @Nonnull V value) {
        return new TemporalLiteralRecord<>(type, value);
    }

    @Nonnull
    static TemporalLiteral<Instant> of(@Nonnull Instant value) {
        return new TemporalLiteralRecord<>(Instant.class, value);
    }

    @Nonnull
    static TemporalLiteral<LocalDate> of(@Nonnull LocalDate value) {
        return new TemporalLiteralRecord<>(LocalDate.class, value);
    }

    @Nonnull
    static TemporalLiteral<LocalDateTime> of(@Nonnull LocalDateTime value) {
        return new TemporalLiteralRecord<>(LocalDateTime.class, value);
    }

    @Nonnull
    static TemporalLiteral<LocalTime> of(@Nonnull LocalTime value) {
        return new TemporalLiteralRecord<>(LocalTime.class, value);
    }

    @Nonnull
    static TemporalLiteral<Year> of(@Nonnull Year value) {
        return new TemporalLiteralRecord<>(Year.class, value);
    }

    @Override
    @Nonnull
    String toString();

}
