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
package com.ibm.ws.transaction.fat.util;

import org.junit.rules.ExternalResource;

import componenttest.containers.SimpleLogConsumer;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.PostgreSQLContainer;

public class TxTestDB extends ExternalResource {

	public TxTestDB(DatabaseContainerType type) {
		switch (type) {
		case Postgres:
	        TxTestContainerSuite.testContainer = new PostgreSQLContainer(PostgresqlContainerSuite.getPostgresqlImageName())
            .withDatabaseName(PostgresqlContainerSuite.POSTGRES_DB)
            .withUsername(PostgresqlContainerSuite.POSTGRES_USER)
            .withPassword(PostgresqlContainerSuite.POSTGRES_PASS)
            .withSSL()
            .withLogConsumer(new SimpleLogConsumer(TxTestDB.class, "postgre-ssl"));
	        break;
		default:
			break;
		}

        TxTestContainerSuite.beforeSuite(type);
	}
}
