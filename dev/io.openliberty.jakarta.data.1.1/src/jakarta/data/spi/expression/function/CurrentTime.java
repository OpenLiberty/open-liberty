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

import java.time.LocalTime;

import jakarta.data.expression.TemporalExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface CurrentTime<T> extends TemporalExpression<T, LocalTime> {

    @SuppressWarnings("unchecked")
    static <T> CurrentTime<T> now() {
        return (CurrentTime<T>) CurrentTimeImpl.INSTANCE;
    }
}

class CurrentTimeImpl<T> implements CurrentTime<T> {
    static final CurrentTime<?> INSTANCE = new CurrentTimeImpl<>();

    @Override
    public String toString() {
        return "LOCAL TIME";
    }
}
