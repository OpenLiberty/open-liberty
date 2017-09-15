/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
public class ConfigExtensionsTest {

    /**
     * Utility to set the method name as a String before the test
     */
    @Rule
    public TestName name = new TestName();

    public String testName = "";
    private static final String servicePidName = "service.pid";

    @Before
    public void setTestName() {
        // set the current test name
        testName = name.getMethodName();
    }

    private static final String CONTEXT_ROOT = "/config-extensions-test";
    private static final String PID_PASS = "PASSED: test bundle was called with properties for ID";
    private static LibertyServer extensionsServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.extensions");

    @BeforeClass
    public static void setUpForConfigExtensionsTests() throws Exception {
        //copy the extensions tests features into the server features location
        extensionsServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configExtensionsTest-1.0.mf");
        extensionsServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configExtensionsTestB-1.0.mf");
        //copy the extensions tests bundles into the server lib location
        extensionsServer.copyFileToLibertyInstallRoot("lib", "bundles/test.config.extensions_1.0.0.jar");
        extensionsServer.copyFileToLibertyInstallRoot("lib", "bundles/test.config.extensions.b_1.0.0.jar");
        extensionsServer.copyFileToLibertyInstallRoot("lib", "bundles/test.config.extensions.schema.generator_1.0.0.jar");

        //use our default server.xml to start with
        extensionsServer.setServerConfigurationFile("extensions/server.xml");
        extensionsServer.startServer();
        //make sure the URL is available
        extensionsServer.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        extensionsServer.stopServer();
        // Delete the files we copied over
        extensionsServer.deleteFileFromLibertyInstallRoot("lib/features/configExtensionsTest-1.0.mf");
        extensionsServer.deleteFileFromLibertyInstallRoot("lib/features/configExtensionsTestB-1.0.mf");

        extensionsServer.deleteFileFromLibertyInstallRoot("lib/test.config.extensions_1.0.0.jar");
        extensionsServer.deleteFileFromLibertyInstallRoot("lib/test.config.extensions.b_1.0.0.jar");
        extensionsServer.deleteFileFromLibertyInstallRoot("lib/test.config.extensions.schema.generator_1.0.0.jar");

    }

    /**
     * Tests whether the ibm:extends works at runtime.
     * The test verifies that the delegating factory service registered by the ExtendedMetatypeManager
     * does make a call to the factory service that is provided by the super type
     */
    @Test
    public void testExtendsBasic() throws Exception {
        String servicePid = servicePidName + "=test.config.extensions.super";
        //verify that the MSF was called with config for the correct id as expected
        //If the ID property was incorrect, the delegating listener forwarded the wrong information or was provided the wrong information by config
        HttpUtils.findStringInUrl(extensionsServer, CONTEXT_ROOT + "/test?id=test.config.extensions.sub.config1&" + servicePid, PID_PASS);
    }

    /**
     * Tests whether the ibm:rename works at runtime.
     * The test verifies that an attribute specified in the server.xml using an ibm:rename
     * gets converted to the original name before being passed back to the super type.
     */
    @Test
    public void testExtendsRename() throws Exception {
        String servicePid = servicePidName + "=test.config.extensions.super";
        //check that the renamed property came from test.config.extensions.sub configuration
        //If the renamed property value was not correct, the delegating listener did not pass the value back to the original key name or did not detect the rename
        HttpUtils.findStringInUrl(extensionsServer, CONTEXT_ROOT + "/test?id=test.config.extensions.sub.config1&prop=testAttribute1&" + servicePid, "renamed value");
    }

    /**
     * This is the same as testExtendsBasic, but for a sub type that ibm:extends another
     * sub type, so it checks that the service factory call gets delegated all the way back to the
     * top of the stack.
     */
    @Test
    public void testExtendsHierarchy() throws Exception {
        String servicePid = servicePidName + "=test.config.extensions.super";
        //check that the properties supplied for next go through the hierarchy to the top parent
        HttpUtils.findStringInUrl(extensionsServer, CONTEXT_ROOT + "/test?id=test.config.extensions.sub.sub.config1&" + servicePid, PID_PASS);

        HttpUtils.findStringInUrl(extensionsServer, CONTEXT_ROOT + "/test?id=test.config.extensions.sub.sub.config1&prop=testAttribute1&" + servicePid, "rqd");
        HttpUtils.findStringInUrl(extensionsServer, CONTEXT_ROOT + "/test?id=test.config.extensions.sub.sub.config1&prop=testAttribute2&" + servicePid, "2");
        HttpUtils.findStringInUrl(extensionsServer, CONTEXT_ROOT + "/test?id=test.config.extensions.sub.sub.config1&prop=testAttr3RenameSub&" + servicePid, "3");
        HttpUtils.findStringInUrl(extensionsServer, CONTEXT_ROOT + "/test?id=test.config.extensions.sub.sub.config1&prop=testAttribute4&" + servicePid, "four");
    }

