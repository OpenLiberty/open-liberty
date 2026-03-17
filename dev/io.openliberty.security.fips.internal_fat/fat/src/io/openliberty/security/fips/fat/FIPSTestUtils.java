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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static final String LIBERTY_BASE_FIPS_PROFILE_FILENAME = "FIPS140-3-Liberty.properties";
    public static final String LIBERTY_APPLICATION_FIPS_PROFILE_FILENAME = "FIPS140-3-Liberty-Application.properties";
    public static final String STANDALONE_FIPS_PROFILE_FILENAME = "semeruFips140_3CustomProfile.properties";

    /**
     * IBM SDK 8 and Semeru Runtimes >=11 support FIPS, Any other Vendor and Version combination is not supported
     * So check the java information of the server to determine whether to run the actual tests
     *
     * If JAVA_HOME for a server is updated via setting JAVA_HOME in a .env file, then the systems JAVA_HOME is what will be picked up
     *
     *
     * There is a Semeru JDK 8 that does not support fips, so ensure that if running is skipped
     *
     * @param javaInfo
     * @return
     */
    public static boolean validFIPS140_3Environment(JavaInfo javaInfo){
        boolean validEnv = true;
        // s390 for Linux is not supported with FIPS mode, so until it is, we should skip the platform, once Java FIPS support is available for s390x we can remove this check
        if (System.getProperty("os.arch").contains("s390")){
           validEnv = false;
           Log.warning(FIPSTestUtils.class, "s390 architecture is not currently supported for either z/OS or Linux");
        } else {
            if (javaInfo.majorVersion() == 8) {
                String dir = javaInfo.javaHome();
                if (!dir.endsWith("jre")) {
                    dir = dir + "/jre";
                }
                Set<String> dirs = Stream.of(new File(dir))
                        .filter(file -> !file.isDirectory())
                        .map(File::getName)
                        .collect(Collectors.toSet());
                if (!dirs.contains("fips140-3")) {
                    validEnv = false;
                    Log.warning(FIPSTestUtils.class, "Java 8 install does not support FIPS140-3");
                }
            } else {
                String javaSecurityPath = javaInfo.javaHome() + "/conf/security/java.security";
                Path path = Paths.get(javaSecurityPath);

                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    String line;
                    boolean fipsCompatible = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("OpenJCEPlusFIPS.FIPS140-3-Strongly-Enforced")) {
                            fipsCompatible = true;
                        }
                    }
                    if (!fipsCompatible) {
                        validEnv = fipsCompatible;
                        Log.warning(FIPSTestUtils.class, "Java install is not FIPS compatible");
                    }
                } catch (IOException e) {
                    validEnv = false;
                    Log.warning(FIPSTestUtils.class, "unable to read java.security file, skipping the tests");
                }
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
