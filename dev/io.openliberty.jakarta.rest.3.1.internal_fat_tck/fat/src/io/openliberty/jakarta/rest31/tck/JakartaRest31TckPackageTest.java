/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.jakarta.rest31.tck;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class JakartaRest31TckPackageTest {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    
    private static final Set<String> featuresToRemove = new HashSet<>();
    static {
        featuresToRemove.add("appSecurity-5.0");
        featuresToRemove.add("concurrent-3.0");
        featuresToRemove.add("xmlBinding-4.0");
        featuresToRemove.add("webProfile-10.0");
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().
        andWith(new FeatureReplacementAction().removeFeatures(featuresToRemove).addFeature("webProfile-10.0").withID("webProfile").fullFATOnly()).
        andWith(new FeatureReplacementAction().removeFeatures(featuresToRemove).withID("coreProfile").fullFATOnly());

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
        // Skip running on the windows platform when not running locally.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) { 
            HashMap<String, String> props = new HashMap<String, String>(); 
            // The Java Se Bootstrap API added in EE10 is optional and not supported by Open Liberty.   So the 
            // following property is being added to exclude those tests.
            if (RepeatTestFilter.isRepeatActionActive("webProfile")) {
                props.put("excludedGroups","se_bootstrap,xml_binding");
            } else if (RepeatTestFilter.isRepeatActionActive("coreProfile")) {
                props.put("excludedGroups","se_bootstrap,xml_binding,servlet,security");
            } else {
                props.put("excludedGroups","se_bootstrap");
            }
            
            String bucketName = "io.openliberty.jakarta.rest.3.1.internal_fat_tck";
            String testName = this.getClass() + ":testJakarta31RestTck";
            Type type = Type.JAKARTA;
            String specName = "Restful Web Services";
            TCKRunner.runTCK(server, bucketName, testName, type, specName, props);
        }
    }
}
