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

import jakarta.data.messages.Messages;

/**
 * Method signatures copied from jakarta.data.Sort from the Jakarta Data repo.
 */
public record Sort<T>(String property,
                boolean isAscending,
                boolean ignoreCase,
                Nulls nullOrdering) {

    public enum Nulls {
        FIRST,
        LAST,
        UNSPECIFIED
    }

    public Sort {
        Messages.requireNonNull(property, "property");
        Messages.requireNonNull(nullOrdering, "nullOrdering");
    }

    public Sort(String property, boolean isAscending, boolean ignoreCase) {
        this(property, //
             isAscending, //
             ignoreCase, //
             Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> asc(String attribute) {
        return new Sort<>(attribute, //
                        true, //
                        false, //
                        Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> ascIgnoreCase(String attribute) {
        return new Sort<>(attribute, //
                        true, //
                        true, //
                        Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> desc(String attribute) {
        return new Sort<>(attribute, //
                        false, //
                        false, //
                        Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> descIgnoreCase(String attribute) {
        return new Sort<>(attribute, //
                        false, //
                        true, //
                        Nulls.UNSPECIFIED);
    }

    public boolean isDescending() {
        return !isAscending;
    }

    public Sort<T> nullsFirst() {
        return new Sort<>(property, //
                        isAscending, //
                        ignoreCase, //
                        Nulls.FIRST);
    }

    public Sort<T> nullsLast() {
        return new Sort<>(property, //
                        isAscending, //
                        ignoreCase, //
                        Nulls.LAST);
    }

    public static <T> Sort<T> of(String attribute,
                                 Direction direction,
                                 boolean ignoreCase) {
        Messages.requireNonNull(direction, "direction");

        return new Sort<>(attribute, //
                        Direction.ASC.equals(direction), //
                        ignoreCase, //
                        Nulls.UNSPECIFIED);
    }

    public static <T> Sort<T> of(String attribute,
                                 Direction direction,
                                 boolean ignoreCase,
                                 Nulls nullOrdering) {
        Messages.requireNonNull(direction, "direction");

        return new Sort<>(attribute, //
                        Direction.ASC.equals(direction), //
                        ignoreCase, //
                        nullOrdering);
    }
}
