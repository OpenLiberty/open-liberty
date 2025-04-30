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
package test.jakarta.data.global.webapp;

import static jakarta.data.repository.By.ID;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

/**
 * A Jakarta Data repository that requires a DataSource with a java:global/env
 * resource reference that that is defined by this application.
 */
@DataSourceDefinition(name = "java:global/jdbc/WebAppDataSource",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      user = "dbuser2",
                      password = "dbpwd2",
                      properties = "createDatabase=create")
@Resource(name = "java:global/env/jdbc/WebAppDataSourceRef",
          lookup = "java:global/jdbc/WebAppDataSource")
@Repository(dataStore = "java:global/env/jdbc/WebAppDataSourceRef")
public interface Alphabet extends DataRepository<Letter, Character> {

    @Insert
    void addLetter(Letter letter);

    @Delete
    int deleteLetter(@By(ID) char ch);

    boolean existsById(char ch);

    default boolean hasLetter(char ch) {
        return existsById(ch);
    }
}
