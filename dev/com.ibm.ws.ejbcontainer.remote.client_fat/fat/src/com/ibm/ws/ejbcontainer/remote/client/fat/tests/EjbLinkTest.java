/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.client.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class EjbLinkTest extends FATServletClient {

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

    private static Class<?> c = EjbLinkTest.class;

    private static LibertyClient client = LibertyClientFactory.getLibertyClient("com.ibm.ws.ejbcontainer.remote.client.fat.clientInjection");

    @Server("com.ibm.ws.ejbcontainer.remote.client.fat.serverInjection")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.client.fat.serverInjection")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.remote.client.fat.serverInjection")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.ejbcontainer.remote.client.fat.serverInjection"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // cleanup from prior repeat actions
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the Ears & Wars

        //#################### StatefulAnnRemoteTest.ear
        JavaArchive EjbLinkBean = ShrinkHelper.buildJavaArchive("EjbLinkBean.jar", "com.ibm.ws.ejbcontainer.ejblink.ejb.");
        EjbLinkBean = (JavaArchive) ShrinkHelper.addDirectory(EjbLinkBean, "test-applications/EjbLinkBean.jar/resources");

        JavaArchive EjbLinkOtherBean = ShrinkHelper.buildJavaArchive("EjbLinkOtherBean.jar", "com.ibm.ws.ejbcontainer.ejblink.ejbo.");
        EjbLinkOtherBean = (JavaArchive) ShrinkHelper.addDirectory(EjbLinkOtherBean, "test-applications/EjbLinkOtherBean.jar/resources");

        WebArchive EjbLinkInWar = ShrinkHelper.buildDefaultApp("EjbLinkInWar.war", "com.ibm.ws.ejbcontainer.ejblink.ejbwar.");
        EjbLinkInWar = (WebArchive) ShrinkHelper.addDirectory(EjbLinkInWar, "test-applications/EjbLinkInWar.war/resources");

        WebArchive EjbLinkInOtherWar = ShrinkHelper.buildDefaultApp("EjbLinkInOtherWar.war", "com.ibm.ws.ejbcontainer.ejblink.ejbwaro.");
        EjbLinkInOtherWar = (WebArchive) ShrinkHelper.addDirectory(EjbLinkInOtherWar, "test-applications/EjbLinkInOtherWar.war/resources");

        JavaArchive EjbLinkClient = ShrinkHelper.buildJavaArchive("EjbLinkClient.jar", "com.ibm.ws.ejbcontainer.ejblink.client.");
        EjbLinkClient = (JavaArchive) ShrinkHelper.addDirectory(EjbLinkClient, "test-applications/EjbLinkClient.jar/resources");

        EnterpriseArchive EjbLinkTest = ShrinkWrap.create(EnterpriseArchive.class, "EjbLinkTest.ear");
        EjbLinkTest.addAsModule(EjbLinkBean).addAsModule(EjbLinkOtherBean);
        EjbLinkTest.addAsModule(EjbLinkInWar).addAsModule(EjbLinkInOtherWar);
        EjbLinkTest.addAsModule(EjbLinkClient);
        EjbLinkTest = (EnterpriseArchive) ShrinkHelper.addDirectory(EjbLinkTest, "test-applications/EjbLinkTest.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EjbLinkTest, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToClient(client, "dropins", EjbLinkTest, DeployOptions.SERVER_ONLY);

        // Start the server and wait for application to start
        server.startServer();

        // verify the appSecurity-2.0 feature is ready
        assertNotNull("Security service did not report it was ready", server.waitForStringInLogUsingMark("CWWKS0008I"));
        assertNotNull("LTPA configuration did not report it was ready", server.waitForStringInLogUsingMark("CWWKS4105I"));
        server.setMarkToEndOfLog();

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        // Only after the server has started and appSecurity-2.0 feature is ready,
        // then allow the @Startup InitTxRecoveryLog bean to start.
        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);

        client.addIgnoreErrors("CWWKC0105W");
        client.startClient();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWKG0033W", "CNTR0019E", "CWNEN0030E", "CWWKO0221E");
    }

    private void check() throws Exception {
        String methodName = getTestMethodSimpleName();
        List<String> strings = client.findStringsInCopiedLogs(methodName + "--PASSED");
        Log.info(c, methodName, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + methodName + "--PASSED", strings != null && strings.size() >= 1);

        // now explicitly check that we do not see messages like this in the log output:
        // W CWNEN0057W: The com.ibm.ws.clientcontainer.fat.javacolon.client.InjectionClientMain.mailSessionComp injection target must not be declared static.
        // The spec says that injection into client modules MUST be static - this message indicates a bug in how injection is
        // processed - usually by the client container injection runtime or CDI.
        strings = client.findStringsInCopiedLogs("CWNEN0057W");
        assertTrue("Invalid warning message about injecting into static field", 0 == strings.size());
    }

    @Test
    public void testStyle2OtherJarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle1OtherJarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle3OtherJarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle1SameJarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle2SameJarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle3SameJarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle1OtherJarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle2OtherJarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle3OtherJarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle1SameJarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle2SameJarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle3SameJarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle1OtherWarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle2OtherWarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle3OtherWarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle1SameWarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle2SameWarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle3SameWarXML() throws Exception {
        check();
    }

    @Test
    public void testStyle1OtherWarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle2OtherWarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle3OtherWarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle1SameWarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle2SameWarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle3SameWarAnn() throws Exception {
        check();
    }

    @Test
    public void testJarStyle1toWarXML() throws Exception {
        check();
    }

    @Test
    public void testJarStyle1toWarAnn() throws Exception {
        check();
    }

    @Test
    public void testWarStyle1toJarXML() throws Exception {
        check();
    }

    @Test
    public void testWarStyle1toJarAnn() throws Exception {
        check();
    }

    @Test
    public void testJarStyle2toWarXML() throws Exception {
        check();
    }

    @Test
    public void testJarStyle2toWarAnn() throws Exception {
        check();
    }

    @Test
    public void testWarStyle2toJarXML() throws Exception {
        check();
    }

    @Test
    public void testWarStyle2toJarAnn() throws Exception {
        check();
    }

    @Test
    public void testJarStyle3toWarXML() throws Exception {
        check();
    }

    @Test
    public void testJarStyle3toWarAnn() throws Exception {
        check();
    }

    @Test
    public void testWarStyle3toJarXML() throws Exception {
        check();
    }

    @Test
    public void testWarStyle3toJarAnn() throws Exception {
        check();
    }

    @Test
    public void testStyle1BeanInJarAndWar() throws Exception {
        check();
    }

    @Test
    public void findBeanInSameJar() throws Exception {
        check();
    }

    @Test
    public void findBeanInSameWar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromJarInOtherJar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromWarInJar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromJarInWar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromWarInOtherWar() throws Exception {
        check();
    }

    @Test
    public void findBeanInSameJarAndJar() throws Exception {
        check();
    }

    @Test
    public void findBeanInSameJarAndWar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromJarInOtherJarAndWar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromJarInTwoWars() throws Exception {
        check();
    }

    @Test
    public void findBeanInSameWarAndJar() throws Exception {
        check();
    }

    @Test
    public void findBeanInSameWarAndWar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromWarInOtherJarAndWar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromWarInTwoJars() throws Exception {
        check();
    }

    @Test
    public void findBeanFromJar2SameJar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromJar2OtherJar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromJar2War() throws Exception {
        check();
    }

    @Test
    public void findBeanFromWar2SameWar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromWar2OtherWar() throws Exception {
        check();
    }

    @Test
    public void findBeanFromWar2Jar() throws Exception {
        check();
    }

}
