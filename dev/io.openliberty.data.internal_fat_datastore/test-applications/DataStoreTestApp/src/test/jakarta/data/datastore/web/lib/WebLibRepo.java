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
package test.jakarta.data.datastore.web.lib;

import static jakarta.data.repository.By.ID;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

/**
 * A repository that is defined within a library of a web module.
 */
@Repository(dataStore = "java:app/jdbc/DataSourceDef")
public interface WebLibRepo {

    Connection connection();

    @Insert
    void create(WebLibEntity e);

    @Find
    Optional<WebLibEntity> request(@By(ID) int id);

    default String user() throws SQLException {
        return connection().getMetaData().getUserName().toLowerCase();
    }
}