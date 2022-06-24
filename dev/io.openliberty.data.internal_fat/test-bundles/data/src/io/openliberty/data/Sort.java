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

import java.util.function.BiFunction;

import io.openliberty.data.internal.SortImpl;

/**
 * Copied from jakarta.nosql.Sort to investigate how well the
 * JNoSQL repository-related classes work for relational database access.
 * Unable to copy the of method because it depends on a JNoSQL class, ServiceLoaderProvider.
 */
public interface Sort {
    String getName();

    SortType getType();

    static Sort of(String name, SortType type) {
        // We don't have the JNoSQL ServiceLoaderProvider here.
        return new SortImpl.Provider().apply(name, type);
    }

    static Sort asc(String name) {
        return of(name, SortType.ASC);
    }

    static Sort desc(String name) {
        return of(name, SortType.DESC);
    }

    interface SortProvider extends BiFunction<String, SortType, Sort> {
    }
}
