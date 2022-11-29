/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.persistence;

/**
 */
enum Condition {
    BETWEEN(null, 7),
    CONTAINS(null, 8),
    ENDS_WITH(null, 8),
    EQUALS("=", 0),
    FALSE("=FALSE", 5),
    GREATER_THAN(">", 11),
    GREATER_THAN_EQUAL(">=", 16),
    IN(" IN ", 2),
    LESS_THAN("<", 8),
    LESS_THAN_EQUAL("<=", 13),
    LIKE(null, 4),
    NOT_EQUALS("<>", 3),
    NOT_NULL(" IS NOT NULL", 7),
    NULL(" IS NULL", 4),
    STARTS_WITH(null, 10),
    TRUE("=TRUE", 4);

    final int length;
    final String operator;

    Condition(String operator, int length) {
        this.operator = operator;
        this.length = length;
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
            case NOT_EQUALS:
                return EQUALS;
            case NOT_NULL:
                return NULL;
            default:
                return null;
        }
    }
}
