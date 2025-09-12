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
package jakarta.data.expression;

import java.util.Collection;

import jakarta.data.constraint.Constraint;
import jakarta.data.constraint.EqualTo;
import jakarta.data.constraint.In;
import jakarta.data.constraint.NotEqualTo;
import jakarta.data.constraint.NotIn;
import jakarta.data.constraint.NotNull;
import jakarta.data.constraint.Null;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.Restriction;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface Expression<T, V> {

    default Restriction<T> equalTo(Expression<? super T, V> expression) {
        Constraint<V> constraint = EqualTo.expression(expression);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> equalTo(V value) {
        Constraint<V> constraint = EqualTo.value(value);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> in(Collection<V> values) {
        Constraint<V> constraint = In.values(values);
        return BasicRestriction.of(this, constraint);
    }

    @SuppressWarnings("unchecked")
    default Restriction<T> in(Expression<? super T, V>... expressions) {
        Constraint<V> constraint = In.expressions(expressions);
        return BasicRestriction.of(this, constraint);
    }

    @SuppressWarnings("unchecked")
    default Restriction<T> in(V... values) {
        Constraint<V> constraint = In.values(values);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> isNull() {
        Constraint<V> constraint = Null.instance();
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notEqualTo(Expression<? super T, V> expression) {
        Constraint<V> constraint = NotEqualTo.expression(expression);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notEqualTo(V value) {
        Constraint<V> constraint = NotEqualTo.value(value);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notIn(Collection<V> values) {
        Constraint<V> constraint = NotIn.values(values);
        return BasicRestriction.of(this, constraint);
    }

    @SuppressWarnings("unchecked")
    default Restriction<T> notIn(Expression<? super T, V>... expressions) {
        Constraint<V> constraint = NotIn.expressions(expressions);
        return BasicRestriction.of(this, constraint);
    }

    @SuppressWarnings("unchecked")
    default Restriction<T> notIn(V... values) {
        Constraint<V> constraint = NotIn.values(values);
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> notNull() {
        Constraint<V> constraint = NotNull.instance();
        return BasicRestriction.of(this, constraint);
    }

    default Restriction<T> satisfies(Constraint<V> constraint) {
        return BasicRestriction.of(this, constraint);
    }
}
