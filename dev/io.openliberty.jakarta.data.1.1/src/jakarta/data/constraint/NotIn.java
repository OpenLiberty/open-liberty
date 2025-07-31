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

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.data.expression.Expression;
import jakarta.data.spi.expression.literal.Literal;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NotIn<V> extends Constraint<V> {

    List<Expression<?, V>> expressions();

    @SafeVarargs
    static <V> NotIn<V> expressions(Expression<?, V>... expressions) {
        if (expressions == null)
            throw new NullPointerException("expressions");

        if (expressions.length == 0)
            throw new IllegalArgumentException("expressions");

        for (Expression<?, V> expression : expressions) {
            if (expression == null)
                throw new NullPointerException();
        }

        return new NotInRecord<>(List.of(expressions));
    }

    static <V> NotIn<V> expressions(List<Expression<?, V>> expressions) {
        if (expressions == null)
            throw new NullPointerException("expressions");

        if (expressions.isEmpty())
            throw new IllegalArgumentException("expressions");

        for (Expression<?, V> expression : expressions) {
            if (expression == null)
                throw new NullPointerException();
        }

        return new NotInRecord<>(List.copyOf(expressions));
    }

    static <V> NotIn<V> values(Collection<V> values) {
        if (values == null)
            throw new NullPointerException("values");

        if (values.isEmpty())
            throw new IllegalArgumentException("values");

        final List<Expression<?, V>> expressions = new ArrayList<>(values.size());
        for (V value : values) {
            if (value == null)
                throw new NullPointerException();

            expressions.add(Literal.of(value));
        }

        return new NotInRecord<>(unmodifiableList(expressions));
    }

    @SafeVarargs
    static <V> NotIn<V> values(V... values) {
        if (values == null)
            throw new NullPointerException("values");

        if (values.length == 0)
            throw new NullPointerException("values");

        final List<Expression<?, V>> expressions = new ArrayList<>(values.length);
        for (V value : values) {
            if (value == null)
                throw new NullPointerException();

            expressions.add(Literal.of(value));
        }

        return new NotInRecord<>(unmodifiableList(expressions));
    }

}
