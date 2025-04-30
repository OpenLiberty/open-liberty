/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence;

import java.util.Set;
import java.util.TreeSet;

/**
 * Represents Query by Method Name condition keywords.
 */
enum Condition {
    Between(null, false),
    Contains(null, true),
    Empty(" IS EMPTY", true),
    EndsWith(null, false),
    Equal("=", true),
    False("=FALSE", false),
    GreaterThan(">", false),
    GreaterThanEqual(">=", false),
    IgnoreCase(null, false),
    In(" IN ", false),
    LessThan("<", false),
    LessThanEqual("<=", false),
    Like(null, false),
    Not("<>", true),
    NotEmpty(" IS NOT EMPTY", true),
    NotNull(" IS NOT NULL", false),
    Null(" IS NULL", false),
    StartsWith(null, false),
    True("=TRUE", false);

    /**
     * Length of the Query by Method Name keyword.
     */
    final int length;

    /**
     * Representation of the operator in query language.
     */
    final String operator;

    /**
     * Indicates if this type of condition is supported on collections.
     */
    final boolean supportsCollections;

    /**
     * Internal constructor for enumeration constants.
     *
     * @param operator            Representation of the operator in query language.
     * @param supportsCollections Indicates if this type of comparison is supported
     *                                on collections.
     */
    private Condition(String operator, boolean supportsCollections) {
        int len = name().length();
        this.operator = operator;
        this.length = len == 5 && name().equals("Equal") ? 0 : len;
        this.supportsCollections = supportsCollections;
    }

    /**
     * Returns the negated condition if possible.
     *
     * @return the negated comparison if possible. Otherwise null.
     */
    Condition negate() {
        switch (this) {
            case Equal:
                return Not;
            case GreaterThan:
                return LessThanEqual;
            case GreaterThanEqual:
                return LessThan;
            case LessThan:
                return GreaterThanEqual;
            case LessThanEqual:
                return GreaterThan;
            case Null:
                return NotNull;
            case True:
                return False;
            case False:
                return True;
            case Empty:
                return NotEmpty;
            case Not:
                return Equal;
            case NotEmpty:
                return Empty;
            case NotNull:
                return Null;
            default:
                return null;
        }
    }

    /**
     * Returns names of all conditions that are supported for collection attributes.
     * This is used in error reporting to display which keywords are valid.
     *
     * @return names of all conditions that are supported for collection attributes.
     */
    static Set<String> supportedForCollections() {
        Set<String> supported = new TreeSet<>();
        for (Condition c : Condition.values())
            if (c.supportsCollections && c.length > 0) {
                String name = c.name();
                supported.add(name);
                // Some negated forms of keywords do not have constants in this
                // enumeration, but they can be formed by combining with Not:
                if (c.negate() == null && !name.startsWith(Not.name()))
                    supported.add(Not.name() + name);
            }
        return supported;
    }
}
