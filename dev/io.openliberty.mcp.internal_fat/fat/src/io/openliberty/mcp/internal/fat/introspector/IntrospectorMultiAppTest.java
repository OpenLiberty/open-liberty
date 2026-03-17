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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
public class IntrospectorMultiAppTest {

    private static final String BASIC_TOOLS = "BasicTools";
    private static final String INTROSPECTOR_TESTAPP = "IntrospectorTestapp";

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, BASIC_TOOLS + ".war")
                                   .addClasses(BasicTools.class);

        WebArchive testApp = ShrinkWrap.create(WebArchive.class, INTROSPECTOR_TESTAPP + ".war")
                                       .addClasses(IntrospectorTestTools.class);

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, testApp, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testMultipleApplicationsToolsAppearAndDisappearInMcpIntrospector() throws Exception {

        // Dump and verify both applications appear
        String introspection1 = getIntrospectorDumpContents();

        assertFalse("Did NOT expect any application to be named 'unknown-app'",
                    introspection1.contains("Application: unknown-app"));
        // Check for expected applications
        assertTrue("Expected BasicIntroSpector application to be listed",
                   introspection1.contains("Application: BasicTools"));

        assertTrue("Expected Testapp application to be listed",
                   introspection1.contains("Application: IntrospectorTestapp"));

        // Check for at least one tool per application
        assertTrue("Expected tool 'IntrospectTool' from IntrospectorTestTools",
                   introspection1.contains("Application: IntrospectorTestapp") && introspection1.contains("Tool: IntrospectTool"));

        assertTrue("Expected at least one tool from BasicTools",
                   introspection1.contains("Application: BasicTools") && introspection1.contains("Tool:"));

        // Undeploy Testapp manually
        Path testAppPath = Paths.get(server.getServerRoot(), "dropins", "Testapp.war");
        server.deleteFileFromLibertyServerRoot("dropins/IntrospectorTestapp.war");
        assertFalse("Testapp.war was not deleted", Files.exists(testAppPath));

        // Wait for undeploy to take effect
        server.removeInstalledAppForValidation(INTROSPECTOR_TESTAPP);

        // Redump and verify Testapp tools are gone, BasicIntroSpector still there
        String introspection2 = getIntrospectorDumpContents();

        assertTrue("Expected BasicIntroSpector to still be listed",
                   introspection2.contains("Application: " + BASIC_TOOLS));

        assertFalse("Did NOT expect Testapp after undeploy",
                    introspection2.contains("Application: " + INTROSPECTOR_TESTAPP));

        assertFalse("Did NOT expect tool 'IntrospectTool' after undeploy",
                    introspection2.contains("Tool: helloFromTestApp"));

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
        ProgramOutput output = server.serverDump(null);
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
