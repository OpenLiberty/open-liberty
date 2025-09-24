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

import jakarta.data.expression.Expression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface Literal<V> extends Expression<Object, V> {

    V value();

    @SuppressWarnings("unchecked")
    static <V> Literal<V> of(V value) {
        if (value instanceof Comparable<?> comparable) {
            return (Literal<V>) ComparableLiteral.of(comparable);
        } else {
            return new LiteralRecord<>(value);
        }
    }

}
