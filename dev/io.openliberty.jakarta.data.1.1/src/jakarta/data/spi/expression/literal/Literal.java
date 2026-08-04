/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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

import jakarta.annotation.Nonnull;
import jakarta.data.expression.Expression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface Literal<V> extends Expression<Object, V> {

    @Nonnull
    V value();

    @Nonnull
    @SuppressWarnings("unchecked")
    static <V> Literal<V> of(@Nonnull V value) {
        if (value instanceof Comparable<?> comparable) {
            return (Literal<V>) ComparableLiteral.of(comparable);
        } else {
            return new LiteralRecord<>( //
                            (Class<? extends V>) value.getClass(), //
                            value);
        }
    }

    @Override
    @Nonnull
    String toString();
}
