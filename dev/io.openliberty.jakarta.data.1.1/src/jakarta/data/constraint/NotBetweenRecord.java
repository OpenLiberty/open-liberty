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

import jakarta.data.expression.ComparableExpression;
import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
record NotBetweenRecord<V extends Comparable<?>>(
                ComparableExpression<?, V> lowerBound,
                ComparableExpression<?, V> upperBound)
                implements NotBetween<V> {

    NotBetweenRecord {
        Messages.requireNonNull(lowerBound, "lower");

        Messages.requireNonNull(upperBound, "upper");
    }

    @Override
    public Between<V> negate() {
        return Between.bounds(lowerBound, upperBound);
    }

    @Override
    public String toString() {
        return "NOT BETWEEN " + lowerBound + " AND " + upperBound;
    }
}
