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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;

import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
record TemporalLiteralRecord<V extends Temporal & Comparable<? extends Temporal>>(V value)
                implements TemporalLiteral<V> {

    TemporalLiteralRecord {
        Messages.requireNonNull(value, "value");
    }

    @Override
    public String toString() {
        Temporal temporal;
        if (value instanceof Instant i)
            temporal = i.atOffset(ZoneOffset.UTC).toLocalDateTime();
        else
            temporal = value;

        if (temporal instanceof Year y)
            return "{d '" + y.getValue() + "'}";
        else if (temporal instanceof LocalDate)
            return "{d '" + value + "'}";
        else if (temporal instanceof LocalDateTime d)
            return "{ts '" + d.toLocalDate() + ' ' + d.toLocalTime() + "'}";
        else if (temporal instanceof LocalTime)
            return "{t '" + value + "'}";
        else
            return "{TemporalLiteral '" +
                   value.getClass().getName() + " '" +
                   value + "'}";
    }

}
