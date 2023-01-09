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

import org.junit.Test;
import org.junit.runner.RunWith;

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
public class DataStandaloneTckLauncher {

    @Server
    public static LibertyServer DONOTSTART;

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchDataTckStandalone() throws Exception {
        // Test groups to run
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("jakarta.tck.platform", "standalone");

        //Additional flag to tell TCK not to deploy standalone tests to a server
        additionalProps.put("jakarta.tck.standalone.test", "true");

        //Always skip signature tests in standalone mode, instead do signature testing on core profile
        additionalProps.put("test.excluded.groups", "signature");

        //TODO Remove once TCK is available from stagging repo
        additionalProps.put("jakarta.data.groupid", "io.openliberty.jakarta.data");
        additionalProps.put("jakarta.data.tck.version", "1.0.0-112222");

        String bucketName = "io.openliberty.jakarta.data.1.0_fat_tck";
        String testName = this.getClass() + ":launchDataTckStandalone";
        Type type = Type.JAKARTA;
        String specName = "Data (Standalone)";
        String relativeTckRunner = "publish/tckRunner/standalone/";
        TCKRunner.runTCK(DONOTSTART, bucketName, testName, type, specName, null, relativeTckRunner, additionalProps);
    }
}
