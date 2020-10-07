/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.fat.tests;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction.RepeatOnError;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction.RepeatOnError.EJBONERROR;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class BndErrorTest extends FATServletClient {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            try {
                System.runFinalization();
                System.gc();
                server.serverDump("heap");
            } catch (Exception e1) {
                System.out.println("Failed to dump server");
                e1.printStackTrace();
            }
        }
    };

    @Server("com.ibm.ws.ejbcontainer.bindings.fat.server.err")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new RepeatOnError(EJBONERROR.WARN, "com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnError(EJBONERROR.FAIL, "com.ibm.ws.ejbcontainer.bindings.fat.server.err"));

    @BeforeClass
    public static void setUp() throws Exception {

        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        server.startServer(true, false);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {

            server.stopServer("CNTR4002E", "CWWKZ0106E", "CWWKZ0002E", "CNTR0136E", "CNTR0137E", "CNTR0138E", "CNTR0139E", "CNTR0130E", "CNTR0140", "CNTR0141E", "CNTR0339W",
                              "CNTR0340W", "CWWKZ0004E");
        }
    }

    private static void installApp(int appNum, String appName) throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive BndErrorEJB = ShrinkHelper.buildJavaArchive("BndErrorEJB.jar", "com.ibm.bnd.err.ejb.error" + appNum + ".ejb.");
        ShrinkHelper.addDirectory(BndErrorEJB, "test-applications/BndErrorEJB.jar/resources/error" + appNum);

        EnterpriseArchive BndErrorTestApp = ShrinkWrap.create(EnterpriseArchive.class, appName);
        BndErrorTestApp.addAsModules(BndErrorEJB);
        ShrinkHelper.addDirectory(BndErrorTestApp, "test-applications/BndErrorTestApp.ear/resources");

        ShrinkHelper.exportToServer(server, "dropins", BndErrorTestApp);
    }

    private static void testHelper(int appNum, String errorText, boolean appStop) throws Exception {

        server.setMarkToEndOfLog();
        String appName = "BndError" + appNum + "TestApp.ear";
        installApp(appNum, appName);

        assertNotNull("Expected error message was not logged: " + errorText, server.waitForStringInLogUsingMark(errorText));

        if (appStop || RepeatTestFilter.CURRENT_REPEAT_ACTION == "EJBCBOnErr_FAIL") {
            String message = "CWWKZ0106E:";
            assertNotNull("Application " + appName + " should have been stopped", server.waitForStringInLogUsingMark(message));
        } else {
            String message = null;
            // check for us to switch back to default bindings
            if (appNum == 26 || appNum == 27) {
                // 2X default binding
                message = "CNTR0167I:.*local:ejb/TargetBean";
            } else {
                message = "CNTR0167I:.*BndErrorEJB.jar/TargetBean#";
            }
            assertNotNull("Application " + appName + " should have still bound default binding", server.waitForStringInLogUsingMark(message));

            message = "CWWKZ0001I:";
            assertNotNull("Application " + appName + " should have still started", server.waitForStringInLogUsingMark(message));
        }

        server.removeDropinsApplications(appName);
    }

    /**
     * Missing "ejblocal:" in binding-name for local business interface binding, but lookup contains "ejblocal:"
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNoEJBLocalInBI() throws Exception {
        testHelper(1, "CNTR0136E:", true);
    }

    /**
     * Missing "ejblocal:" in local-home-binding-name, but lookup contains "ejblocal:"
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNoEJBLocalInHome() throws Exception {
        testHelper(2, "CNTR0136E:", true);
    }

    /**
     * No <business-local> defined in ejb-jar.xml, but the corresponding interface binding is specified in binding xml
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNoBusinessLocal() throws Exception {
        testHelper(3, "CNTR0140E:", true);
    }

    /**
     * No <business-remote> defined in ejb-jar.xml, but the corresponding interface binding is specified in binding xml
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNoBusinessRemote() throws Exception {
        testHelper(4, "CNTR0140E:", true);
    }

    /**
     * Add "ejblocal:" into remote buiness interface binding name
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testEJBLocalInRemoteBI() throws Exception {
        testHelper(7, "CNTR0137E:", true);
    }

    /**
     * Add "ejblocal:" into remote home interface binding name
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testEJBLocalInRemoteHome() throws Exception {
        testHelper(8, "CNTR0137E:", true);
    }

    /**
     * Binding name contains blank (" ") string
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testBlankString() throws Exception {
        testHelper(9, "CNTR0138E:", false);
    }

    /**
     * Binding name contains empty ("") string
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testEmptyString() throws Exception {
        testHelper(10, "CNTR0138E:", false);
    }

    /**
     * Duplicated bindings - try to specify different binding names to the same interface
     */
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testDuplicates() throws Exception {
        testHelper(11, "CNTR0139E:", true);
    }

    /*
     * Incorrect interface name - Name does not match any interfaces in the jar
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testIncorrect() throws Exception {
        testHelper(12, "CNTR0140E:", true);
    }

    /**
     * Mixed simplebinding with interface bindings
     */
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testSimpleWithInterface() throws Exception {
        testHelper(13, "CNTR0130E:", true);
    }

    /**
     * Mixed simplebinding with local-home-binding
     */
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testSimpleWithLocalHome() throws Exception {
        testHelper(14, "CNTR0130E:", true);
    }

    /**
     * Mixed simplebinding with remote-home-binding
     */
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testSimpleWithRemoteHome() throws Exception {
        testHelper(15, "CNTR0130E:", true);
    }

    /**
     * Specify a local-home-binding-name for a remote home
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testLocalHomeForRemoteHome() throws Exception {
        testHelper(16, "CNTR0141E:", true);
    }

    /**
     * Specify a remote-home-binding-name for a local home
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testRemoteHomeForLocalHome() throws Exception {
        testHelper(17, "CNTR0141E:", true);
    }

    /**
     * Has ejblocal:local:ejb in local-home-binding-name
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNamepsaceInLocalHomeBindingName() throws Exception {
        testHelper(18, "CNTR0340W:.*ejblocal:local:ejb/myBean", false);
    }

    /**
     * Has ejblocal:local:ejb in simple-binding-name (for local bean)
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNamepsaceInBindingName() throws Exception {
        testHelper(19, "CNTR0339W:.*ejblocal:local:ejb/myBean", false);
    }

    /**
     * Has local: in remote-home-binding-name
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNamepsaceInRemoteHomeBindingName() throws Exception {
        testHelper(20, "CNTR0339W:.*local:RemoteTargetHome", false);
    }

    /**
     * Has random colon in remote-home-binding-name
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testRandomColonInRemoteHomeBindingName() throws Exception {
        testHelper(21, "CNTR0339W:.*myBean:RemoteTargetHome", false);
    }

    /**
     * Has java:app/ in simple-binding-name
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testJavaAppInSimpleBindingName() throws Exception {
        testHelper(22, "CNTR0339W:.*java:app/MyLocalTargetBean", false);
    }

    /**
     * Has local: in binding-name
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNamespaceInBindingName() throws Exception {
        testHelper(23, "CNTR0339W:.*local:ejb/RemoteTargetBiz", false);
    }

    /**
     * Has local:ejb in component-id
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNamespaceInComponentId() throws Exception {
        testHelper(24, "CNTR0339W:.*local:ejb/MyLocalTargetBean", false);
    }

    /**
     * Has empty string in component-id
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testEmptyComponentId() throws Exception {
        testHelper(25, "CNTR0138E:", false);
    }

    /**
     * Has local:ejb in JNDIName
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testNamespaceInJNDIName() throws Exception {
        testHelper(26, "CNTR0339W:.*local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome1", false);
    }

    /**
     * Has empty string in JNDIName
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "javax.ejb.EJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testEmptyJNDIName() throws Exception {
        testHelper(27, "CNTR0138E:", false);
    }

    /**
     * Has ejblocal::ejb in local-home-binding-name
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testDoubleColonInLocalHomeBindingName() throws Exception {
        testHelper(28, "CNTR0340W:.*ejblocal::ejb/myBean", false);
    }

    /**
     * Has ejblocal: in local-home-binding-name (empty besides ejblocal:)
     */
    @Test
    @ExpectedFFDC(repeatAction = "EJBCBOnErr_FAIL",
                  value = { "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testEmptyAfterEJBLocalInLocalHomeBindingName() throws Exception {
        testHelper(29, "CNTR0138E:", false);
    }

}
