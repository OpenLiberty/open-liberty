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

import java.time.LocalDateTime;

import jakarta.data.expression.TemporalExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface CurrentDateTime<T> extends TemporalExpression<T, LocalDateTime> {

    @SuppressWarnings("unchecked")
    static <T> CurrentDateTime<T> now() {
        return (CurrentDateTime<T>) CurrentDateTimeImpl.INSTANCE;
    }
}

class CurrentDateTimeImpl<T> implements CurrentDateTime<T> {
    static final CurrentDateTime<?> INSTANCE = new CurrentDateTimeImpl<>();

    @Override
    public String toString() {
        return "LOCAL DATETIME";
    }
}
