/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.test.FATSuite;

import componenttest.containers.TestContainerSuite;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;

/**
 *
 */
public class FATSuiteBase extends TestContainerSuite {

    public static DatabaseContainerType databaseContainerType;
    public static JdbcDatabaseContainer<?> testContainer;

    public static void beforeSuite() throws Exception {
        testContainer = DatabaseContainerFactory.createType(databaseContainerType);
        Log.info(FATSuite.class, "beforeSuite", "starting test container of type: " + databaseContainerType);
        testContainer.withStartupTimeout(FATUtils.TESTCONTAINER_STARTUP_TIMEOUT).start();
        Log.info(FATSuite.class, "beforeSuite", "started test container of type: " + databaseContainerType);
    }

    public static void afterSuite() {
        Log.info(FATSuite.class, "afterSuite", "stop test container");
        testContainer.stop();
    }
}
