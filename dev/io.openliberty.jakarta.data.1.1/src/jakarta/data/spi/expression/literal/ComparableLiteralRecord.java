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
package jakarta.data.spi.expression.literal;

import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
record ComparableLiteralRecord<T, V extends Comparable<?>>(V value)
                implements ComparableLiteral<V> {

    ComparableLiteralRecord {
        Messages.requireNonNull(value, "value");
    }

    @Override
    public String toString() {
        if (value instanceof Enum e)
            return e.getClass().getName() + '.' + e.name();
        if (value instanceof Character c)
            return c == '\'' ? "''''" : ("'" + c + "'");
        if (value instanceof Boolean b)
            return b.toString().toUpperCase();
        else
            return "{ComparableLiteral " +
                   value.getClass().getName() +
                   " '" + value + "'}";
    }

}
