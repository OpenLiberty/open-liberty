/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.data.repository;

/**
 * Interface methods copied from the Jakarta Data git repository.
 */
public interface PageableRepository<T, K> extends CrudRepository<T, K> {
    Page<T> findAll(Pageable pageable);
}
