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
package jakarta.data.repository;

import java.util.Arrays;

public class KeysetPagination extends Pagination implements KeysetPageable {

    static class CursorImpl implements Cursor {
        private final Object[] keyset;

        CursorImpl(Object... keyset) {
            this.keyset = keyset;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o != null && o.getClass().equals(getClass()) && Arrays.equals(((CursorImpl) o).keyset, keyset);
        }

        @Override
        public Object getKeysetElement(int index) {
            return keyset[index];
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(keyset);
        }

        @Override
        public int size() {
            return keyset.length;
        }

        @Override
        public String toString() {
            return new StringBuilder("Cursor@").append(Integer.toHexString(hashCode())) //
                            .append(" with ").append(keyset.length).append(" keys") //
                            .toString();
        }
    }

    private final Cursor cursor;
    private final Mode mode;

    KeysetPagination(Pageable copyFrom, Mode mode, Cursor cursor) {
        super(copyFrom.page(), copyFrom.size(), copyFrom.sorts());
        this.cursor = cursor;
        this.mode = mode;
        if (cursor == null || cursor.size() == 0)
            throw new IllegalArgumentException();
    }

    @Override
    public Cursor cursor() {
        return cursor;
    }

    @Override
    public Mode mode() {
        return mode;
    }

    @Override
    public String toString() {
        return new StringBuilder("KeysetPageable{size=") //
                        .append(size()).append(", page=") //
                        .append(page()).append(", mode=") //
                        .append(mode).append(", cursor=") //
                        .append(cursor).append("}") //
                        .toString();
    }
}