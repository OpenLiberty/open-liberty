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

import java.time.temporal.Temporal;

import jakarta.data.expression.NavigableExpression;
import jakarta.data.messages.Messages;
import jakarta.data.metamodel.TemporalAttribute;

record TemporalPathRecord<T, U, V extends Temporal & Comparable<? extends Temporal>>(
                NavigableExpression<T, U> expression,
                TemporalAttribute<U, V> attribute) implements TemporalPath<T, U, V> {

    TemporalPathRecord {
        Messages.requireNonNull(expression, "expression");
        Messages.requireNonNull(attribute, "attribute");
    }

    @Override
    public String toString() {
        String exp = expression.toString();
        String name = attribute.name();
        return new StringBuilder(exp.length() +
                                 name.length() +
                                 1) //
                                                 .append(exp) //
                                                 .append('.') //
                                                 .append(name)//
                                                 .toString();
    }
}
