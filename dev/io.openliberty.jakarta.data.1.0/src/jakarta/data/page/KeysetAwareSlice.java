/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

/**
 * Methods are copied from proposed interfaces in the Jakarta Data repo.
 */
public interface KeysetAwareSlice<T> extends Slice<T> {
    PageRequest.Cursor getKeysetCursor(int index);

    boolean hasPrevious();

    @Override
    PageRequest<T> nextPageRequest();

    PageRequest<T> previousPageRequest();
}
