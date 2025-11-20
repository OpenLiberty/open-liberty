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
package com.ibm.ws.testcontainers.example;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.app.JavaInfo;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.impl.LibertyServer;
import web.dbrotation.DbRotationServlet;

/**
 * Functional test that could not be accomplished via unit testing.
 * Verify that without @MinimumJavaLevel(javaLevel = 17)
 * we correctly throw an error alerting users of DatabaseRotation that
 * the container they are trying to use is not supported on their current java level.
 */
@RunWith(FATRunner.class)
public class DatabaseRotationJava17PlusTest {

    public static final String APP_NAME = "app";

    @Server("build.example.testcontainers.dbrotation")
    @TestServlet(servlet = DbRotationServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @Test
    public void containerAcceptedAtOrAboveJava17() {
        assumeTrue(JavaInfo.majorVersion() >= 17);

        try {
            JdbcDatabaseContainer<?> jdbcContainer = DatabaseContainerFactory.create(DatabaseContainerType.DerbyClientJava17Plus);
        } catch (Exception e) {
            fail("Should not have failed to create a default container for DerbyClient "
                 + "by calling create() post-java17 but did: " + e.getMessage());
        }

        assumeTrue(System.getProperty("fat.bucket.db.type").equals("derby"));

        try {
            JdbcDatabaseContainer<?> jdbcContainer = DatabaseContainerFactory.createLatest();
        } catch (Exception e) {
            fail("Should not have failed to create a default container for Derby "
                 + "by calling createLatest() post-java17 but did: " + e.getMessage());
        }
    }

    @Test
    public void containerRejectedBelowJava17() {
        assumeTrue(JavaInfo.majorVersion() < 17);
        //Attempt to call create() with a post java 17 type
        try {
            JdbcDatabaseContainer<?> jdbcContainer = DatabaseContainerFactory.create(DatabaseContainerType.DerbyClientJava17Plus);
            fail("Should not have been able to create a default container for DerbyClient "
                 + "by calling create() pre-java17 but did: " + jdbcContainer);
        } catch (IllegalStateException e) {
            assertTrue("Caught expection has the wrong message.",
                       e.getMessage().startsWith("Cannot initialize a container of type"));
        }

        assumeTrue(System.getProperty("fat.bucket.db.type").equals("derby"));

        //Attempt to call createLatest()
        try {
            JdbcDatabaseContainer<?> jdbcContainer = DatabaseContainerFactory.createLatest();
            fail("Should not have been able to create a default container for Derby "
                 + "by calling createLatest() pre-java17 but did: " + jdbcContainer);
        } catch (IllegalStateException e) {
            assertTrue("Caught expection has the wrong message.",
                       e.getMessage().startsWith("Cannot initialize a container of type"));
        }

    }
}
