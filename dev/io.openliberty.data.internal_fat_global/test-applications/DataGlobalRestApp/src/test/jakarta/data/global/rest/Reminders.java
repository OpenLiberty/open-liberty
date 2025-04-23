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
package test.jakarta.data.global.rest;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

@DataSourceDefinition(name = "java:global/jdbc/RestResourceDataSource",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      user = "dbuser1",
                      password = "dbpwd1",
                      properties = "createDatabase=create")
@Repository(dataStore = "java:global/jdbc/RestResourceDataSource")
public interface Reminders extends CrudRepository<Reminder, Long> {
}
