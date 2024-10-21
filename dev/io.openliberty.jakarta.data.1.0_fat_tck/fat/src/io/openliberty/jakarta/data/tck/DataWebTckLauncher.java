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

import org.junit.AfterClass;
import org.junit.BeforeClass;
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

//@Mode(TestMode.FULL) TODO switch to full mode after feature is beta
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataWebTckLauncher {

    @Server("io.openliberty.jakarta.data.1.0.web")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, FATSuite.relationalDatabase);
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(FATSuite.relationalDatabase).getDriverName());
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "CWWKE0955E" //websphere.java.security java 18+
        );
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchDataTckWebPersistence() throws Exception {
        // Test groups to run
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("jimage.dir", server.getServerSharedPath() + "jimage/output/");
        additionalProps.put("tck_protocol", "servlet");
        additionalProps.put("jakarta.profile", "web");
        additionalProps.put("jakarta.tck.database.type", "relational");
        additionalProps.put("jakarta.tck.database.name", FATSuite.relationalDatabase.getClass().getSimpleName());

        //Always skip signature tests on Web profile (already tested in core profile)
        additionalProps.put("included.groups", "web & persistence & !signature");

        additionalProps.put("excluded.tests", FATSuite.getExcludedTestByDatabase(DatabaseContainerType.valueOf(FATSuite.relationalDatabase)));

        //Comment out to use SNAPSHOT
        additionalProps.put("jakarta.data.groupid", "jakarta.data");
        additionalProps.put("jakarta.data.tck.version", "1.0.1");

        TCKRunner.build(server, Type.JAKARTA, "Data")
                        .withPlatfromVersion("11")
                        .withQualifiers("web", "persistence")
                        .withRelativeTCKRunner("publish/tckRunner/platform/")
                        .withAdditionalMvnProps(additionalProps)
                        .runTCK();
    }

    // Cannot test NoSQL database on Web profile since the persistence feature is automatically included
}