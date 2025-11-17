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
import jakarta.data.messages.Messages;
import jakarta.data.spi.expression.literal.Literal;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NotEqualTo<V> extends Constraint<V> {

    Expression<?, V> expression();

    static <V> NotEqualTo<V> expression(Expression<?, V> expression) {
        Messages.requireNonNull(expression, "expression");

        return new NotEqualToRecord<>(expression);
    }

    static <V> NotEqualTo<V> value(V value) {
        Messages.requireNonNull(value, "value");

        return new NotEqualToRecord<>(Literal.of(value));
    }

}
