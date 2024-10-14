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
package test.jakarta.data.datastore.web2;

import java.util.Optional;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

import javax.sql.DataSource;

import test.jakarta.data.datastore.lib.ServerDSEntity;

/**
 * A repository that uses the same resource reference JNDI name
 * as exists in the other web module and the EJB module,
 * except with a different container managed authentication alias,
 * which has auth data where the user name is resrefuser2.
 */
@Repository(dataStore = "java:module/env/jdbc/ServerDataSourceRef")
public interface WebModule2DSResRefRepo {

    DataSource getDataStore();

    @Find
    Optional<ServerDSEntity> read(@By("id") String id);

    @Save
    ServerDSEntity write(ServerDSEntity e);
}