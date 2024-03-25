/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.datastore.web;

import java.util.List;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.persistence.EntityManager;

@Repository
public interface QualifiedPersistenceUnitRepo {
    @ResourceQualifier
    EntityManager entityManager();

    @Insert
    void add(List<PersistenceUnitEntity> entities);

    @Find
    @OrderBy("id")
    List<PersistenceUnitEntity> getAll(@By("value") int val);
}