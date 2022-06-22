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
package io.openliberty.data;

/**
 * Copied from jakarta.nosql.SortType to investigate how well the
 * JNoSQL repository-related classes work for relational database access.
 */
public enum SortType {
    ASC,
    DESC
}
