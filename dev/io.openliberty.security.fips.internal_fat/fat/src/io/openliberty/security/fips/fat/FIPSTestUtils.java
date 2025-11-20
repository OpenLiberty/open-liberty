/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.security.fips.fat;

import componenttest.topology.impl.JavaInfo;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyServer;

import java.util.HashMap;

import static org.junit.Assert.assertNotNull;

public class FIPSTestUtils {

    public static final String DEFAULT_ENV_FILE = "default.env";
    public static final String SERVER_ENV_FILE = "server.env";
    public static final String CLIENT_ENV_FILE = "client.env";
    public static final String PROPS_FILE_EXTENSION = ".properties";
    public static final String FIPS_PROFILE_FILE_NAME = "FIPS140-3-Liberty-Application.properties";
    public static final String VAR_EXPANSION_ENV = "# enable_variable_expansion";
    public static final String ENABLE_FIPS140_3_ENV_VAR = "ENABLE_FIPS140_3";
    public static final String SEMERU_FIPS_PROVIDER = "OpenJCEPlusFIPS";
    public static final String IBM_FIPS_PROVIDER = "IBMJCEPlusFIPS";
    public static final String LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME = "FIPS140-3-Liberty-Application.properties";
    public static final String STANDALONE_FIPS_PROFILE_FILENAME = "semeruFips140_3CustomProfile.properties";


    public static HashMap<Integer,Integer> supportedVersions = new HashMap<>();

    /**
     * From 25.0.0.0 and onwards, the base version has our base level of FIPS support
     * This only stores where a particular level of a base version is required
     */
    static {
        supportedVersions.put(11,29);
        supportedVersions.put(17,17);
        supportedVersions.put(21,9);
    }

    /**
     * IBM SDK 8 and Semeru Runtimes >=11 support FIPS, Any other Vendor and Version combination is not supported
     * So check the java information of the server to determine whether to run the actual tests
     *
     * If JAVA_HOME for a server is updated via setting JAVA_HOME in a .env file, then the systems JAVA_HOME is what will be picked up
     *
     * Once 11, 17 and 21 are no longer supported then just major version can
     *
     * There is a Semeru JDK 8 that does not support fips, so ensure that if running is skipped
     *
     * @param javaInfo
     * @return
     */
    public static boolean validFIPS140_3Environment(JavaInfo javaInfo){
        boolean validEnv = true;
        if(javaInfo.vendor() != JavaInfo.Vendor.IBM && javaInfo.vendor() != JavaInfo.Vendor.OPENJ9){
            validEnv = false;
            Log.warning(FIPSTestUtils.class, "Java Vendor not supported with FIPS, tests will be skipped");
        } else if (javaInfo.majorVersion()==8){
            if (javaInfo.runtimeName().toLowerCase().contains("semeru")) {
                validEnv = false;
                Log.warning(FIPSTestUtils.class, "Semeru JDK 8 not supported with FIPS, tests will be skipped");
            } else if (javaInfo.microVersion()<8 || (javaInfo.microVersion()==8 && javaInfo.fixpack()<30)){
                validEnv = false;
                Log.warning(FIPSTestUtils.class, "IBM SDK 8.0.8.30 or newer is required for FIPS support");
            }
        } else if(javaInfo.majorVersion()<25){
            // Java 25 onwards meet our requirements for <Major>.0.0. so no need to calculate their microversion
            Integer microVersion = supportedVersions.get(javaInfo.majorVersion());
            if(microVersion!=null && javaInfo.microVersion()<microVersion) {
                validEnv = false;
                Log.warning(FIPSTestUtils.class, "Invalid Version combination Major: " + javaInfo.majorVersion() + " MicroVersion: " + javaInfo.microVersion() + ". Check supported versions.");
            } else if (microVersion==null){
                // In case someone tries Semeru 24
                validEnv = false;
                Log.warning(FIPSTestUtils.class, "Major version not supported with FIPS");
            }
        }
        return validEnv;
    }

    public static void checkServerLogForFipsEnablementMessage(LibertyServer server, String expectedProvider){
        assertNotNull("Expected FIPS 140-3 enabled message to be found in Server logs, but was not found.", server.waitForStringInLog("CWWKS5903I:.*" + expectedProvider));
    }

    public static void checkClientLogForFipsEnablementMessage(LibertyClient client, String expectedProvider){
        // Check the copied logs as they are reliable
        assertNotNull("Expected FIPS 140-3 enabled message to be found in Client logs, but was not found.", client.waitForStringInCopiedLog("CWWKS5903I:.*" + expectedProvider));
    }

}
