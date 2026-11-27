/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.mcp.internal.fat.introspector;

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import io.openliberty.mcp.internal.fat.suite.BetaRepeatAction;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;

/**
 * Skipped on the beta runtime repeat: this test does not depend on the protocol version, and each server dump
 * takes about a minute, so repeating it adds about three minutes to the build.
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
@SkipForRepeat(BetaRepeatAction.ID)
public class IntrospectorMultiAppTest {

    private static final String BASIC_TOOLS = "BasicTools";
    private static final String INTROSPECTOR_TESTAPP = "IntrospectorTestapp";
    private static final String ROOT_CONTEXT_APP = "RootContextApp";

    private static final String IBM_WEB_EXT_ROOT = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <web-ext xmlns="http://websphere.ibm.com/xml/ns/javaee"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="http://websphere.ibm.com/xml/ns/javaee
                                                 http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_1.xsd"
                             version="1.1">
                        <context-root uri="/"/>
                    </web-ext>
                    """;

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, BASIC_TOOLS + ".war")
                                   .addClasses(BasicTools.class);

        WebArchive testApp = ShrinkWrap.create(WebArchive.class, INTROSPECTOR_TESTAPP + ".war")
                                       .addClasses(IntrospectorTestTools.class);

        WebArchive rootWar = ShrinkWrap.create(WebArchive.class, ROOT_CONTEXT_APP + ".war")
                                       .addClasses(IntrospectorTestTools.class)
                                       .addAsWebInfResource(new StringAsset(IBM_WEB_EXT_ROOT), "ibm-web-ext.xml");

        serverLifecycle.deployDropinApp(war);
        serverLifecycle.deployDropinApp(testApp);
        serverLifecycle.deployDropinApp(rootWar);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(ROOT_CONTEXT_APP);
        serverLifecycle.undeployDropinApp(BASIC_TOOLS);
        // INTROSPECTOR_TESTAPP may already be undeployed by the test — only remove if still present
        if (serverLifecycle.getServer().fileExistsInLibertyServerRoot("dropins/" + INTROSPECTOR_TESTAPP + ".war")) {
            serverLifecycle.undeployDropinApp(INTROSPECTOR_TESTAPP);
        }
    }

    @Test
    public void testMultipleApplicationsToolsAppearAndDisappearInMcpIntrospector() throws Exception {

        // Dump and verify both applications appear
        String introspection1 = getIntrospectorDumpContents();

        assertFalse("Did NOT expect any context path to be listed as 'unknown-app'",
                    introspection1.contains("Context path: unknown-app"));
        // Check for expected applications
        assertTrue("Expected BasicIntroSpector application to be listed",
                   introspection1.contains("Context path: BasicTools"));

        assertTrue("Expected Testapp application to be listed",
                   introspection1.contains("Context path: IntrospectorTestapp"));

        // Check for at least one tool per application
        assertTrue("Expected tool 'IntrospectTool' from IntrospectorTestTools",
                   introspection1.contains("Context path: IntrospectorTestapp") && introspection1.contains("Tool: IntrospectTool"));

        assertTrue("Expected at least one tool from BasicTools",
                   introspection1.contains("Context path: BasicTools") && introspection1.contains("Tool:"));

        // Undeploy Testapp manually
        Path testAppPath = Paths.get(serverLifecycle.getServer().getServerRoot(), "dropins", "Testapp.war");
        serverLifecycle.getServer().deleteFileFromLibertyServerRoot("dropins/IntrospectorTestapp.war");
        assertFalse("Testapp.war was not deleted", Files.exists(testAppPath));

        // Wait for undeploy to take effect
        serverLifecycle.getServer().removeInstalledAppForValidation(INTROSPECTOR_TESTAPP);

        // Redump and verify Testapp tools are gone, BasicIntroSpector still there
        String introspection2 = getIntrospectorDumpContents();

        assertTrue("Expected BasicIntroSpector to still be listed",
                   introspection2.contains("Context path: " + BASIC_TOOLS));

        assertFalse("Did NOT expect Testapp after undeploy",
                    introspection2.contains("Context path: " + INTROSPECTOR_TESTAPP));

        assertFalse("Did NOT expect tool 'IntrospectTool' after undeploy",
                    introspection2.contains("Tool: helloFromTestApp"));

    }

    /**
     * Verifies that a web module deployed at context root {@code /} appears in the introspection output
     * as {@code Context path: /} and is never labelled {@code unknown-app}.
     */
    @Test
    public void testRootContextPathAppIsNotLabelledUnknownApp() throws Exception {
        String introspection = getIntrospectorDumpContents();

        assertFalse("Did NOT expect context path '/' to be labelled 'unknown-app'",
                    introspection.contains("Context path: unknown-app"));

        assertTrue("Expected root context path app to appear as 'Context path: /'",
                   introspection.contains("Context path: /"));
    }

    /**
     * Runs a server dump and extracts the contents of the {@code MCPIntrospector.txt} file.
     *
     * <p>This method finds the generated dump ZIP file, locates the introspector text file inside it,
     * and returns its contents as a string.
     *
     * @return contents of {@code MCPIntrospector.txt} from the server dump
     * @throws Exception if the dump file or introspector entry is missing or cannot be read
     */
    private String getIntrospectorDumpContents() throws Exception {
        ProgramOutput output = serverLifecycle.getServer().serverDump(null);
        String stdout = output.getStdout();

        Pattern zipPathPattern = Pattern.compile("dump complete in (.*?\\.zip)\\.");
        Matcher matcher = zipPathPattern.matcher(stdout);
        assertTrue("Could not find dump zip path in stdout", matcher.find());

        String zipFilePath = matcher.group(1);
        Log.info(IntrospectorMultiAppTest.class, "getIntrospectorDumpContents", "Server dump output to " + zipFilePath);

        try (ZipFile zip = new ZipFile(zipFilePath)) {
            ZipEntry introspectorEntry = null;
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith("MCPIntrospector.txt")) {
                    introspectorEntry = entry;
                    break;
                }
            }

            assertNotNull("MCPIntrospector.txt not found in dump zip", introspectorEntry);

            StringBuilder contents = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                                                            new java.io.InputStreamReader(zip.getInputStream(introspectorEntry), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    contents.append(line).append('\n');
                }
            }

            return contents.toString();
        }
    }
}
