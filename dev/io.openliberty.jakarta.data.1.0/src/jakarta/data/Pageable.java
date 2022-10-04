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

/**
 * Method signatures are copied from jakarta.data.repository.Pageable.
 */
public class Pageable {
    private final long pageNumber, pageSize;

    private Pageable(long pageNumber, long pageSize) {
        if (pageNumber < 1 || pageSize < 1)
            throw new IllegalArgumentException();
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public long getPage() {
        return pageNumber;
    }

    public long getSize() {
        return pageSize;
    }

    public Pageable next() {
        return new Pageable(pageNumber + 1, pageSize);
    }

    public static Pageable of(long page, long size) {
        return new Pageable(page, size);
    }

    public static Pageable page(long page) {
        return new Pageable(page, 10);
    }

    @Override
    public String toString() {
        return new StringBuilder("Pageable@").append(Integer.toHexString(hashCode())).append('#').append(pageNumber).append(" max page size ").append(pageSize).toString();
    }
}
