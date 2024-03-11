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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataFullTckLauncher {

    @Server("io.openliberty.org.jakarta.data.1.0.full")
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
    @Ignore("Behavior change in spec after M3 caused failures") // TODO re-enable with M4 or RC1
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchDataTckFullPersistence() throws Exception {
        // Test groups to run
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("jimage.dir", server.getServerSharedPath() + "jimage/output/");
        additionalProps.put("tck_protocol", "servlet");
        additionalProps.put("jakarta.tck.profile", "full");

        //Always skip signature tests on full profile (already tested in core profile)
        additionalProps.put("included.groups", "full & persistence & !signature");

        //Comment out to use SNAPSHOT
        additionalProps.put("jakarta.data.groupid", "jakarta.data");
        additionalProps.put("jakarta.data.tck.version", "1.0.0-M3");

        String bucketName = "io.openliberty.jakarta.data.1.0_fat_tck";
        String testName = this.getClass() + ":launchDataTckFull";
        Type type = Type.JAKARTA;
        String specName = "Data (Full, Persistence)";
        String relativeTckRunner = "publish/tckRunner/platform/";
        TCKRunner.runTCK(server, bucketName, testName, type, specName, null, relativeTckRunner, additionalProps);
    }

    // Cannot test NoSQL database on Full profile since the persistence feature is automatically included
}