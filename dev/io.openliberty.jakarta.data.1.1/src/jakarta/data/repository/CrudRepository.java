/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
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

import jakarta.annotation.Nonnull;

/**
 * Interface methods copied from Jakarta Data.
 */
public interface CrudRepository<T, K> extends BasicRepository<T, K> {
    @Insert
    @Nonnull
    <S extends T> S insert(@Nonnull S entity);

    @Insert
    @Nonnull
    <S extends T> List<S> insertAll(@Nonnull List<S> entities);

    @Update
    @Nonnull
    <S extends T> S update(@Nonnull S entity);

    @Update
    @Nonnull
    <S extends T> List<S> updateAll(@Nonnull List<S> entities);
}
