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
package test.jakarta.data.datastore.web;

import java.util.List;

import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

/**
 * This repository has its dataStore set to a PersistenceUnit reference from DataStoreTestServlet
 * that is pointing to the MyPersistenceUnit persistence-unit in persistence.xml
 */
@Repository(dataStore = "persistence/MyPersistenceUnitRef") // java:comp/env/ is implied
public interface PersistenceUnitRepo {

    // Entity type is inferred from the Insert annotation.
    // Do not add other methods or inheritance to this class.
    @Save
    List<PersistenceUnitEntity> save(List<PersistenceUnitEntity> e);

    int countByIdStartsWith(String pattern);
}