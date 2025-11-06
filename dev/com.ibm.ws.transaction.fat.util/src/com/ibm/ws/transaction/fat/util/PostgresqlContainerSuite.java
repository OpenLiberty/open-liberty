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

import org.testcontainers.utility.DockerImageName;

import componenttest.containers.ImageBuilder;

public class PostgresqlContainerSuite extends TxTestContainerSuite {

    public static final String POSTGRES_DB = "testdb";
    public static final String POSTGRES_USER = "postgresUser";
    public static final String POSTGRES_PASS = "superSecret";
    
    private static DockerImageName _postgresqlImageName;

    public static DockerImageName getPostgresqlImageName() {
    	if (_postgresqlImageName == null) {
    		_postgresqlImageName = ImageBuilder.build("postgres-ssl:17.0.0.1").getDockerImageName();
    	}
    	
    	return _postgresqlImageName;
    }
}