    /**
     * This tests the error scenario where someone tries to use the ibm:extends
     * attribute on a non-factory PID
     */
    @Test
    public void testExtendsNonFactory() throws Exception {
        String id = "CWWKG0061E";
        assertNotNull("The error message " + id + " for trying to use ibm:extends on a non-factory PID was not found",
                      extensionsServer.waitForStringInLog(id + "(?=.*test\\.config\\.extensions\\.sub\\.non\\.factorypid)(?=.*test\\.config\\.extensions\\.sub)"));
    }

    /**
     * This tests the error scenario where someone tries uses the ibm:extends
     * attribute on a factory PID, but the specified super is a non-factory PID
     */
    @Test
    public void testExtendsNonFactorySuper() {
        String id = "CWWKG0062E";
        assertNotNull("The error message " + id + " for trying to use ibm:extends with a non-factory super was not found",
                      extensionsServer.waitForStringInLog(id
                                                          + "(?=.*test\\.config\\.extensions\\.parent\\.non\\.factorypid)(?=.*test\\.config\\.extensions\\.extends\\.non\\.factorypid)"));
    }

    /**
     * This tests the scenario where ibm:extends provides a value that does not exist
     */
    @Test
    public void testInvalidSuper() {
        String id = "CWWKG0059E";
        assertNotNull("The error message " + id + " for invalid super was not found",
                      extensionsServer.waitForStringInLog(id
                                                          + "(?=.*test\\.config\\.extensions\\.sub\\.error\\.no\\.parent\\.pid)(?=.*test\\.config\\.extensions\\.invalid\\.parent)"));
    }

    /**
     * This tests the scenario where ibm:extends provides a value that does not exist
     */
    @Test
    public void testInvalidRename() {
        String id = "CWWKG0067E";
        assertNotNull(
                      "The error message " + id + " for invalid rename was not found",
                      extensionsServer.waitForStringInLog(id
                                                          + "(?=.*testInvalidAttribute)(?=.*testInvalidAttrRename)(?=.*test\\.config\\.extensions\\.sub\\.error\\.no\\.parent\\.override\\.attr)"));
    }

    @Test
    public void testRequiredAttributes() {
        String id = "CWWKG0058E";
        //[ERROR   ] CWWKG0058E: test.config.extensions.extends.attr.required is missing required attribute testAttr4
        assertNotNull("The error message " + id + " for a missing required attribute defined on a sub type was not found",
                      extensionsServer.waitForStringInLog(id + "(?=.*test\\.config\\.extensions\\.extends\\.attr\\.required)" + "(?=.*testAttr4)"));

        assertNotNull("The error message " + id + " for a missing required attribute defined on a super type was not found",
                      extensionsServer.waitForStringInLog(id + "(?=.*test\\.config\\.extensions\\.extends\\.attr\\.required\\.parent)" + "(?=.*testAttribute2)"));
    }

    @Test
    public void testAttemptToOverrideFinalMeta() {
        String id = "CWWKG0060E";
        assertNotNull("The error message " + id + " for attempting to re-assign a final value was not found",
                      extensionsServer.waitForStringInLog(id + "(?=.*testOverrideFinal)" + "(?=.*test.config.extensions.override.final)" + "(?=.*test.config.extensions.super)"));
    }

