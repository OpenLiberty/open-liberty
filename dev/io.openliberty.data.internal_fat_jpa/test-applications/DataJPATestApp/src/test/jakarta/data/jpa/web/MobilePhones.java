/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
package test.jakarta.data.jpa.web;

import java.util.List;
import java.util.UUID;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Repository;

/**
 * Repository for the Mobile Entity
 */
@Repository(dataStore = "java:app/env/data/DataStoreRef")
public interface MobilePhones extends CrudRepository<Mobile, UUID> {

    List<Mobile> findByEmailsEmpty();

    List<Mobile> findByEmailsNotEmpty();

    @Delete
    public long removeAll();
}
