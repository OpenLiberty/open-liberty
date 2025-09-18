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
import jakarta.data.messages.Messages;

record NumericCastRecord<T, N extends Number & Comparable<N>>(
                NumericExpression<T, ?> expression,
                Class<N> type)
                implements NumericCast<T, N> {

    NumericCastRecord {
        Messages.requireNonNull(expression, "expression");
        Messages.requireNonNull(expression, "type");
    }

    @Override
    public String toString() {
        String exp = expression.toString();
        String className = type.getSimpleName();

        return new StringBuilder(exp.length() +
                                 className.length() +
                                 10) //
                                                 .append("CAST(") //
                                                 .append(exp) //
                                                 .append(" AS ") //
                                                 .append(className.toUpperCase()) //
                                                 .append(')') //
                                                 .toString();
    }
}
