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
import jakarta.data.messages.Messages;
import jakarta.data.spi.expression.literal.Literal;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface In<V> extends Constraint<V> {

    List<Expression<?, V>> expressions();

    static <V> In<V> values(Collection<V> values) {

        Messages.requireNonNull(values, "values");

        if (values.isEmpty())
            throw new IllegalArgumentException(Messages.get("002.no.elements",
                                                            "values"));

        List<Expression<?, V>> expressions = new ArrayList<>(values.size());
        for (V value : values) {
            if (value == null)
                throw new NullPointerException(Messages.get("003.null.element",
                                                            "values"));

            expressions.add(Literal.of(value));
        }

        return new InRecord<>(unmodifiableList(expressions));
    }

    @SafeVarargs
    static <V> In<V> expressions(Expression<?, V>... expressions) {

        Messages.requireNonNull(expressions, "expressions");

        if (expressions.length == 0)
            throw new IllegalArgumentException(Messages.get("002.no.elements",
                                                            "expressions"));

        for (Expression<?, V> expression : expressions) {
            if (expression == null)
                throw new NullPointerException(Messages.get("003.null.element",
                                                            "expressions"));
        }

        return new InRecord<>(List.of(expressions));
    }

    static <V> In<V> expressions(List<Expression<?, V>> expressions) {

        Messages.requireNonNull(expressions, "expressions");

        if (expressions.isEmpty())
            throw new IllegalArgumentException(Messages.get("002.no.elements",
                                                            "expressions"));

        for (Expression<?, V> expression : expressions) {
            if (expression == null)
                throw new NullPointerException(Messages.get("003.null.element",
                                                            "expressions"));
        }

        return new InRecord<>(List.copyOf(expressions));
    }

    @SafeVarargs
    static <V> In<V> values(V... values) {
        Messages.requireNonNull(values, "values");

        if (values.length == 0)
            throw new IllegalArgumentException(Messages.get("002.no.elements",
                                                            "values"));

        List<Expression<?, V>> expressions = new ArrayList<>(values.length);
        for (V value : values) {
            if (value == null)
                throw new NullPointerException(Messages.get("003.null.element",
                                                            "values"));

            expressions.add(Literal.of(value));
        }

        return new InRecord<>(unmodifiableList(expressions));
    }

}
