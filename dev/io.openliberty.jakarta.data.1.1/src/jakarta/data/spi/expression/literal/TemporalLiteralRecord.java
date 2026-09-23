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
import java.time.ZoneOffset;
import java.time.temporal.Temporal;

import jakarta.annotation.Nonnull;
import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
record TemporalLiteralRecord<V extends Temporal & Comparable<? extends Temporal>>(
                @Nonnull Class<V> type,
                @Nonnull V value)
                implements TemporalLiteral<V> {

    TemporalLiteralRecord {
        Messages.requireNonNull(type, "type");
        Messages.requireNonNull(value, "value");
    }

    @Override
    @Nonnull
    public String toString() {
        Temporal temporal;
        if (value instanceof Instant i)
            temporal = i.atOffset(ZoneOffset.UTC).toLocalDateTime();
        else
            temporal = value;

        if (temporal instanceof Year year)
            return "YEAR " + year.getValue();
        else if (temporal instanceof LocalDate date)
            return "DATE " + toString(date);
        else if (temporal instanceof LocalDateTime ldt)
            return "DATETIME " +
                   toString(ldt.toLocalDate()) + ' ' +
                   ldt.toLocalTime().toString();
        else if (temporal instanceof LocalTime)
            return "TIME " + temporal.toString();
        else
            return "TEMPORAL " + temporal.getClass().getName() + " " + temporal;
    }

    private static String toString(LocalDate date) {
        return date.getYear() > 9999 //
                        ? date.toString().substring(1) // first char (+) is unwanted
                        : date.toString();
    }
}
