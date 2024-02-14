/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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

import jakarta.data.page.Page;
import jakarta.data.page.Pageable;
import jakarta.data.repository.Find;

/**
 * Interface methods copied from the Jakarta Data git repository.
 */
public interface PageableRepository<T, K> extends BasicRepository<T, K> {
    @Find
    Page<T> findAll(Pageable<T> pageable);
}
