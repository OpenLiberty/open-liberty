/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.session.async.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@Mode(FULL)
@RunWith(FATRunner.class)
public class AsyncWarnTest extends FATServletClient {
    public static LibertyServer server;

    private static RemoteFile warnTraceLog = null;

    private static boolean isCheckFalse;
    private static boolean isCheckTrue;
    private static boolean isEJBTrace;
    private static boolean isMetaDataTrace;
    private static boolean isDefault;

    List<String> warnMsgs = null;

    private static final String SERVLET = "AsyncWarnTest/AsyncWarnServlet";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.session.async.fat.AsyncWarnServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.session.async.fat.AsyncWarnServer"));

    protected void runTest(String testName) throws Exception {
        FATServletClient.runTest(server, SERVLET, testName);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.ejbcontainer.session.async.fat.AsyncWarnServer");

        // Use ShrinkHelper to build the ears
        JavaArchive AsyncAWarnIntf = ShrinkHelper.buildJavaArchive("AsyncAWarnIntf.jar", "com.ibm.ws.ejbcontainer.session.async.warn.shared.");
        JavaArchive AsyncInLocalIf1Bean = ShrinkHelper.buildJavaArchive("AsyncInLocalIf1Bean.jar", "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncInLocalInterface1.ejb.");
        EnterpriseArchive AsyncInLocalIf1BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncInLocalIf1Bean.ear");
        AsyncInLocalIf1BeanApp.addAsModule(AsyncInLocalIf1Bean);
        JavaArchive AsyncInLocalIf2Bean = ShrinkHelper.buildJavaArchive("AsyncInLocalIf2Bean.jar", "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncInLocalInterface2.ejb.");
        EnterpriseArchive AsyncInLocalIf2BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncInLocalIf2Bean.ear");
        AsyncInLocalIf2BeanApp.addAsModule(AsyncInLocalIf2Bean);
        JavaArchive AsyncInLocalIf3Bean = ShrinkHelper.buildJavaArchive("AsyncInLocalIf3Bean.jar", "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncInLocalInterface3.ejb.");
        EnterpriseArchive AsyncInLocalIf3BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncInLocalIf3Bean.ear");
        AsyncInLocalIf3BeanApp.addAsModule(AsyncInLocalIf3Bean);

        JavaArchive AsyncInRemoteIf1Bean = ShrinkHelper.buildJavaArchive("AsyncInRemoteIf1Bean.jar",
                                                                         "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncInRemoteInterface1.ejb.");
        EnterpriseArchive AsyncInRemoteIf1BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncInRemoteIf1Bean.ear");
        AsyncInRemoteIf1BeanApp.addAsModule(AsyncInRemoteIf1Bean);
        JavaArchive AsyncInRemoteIf2Bean = ShrinkHelper.buildJavaArchive("AsyncInRemoteIf2Bean.jar",
                                                                         "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncInRemoteInterface2.ejb.");
        EnterpriseArchive AsyncInRemoteIf2BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncInRemoteIf2Bean.ear");
        AsyncInRemoteIf2BeanApp.addAsModule(AsyncInRemoteIf2Bean);
        JavaArchive AsyncInRemoteIf3Bean = ShrinkHelper.buildJavaArchive("AsyncInRemoteIf3Bean.jar",
                                                                         "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncInRemoteInterface3.ejb.");
        EnterpriseArchive AsyncInRemoteIf3BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncInRemoteIf3Bean.ear");
        AsyncInRemoteIf3BeanApp.addAsModule(AsyncInRemoteIf3Bean);

        JavaArchive AsyncNotInLocalIf1Bean = ShrinkHelper.buildJavaArchive("AsyncNotInLocalIf1Bean.jar",
                                                                           "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncNotInLocalInterface1.ejb.");
        EnterpriseArchive AsyncNotInLocalIf1BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncNotInLocalIf1Bean.ear");
        AsyncNotInLocalIf1BeanApp.addAsModule(AsyncNotInLocalIf1Bean);
        JavaArchive AsyncNotInLocalIf2Bean = ShrinkHelper.buildJavaArchive("AsyncNotInLocalIf2Bean.jar",
                                                                           "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncNotInLocalInterface2.ejb.");
        EnterpriseArchive AsyncNotInLocalIf2BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncNotInLocalIf2Bean.ear");
        AsyncNotInLocalIf2BeanApp.addAsModule(AsyncNotInLocalIf2Bean);
        JavaArchive AsyncNotInLocalIf3Bean = ShrinkHelper.buildJavaArchive("AsyncNotInLocalIf3Bean.jar",
                                                                           "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncNotInLocalInterface3.ejb.");
        EnterpriseArchive AsyncNotInLocalIf3BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncNotInLocalIf3Bean.ear");
        AsyncNotInLocalIf3BeanApp.addAsModule(AsyncNotInLocalIf3Bean);

        JavaArchive AsyncNotInRemoteIf1Bean = ShrinkHelper.buildJavaArchive("AsyncNotInRemoteIf1Bean.jar",
                                                                            "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncNotInRemoteInterface1.ejb.");
        EnterpriseArchive AsyncNotInRemoteIf1BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncNotInRemoteIf1Bean.ear");
        AsyncNotInRemoteIf1BeanApp.addAsModule(AsyncNotInRemoteIf1Bean);
        JavaArchive AsyncNotInRemoteIf2Bean = ShrinkHelper.buildJavaArchive("AsyncNotInRemoteIf2Bean.jar",
                                                                            "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncNotInRemoteInterface2.ejb.");
        EnterpriseArchive AsyncNotInRemoteIf2BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncNotInRemoteIf2Bean.ear");
        AsyncNotInRemoteIf2BeanApp.addAsModule(AsyncNotInRemoteIf2Bean);
        JavaArchive AsyncNotInRemoteIf3Bean = ShrinkHelper.buildJavaArchive("AsyncNotInRemoteIf3Bean.jar",
                                                                            "com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncNotInRemoteInterface3.ejb.");
        EnterpriseArchive AsyncNotInRemoteIf3BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncNotInRemoteIf3Bean.ear");
        AsyncNotInRemoteIf3BeanApp.addAsModule(AsyncNotInRemoteIf3Bean);

        WebArchive AsyncWarnTestWar = ShrinkHelper.buildDefaultApp("AsyncWarnTest.war", "com.ibm.ws.ejbcontainer.session.async.warn.web.");
        JavaArchive AsyncWarnTestBean = ShrinkHelper.buildJavaArchive("AsyncWarnTestBean.jar", "com.ibm.ws.ejbcontainer.session.async.warn.ejb.");
        EnterpriseArchive AsyncWarnTest = ShrinkWrap.create(EnterpriseArchive.class, "AsyncWarnTest.ear");
        AsyncWarnTest.addAsModule(AsyncWarnTestWar).addAsModule(AsyncWarnTestBean);
        AsyncWarnTest = (EnterpriseArchive) ShrinkHelper.addDirectory(AsyncWarnTest, "test-applications/AsyncWarnTest.ear/resources/");

        ShrinkHelper.exportAppToServer(server, AsyncInLocalIf1BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncInLocalIf2BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncInLocalIf3BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncInRemoteIf1BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncInRemoteIf2BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncInRemoteIf3BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncNotInLocalIf1BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncNotInLocalIf2BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncNotInLocalIf3BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncNotInRemoteIf1BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncNotInRemoteIf2BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncNotInRemoteIf3BeanApp);
        ShrinkHelper.exportDropinAppToServer(server, AsyncWarnTest);

        ShrinkHelper.exportToServer(server, "lib/global", AsyncAWarnIntf);

        server.startServer();

        isCheckFalse = false;
        isCheckTrue = false;
        isEJBTrace = false;
        isMetaDataTrace = false;
        isDefault = false;
    }

