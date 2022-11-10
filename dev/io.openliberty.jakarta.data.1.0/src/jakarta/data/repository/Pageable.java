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

import java.util.Collections;
import java.util.List;

/**
 * Method signatures are copied from jakarta.data.repository.Pageable from the Jakarta Data repo.
 */
public interface Pageable {

    public static Pageable ofPage(long page) {
        return new Pagination(page, 10, Collections.emptyList());
    }

    public static Pageable ofSize(int size) {
        return new Pagination(1, size, Collections.emptyList());
    }

    public KeysetPageable afterKeyset(Object... keyset);

    public KeysetPageable afterKeysetCursor(KeysetPageable.Cursor cursor);

    public KeysetPageable beforeKeyset(Object... keyset);

    public KeysetPageable beforeKeysetCursor(KeysetPageable.Cursor cursor);

    public Pageable next();

    public long page();

    public int size();

    public List<Sort> sorts();

    public Pageable newPage(long page);

    public Pageable newSize(int size);

    public Pageable sortBy(Iterable<Sort> sorts);

    public Pageable sortBy(Sort... sorts);
}
