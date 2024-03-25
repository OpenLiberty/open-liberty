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
package test.jakarta.data.web;

import static jakarta.data.repository.By.ID;

import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Save;

/**
 * Custom repository interface that provides entity and key type parameters.
 */
public interface CustomRepository<T, K> {
    long countBySSN_IdBetween(K minId, K maxId);

    long deleteBySSN_IdBetween(K minId, K maxId);

    @OrderBy("firstName")
    @OrderBy(ID)
    T[] findByLastName(String lastName);

    @Save
    void updateOrAdd(Iterable<T> entities);
}
