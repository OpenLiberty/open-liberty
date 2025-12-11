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
import jakarta.data.spi.expression.literal.NumericLiteral;

record TextFunctionExpressionRecord<T>(
                String name,
                List<ComparableExpression<? super T, ?>> arguments)
                implements TextFunctionExpression<T> {

    TextFunctionExpressionRecord {
        Messages.requireNonNull(name, "name");

        if (TextFunctionExpression.RIGHT == name ||
            TextFunctionExpression.LEFT == name) {
            ComparableExpression<? super T, ?> exp = arguments.get(1);
            if (exp instanceof NumericLiteral lit &&
                lit.value() instanceof Integer size &&
                size < 0) {
                throw new IllegalArgumentException(Messages.get("004.arg.negative",
                                                                "length"));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(name.length() +
                                            arguments.size() * 80 +
                                            2);
        s.append(name).append('(');

        for (ComparableExpression<?, ?> exp : arguments) {
            if (!s.isEmpty())
                s.append(", ");
            s.append(exp);
        }

        s.append(')');

        return s.toString();
    }
}
