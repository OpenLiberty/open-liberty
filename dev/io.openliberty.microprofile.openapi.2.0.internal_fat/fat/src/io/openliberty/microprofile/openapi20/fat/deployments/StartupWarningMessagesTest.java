/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.deployments;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.MpOpenAPIElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi20.fat.FATSuite;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestApp;
import io.openliberty.microprofile.openapi20.fat.deployments.test1.DeploymentTestResource;
import io.openliberty.microprofile.openapi20.fat.deployments.test2.DeploymentTestResource2;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class StartupWarningMessagesTest {
    private static final String SERVER_NAME = "OpenAPIWarningMessageTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        server.saveServerConfiguration();
    }

    @After
    public void cleanup() throws Exception {
        try {
            if (server.isStarted()) {
                server.stopServer("CWWKO1680W|CWWKO1684W");
            }
        } finally {
            server.deleteAllDropinApplications();
            server.removeAllInstalledAppsForValidation();
            server.clearAdditionalSystemProperties();
            server.restoreServerConfiguration();
        }
    }

    @Test
    public void testAppNameMatchesOtherAppDeploymentName() throws Exception {

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "nameClash.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        ServerConfiguration serverConfig = server.getServerConfiguration().clone();
        serverConfig.addApplication("nameClash", "test1.war", "war");
        serverConfig.addApplication("nameUnclashed", "nameClash.war", "war");

        MpOpenAPIElement.MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                // "nameClash" matches the server.xml name of one app
                                                // and the deployment descriptor name of the other
                                                .addIncludedApplicaiton("nameClash")
                                                .buildAndOverwrite(serverConfig.getMpOpenAPIElement());

        server.updateServerConfiguration(serverConfig);

        // Set up validation manually because the name doesn't match the archive filename
        ShrinkHelper.exportAppToServer(server, war1, SERVER_ONLY, DISABLE_VALIDATION);
        server.addInstalledAppForValidation("nameClash");
        ShrinkHelper.exportAppToServer(server, war2, SERVER_ONLY, DISABLE_VALIDATION);
        server.addInstalledAppForValidation("nameUnclashed");

        server.startServer();

        // Expect no warning because "nameClash" does match the server.xml name of an app
        // Example message: CWWKO1680W: The testEar application name in the includeApplication or includeModule configuration element does not match the name of any deployed application but it does match the name from the deployment descriptor of the serverXMLName application. The application name used here must be the application name specified in server.xml, or the archive file name with the extension removed if no name is specified in server.xml.
        assertThat(server.findStringsInLogs("CWWKO1680W"), is(empty()));

        // Asserts we have no other error messages
        server.stopServer();
    }

    @Test
    public void testWarningWhenMatchesDeploymentName() throws Exception {

        ServerConfiguration serverConfig = server.getServerConfiguration().clone();
        serverConfig.addApplication("serverXMLNameIncluded", "testEarIncluded.ear", "ear");
        serverConfig.addApplication("serverXMLNameExcluded", "testEarExcluded.ear", "ear");

        MpOpenAPIElement.MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                //these match the application's deployment descriptor but not its name as set in server.xml.
                                                //a warning should be emitted informing the user of their likely mistake and how o fix it.
                                                .addIncludedApplicaiton("testEarIncluded")
                                                .addExcludedApplicaiton("testEarExcluded")
                                                .addIncludedModule("testEarIncluded/test1")
                                                .addExcludedModule("testEarExcluded/test2")
                                                .buildAndOverwrite(serverConfig.getMpOpenAPIElement());

        server.updateServerConfiguration(serverConfig);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "testEarIncluded.ear")
                                          .addAsModules(war1);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);

        EnterpriseArchive ear2 = ShrinkWrap.create(EnterpriseArchive.class, "testEarExcluded.ear")
                                           .addAsModules(war2);

        ShrinkHelper.exportAppToServer(server, ear, SERVER_ONLY, DISABLE_VALIDATION);//set up validation manually because the app name doesn't match the archive name
        server.addInstalledAppForValidation("serverXMLNameIncluded");
        ShrinkHelper.exportAppToServer(server, ear2, SERVER_ONLY, DISABLE_VALIDATION);
        server.addInstalledAppForValidation("serverXMLNameExcluded");

        server.startServer();

        //Example message: CWWKO1680W: The testEar application name in the includeApplication or includeModule configuration element does not match the name of any deployed application but it does match the name from the deployment descriptor of the serverXMLName application. The application name used here must be the application name specified in server.xml, or the archive file name with the extension removed if no name is specified in server.xml.
        List<String> messages = new ArrayList<>();
        messages.add("CWWKO1680W: The testEarIncluded application name in the includeModule configuration.*serverXMLNameIncluded application. The application name");
        messages.add("CWWKO1680W: The testEarIncluded application name in the includeApplication configuration.*serverXMLNameIncluded application. The application name");
        messages.add("CWWKO1680W: The testEarExcluded application name in the excludeModule configuration.*serverXMLNameExcluded application. The application name");
        messages.add("CWWKO1680W: The testEarExcluded application name in the excludeApplication configuration.*serverXMLNameExcluded application. The application name");
        server.waitForStringsInLogUsingMark(messages); //This method asserts the messages exist so no need to check its output.

        server.stopServer("CWWKO1680W"); // Assert there are no other error messages
    }

    @Test
    public void testUnusedConfigWarningPresent() throws Exception {
        ServerConfiguration serverConfig = server.getServerConfiguration().clone();

        MpOpenAPIElement.MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                //these match the application's deployment descriptor but not its name as set in server.xml.
                                                //a warning should be emitted informing the user of their likely mistake and how o fix it.
                                                .addIncludedApplicaiton("test1")
                                                .addIncludedApplicaiton("appWibble")
                                                .addIncludedModule("test1/module")
                                                .buildAndOverwrite(serverConfig.getMpOpenAPIElement());

        server.updateServerConfiguration(serverConfig);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        ShrinkHelper.exportDropinAppToServer(server, war1, SERVER_ONLY);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        ShrinkHelper.exportDropinAppToServer(server, war2, SERVER_ONLY);

        server.startServer();

        // Example message: CWWKO1684W: The includeApplication configuration element includes "appWibble" but that does not match any deployed application or web module.
        List<String> messages = new ArrayList<>();
        messages.add("CWWKO1684W:.*includeApplication.*appWibble");
        messages.add("CWWKO1684W:.*includeModule.*test1/module");
        server.waitForStringsInLogUsingMark(messages);

        // Assert there are no other errors
        server.stopServer("CWWKO1684W");
    }

    @Test
    public void testUnusedConfigWarningNotPresent() throws Exception {
        ServerConfiguration serverConfig = server.getServerConfiguration().clone();

        MpOpenAPIElement.MpOpenAPIElementBuilder.cloneBuilderFromServerResetAppsAndModules(server)
                                                //these match the application's deployment descriptor but not its name as set in server.xml.
                                                //a warning should be emitted informing the user of their likely mistake and how o fix it.
                                                .addIncludedApplicaiton("test1")
                                                .addIncludedApplicaiton("test2")
                                                .buildAndOverwrite(serverConfig.getMpOpenAPIElement());

        server.updateServerConfiguration(serverConfig);

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "test1.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource.class);
        ShrinkHelper.exportDropinAppToServer(server, war1, SERVER_ONLY);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "test2.war")
                                    .addClasses(DeploymentTestApp.class, DeploymentTestResource2.class);
        ShrinkHelper.exportDropinAppToServer(server, war2, SERVER_ONLY);

        server.startServer();

        assertNotNull(server.waitForStringInTrace("Checking for unused configuration entries"));
        assertThat(server.findStringsInLogs("CWWKO1684W"), is(empty())); // No warning about app configured but not deployed

        // Assert there are no other errors
        server.stopServer();
    }

}
