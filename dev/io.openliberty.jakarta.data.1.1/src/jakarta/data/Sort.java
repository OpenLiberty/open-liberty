/*******************************************************************************
 * Copyright (c) 2022,2026 IBM Corporation and others.
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
package jakarta.data;

import jakarta.data.expression.ComparableExpression;
import jakarta.data.expression.TextExpression;
import jakarta.data.messages.Messages;
import jakarta.data.metamodel.Attribute;

/**
 * Method signatures copied from jakarta.data.Sort from the Jakarta Data repo.
 */
public record Sort<T>(
                ComparableExpression<T, ? extends Comparable<?>> expression,
                String property,
                boolean isAscending,
                boolean ignoreCase,
                Nulls nullOrdering) {

    public enum Nulls {
        FIRST,
        LAST,
        UNSPECIFIED
    }

    public Sort {
        if (expression == null && property == null)
            throw new NullPointerException(Messages.get("001.arg.required",
                                                        "expression"));

        Messages.requireNonNull(nullOrdering, "nullOrdering");

        if (expression != null) {
            if (property != null)
                throw new IllegalArgumentException("property: " + property +
                                                   ", expression: " + expression);

            if (expression instanceof Attribute<?> attribute)
                property = attribute.name();
        }
    }

    public Sort(String property, boolean isAscending, boolean ignoreCase) {
        this(null, //
             property, //
             isAscending, //
             ignoreCase, //
             Nulls.UNSPECIFIED);
    }

    public static <T, V extends Comparable<?>> Sort<T> //
                    asc(ComparableExpression<T, V> expression) {
        return new Sort<>(expression, null, true, false, Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> asc(String attribute) {
        return new Sort<>( //
                        null, //
                        attribute, //
                        true, //
                        false, //
                        Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> ascIgnoreCase(String attribute) {
        return new Sort<>( //
                        null, //
                        attribute, //
                        true, //
                        true, //
                        Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> ascIgnoreCase(TextExpression<T> expression) {
        return new Sort<>(expression, null, true, true, Nulls.UNSPECIFIED);
    }

    public static <T, V extends Comparable<?>> Sort<T> //
                    desc(ComparableExpression<T, V> expression) {
        return new Sort<>(expression, null, false, false, Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> desc(String attribute) {
        return new Sort<>( //
                        null, //
                        attribute, //
                        false, //
                        false, //
                        Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> descIgnoreCase(String attribute) {
        return new Sort<>( //
                        null, //
                        attribute, //
                        false, //
                        true, //
                        Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> descIgnoreCase(TextExpression<T> expression) {
        return new Sort<>(expression, null, false, true, Nulls.UNSPECIFIED);
    }

    public boolean isDescending() {
        return !isAscending;
    }

    public Sort<T> nullsFirst() {
        return new Sort<>( //
                        expression, //
                        expression == null ? property : null, //
                        isAscending, //
                        ignoreCase, //
                        Nulls.FIRST);
    }

    public Sort<T> nullsLast() {
        return new Sort<>( //
                        expression, //
                        expression == null ? property : null, //
                        isAscending, //
                        ignoreCase, //
                        Nulls.LAST);
    }

    public static <T> Sort<T> of(String attribute,
                                 Direction direction,
                                 boolean ignoreCase) {
        Messages.requireNonNull(direction, "direction");

        return new Sort<>( //
                        null, //
                        attribute, //
                        Direction.ASC.equals(direction), //
                        ignoreCase, //
                        Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> of(String attribute,
                                 Direction direction,
                                 boolean ignoreCase,
                                 Nulls nullOrdering) {
        Messages.requireNonNull(direction, "direction");

        return new Sort<>( //
                        null, //
                        attribute, //
                        Direction.ASC.equals(direction), //
                        ignoreCase, //
                        nullOrdering);
    }
}
