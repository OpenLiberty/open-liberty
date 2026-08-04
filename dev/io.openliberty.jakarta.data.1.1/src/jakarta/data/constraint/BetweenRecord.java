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
import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
record BetweenRecord<V extends Comparable<?>>(
                @Nonnull ComparableExpression<?, V> lowerBound,
                @Nonnull ComparableExpression<?, V> upperBound)
                implements Between<V> {

    public BetweenRecord {
        Messages.requireNonNull(lowerBound, "lower");

        Messages.requireNonNull(upperBound, "upper");
    }

    @Override
    @Nonnull
    public String toString() {
        return "BETWEEN " + lowerBound + " AND " + upperBound;
    }

    @Override
    @Nonnull
    public NotBetween<V> negate() {
        return NotBetween.bounds(lowerBound, upperBound);
    }
}
