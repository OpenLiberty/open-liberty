/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config13.varExpansion.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Servlet for VarExpansionTest.
 *
 * Tests that server.xml {@code <variable>} list expansion (via the
 * {@code list()} function) is correctly surfaced through MicroProfile Config.
 */
@WebServlet("/VarExpansionServlet")
public class VarExpansionServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    UtilBean utilBean;

    /**
     * Verifies that comma-separated server.xml variables exposed via
     * {@code list()} are injected as {@code String[]} and {@code List<String>}
     * with the expected individual values.
     */
    @Test
    public void testAppPropertyExpansionOfList() throws Exception {

        utilBean.getAndCheckVarValue("app.appPropertyExamplePorts", new String[] { "27017" }); //We get this value from io.openliberty.microprofile.config.internal.serverxml.AppPropertiesComponent.activate
        //And its already been stripped down to the first entry.

        List<String> hostsTestList = new ArrayList<String>();
        hostsTestList.add("mongo1.example.com");//Same limitation as above
        utilBean.getAndCheckVarValue("app.appPropertyExampleHosts", hostsTestList);
    }

    /**
     * Verifies that server.xml {@code <variable>} entries are surfaced directly
     * through MicroProfile Config (without going via appProperties) and that the
     * full comma-separated value is converted to a {@code String[]} and
     * {@code List<String>} containing all expected elements.
     */
    @Test
    public void testDirectVariableExpansionOfList() throws Exception {

        //This method's variables are coming from ServerXMLVariableConfigSource.getProperties without needing
        //any extra code for handling list expansion
        utilBean.getAndCheckVarValue("examplePorts", new String[] { "27017", "27018", "27019" });

        List<String> hostsTestList = new ArrayList<String>();
        hostsTestList.add("mongo1.example.com");
        hostsTestList.add("mongo2.example.com");
        hostsTestList.add("mongo3.example.com");
        utilBean.getAndCheckVarValue("exampleHosts", hostsTestList);
    }

    /**
     * Verifies that server.xml arithmetic variables are surfaced directly
     * through MicroProfile Config (without going via appProperties) and that
     * each resolved integer value is correct.
     *
     * The server.xml defines:
     *
     * <pre>
     *   arithmatic_one        = 1
     *   arithmatic_two        = ${one+1}        → 2
     *   arithmatic_three      = ${one+two}      → 3
     *   arithmatic_six        = ${two*three}    → 6
     *   arithmatic_five       = ${six-one}      → 5
     *   arithmatic_threeagain = ${six/two}      → 3
     * </pre>
     */
    @Test
    @Mode(TestMode.EXPERIMENTAL) //The code for server.xml arithmatic is not exposed on any OSGi interface, at all.
    public void testDirectArithmaticVariables() throws Exception {

        utilBean.getAndCheckVarValue("arithmatic_one", "1");
        utilBean.getAndCheckVarValue("arithmatic_two", "2");
        utilBean.getAndCheckVarValue("arithmatic_three", "3");
        utilBean.getAndCheckVarValue("arithmatic_six", "6");
        utilBean.getAndCheckVarValue("arithmatic_five", "5");
        utilBean.getAndCheckVarValue("arithmatic_threeagain", "3");
    }

    /**
     * Verifies that server.xml arithmetic variables surfaced via
     * {@code appProperties} carry the same resolved integer values.
     */
    @Test
    @Mode(TestMode.EXPERIMENTAL) //The code for server.xml arithmatic is not exposed on any OSGi interface, at all.
    public void testAppPropertyArithmaticVariables() throws Exception {

        utilBean.getAndCheckVarValue("app.appPropertyArithmaticOne", "1");
        utilBean.getAndCheckVarValue("app.appPropertyArithmaticTwo", "2");
        utilBean.getAndCheckVarValue("app.appPropertyArithmaticThree", "3");
        utilBean.getAndCheckVarValue("app.appPropertyArithmaticSix", "6");
        utilBean.getAndCheckVarValue("app.appPropertyArithmaticFive", "5");
        utilBean.getAndCheckVarValue("app.appPropertyArithmaticThreeAgain", "3");
    }

    /**
     * Verifies that the Open Liberty predefined variables are accessible via
     * MicroProfile Config and hold structurally valid, self-consistent values.
     *
     * <p>The expected values are derived at runtime from Java system properties
     * (which Liberty populates before the JVM starts), so the test is
     * machine-independent and requires no hard-coded paths.
     *
     * <p>Variables verified:
     * <ul>
     *   <li>{@code wlp.install.dir}   – Liberty installation directory</li>
     *   <li>{@code wlp.server.name}   – server name</li>
     *   <li>{@code wlp.user.dir}      – usr directory (default: {@code ${wlp.install.dir}/usr})</li>
     *   <li>{@code shared.app.dir}    – shared apps directory (default: {@code ${wlp.user.dir}/shared/apps})</li>
     *   <li>{@code shared.config.dir} – shared config directory (default: {@code ${wlp.user.dir}/shared/config})</li>
     *   <li>{@code shared.resource.dir} – shared resources directory (default: {@code ${wlp.user.dir}/shared/resources})</li>
     *   <li>{@code server.config.dir} – server configuration directory (default: {@code ${wlp.user.dir}/servers/${wlp.server.name}})</li>
     *   <li>{@code server.output.dir} – server output directory (default: same as {@code server.config.dir})</li>
     * </ul>
     */
    @Test
    public void testPredefinedVariables() throws Exception {

        // Obtain expected values from system properties set by Liberty at startup.
        // This keeps the test machine-independent: the assertions are structural,
        // not tied to any absolute path on a specific host.
        String installDir = System.getProperty("wlp.install.dir");
        String serverName = System.getProperty("wlp.server.name");
        String userDir    = System.getProperty("wlp.user.dir");

        assertNotNull("System property wlp.install.dir must be set by Liberty", installDir);
        assertNotNull("System property wlp.server.name must be set by Liberty", serverName);
        assertNotNull("System property wlp.user.dir must be set by Liberty", userDir);

        // 1. wlp.install.dir – must match the system property value
        utilBean.getAndCheckVarValue("wlp.install.dir", installDir);

        // 2. wlp.server.name – must match the system property value
        utilBean.getAndCheckVarValue("wlp.server.name", serverName);

        // 3. wlp.user.dir – must match the system property value
        utilBean.getAndCheckVarValue("wlp.user.dir", userDir);

        // 4. shared.app.dir – must be under wlp.user.dir
        utilBean.getAndCheckVarValueContains("shared.app.dir", userDir.replace("\\", "/"));

        // 5. shared.config.dir – must be under wlp.user.dir
        utilBean.getAndCheckVarValueContains("shared.config.dir", userDir.replace("\\", "/"));

        // 6. shared.resource.dir – must be under wlp.user.dir
        utilBean.getAndCheckVarValueContains("shared.resource.dir", userDir.replace("\\", "/"));

        // 7. server.config.dir – must contain the server name
        utilBean.getAndCheckVarValueContains("server.config.dir", serverName);

        // 8. server.output.dir – must contain the server name (defaults to server.config.dir)
        utilBean.getAndCheckVarValueContains("server.output.dir", serverName);

        // Structural invariants between the variables
        String sharedAppDir     = System.getProperty("shared.app.dir",      userDir + "/shared/apps");
        String sharedConfigDir  = System.getProperty("shared.config.dir",   userDir + "/shared/config");
        String sharedResourceDir = System.getProperty("shared.resource.dir", userDir + "/shared/resources");
        String serverConfigDir  = System.getProperty("server.config.dir",   userDir + "/servers/" + serverName);
        String serverOutputDir  = System.getProperty("server.output.dir",   serverConfigDir);

        // shared dirs must sit within wlp.user.dir
        assertTrue("shared.app.dir should reside under wlp.user.dir",
                   sharedAppDir.replace("\\", "/").startsWith(userDir.replace("\\", "/")));
        assertTrue("shared.config.dir should reside under wlp.user.dir",
                   sharedConfigDir.replace("\\", "/").startsWith(userDir.replace("\\", "/")));
        assertTrue("shared.resource.dir should reside under wlp.user.dir",
                   sharedResourceDir.replace("\\", "/").startsWith(userDir.replace("\\", "/")));

        // server dirs must contain the server name
        assertTrue("server.config.dir should contain the server name",
                   serverConfigDir.replace("\\", "/").contains(serverName));
        assertTrue("server.output.dir should contain the server name",
                   serverOutputDir.replace("\\", "/").contains(serverName));
    }

}
