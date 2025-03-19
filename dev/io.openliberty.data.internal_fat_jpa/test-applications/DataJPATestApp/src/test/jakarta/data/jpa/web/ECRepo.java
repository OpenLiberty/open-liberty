/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.persistence.EntityManager;

/**
 * Repository with an entity that has ElementCollection attributes
 * and non-ElementCollection attributes.
 */
@Repository
public interface ECRepo extends DataRepository<ECEntity, String> {

    EntityManager getEntityManager();

    @Insert
    void insert(ECEntity e);
}