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
package jakarta.data.spi.expression.function;

import java.time.LocalDate;

import jakarta.annotation.Nonnull;
import jakarta.data.expression.TemporalExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface CurrentDate<T> extends TemporalExpression<T, LocalDate> {

    @Nonnull
    @SuppressWarnings("unchecked")
    static <T> CurrentDate<T> now() {
        return (CurrentDate<T>) CurrentDateInstance.instance;
    }
}

class CurrentDateInstance implements CurrentDate<Object> {
    @Nonnull
    static final CurrentDate<?> instance = new CurrentDateInstance();

    private CurrentDateInstance() {
    }

    @Override
    @Nonnull
    public String toString() {
        return "LOCAL DATE";
    }

    @Override
    @Nonnull
    public Class<LocalDate> type() {
        return LocalDate.class;
    }
}
