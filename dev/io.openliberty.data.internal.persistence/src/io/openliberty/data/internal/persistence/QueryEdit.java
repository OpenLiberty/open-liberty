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
     * Instruction to add a closing parenthesis ) for an added constructor after
     * the specified index. This instruction is always paired with the
     * ADD_CONSTRUCTOR_START instruction.
     */
    ADD_CONSTRUCTOR_END,

    // TODO 1.1 use Jakarta Persistence enhancement issue 420 instead of
    // editing the query to enclose the SELECT clause content in
    // NEW fully.qualified.ClassName(...)

    /**
     * Instruction to add NEW fully.qualified.ClassName( to the SELECT clause
     * at the specified index. This instruction is always paired with the
     * ADD_CONSTRUCTOR_END instruction.
     */
    ADD_CONSTRUCTOR_START,

    /**
     * Instruction to add a FROM clause to the query. The FROM clause is added
     * after the first SELECT clause, or, if there is no SELECT clause, then
     * at the beginning of the query.
     */
    ADD_FROM,

    /**
     * Instruction to add a SELECT clause to the query if it is determined that
     * one is needed.
     */
    ADD_SELECT_IF_NEEDED,

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
