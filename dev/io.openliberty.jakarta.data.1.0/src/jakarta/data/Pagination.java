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
package jakarta.data;

import java.util.function.Function;

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

/**
 * Fake implementation:
 */
class PaginationImpl implements Pagination {
    private final boolean allowNext;
    private final long pageNumber;
    private final long pageSize;

    public static class Builder implements PaginationBuilder {
        final long pageNumber;

        public Builder(long pageNumber) {
            this.pageNumber = pageNumber;
        }

        @Override
        public Pagination size(long size) {
            return new PaginationImpl(pageNumber, size, true);
        }
    }

    PaginationImpl(long pageNumber, long pageSize, boolean allowNext) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.allowNext = allowNext;
    }

    @Override
    public long getPageNumber() {
        return pageNumber;
    }

    @Override
    public long getPageSize() {
        return pageSize;
    }

    @Override
    public long getLimit() {
        return getPageSize();
    }

    @Override
    public long getSkip() {
        return (pageNumber - 1) * pageSize;
    }

    @Override
    public Pagination next() {
        if (!allowNext)
            throw new UnsupportedOperationException();
        return new PaginationImpl(pageNumber + 1, pageSize, true);
    }

    @Override
    public Pagination unmodifiable() {
        return new PaginationImpl(pageNumber, pageSize, false);
    }

    @Override
    public String toString() {
        return new StringBuilder("Pagination@").append(Integer.toHexString(hashCode())).append('#').append(pageNumber).append(" max page size ").append(pageSize).toString();
    }
}