    /**
     * This tests the scenario where ibm:extends is used on an OCD when the super type does not exist.
     * A feature is then installed that provides the super.
     * After the feature is installed it should be possible to use the sub type.
     *
     * This test requires a different server configuration, so it must restore
     * the original that other tests rely upon.
     */
    @Test
    public void testLateArrivingSecondBundleSuper() throws Exception {
        try {
            //stop the server, collecting logs from any previous tests
            //and switch to the alternate serverB1.xml config
            extensionsServer.stopServer(true);
            extensionsServer.setServerConfigurationFile("extensions/serverB1.xml");
            extensionsServer.startServer();
            String id = "CWWKG0059E";
            assertNotNull("The error message " + id + " for invalid super was not found",
                          extensionsServer.waitForStringInLog(id + "(?=.*test\\.config\\.extensions\\.different\\.bundle)(?=.*test\\.config\\.extensions\\.super)"));
            //now switch on the bundle providing the super (and our test system wab)
            //by setting the serverB2.xml config
            extensionsServer.setMarkToEndOfLog();
            extensionsServer.setServerConfigurationFile("extensions/serverB2.xml");
            //wait for our test app to be available
            extensionsServer.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT);
            String servicePid = servicePidName + "=test.config.extensions.super";
            //now check that the super got a call for the sub defined in the other bundle
            HttpUtils.findStringInUrl(extensionsServer, CONTEXT_ROOT + "/test?id=test.config.extensions.different.bundle.config&" + servicePid, PID_PASS);
        } finally {
            //stop and package up this server
            extensionsServer.stopServer(true);
            //restore the original server.xml
            //by setting our original server.xml back again
            extensionsServer.setServerConfigurationFile("extensions/server.xml");
            //and finally restart the server
            extensionsServer.startServer();
            //make sure the URL is available
            extensionsServer.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT);
        }
    }

    /**
     * This test requires a different server configuration, so it must restore
     * the original that other tests rely upon.
     *
     * @throws Exception
     */
    @Test
    public void testSchemaGeneratorErrorMessages() throws Exception {
        try {
            extensionsServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configExtensionsSchemaGeneratorTest-1.0.mf");
            extensionsServer.stopServer(true);
            extensionsServer.setServerConfigurationFile("extensions/serverSchemaGenerator.xml");
            extensionsServer.startServer();

            String msgId = "CWWKG0063E";
            assertNotNull("Expected message " + msgId + " not found in log.", extensionsServer.waitForStringInLog(msgId + ".*testInvalidAttribute"));

            msgId = "CWWKG0064E";
            assertNotNull("Expected message " + msgId + " not found in log.", extensionsServer.waitForStringInLog(msgId + ".*testAttr2Rename"));

            msgId = "CWWKG0065E";
            assertNotNull("Expected message " + msgId + " not found in log.", extensionsServer.waitForStringInLog(msgId + ".*test.config.extensions.parent.non.factorypid"));

            msgId = "CWWKG0066E";
            assertNotNull("Expected message " + msgId + " not found in log.", extensionsServer.waitForStringInLog(msgId + ".*test.config.extensions.sub.error.no.parent.pid"));
        } finally {
            //stop and package up this server
            extensionsServer.stopServer(true);
            //restore the original server.xml
            //by setting our original server.xml back again
            extensionsServer.setServerConfigurationFile("extensions/server.xml");
            //and finally restart the server
            extensionsServer.startServer();
            //make sure the URL is available
            extensionsServer.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT);
        }
    }

    /**
     * This tests the scenario where a PID that extends another PID that has a name=internal attr, is configured correctly.
     */
    @Test
    public void testInternalExtension() throws Exception {

        String servicePid = servicePidName + "=test.config.extensions.internal.super";

        //check that the required attribute has been set on the internal super class
        HttpUtils.findStringInUrl(extensionsServer,
                                  CONTEXT_ROOT + "/test?pid=test.config.extensions.internal.sub1&id=internal1&prop=internalAttr1&" + servicePid,
                                  "sub1Attr1");
    }

    /**
     * This tests the scenario where a PID that extends another PID that has a name=internal attr, and is missing a required attr, issues the
     * expected error message.
     */
    @Test
    public void testInternalExtensionWithMissingRequiredAttribute() throws Exception {

        String id = "CWWKG0058E";
        assertNotNull(
                      "The error message " + id + " for internal required attribute was not found",
                      extensionsServer.waitForStringInLog(id + "(?=.*internalsub2)(?=.*internalAttr1)"));
    }

}
