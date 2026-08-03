/*******************************************************************************
 * Copyright (c) 2022,2026 IBM Corporation and others.
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
package jakarta.data.page;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;

import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from jakarta.data.page.PageRequest from the
 * Jakarta Data repo.
 */
record Pagination(long pageNumber,
                int size,
                @Nonnull Mode mode,
                @Nullable Cursor type,
                boolean requestTotal)
                implements PageRequest {

    Pagination {
        if (pageNumber < 1)
            throw new IllegalArgumentException("pageNumber: " + pageNumber);
        if (size < 1)
            throw new IllegalArgumentException("maxPageSize: " + size);
        if (mode != Mode.OFFSET && (type == null || type.size() == 0))
            throw new IllegalArgumentException(Messages.get("006.zero.size.key"));
    }

    @Override
    @Nonnull
    public PageRequest afterCursor(@Nonnull PageRequest.Cursor cursor) {
        return new Pagination(pageNumber, size, Mode.CURSOR_NEXT, cursor, requestTotal);
    }

    @Override
    @Nonnull
    public PageRequest beforeCursor(@Nonnull PageRequest.Cursor cursor) {
        return new Pagination(pageNumber, size, Mode.CURSOR_PREVIOUS, cursor, requestTotal);
    }

    @Override
    @Nonnull
    public Optional<Cursor> cursor() {
        return type == null ? Optional.empty() : Optional.of(type);
    }

    @Override
    @Nonnull
    public PageRequest pageNumber(long pageNum) {
        return new Pagination(pageNum, size, mode, type, requestTotal);
    }

    @Override
    @Nonnull
    public Pagination size(int maxPageSize) {
        return new Pagination(pageNumber, maxPageSize, mode, type, requestTotal);
    }

    @Override
    @Nonnull
    public String toString() {
        StringBuilder b = new StringBuilder("PageRequest{pageNumber=") //
                        .append(pageNumber) //
                        .append(", size=").append(size) //
                        .append(", mode=").append(mode);

        if (type != null)
            b.append(", cursor size=").append(type.size());

        b.append("}");

        return b.toString();
    }

    @Override
    @Nonnull
    public PageRequest withoutTotal() {
        return new Pagination(pageNumber, size, mode, type, false);
    }

    @Override
    @Nonnull
    public PageRequest withTotal() {
        return new Pagination(pageNumber, size, mode, type, true);
    }

}
