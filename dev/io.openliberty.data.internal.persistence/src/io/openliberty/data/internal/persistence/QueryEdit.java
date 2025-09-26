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
     * Instruction to add a FROM clause to the query. The FROM clause is added
     * after the first SELECT clause, or, if there is no SELECT clause, then
     * at the beginning of the query.
     */
    ADD_FROM,

    /**
     * Instruction to replace record names with the generated entity class name
     * when the record name appears after the FROM keyword.
     */
    REPLACE_RECORD_ENTITY
}
