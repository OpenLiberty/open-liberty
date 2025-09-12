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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;

import jakarta.data.spi.expression.function.CurrentDate;
import jakarta.data.spi.expression.function.CurrentDateTime;
import jakarta.data.spi.expression.function.CurrentTime;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface TemporalExpression//
/*           */ <T, V extends Temporal & Comparable<? extends Temporal>> //
                extends ComparableExpression<T, V> {

    static TemporalExpression<Object, LocalDate> localDate() {
        return CurrentDate.now();
    }

    static TemporalExpression<Object, LocalDateTime> localDateTime() {
        return CurrentDateTime.now();
    }

    static TemporalExpression<Object, LocalTime> localTime() {
        return CurrentTime.now();
    }
}
