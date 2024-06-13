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

import java.util.Optional;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

import javax.sql.DataSource;

import test.jakarta.data.datastore.lib.ServerDSEntity;

/**
 * A repository that uses the same resource reference JNDI name
 * as exists in both of the web modules,
 * except with a different container managed authentication alias,
 * which has auth data where the user name is resrefuser3.
 */
@Repository(// TODO dataStore = "java:module/env/jdbc/ServerDataSourceRef")
            dataStore = "java:app/env/jdbc/ServerDataSourceRef") // replace with the above
public interface EJBModuleDSResRefRepo {

    @Insert
    ServerDSEntity addItem(ServerDSEntity e);

    @Find
    Optional<ServerDSEntity> fetch(@By("id") String id);

    DataSource obtainDataSource();
}