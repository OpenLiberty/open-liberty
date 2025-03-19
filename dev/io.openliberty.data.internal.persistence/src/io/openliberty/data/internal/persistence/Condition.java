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

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.exceptions.MappingException;

/**
 */
enum Condition {
    BETWEEN(null, 7, false),
    CONTAINS(null, 8, true),
    EMPTY(" IS EMPTY", 5, true),
    ENDS_WITH(null, 8, false),
    EQUALS("=", 0, true),
    FALSE("=FALSE", 5, false),
    GREATER_THAN(">", 11, false),
    GREATER_THAN_EQUAL(">=", 16, false),
    IN(" IN ", 2, false),
    LESS_THAN("<", 8, false),
    LESS_THAN_EQUAL("<=", 13, false),
    LIKE(null, 4, false),
    NOT_EMPTY(" IS NOT EMPTY", 8, true),
    NOT_EQUALS("<>", 3, true),
    NOT_NULL(" IS NOT NULL", 7, false),
    NULL(" IS NULL", 4, false),
    STARTS_WITH(null, 10, false),
    TRUE("=TRUE", 4, false);

    final int length;
    final String operator;
    final boolean supportsCollections;

    Condition(String operator, int length, boolean supportsCollections) {
        this.operator = operator;
        this.length = length;
        this.supportsCollections = supportsCollections;
    }

    Condition negate() {
        switch (this) {
            case EQUALS:
                return NOT_EQUALS;
            case GREATER_THAN:
                return LESS_THAN_EQUAL;
            case GREATER_THAN_EQUAL:
                return LESS_THAN;
            case LESS_THAN:
                return GREATER_THAN_EQUAL;
            case LESS_THAN_EQUAL:
                return GREATER_THAN;
            case NULL:
                return NOT_NULL;
            case TRUE:
                return FALSE;
            case FALSE:
                return TRUE;
            case EMPTY:
                return NOT_EMPTY;
            case NOT_EMPTY:
                return EMPTY;
            case NOT_EQUALS:
                return EQUALS;
            case NOT_NULL:
                return NULL;
            default:
                return null;
        }
    }

    /**
     * Confirm that collections are supported for this condition,
     * based on whether case insensitive comparison is requested.
     *
     * @param attributeName entity attribute to which the condition is to be applied.
     * @param ignoreCase    indicates if the condition is to be performed ignoring case.
     * @throws MappingException with chained UnsupportedOperationException if not supported.
     */
    @Trivial
    void verifyCollectionsSupported(String attributeName, boolean ignoreCase) {
        if (!supportsCollections || ignoreCase)
            throw new MappingException(new UnsupportedOperationException("Repository keyword " +
                                                                         (ignoreCase ? "IgnoreCase" : name()) +
                                                                         " which is applied to entity attribute " + attributeName +
                                                                         " is not supported for collection attributes.")); // TODO
    }
}
