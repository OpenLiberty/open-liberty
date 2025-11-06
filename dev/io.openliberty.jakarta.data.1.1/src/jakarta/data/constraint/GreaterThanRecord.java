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
record GreaterThanRecord<V extends Comparable<?>>(
                ComparableExpression<?, V> bound)
                implements GreaterThan<V> {

    public GreaterThanRecord {
        Messages.requireNonNull(bound, "lowerBound");
    }

    @Override
    public AtMost<V> negate() {
        return AtMost.max(bound);
    }

    @Override
    public String toString() {
        return "> " + bound.toString();
    }
}
