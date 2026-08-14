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
public interface Between<V extends Comparable<?>> extends Constraint<V> {

    @Nonnull
    static <V extends Comparable<?>> Between<V> bounds(@Nonnull ComparableExpression<?, V> minimum,
                                                       @Nonnull ComparableExpression<?, V> maximum) {
        return new BetweenRecord<>(minimum, maximum);
    }

    @Nonnull
    static <V extends Comparable<?>> Between<V> bounds(@Nonnull ComparableExpression<?, V> minimum,
                                                       @Nonnull V maximum) {
        return new BetweenRecord<>(//
                        minimum, //
                        ComparableLiteral.of(maximum));
    }

    @Nonnull
    static <V extends Comparable<?>> Between<V> bounds(@Nonnull V minimum,
                                                       @Nonnull ComparableExpression<?, V> maximum) {
        return new BetweenRecord<>(//
                        ComparableLiteral.of(minimum), //
                        maximum);
    }

    @Nonnull
    static <V extends Comparable<?>> Between<V> bounds(@Nonnull V minimum,
                                                       @Nonnull V maximum) {
        return new BetweenRecord<>(//
                        ComparableLiteral.of(minimum), //
                        ComparableLiteral.of(maximum));
    }

    @Nonnull
    ComparableExpression<?, V> lowerBound();

    @Nonnull
    ComparableExpression<?, V> upperBound();
}
