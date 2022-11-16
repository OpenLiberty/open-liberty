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
package com.ibm.ws.transaction.fat.util;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;

/**
 *
 */
public class TxTestContainerSuite extends TestContainerSuite {

    public static DatabaseContainerType databaseContainerType;
    public static JdbcDatabaseContainer<?> testContainer;

    public static void beforeSuite() throws Exception {
        if (testContainer == null) {
          testContainer = DatabaseContainerFactory.createType(databaseContainerType);
        }

        switch (databaseContainerType) {
          case Derby:
          case SQLServer:
            testContainer.waitingFor(Wait.forHealthcheck()).start();
            break;
          case Oracle:
            testContainer.waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!.*", 1)).start();
            break;
          case Postgres:
            testContainer.waitingFor(Wait.forLogMessage(".*database system is ready.*", 2)).start();
            break;
          default:
            testContainer.start();
            break;
        }
        Log.info(TxTestContainerSuite.class, "beforeSuite", "started test container of type: " + databaseContainerType);
    }

    public static void afterSuite() {
        Log.info(TxTestContainerSuite.class, "afterSuite", "stop test container");
        testContainer.stop();
    }
}
