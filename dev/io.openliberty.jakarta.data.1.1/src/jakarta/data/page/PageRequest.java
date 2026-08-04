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
import java.util.List;
import java.util.Optional;

import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from jakarta.data.page.PageRequest
 * from the Jakarta Data repo.
 */
public interface PageRequest {
    public static enum Mode {
        CURSOR_NEXT, CURSOR_PREVIOUS, OFFSET
    }

    public interface Cursor {
        @Nonnull
        public static Cursor forKey(@Nonnull Object... componentsOfKey) {
            return new PageRequestCursor(componentsOfKey);
        }

        @Nonnull
        public List<?> elements();

        @Override
        boolean equals(@Nullable Object cursor);

        public Object get(int index);

        public int size();
    }

    @Nonnull
    public static PageRequest afterCursor(@Nonnull Cursor cursor, long page, int size, boolean withTotal) {
        return new Pagination(page, size, Mode.CURSOR_NEXT, cursor, withTotal);
    }

    @Nonnull
    public static PageRequest beforeCursor(@Nonnull Cursor cursor, long page, int size, boolean withTotal) {
        return new Pagination(page, size, Mode.CURSOR_PREVIOUS, cursor, withTotal);
    }

    @Nonnull
    public static PageRequest ofPage(long page) {
        return new Pagination(page, 10, Mode.OFFSET, null, true);
    }

    @Nonnull
    public static PageRequest ofPage(long page, int size, boolean withTotal) {
        return new Pagination(page, size, Mode.OFFSET, null, withTotal);
    }

    @Nonnull
    public static PageRequest ofSize(int size) {
        return new Pagination(1, size, Mode.OFFSET, null, true);
    }

    @Nonnull
    public PageRequest afterCursor(@Nonnull PageRequest.Cursor cursor);

    @Nonnull
    public PageRequest beforeCursor(@Nonnull PageRequest.Cursor cursor);

    @Nonnull
    public Optional<Cursor> cursor();

    @Nonnull
    public Mode mode();

    public default long page() {
        return pageNumber();
    }

    public long pageNumber();

    @Nonnull
    public PageRequest pageNumber(long pageNum);

    @Nonnull
    public default PageRequest pageOffset(long offset) {
        if (mode() != Mode.OFFSET)
            throw new IllegalStateException(Messages.get("014.mode.disallows.offset",
                                                         mode()));

        if (offset < 0)
            throw new IllegalArgumentException(Messages.get("004.arg.negative",
                                                            "offset"));

        if (offset == Long.MAX_VALUE)
            throw new IllegalArgumentException(Messages.get("013.arg.invalid",
                                                            "offset",
                                                            offset));

        return pageNumber(offset + 1);
    }

    public boolean requestTotal();

    public int size();

    @Nonnull
    public PageRequest size(int size);

    @Nonnull
    public PageRequest withoutTotal();

    @Nonnull
    public PageRequest withTotal();

}
