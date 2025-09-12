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
package jakarta.data.spi.expression.function;

import jakarta.data.expression.NumericExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NumericCast<T, N extends Number & Comparable<N>> //
                extends NumericExpression<T, N> {

    NumericExpression<T, ?> expression();

    static <T, N extends Number & Comparable<N>> NumericCast<T, N> //
                    of(NumericExpression<T, ?> expression, Class<N> type) {

        return new NumericCastRecord<>(expression, type);
    }

    Class<N> type();
}
