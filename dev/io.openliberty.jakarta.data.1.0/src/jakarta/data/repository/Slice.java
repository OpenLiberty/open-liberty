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

import java.util.List;

/**
 * Method signatures copied from the Jakarta Data git repo.
 */
public interface Slice<T> extends Streamable<T> {
    List<T> content();

    long number(); // from Spring Data and Micronaut. Not currently in Jakarta Data.

    int numberOfElements();

    Pageable pageable();

    boolean hasContent();

    Pageable nextPageable();
}
