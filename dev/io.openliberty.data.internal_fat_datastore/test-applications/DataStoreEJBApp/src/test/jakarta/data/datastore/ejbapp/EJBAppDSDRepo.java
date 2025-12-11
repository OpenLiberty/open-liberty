/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.datastore.ejbapp;

import static jakarta.data.repository.By.ID;

import java.sql.Connection;
import java.util.Optional;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

@DataSourceDefinition(name = "java:app/jdbc/DataSourceDef",
                      className = "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource",
                      databaseName = "memory:testdb",
                      user = "ejbuser2",
                      password = "ejbpwd2",
                      properties = "createDatabase=create")
@Repository(dataStore = "java:app/jdbc/DataSourceDef")
public interface EJBAppDSDRepo extends DataRepository<EJBAppEntity, String> {

    Connection establishConnection();

    @Find
    Optional<EJBAppEntity> seek(@By(ID) String id);

    @Insert
    EJBAppEntity writeToDatabase(EJBAppEntity e);
}