    public void setupTest(String testType) throws Exception {
        if (testType.equals("checkFalse")) {
            if (!isCheckFalse) {
                LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "jvm.options", "lib/LibertyFATTestFiles/checkFalse.options");
                LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "bootstrap.properties", "lib/LibertyFATTestFiles/default.properties");
                server.stopServer("CNTR0305W");

                server.setServerConfigurationFile("checkFalse_server.xml");
                server.startServer();
                runTest("initRecoveryLog");

                warnTraceLog = null;

                isCheckFalse = true;
                isCheckTrue = false;
                isEJBTrace = false;
                isMetaDataTrace = false;
                isDefault = false;
            }
        } else if (testType.equals("checkTrue")) {
            if (!isCheckTrue) {
                LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "jvm.options", "lib/LibertyFATTestFiles/checkTrue.options");
                LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "bootstrap.properties", "lib/LibertyFATTestFiles/default.properties");
                server.stopServer("CNTR0305W");

                server.setServerConfigurationFile("checkTrue_server.xml");
                server.startServer();
                runTest("initRecoveryLog");

                warnTraceLog = null;

                isCheckFalse = false;
                isCheckTrue = true;
                isEJBTrace = false;
                isMetaDataTrace = false;
                isDefault = false;
            }
        } else if (testType.equals("EJBTrace")) {
            if (!isEJBTrace) {
                LibertyFileManager.deleteLibertyFile(server.getMachine(), server.getServerRoot() + "/jvm.options");
                LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "bootstrap.properties", "lib/LibertyFATTestFiles/EJBTrace.properties");
                server.stopServer("CNTR0305W");

                server.setServerConfigurationFile("EJBTrace_server.xml");
                server.startServer();
                runTest("initRecoveryLog");

                if (warnTraceLog == null)
                    warnTraceLog = new RemoteFile(server.getMachine(), server.getLogsRoot() + "trace.log");

                isCheckFalse = false;
                isCheckTrue = false;
                isEJBTrace = true;
                isMetaDataTrace = false;
                isDefault = false;
            }
        } else if (testType.equals("MetaDataTrace")) {
            if (!isMetaDataTrace) {
                LibertyFileManager.deleteLibertyFile(server.getMachine(), server.getServerRoot() + "/jvm.options");
                LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "bootstrap.properties", "lib/LibertyFATTestFiles/MetaDataTrace.properties");
                server.stopServer("CNTR0305W");

                server.setServerConfigurationFile("MetaDataTrace_server.xml");
                server.startServer();
                runTest("initRecoveryLog");

                if (warnTraceLog == null)
                    warnTraceLog = new RemoteFile(server.getMachine(), server.getLogsRoot() + "trace.log");

                isCheckFalse = false;
                isCheckTrue = false;
                isEJBTrace = false;
                isMetaDataTrace = true;
                isDefault = false;
            }
        } else if (testType.equals("default")) {
            if (!isDefault) {
                LibertyFileManager.deleteLibertyFile(server.getMachine(), server.getServerRoot() + "/jvm.options");
                LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getServerRoot(), "bootstrap.properties", "lib/LibertyFATTestFiles/default.properties");
                server.stopServer("CNTR0305W");

                server.setServerConfigurationFile("default_server.xml");
                server.startServer();
                runTest("initRecoveryLog");

                warnTraceLog = null;

                isCheckFalse = false;
                isCheckTrue = false;
                isEJBTrace = false;
                isMetaDataTrace = false;
                isDefault = true;
            }
        }

        server.setMarkToEndOfLog();
        if (warnTraceLog != null)
            server.setMarkToEndOfLog(warnTraceLog);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0305W");
        }
    }

    /* @Asynchronous defined on bean methods and interface class, checkEJBApplicationConfiguration property set to false, no trace enabled. */
    @Test
    public void testInLocalIf_asyncOnBeanMethods_checkFalse() throws Exception {
        setupTest("checkFalse");
        runTest("testInLocalIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean class and interface class, checkEJBApplicationConfiguration property set to false, no trace enabled. */
    @Test
    public void testInRemoteIf_asyncOnBeanClass_checkFalse() throws Exception {
        setupTest("checkFalse");
        runTest("testInRemoteIf_asyncOnBeanClass");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on interface class only, checkEJBApplicationConfiguration property set to true, no trace enabled. */
    @Test
    public void testInLocalIf_NOasyncOnBean_checkTrue() throws Exception {
        setupTest("checkTrue");
        runTest("testInLocalIf_NOasyncOnBean");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertFalse("The message was expected to be logged, but was not: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods and interface class, checkEJBApplicationConfiguration property set to true, no trace enabled. */
    @Test
    public void testInLocalIf_asyncOnBeanMethods_checkTrue() throws Exception {
        setupTest("checkTrue");
        runTest("testInLocalIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertFalse("The message was expected to be logged, but was not: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods and interface class, checkEJBApplicationConfiguration property set to true, no trace enabled. */
    @Test
    public void testInRemoteIf_asyncOnBeanMethods_checkTrue() throws Exception {
        setupTest("checkTrue");
        runTest("testInRemoteIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertFalse("The message was expected to be logged, but was not: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods, checkEJBApplicationConfiguration property set to true, no trace enabled. */
    @Test
    public void testNotInLocalIf_asyncOnBeanMethods_checkTrue() throws Exception {
        setupTest("checkTrue");
        runTest("testNotInLocalIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since @Asynchronous not annotated on the interface: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods, checkEJBApplicationConfiguration property set to true, no trace enabled. */
    @Test
    public void testNotInRemoteIf_asyncOnBeanMethods_checkTrue() throws Exception {
        setupTest("checkTrue");
        runTest("testNotInRemoteIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since @Asynchronous not annotated on the interface: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean class and interface class, checkEJBApplicationConfiguration property NOT set, EJBContainer trace enabled. */
    @Test
    public void testInLocalIf_asyncOnBeanClass_EJBTrace() throws Exception {
        setupTest("EJBTrace");
        runTest("testInLocalIf_asyncOnBeanClass");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertFalse("The message was expected to be logged, but was not: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on interface class only, checkEJBApplicationConfiguration property NOT set, EJBContainer trace enabled. */
    @Test
    public void testInRemoteIf_NOasyncOnBean_EJBTrace() throws Exception {
        setupTest("EJBTrace");
        runTest("testInRemoteIf_NOasyncOnBean");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertFalse("The message was expected to be logged, but was not: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean class and interface class, checkEJBApplicationConfiguration property NOT set, EJBContainer trace enabled. */
    @Test
    public void testInRemoteIf_asyncOnBeanClass_EJBTrace() throws Exception {
        setupTest("EJBTrace");
        runTest("testInRemoteIf_asyncOnBeanClass");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertFalse("The message was expected to be logged, but was not: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean class, checkEJBApplicationConfiguration property NOT set, EJBContainer trace enabled. */
    @Test
    public void testNotInLocalIf_asyncOnBeanClass_EJBTrace() throws Exception {
        setupTest("EJBTrace");
        runTest("testNotInLocalIf_asyncOnBeanClass");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since @Asynchronous not annotated on the interface: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean class, checkEJBApplicationConfiguration property NOT set, EJBContainer trace enabled. */
    @Test
    public void testNotInRemoteIf_asyncOnBeanClass_EJBTrace() throws Exception {
        setupTest("EJBTrace");
        runTest("testNotInRemoteIf_asyncOnBeanClass");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since @Asynchronous not annotated on the interface: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods and interface class, checkEJBApplicationConfiguration property NOT set, MataData trace enabled. */
    @Test
    public void testInLocalIf_asyncOnBeanMethods_MetaDataTrace() throws Exception {
        setupTest("MetaDataTrace");
        runTest("testInLocalIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertFalse("The message was expected to be logged, but was not: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods and interface class, checkEJBApplicationConfiguration property NOT set, MataData trace enabled. */
    @Test
    public void testInRemoteIf_asyncOnBeanMethods_MetaDataTrace() throws Exception {
        setupTest("MetaDataTrace");
        runTest("testInRemoteIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertFalse("The message was expected to be logged, but was not: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods, checkEJBApplicationConfiguration property NOT set, MataData trace enabled. */
    @Test
    public void testNotInLocalIf_asyncOnBeanMethods_MetaDataTrace() throws Exception {
        setupTest("MetaDataTrace");
        runTest("testNotInLocalIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since @Asynchronous not annotated on the interface: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods, checkEJBApplicationConfiguration property NOT set, MataData trace enabled. */
    @Test
    public void testNotInRemoteIf_asyncOnBeanMethods_MetaDataTrace() throws Exception {
        setupTest("MetaDataTrace");
        runTest("testNotInRemoteIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since @Asynchronous not annotated on the interface: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean class and interface class, checkEJBApplicationConfiguration property NOT set, no trace enabled. */
    @Test
    public void testInLocalIf_asyncOnBeanClass() throws Exception {
        setupTest("default");
        runTest("testInLocalIf_asyncOnBeanClass");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods and interface class, checkEJBApplicationConfiguration property NOT set, no trace enabled. */
    @Test
    public void testInRemoteIf_asyncOnBeanMethods() throws Exception {
        setupTest("default");
        runTest("testInRemoteIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean class, checkEJBApplicationConfiguration property NOT set, no trace enabled. */
    @Test
    public void testNotInLocalIf_asyncOnBeanClass() throws Exception {
        setupTest("default");
        runTest("testNotInLocalIf_asyncOnBeanClass");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods, checkEJBApplicationConfiguration property NOT set, no trace enabled. */
    @Test
    public void testNotInLocalIf_asyncOnBeanMethods() throws Exception {
        setupTest("default");
        runTest("testNotInLocalIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods, checkEJBApplicationConfiguration property NOT set, no trace enabled. */
    @Test
    public void testNotInLocalIf_NOasyncOnBean() throws Exception {
        setupTest("default");
        runTest("testNotInLocalIf_NOasyncOnBean");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean class, checkEJBApplicationConfiguration property NOT set, no trace enabled. */
    @Test
    public void testNotInRemoteIf_asyncOnBeanClass() throws Exception {
        setupTest("default");
        runTest("testNotInRemoteIf_asyncOnBeanClass");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods, checkEJBApplicationConfiguration property NOT set, no trace enabled. */
    @Test
    public void testNotInRemoteIf_asyncOnBeanMethods() throws Exception {
        setupTest("default");
        runTest("testNotInRemoteIf_asyncOnBeanMethods");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CNTR0305W", warnMsgs.isEmpty());
    }

    /* @Asynchronous defined on bean methods, checkEJBApplicationConfiguration property NOT set, no trace enabled. */
    @Test
    public void testNotInRemoteIf_NOasyncOnBean() throws Exception {
        setupTest("default");
        runTest("testNotInRemoteIf_NOasyncOnBean");

        warnMsgs = server.findStringsInLogsAndTraceUsingMark("CNTR0305W");
        assertTrue("The message was logged but should not have been since neither the trace string nor the system property was enabled: CNTR0305W", warnMsgs.isEmpty());
    }
}
