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
package jakarta.data.constraint;

import jakarta.annotation.Nonnull;
import jakarta.data.expression.ComparableExpression;
import jakarta.data.spi.expression.literal.ComparableLiteral;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface GreaterThan<V extends Comparable<?>> extends Constraint<V> {

    @Nonnull
    ComparableExpression<?, V> bound();

    @Nonnull
    static <V extends Comparable<?>> GreaterThan<V> bound(@Nonnull ComparableExpression<?, V> exp) {
        return new GreaterThanRecord<>(exp);
    }

    @Nonnull
    static <V extends Comparable<?>> GreaterThan<V> bound(@Nonnull V lowerBound) {
        return new GreaterThanRecord<>(ComparableLiteral.of(lowerBound));
    }

}
