/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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

import jakarta.annotation.Nonnull;
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

    @Nonnull
    default Restriction<T> equalTo(@Nonnull Expression<? super T, V> expression) {
        Constraint<V> constraint = EqualTo.expression(expression);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> equalTo(@Nonnull V value) {
        Constraint<V> constraint = EqualTo.value(value);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> in(@Nonnull Collection<V> values) {
        Constraint<V> constraint = In.values(values);
        return BasicRestriction.of(this, constraint);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    default Restriction<T> in(@Nonnull Expression<? super T, V>... expressions) {
        Constraint<V> constraint = In.expressions(expressions);
        return BasicRestriction.of(this, constraint);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    default Restriction<T> in(@Nonnull V... values) {
        Constraint<V> constraint = In.values(values);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> isNull() {
        Constraint<V> constraint = Null.instance();
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notEqualTo(@Nonnull Expression<? super T, V> expression) {
        Constraint<V> constraint = NotEqualTo.expression(expression);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notEqualTo(@Nonnull V value) {
        Constraint<V> constraint = NotEqualTo.value(value);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notIn(@Nonnull Collection<V> values) {
        Constraint<V> constraint = NotIn.values(values);
        return BasicRestriction.of(this, constraint);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    default Restriction<T> notIn(@Nonnull Expression<? super T, V>... expressions) {
        Constraint<V> constraint = NotIn.expressions(expressions);
        return BasicRestriction.of(this, constraint);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    default Restriction<T> notIn(@Nonnull V... values) {
        Constraint<V> constraint = NotIn.values(values);
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> notNull() {
        Constraint<V> constraint = NotNull.instance();
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    default Restriction<T> satisfies(@Nonnull Constraint<V> constraint) {
        return BasicRestriction.of(this, constraint);
    }

    @Nonnull
    Class<? extends V> type();
}
