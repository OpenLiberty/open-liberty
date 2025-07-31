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
package jakarta.data.constraint;

import jakarta.data.expression.Expression;
import jakarta.data.spi.expression.literal.Literal;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface EqualTo<V> extends Constraint<V> {

    Expression<?, V> expression();

    static <V> EqualTo<V> expression(Expression<?, V> expression) {
        if (expression == null)
            throw new NullPointerException("expression");

        return new EqualToRecord<>(expression);
    }

    static <V> EqualTo<V> value(V value) {
        if (value == null)
            throw new NullPointerException("value");

        return new EqualToRecord<>(Literal.of(value));
    }

}
