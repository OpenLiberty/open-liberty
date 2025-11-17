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
package io.openliberty.data.internal;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Types of constraints that can be imposed on an entity attribute. The names
 * used in this enumeration match the respective Query by Method Name keywords.
 */
@Trivial
public enum AttributeConstraint {
    Between(" BETWEEN ", 2, false),
    Contains(null, 1, Supports.COLLECTIONS),
    Empty(" IS EMPTY", 0, Supports.COLLECTIONS),
    EndsWith(null, 1, false),
    Equal("=", 1, Supports.COLLECTIONS, "EqualTo"),
    False("=FALSE", 0, false),
    GreaterThan(">", 1, false),
    GreaterThanEqual(">=", 1, false, "AtLeast"),
    IgnoreCase(null, 1, false),
    In(" IN ", 1, false),
    LessThan("<", 1, false),
    LessThanEqual("<=", 1, false, "AtMost"),
    Like(" LIKE ", 1, false),
    LikeEscaped(" LIKE ", 2, false),
    Not("<>", 1, Supports.COLLECTIONS, "NotEqualTo"),
    NotBetween(" NOT BETWEEN ", 2, false),
    NotContains(null, 1, Supports.COLLECTIONS),
    NotEmpty(" IS NOT EMPTY", 0, Supports.COLLECTIONS),
    NotEndsWith(null, 1, false),
    NotIn(" NOT IN ", 1, false),
    NotLike(" NOT LIKE ", 1, false),
    NotLikeEscaped(" NOT LIKE ", 2, false),
    NotNull(" IS NOT NULL", 0, false),
    NotStartsWith(null, 1, false),
    Null(" IS NULL", 0, false),
    StartsWith(null, 1, false),
    True("=TRUE", 0, false);

    /**
     * Name when used as a Constraint subtype.
     */
    private final String constraintName;

    /**
     * Indicates if the constraint is a negative condition, such as NotLike.
     */
    private final boolean isNegative;

    /**
     * Length of the Query by Method Name keyword.
     */
    private final int length;

    /**
     * Number of repository method parameters needed by this type of constraint.
     */
    private final int numMethodParams;

    /**
     * Representation of the operator in query language.
     */
    private final String operator;

    /**
     * Indicates if this type of condition is supported on collections.
     */
    private final boolean supportsCollections;

    /**
     * Internal constructor for enumeration constants.
     *
     * @param operator            Representation of the operator in query language.
     * @param numMethodParams     Number of method parameters required for this
     *                                type of constraint.
     * @param supportsCollections Indicates if this type of comparison is
     *                                supported on collections.
     */
    private AttributeConstraint(String operator,
                                int numMethodParams,
                                boolean supportsCollections) {
        String name = name();
        int len = name.length();
        this.constraintName = name;
        this.length = len == 5 && name.equals("Equal") ? 0 : len;
        this.isNegative = name.startsWith("Not");
        this.numMethodParams = numMethodParams;
        this.operator = operator;
        this.supportsCollections = supportsCollections;
    }

    /**
     * Internal constructor for enumeration constants.
     *
     * @param numMethodParams     Number of method parameters required for this
     *                                type of constraint.
     * @param operator            Representation of the operator in query language.
     * @param supportsCollections Indicates if this type of comparison is
     *                                supported on collections.
     * @param constraintAlias     Different name that is used when a Constraint
     *                                subtype.
     */
    private AttributeConstraint(String operator,
                                int numMethodParams,
                                boolean supportsCollections,
                                String constraintAlias) {
        String name = name();
        int len = name.length();
        this.constraintName = constraintAlias == null ? name : constraintAlias;
        this.length = len == 5 && name.equals("Equal") ? 0 : len;
        this.isNegative = name.startsWith("Not");
        this.numMethodParams = numMethodParams;
        this.operator = operator;
        this.supportsCollections = supportsCollections;
    }

    /**
     * Name of this constraint when used as a parameter type or Is annotation value
     * in a parameter-based repository method rather than within a method name query.
     *
     * @return the constraint name.
     */
    public final String constraintName() {
        return constraintName;
    }

    /**
     * Indicates if the constraint is a negative condition, such as NotLike.
     *
     * @return true if a negative condition, otherwise false.
     */
    public final boolean isNegative() {
        return isNegative;
    }

    /**
     * Length of the Query by Method Name keyword.
     */
    public final int lengthWithinMethodName() {
        return length;
    }

    /**
     * Returns the negated condition.
     *
     * @return the negated comparison.
     * @throws UnsupportedOperationException for IgnoreCase.
     */
    public final AttributeConstraint negate() {
        return switch (this) {
            case Between -> NotBetween;
            case Contains -> NotContains;
            case EndsWith -> NotEndsWith;
            case Empty -> NotEmpty;
            case Equal -> Not;
            case False -> True;
            case GreaterThan -> LessThanEqual;
            case GreaterThanEqual -> LessThan;
            case IgnoreCase -> throw new UnsupportedOperationException();
            case In -> NotIn;
            case LessThan -> GreaterThanEqual;
            case LessThanEqual -> GreaterThan;
            case Like -> NotLike;
            case LikeEscaped -> NotLikeEscaped;
            case Not -> Equal;
            case NotBetween -> Between;
            case NotContains -> Contains;
            case NotEmpty -> Empty;
            case NotEndsWith -> EndsWith;
            case NotIn -> In;
            case NotLike -> Like;
            case NotLikeEscaped -> LikeEscaped;
            case NotNull -> Null;
            case NotStartsWith -> StartsWith;
            case Null -> NotNull;
            case StartsWith -> NotStartsWith;
            case True -> False;
        };
    }

    /**
     * Number of repository method parameters needed by this type of constraint.
     *
     * @return the number of method parameters needed.
     */
    public final int numMethodParams() {
        return numMethodParams;
    }

    /**
     * Representation of the operator in query language.
     * This method returns null if the operator is not representable in a
     * continuous string of characters.
     *
     * @return a representation of the operator in query language.
     */
    public final String operator() {
        return operator;
    }

    /**
     * Indicates if this type of condition is supported on collections.
     *
     * @return true if supported, otherwise false;
     */
    public final boolean supportsCollections() {
        return supportsCollections;
    }
}

// Internal class that helps with readability of enumeration constructors
class Supports {
    static final boolean COLLECTIONS = true;
}