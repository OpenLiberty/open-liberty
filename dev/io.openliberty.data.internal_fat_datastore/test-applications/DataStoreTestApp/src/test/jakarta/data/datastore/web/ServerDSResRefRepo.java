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

import java.util.Optional;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

import javax.sql.DataSource;

import test.jakarta.data.datastore.lib.ServerDSEntity;

/**
 * A repository that uses a resource reference to a dataSource in server.xml.
 * The resource reference has container managed authentication with
 * auth data where the user name is resrefuser1.
 */
@Repository(dataStore = "java:module/env/jdbc/ServerDataSourceRef")
public interface ServerDSResRefRepo {

    DataSource getDataStore();

    @Find
    Optional<ServerDSEntity> read(@By("id") String id);

    @Save
    ServerDSEntity write(ServerDSEntity e);
}