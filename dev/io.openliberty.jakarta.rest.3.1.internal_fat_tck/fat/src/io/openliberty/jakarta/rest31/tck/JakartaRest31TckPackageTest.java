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
package io.openliberty.jakarta.rest31.tck;


import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKUtils;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class JakartaRest31TckPackageTest {

    @Server("FATServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        String javaVersion = System.getProperty("java.version");
        Log.info(JakartaRest31TckPackageTest.class, "setup", "javaVersion: " + javaVersion);
        System.out.println("java.version = " + javaVersion);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
//            server.postStopServerArchive(); // must explicitly collect since arquillian is starting/stopping the server
            server.stopServer(".*"); // Logs will contain tons of warnings/errors due to exception testing from TCK
        }
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void testJakarta31RestTck() throws Exception {
        HashMap<String, String> props = new HashMap<String, String>(); 
        // The Java Se Bootstrap API added in EE10 is optional and not supported by Open Liberty.   So the 
        // following property is being added to exclude those tests.
        props.put("excludedGroups","se_bootstrap");
        
        String bucketName = "io.openliberty.jakarta.rest.3.1.internal_fat_tck";
        String testName = this.getClass() + ":testJakarta31RestTck";
        Type type = Type.JAKARTA;
        String specName = "Restful Web Services";
        String specVersion = "3.1";
        TCKUtils.runTCKMvnCmd(server, bucketName, testName, type, specName, specVersion, props);
    }
}
