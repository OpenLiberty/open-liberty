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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * NOTE: This test class is not run since it isn't in the FATSuite.
 * The OpenLiberty implementation of Jakarta Data doesn't support standalone mode,
 * but this test class is nice for manually testing the TCK framework to make sure it works as intended.
 * Keep it around until the TCK is finished being written.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataStandaloneTckLauncher {

    @Server
    public static LibertyServer DONOTSTART;

    // Cannot test Relation database in Standalone mode since our implementation depends on the container.

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @Ignore("jnosql does not support static metamodel yet")
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchDataTckStandaloneNoSQL() throws Exception {
        // Test groups to run
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("jimage.dir", "/jimage/output/");
        additionalProps.put("jakarta.tck.profile", "none");

        //FIXME Always skip signature tests since our implementation has experimental API
        additionalProps.put("included.groups", "standalone & nosql & !signature");

        //Comment out to use SNAPSHOT
        additionalProps.put("jakarta.data.groupid", "jakarta.data");
        additionalProps.put("jakarta.data.tck.version", "1.0.0-M3");

        String bucketName = "io.openliberty.jakarta.data.1.0_fat_tck";
        String testName = this.getClass() + ":launchDataTckStandaloneNoSQL";
        Type type = Type.JAKARTA;
        String specName = "Data (Standalone, NoSQL)";
        String relativeTckRunner = "publish/tckRunner/standalone/";
        TCKRunner.runTCK(DONOTSTART, bucketName, testName, type, specName, null, relativeTckRunner, additionalProps);
    }
}
