/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.internal.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.CDIVersion;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.authorization.ejb.annotations.EJBAnnotationServlet1;
import io.openliberty.security.authorization.fat.TestUtil;
import io.openliberty.security.authorization.rest.annotations.RestApplication;
import io.openliberty.security.authorization.web.annotations.WebAnnotationServlet1;

@RunWith(FATRunner.class)
public class AnnotationRolesTest extends AbstractRolesTest {

    private static final String SERVER_NAME = "AnnotationTest";

    @ClassRule
    public static RepeatTests repeats = RepeatTests.withoutModification() //
                    .andWith(new FeatureReplacementAction().forServers(SERVER_NAME).addFeature("appAuthorization-3.0").withID(WITH_AUTHZ_ID)) //
                    .andWith(new FeatureReplacementAction().forServers(SERVER_NAME).addFeature("usr:authzTestProvider-3.0").withID(WITH_POLICY_ID));

    @Server(SERVER_NAME)
    public static LibertyServer staticServer;

    @BeforeClass
    public static void setUp() throws Exception {
        installJaccUserFeature(staticServer);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (staticServer.isStarted()) {
            staticServer.stopServer();
        }
        uninstallJaccUserFeature(staticServer);
    }

    protected LibertyServer getLibertyServer() {
        return staticServer;
    }

