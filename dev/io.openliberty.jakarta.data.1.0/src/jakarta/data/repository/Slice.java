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

import java.util.List;

/**
 * Method signatures copied from the Jakarta Data git repo.
 */
public interface Slice<T> {
    List<T> getContent();

    long getNumber(); // from Spring Data and Micronaut. Not currently in Jakarta Data.

    int getNumberOfElements();

    Pageable getPageable();

    boolean hasContent();

    Pageable nextPageable();
}
