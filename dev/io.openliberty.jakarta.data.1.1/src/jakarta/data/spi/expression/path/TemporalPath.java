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
package jakarta.data.spi.expression.path;

import java.time.temporal.Temporal;

import jakarta.data.expression.NavigableExpression;
import jakarta.data.expression.TemporalExpression;
import jakarta.data.metamodel.TemporalAttribute;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface TemporalPath< //
                T, //
                U, //
                V extends Temporal & Comparable<? extends Temporal>> //
                extends //
                Path<T, U>, //
                TemporalExpression<T, V> {

    static <T, U, V extends Temporal & Comparable<? extends Temporal>> //
    TemporalPath<T, U, V> of(NavigableExpression<T, U> expression,
                             TemporalAttribute<U, V> attribute) {

        return new TemporalPathRecord<>(expression, attribute);
    }
}
