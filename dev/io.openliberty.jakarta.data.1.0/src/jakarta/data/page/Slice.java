/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import java.util.List;

import jakarta.data.Streamable;

/**
 * Method signatures copied from the Jakarta Data git repo.
 */
public interface Slice<T> extends Streamable<T> {
    List<T> content();

    int numberOfElements();

    PageRequest<T> pageRequest();

    boolean hasContent();

    boolean hasNext();

    PageRequest<T> nextPageRequest();

    <E> PageRequest<E> nextPageRequest(Class<E> entityClass);

    <E> PageRequest<E> pageRequest(Class<E> entityClass);
}
