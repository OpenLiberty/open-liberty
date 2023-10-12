/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

/**
 * Interface methods copied from Jakarta Data.
 */
public interface CrudRepository<T, K> extends BasicRepository<T, K> {
    @Insert
    void insert(T entity);

    @Insert
    void insertAll(Iterable<T> entities);

    @Update
    boolean update(T entity);

    @Update
    int updateAll(Iterable<T> entities);
}
