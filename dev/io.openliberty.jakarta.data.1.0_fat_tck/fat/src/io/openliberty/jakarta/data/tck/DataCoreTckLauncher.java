/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package io.openliberty.jakarta.data.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataCoreTckLauncher {

    @Server("io.openliberty.org.jakarta.data.1.0.core")
    public static LibertyServer persistenceServer;

    @Server("io.openliberty.org.jakarta.data.1.0.core.nosql")
    public static LibertyServer noSQLServer;

    @After
    public void tearDown() throws Exception {
        String[] ignoredMessages = new String[] {
                                                  "CWWKE0955E" //websphere.java.security java 18+
        };
        if (persistenceServer.isStarted()) {
            persistenceServer.stopServer(ignoredMessages);
        }

        if (noSQLServer.isStarted()) {
            noSQLServer.stopServer(ignoredMessages);
        }
    }

    @Test
    @Ignore("Behavior change in spec after M3 caused failures") // TODO re-enable with M4 or RC1
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchDataTckCorePersistence() throws Exception {

        // Setup persistence server
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(persistenceServer, FATSuite.relationalDatabase);
        persistenceServer.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(FATSuite.relationalDatabase).getDriverName());
        persistenceServer.startServer();

        // Test groups to run
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("jimage.dir", persistenceServer.getServerSharedPath() + "jimage/output/");
        additionalProps.put("tck_protocol", "rest");
        additionalProps.put("jakarta.tck.profile", "core");

        //FIXME Always skip signature tests since our implementation has experimental API
        additionalProps.put("included.groups", "core & persistence & !signature");

        //Comment out to use SNAPSHOT
        additionalProps.put("jakarta.data.groupid", "jakarta.data");
        additionalProps.put("jakarta.data.tck.version", "1.0.0-M3");

        String bucketName = "io.openliberty.jakarta.data.1.0_fat_tck";
        String testName = this.getClass() + ":launchDataTckCorePersistence";
        Type type = Type.JAKARTA;
        String specName = "Data (Core, Persistence)";
        String relativeTckRunner = "publish/tckRunner/platform/";
        TCKRunner.runTCK(persistenceServer, bucketName, testName, type, specName, null, relativeTckRunner, additionalProps);
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @Ignore("jnosql does not support static metamodel yet")
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchDataTckCoreNoSQL() throws Exception {

        // Setup nosql server
        noSQLServer.addEnvVar("MONGO_DBNAME", "testdb");
        noSQLServer.addEnvVar("MONGO_HOST", FATSuite.noSQLDatabase.getHost() + ":" + String.valueOf(FATSuite.noSQLDatabase.getMappedPort(27017)));
        noSQLServer.startServer();

        // Test groups to run
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("jimage.dir", noSQLServer.getServerSharedPath() + "jimage/output/");
        additionalProps.put("tck_protocol", "rest");
        additionalProps.put("jakarta.tck.profile", "core");

        //FIXME Always skip signature tests since our implementation has experimental API
        additionalProps.put("included.groups", "core & nosql & !signature");

        //Comment out to use SNAPSHOT
//        additionalProps.put("jakarta.data.groupid", "jakarta.data");
//        additionalProps.put("jakarta.data.tck.version", "1.0.0-M3");

        String bucketName = "io.openliberty.jakarta.data.1.0_fat_tck";
        String testName = this.getClass() + ":launchDataTckCoreNoSQL";
        Type type = Type.JAKARTA;
        String specName = "Data (Core, NoSQL)";
        String relativeTckRunner = "publish/tckRunner/platform/";
        TCKRunner.runTCK(noSQLServer, bucketName, testName, type, specName, null, relativeTckRunner, additionalProps);
    }
}