/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.exception.fat.tests;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class ExceptionOnStartTest {
    @Server("com.ibm.ws.ejbcontainer.exception.fat.ExceptionOnStartServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.exception.fat.ExceptionOnStartServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.exception.fat.ExceptionOnStartServer")).andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("com.ibm.ws.ejbcontainer.exception.fat.ExceptionOnStartServer")).andWith(FeatureReplacementAction.EE10_FEATURES().forServers("com.ibm.ws.ejbcontainer.exception.fat.ExceptionOnStartServer"));

    public static String[] ignoredFailuresRegExps;

    @Before
    public void before() throws Exception {
        ignoredFailuresRegExps = null;
    }

    @After
    public void after() throws Exception {
        if (server.isStarted()) {
            server.stopServer(ignoredFailuresRegExps);
        }
    }

    /**
     * Test that an application fails to start when a bean configured to start on application start
     * is missing a referenced class file. Verifies the appropriate messages and FFDC are logged.
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testExceptionOnBeanInitialization() throws Exception {
        String BEAN_START_FAILED = "CNTR5007E:.*ExceptionOnStartSingletonBean";
        String MODULE_START_FAILED = "CNTR4002E:.*ExceptionOnStartBean.jar";
        String APP_START_FAILED = "CWWKZ0106E:.*ExceptionOnStartApp";
        String EXCEPTION_STARTING_APP = "CWWKZ0002E:.*ExceptionOnStartApp";

        ignoredFailuresRegExps = new String[] { BEAN_START_FAILED, MODULE_START_FAILED, APP_START_FAILED, EXCEPTION_STARTING_APP };

        // Use ShrinkHelper to build the application ear; excluding the exception class to cause a failure
        JavaArchive ExceptionOnStartBean = ShrinkHelper.buildJavaArchive("ExceptionOnStartBean.jar", "com.ibm.ws.ejbcontainer.exception.start.ejb.");
        EnterpriseArchive ExceptionOnStartApp = ShrinkWrap.create(EnterpriseArchive.class, "ExceptionOnStartApp.ear");
        ExceptionOnStartApp.addAsModule(ExceptionOnStartBean);

        ShrinkHelper.exportDropinAppToServer(server, ExceptionOnStartApp, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY);

        server.startServer();

        assertNotNull("Bean ExceptionOnStartSingletonBean started", server.waitForStringInLog(BEAN_START_FAILED));
        assertNotNull("EJB module ExceptionOnStartBean.jar started", server.waitForStringInLog(MODULE_START_FAILED));
        assertNotNull("Application ExceptionOnStartApp started", server.waitForStringInLog(APP_START_FAILED));
        assertNotNull("Application ExceptionOnStartApp started", server.waitForStringInLog(EXCEPTION_STARTING_APP));
    }

}
