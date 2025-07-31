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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Method signatures are copied from Jakarta Data.
 */
record NumericLiteralRecord<N extends Number & Comparable<N>>(N value)
                implements NumericLiteral<N> {

    NumericLiteralRecord {
        if (value == null)
            throw new NullPointerException();
    }

    @Override
    public String toString() {
        if (value instanceof Long l)
            return l + "L";
        else if (value instanceof Integer i)
            return i.toString();
        else if (value instanceof Double d)
            return d + "D";
        else if (value instanceof Float f)
            return f + "F";
        else if (value instanceof BigDecimal b)
            return b + "BD";
        else if (value instanceof BigInteger b)
            return b + "BI";
        else
            return "{NumericLiteral " +
                   value.getClass().getName() +
                   " '" + value + "'}";
    }
}
