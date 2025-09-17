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

import jakarta.annotation.Resource;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

@Resource(name = "java:module/env/jdbc/ServerDataSourceRef",
          lookup = "jdbc/ServerDataSource") // jndiName from server.xml
@Repository(dataStore = "java:module/env/jdbc/ServerDataSourceRef")
public interface EJBAppDSRefRepo {

    @Find
    Optional<EJBAppEntity> lookFor(@By(ID) String id);

    Connection makeConnection();

    @Save
    EJBAppEntity persist(EJBAppEntity e);
}