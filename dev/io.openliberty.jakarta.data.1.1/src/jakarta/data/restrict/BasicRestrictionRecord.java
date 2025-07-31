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
package jakarta.data.restrict;

import java.util.Objects;

import jakarta.data.constraint.Constraint;
import jakarta.data.expression.Expression;
import jakarta.data.metamodel.Attribute;

/**
 * Method signatures are copied from Jakarta Data.
 */
record BasicRestrictionRecord<T, V>(Expression<T, V> expression,
                Constraint<V> constraint)
                implements BasicRestriction<T, V> {

    BasicRestrictionRecord {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(constraint, "constraint");
    }

    @Override
    public Restriction<T> negate() {
        return BasicRestriction.of(expression, constraint.negate());
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        if (expression instanceof Attribute<?> attribute)
            b.append(attribute.name());
        else
            b.append(expression.toString());

        b.append(' ');
        b.append(constraint);

        return b.toString();
    }
}
