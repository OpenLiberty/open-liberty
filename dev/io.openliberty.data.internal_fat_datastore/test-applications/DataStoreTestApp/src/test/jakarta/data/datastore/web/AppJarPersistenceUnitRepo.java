/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnit;

import test.jakarta.data.datastore.lib.ServerDSEntity;

/**
 * This repository has its dataStore reference a persistence unit that is defined
 * in a JAR of the application. The persistence-unit is defined in
 * META-INF/persistence.xml of lib/DataStoreTestAppLib.jar.
 */
@PersistenceUnit(name = "java:app/env/persistence/AppPersistenceUnitRef",
                 unitName = "AppPersistenceUnit")
@Repository(dataStore = "java:app/env/persistence/AppPersistenceUnitRef")
public interface AppJarPersistenceUnitRepo extends DataRepository<ServerDSEntity, String> {

    EntityManager entityMgr();

    @Query("UPDATE ServerDSEntity SET value = value * 4 WHERE id = ?1")
    boolean quadruple(String id);

}