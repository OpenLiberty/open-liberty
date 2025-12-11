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
import jakarta.data.messages.Messages;

record NumericFunctionExpressionRecord<T, N extends Number & Comparable<N>>(
                String name,
                List<ComparableExpression<? super T, ?>> arguments)
                implements NumericFunctionExpression<T, N> {

    NumericFunctionExpressionRecord {
        Messages.requireNonNull(name, "name");
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(name.length() +
                                            80 * arguments.size() +
                                            2);
        s.append(name).append('(');
        for (ComparableExpression<? super T, ?> exp : arguments) {
            if (!s.isEmpty())
                s.append(", ");
            s.append(exp);
        }
        s.append(')');

        return s.toString();
    }
}
