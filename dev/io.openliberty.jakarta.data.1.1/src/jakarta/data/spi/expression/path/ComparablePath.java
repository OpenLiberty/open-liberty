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

import jakarta.annotation.Nonnull;
import jakarta.data.expression.ComparableExpression;
import jakarta.data.expression.NavigableExpression;
import jakarta.data.metamodel.ComparableAttribute;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface ComparablePath<T, U, C extends Comparable<?>> extends //
                Path<T, U>, //
                ComparableExpression<T, C> {

    @Nonnull
    static <T, U, C extends Comparable<C>> ComparablePath<T, U, C> //
                    of(@Nonnull NavigableExpression<T, U> expression,
                       @Nonnull ComparableAttribute<U, C> attribute) {

        return new ComparablePathRecord<>(expression, attribute);
    }
}