    @Test
    public void validateServlet() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "annotationTest.war") //
                        .addPackages(false, TestUtil.class.getPackageName(), WebAnnotationServlet1.class.getPackageName()) //
                        .addAsWebInfResource(BeansAsset.getBeansAsset(DiscoveryMode.ALL, CDIVersion.CDI41), "beans.xml");
        if (RepeatTestFilter.isRepeatActionActive(WITH_POLICY_ID)) {
            war.addAsWebInfResource(WebAnnotationServlet1.class.getPackage(), "web.xml", "web.xml");
        }
        LibertyServer server = getLibertyServer();
        if (!server.isStarted()) {
            ShrinkHelper.exportAppToServer(server, war, DeployOptions.SERVER_ONLY);
            server.startServer();
        } else {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportAppToServer(server, war, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);
            assertNotNull("The application annotationTest did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* annotationTest"));
        }

        HttpClient client = createHttpClient("user1", "user1pwd");
        HttpResponse<String> response = sendGetRequest(server, client, "/annotationTest/servlet1");
        assertEquals(200, response.statusCode());
        validateResponse(response.body(), "user1", Set.of("WebRole1", "WebRole3"), Set.of("CDIRole2", "CDIRole3", "CDIRole4",
                                                                                          "WebRole1", "WebRole3", "WebRole4",
                                                                                          "EJBRole1", "EJBRole2", "EJBRole4",
                                                                                          "RestRole1", "RestRole2", "RestRole3",
                                                                                          "WebServicesRole2", "WebServicesRole3", "WebServicesRole4",
                                                                                          "AllAuthenticated"),
                         false, InRoleOutput.DECLARED_IN_COMPONENT);
        client = createHttpClient();
        response = sendGetRequest(server, client, "/annotationTest/servlet1");
        assertEquals(401, response.statusCode());

        if (RepeatTestFilter.isRepeatActionActive(WITH_POLICY_ID)) {
            response = sendGetRequest(server, client, "/annotationTest/servlet1/allowUnauthenticated");
            assertEquals(200, response.statusCode());
            validateResponse(response.body(), "Not authenticated", Set.of(), Set.of(),
                             false, InRoleOutput.DECLARED_IN_COMPONENT);
        }
    }

    @Test
    public void validateEJB() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "annotationTest.war") //
                        .addPackages(false, TestUtil.class.getPackageName(), EJBAnnotationServlet1.class.getPackageName()) //
                        .addAsWebInfResource(BeansAsset.getBeansAsset(DiscoveryMode.ALL, CDIVersion.CDI41), "beans.xml");
        if (RepeatTestFilter.isRepeatActionActive(WITH_POLICY_ID)) {
            war.addAsWebInfResource(EJBAnnotationServlet1.class.getPackage(), "overrideweb.xml", "web.xml");
        } else {
            war.addAsWebInfResource(EJBAnnotationServlet1.class.getPackage(), "web.xml", "web.xml");
        }
        LibertyServer server = getLibertyServer();
        if (!server.isStarted()) {
            ShrinkHelper.exportAppToServer(server, war, DeployOptions.SERVER_ONLY);
            server.startServer();
        } else {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportAppToServer(server, war, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);
            assertNotNull("The application annotationTest did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* annotationTest"));
        }

        HttpClient client = createHttpClient("user1", "user1pwd");
        HttpResponse<String> response = sendGetRequest(server, client, "/annotationTest/ejbServlet1");
        assertEquals(200, response.statusCode());
        validateResponse(response.body(), "user1", Set.of("EJBRole1", "EJBRole2"), Set.of("CDIRole2", "CDIRole3", "CDIRole4",
                                                                                          "WebRole1", "WebRole3", "WebRole4",
                                                                                          "EJBRole1", "EJBRole2", "EJBRole4",
                                                                                          "RestRole1", "RestRole2", "RestRole3",
                                                                                          "WebServicesRole2", "WebServicesRole3", "WebServicesRole4",
                                                                                          "AllAuthenticated"),
                         // When using not using a policy, all roles are returned instead of only the ones declared in the EJBs.
                         // When there is a policy, then it only returns the ones that are declared.
                         false, RepeatTestFilter.isRepeatActionActive(WITH_POLICY_ID) ? InRoleOutput.DECLARED_IN_COMPONENT : InRoleOutput.ALL);

        client = createHttpClient();
        response = sendGetRequest(server, client, "/annotationTest/ejbServlet1");
        assertEquals(401, response.statusCode());

        if (RepeatTestFilter.isRepeatActionActive(WITH_POLICY_ID)) {
            response = sendGetRequest(server, client, "/annotationTest/ejbServlet1/allowUnauthenticated");
            assertEquals(200, response.statusCode());
            validateResponse(response.body(), "Not authenticated", Set.of(), Set.of(),
                             false, InRoleOutput.DECLARED_IN_COMPONENT);
        }
    }

    @Test
    @SkipForRepeat(WITH_POLICY_ID) // Rest Permissions are not put into the Jakarta Authorization policy, so will not get expected behavior
    public void validateRest() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "annotationTest.war") //
                        .addPackages(false, TestUtil.class.getPackageName(), RestApplication.class.getPackageName()) //
                        .addAsWebInfResource(BeansAsset.getBeansAsset(DiscoveryMode.ALL, CDIVersion.CDI41), "beans.xml");
        //.addAsWebInfResource(RestApplication.class.getPackage(), "web.xml", "web.xml");
        LibertyServer server = getLibertyServer();
        if (!server.isStarted()) {
            ShrinkHelper.exportAppToServer(server, war, DeployOptions.SERVER_ONLY);
            server.startServer();
        } else {
            server.setMarkToEndOfLog();
            ShrinkHelper.exportAppToServer(server, war, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);
            assertNotNull("The application annotationTest did not appear to have been updated.",
                          server.waitForStringInLog("CWWKZ0003I.* annotationTest"));
        }

        HttpClient client = createHttpClient("user1", "user1pwd");
        HttpResponse<String> response = sendGetRequest(server, client, "/annotationTest/rest/resource1/doTest");
        assertEquals(200, response.statusCode());
        validateResponse(response.body(), "user1", Set.of("RestRole1", "RestRole2"), Set.of("CDIRole2", "CDIRole3", "CDIRole4",
                                                                                            "WebRole1", "WebRole3", "WebRole4",
                                                                                            "EJBRole1", "EJBRole2", "EJBRole4",
                                                                                            "RestRole1", "RestRole2", "RestRole3",
                                                                                            "WebServicesRole2", "WebServicesRole3", "WebServicesRole4",
                                                                                            "AllAuthenticated"),
                         false, InRoleOutput.DECLARED_IN_COMPONENT);

        client = createHttpClient();
        response = sendGetRequest(server, client, "/annotationTest/rest/resource1/doTest");
        assertEquals(401, response.statusCode());
    }
}
