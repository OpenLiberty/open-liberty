/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package jakarta.data.repository;

import java.util.Collections;
import java.util.List;

/**
 * Method signatures are copied from jakarta.data.repository.Pageable from the Jakarta Data repo.
 */
public interface Pageable {
    public static enum Mode {
        CURSOR_NEXT, CURSOR_PREVIOUS, OFFSET
    }

    public interface Cursor {
        public Object getKeysetElement(int index);

        public int size();
    }

    public static Pageable ofPage(long page) {
        return new Pagination(page, 10, Collections.emptyList(), Mode.OFFSET, null);
    }

    public static Pageable ofSize(int size) {
        return new Pagination(1, size, Collections.emptyList(), Mode.OFFSET, null);
    }

    public Pageable afterKeyset(Object... keyset);

    public Pageable afterKeysetCursor(Pageable.Cursor cursor);

    public Pageable beforeKeyset(Object... keyset);

    public Pageable beforeKeysetCursor(Pageable.Cursor cursor);

    public Cursor cursor();

    public Mode mode();

    public Pageable next();

    public long page();

    public int size();

    public List<Sort> sorts();

    public Pageable page(long page);

    public Pageable size(int size);

    public Pageable sortBy(Iterable<Sort> sorts);

    public Pageable sortBy(Sort... sorts);
}
