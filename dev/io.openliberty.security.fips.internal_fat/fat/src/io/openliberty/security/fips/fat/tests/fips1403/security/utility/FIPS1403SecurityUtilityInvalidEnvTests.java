/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.security.fips.fat.tests.fips1403.security.utility;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.fips.fat.FIPSTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.openliberty.security.fips.fat.FIPSTestUtils.SEC_CONF_FIPS_COMMAND;
import static io.openliberty.security.fips.fat.tests.fips1403.security.utility.FIPS1403SecurityUtilityTests.runSecurityUtilityCommand;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
@SkipIfSysProp({SkipIfSysProp.OS_ZOS, SkipIfSysProp.OS_IBMI})
/**
 * This is tests for environments that should never be FIPS compatible e.g. Non-Semeru OpenJDK JDKs
 * Therefore any additional checks that we might apply to ensure a valid env such as sunsetDate should not be applied here
 */
public class FIPS1403SecurityUtilityInvalidEnvTests{

    private static final String SERVER_NAME = "FIPSServer";
    private static Machine machine;
    private static Properties env;
    private static String installRoot;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws IOException {
        JavaInfo ji = JavaInfo.forServer(server);
        boolean validEnv = false;
        if (ji.majorVersion() == 8) {
            String dir = ji.javaHome();
            Set<String> dirs = Stream.of(new File(dir).listFiles())
                    .filter(File::isDirectory)
                    .map(File::getName)
                    .collect(Collectors.toSet());
            if (dirs.contains("fips140-3")) {
                validEnv = true;
            }
        }  else {
            String javaSecurityPath = ji.javaHome() + "/conf/security/java.security";
            Path path = Paths.get(javaSecurityPath);
            if (path.toFile().exists()) {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    String line;
                    boolean fipsCompatible = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("OpenJCEPlusFIPS.FIPS140-3-Strongly-Enforced")) {
                            fipsCompatible = true;
                        }
                    }
                    validEnv = fipsCompatible;
                } catch (IOException e) {
                    throw e;
                }
            }
        }
        assumeThat(validEnv, is(false));

        installRoot = server.getInstallRoot();
        env = new Properties();
        machine = server.getMachine();
    }

    @Test
    public void invalidFIPSEnvironmentCheck() throws Exception {
        ProgramOutput po = FIPSTestUtils.runSecurityUtilityCommand(machine, installRoot, FIPSTestUtils.SEC_UTILITY_COMMAND, new String[] {SEC_CONF_FIPS_COMMAND}, env);
        // cxommand should fail for non-FIPS envs
        assertEquals("securityUtility configureFIPS did not result in expected return code.",1, po.getReturnCode());
        // the error test comes out on Stdout, not stderr
        String result = po.getStdout();
        assertTrue(!result.isEmpty());
        // Semeru version strings are most language agnostic part of the invalid env check
        assertTrue(result.contains("11.0.29.0, 17.0.17.0, 21.0.9.0, 25.0.1.0"));

    }
}
