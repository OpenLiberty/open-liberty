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

import jakarta.data.expression.NavigableExpression;
import jakarta.data.expression.NumericExpression;
import jakarta.data.metamodel.NumericAttribute;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NumericPath<T, U, N extends Number & Comparable<N>> extends //
                Path<T, U>, //
                NumericExpression<T, N> {

    static <T, U, N extends Number & Comparable<N>> NumericPath<T, U, N> //
                    of(NavigableExpression<T, U> expression,
                       NumericAttribute<U, N> attribute) {

        return new NumericPathRecord<>(expression, attribute);
    }
}
