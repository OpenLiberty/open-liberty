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
package io.openliberty.data.internal.persistence;

/**
 * Instructions for editing a query that is written in query language.
 */
enum QueryEdit {
    /**
     * Instruction to add NEW fully.qualified.ClassName( to the SELECT clause
     * at the specified index. This instruction is always paired with the
     * ADD_CONSTRUCTOR_END instruction.
     */
    ADD_CONSTRUCTOR_BEGIN,

    // TODO 1.1 use Jakarta Persistence enhancement issue 420 instead of
    // editing the query to enclose the SELECT clause content in
    // NEW fully.qualified.ClassName(...)

    /**
     * Instruction to add a closing parenthesis ) for an added constructor after
     * the specified index. This instruction is always paired with the
     * ADD_CONSTRUCTOR_BEGIN instruction.
     */
    ADD_CONSTRUCTOR_END,

    /**
     * Instruction to add a FROM clause to the query. The FROM clause is added
     * after the first SELECT clause, or, if there is no SELECT clause, then
     * at the beginning of the query.
     */
    ADD_FROM,

    /**
     * Instruction to add ( to the WHERE clause at the specified index.
     * This instruction is always paired with the ADD_PARENTHESIS_END
     * instruction.
     */
    ADD_PARENTHESIS_BEGIN,

    /**
     * Instruction to add ) to the WHERE clause at the specified index.
     * This instruction is always paired with the ADD_PARENTHESIS_BEGIN
     * instruction.
     */
    ADD_PARENTHESIS_END,

    /**
     * Instruction to add a SELECT clause to the query if it is determined that
     * one is needed.
     */
    ADD_SELECT_IF_NEEDED,

    /**
     * Instruction to omit the ORDER BY clause when generating a count query.
     * The key for this instruction is a negative value (to avoid collision)
     * of which the absolute value points to the position after the
     * ORDER keyword.
     */
    OMIT_ORDER_IN_COUNT,

    /**
     * Instruction to omit the SELECT clause when generating a count query.
     * The key for this instruction is a negative value (to avoid collision)
     * of which the absolute value points to the position after the end of the
     * SELECT clause.
     */
    OMIT_SELECT_IN_COUNT,

    /**
     * Instruction to replace record names with the generated entity class name
     * when the record name appears after the FROM keyword.
     */
    REPLACE_RECORD_ENTITY;

    /**
     * Indicates the position before the beginning of the query, which applies
     * when inserting a SELECT clause. A negative value is used to ensure that
     * an added SELECT clause is positioned before an added FROM clause at
     * position 0.
     */
    static final int BEFORE_QUERY = -1;
}
