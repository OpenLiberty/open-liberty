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
package test.jakarta.data.datastore.ejb;

import java.sql.Connection;
import java.util.Optional;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

/**
 * A repository that uses a DataSourceDefinition that is defined in
 * the EJB module.
 */
@Repository(dataStore = "java:global/jdbc/DataSourceDef") // TODO java:module
public interface EJBModuleDSDRepo {

    @Find
    Optional<EJBModuleDSDEntity> acquire(@By("id") int id);

    Connection acquireConnection();

    @Save
    void store(EJBModuleDSDEntity e);
}