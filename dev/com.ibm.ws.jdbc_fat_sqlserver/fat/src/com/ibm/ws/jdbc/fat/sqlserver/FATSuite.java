/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.sqlserver;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;

@RunWith(Suite.class)
@SuiteClasses(SQLServerTest.class)
public class FATSuite {

    private static final DockerImageName sqlserverImage = DockerImageName.parse("aguibert/sqlserver-ssl:1.0")//
                    .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server");

    static final MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>(sqlserverImage)//
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "sqlserver"));

    @BeforeClass
    public static void beforeSuite() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client.
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();

        sqlserver.start();
    }

    @AfterClass
    public static void afterSuite() {
        sqlserver.stop();
    }
}