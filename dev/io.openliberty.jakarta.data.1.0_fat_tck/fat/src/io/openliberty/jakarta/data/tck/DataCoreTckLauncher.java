/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11) //TODO Jakarta 11 might require java 17
public class DataCoreTckLauncher {

    @Server("io.openliberty.org.jakarta.data.1.0.core")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        //Path that jimage will output modules for signature testing
        Map<String, String> opts = server.getJvmOptionsAsMap();
        opts.put("-Djimage.dir", server.getServerSharedPath() + "jimage/output/");
        server.setJvmOptions(opts);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchDataTckCore() throws Exception {
        // Test groups to run
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("jakarta.tck.platform", "core");

        // Skip signature testing on Windows.
        // So far as I can tell the signature test plugin is not supported on this configuration
        if (System.getProperty("os.name").contains("Windows")) {
            Log.info(DataWebTckLauncher.class, "launchDataTckWeb", "Skipping Jakarta Data Signature Test on Windows");
            additionalProps.put("test.excluded.groups", "signature");
        }

        //FIXME Always skip signature tests since our implementation has experimental API
        additionalProps.put("test.excluded.groups", "signature");

        //TODO Remove once TCK is available from stagging repo
        additionalProps.put("jakarta.data.groupid", "io.openliberty.jakarta.data");
        additionalProps.put("jakarta.data.tck.version", "1.0.0-112222");

        String bucketName = "io.openliberty.jakarta.data.1.0_fat_tck";
        String testName = this.getClass() + ":launchDataTckCore";
        Type type = Type.JAKARTA;
        String specName = "Data (Core)";
        String relativeTckRunner = "publish/tckRunner/core/";
        TCKRunner.runTCK(server, bucketName, testName, type, specName, null, relativeTckRunner, additionalProps);
    }
}