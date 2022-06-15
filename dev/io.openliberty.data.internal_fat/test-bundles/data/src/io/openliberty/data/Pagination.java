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

import java.util.function.Function;

import io.openliberty.data.internal.PaginationImpl;

/**
 * Copied from jakarta.nosql.mapping.Pagination to investigate how well the
 * JNoSQL repository-related annotations work for relational database access.
 * Unable to copy the page method because it depends on a JNoSQL class, ServiceLoaderProvider.
 */
public interface Pagination {
    long getPageNumber();

    long getPageSize();

    long getLimit();

    long getSkip();

    Pagination next();

    Pagination unmodifiable();

    static PaginationBuilder page(long page) {
        // return ServiceLoaderProvider.get(PaginationBuilderProvider.class).apply(page);
        // We don't have the JNoSQL ServiceLoaderProvider here.
        return new PaginationImpl.Builder(page);
    }

    interface PaginationBuilder {
        Pagination size(long size);
    }

    interface PaginationBuilderProvider extends Function<Long, PaginationBuilder> {
    }
}
