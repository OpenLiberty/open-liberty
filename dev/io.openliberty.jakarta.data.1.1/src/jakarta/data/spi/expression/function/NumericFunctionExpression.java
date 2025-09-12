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

import java.util.List;

import jakarta.data.expression.ComparableExpression;
import jakarta.data.expression.NumericExpression;
import jakarta.data.expression.TextExpression;
import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NumericFunctionExpression<T, N extends Number & Comparable<N>> //
                extends FunctionExpression<T, N>, NumericExpression<T, N> {

    String ABS = "abs";
    String LENGTH = "length";
    String NEG = "-";

    @Override
    List<? extends ComparableExpression<? super T, ?>> arguments();

    static <T, N extends Number & Comparable<N>> NumericFunctionExpression<T, N> //
                    of(String name,
                       NumericExpression<? super T, N> expression) {

        Messages.requireNonNull(expression, "expression");

        return new NumericFunctionExpressionRecord<>(name, List.of(expression));
    }

    static <T, N extends Number & Comparable<N>> NumericFunctionExpression<T, N> //
                    of(String name,
                       TextExpression<? super T> expression) {

        Messages.requireNonNull(expression, "expression");

        return new NumericFunctionExpressionRecord<>(name, List.of(expression));
    }

}
