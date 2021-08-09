/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.auto.fat.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.EJBTimerServiceElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class AutomaticNPTimerContextPropTest extends FATServletClient {

    private final static Logger logger = Logger.getLogger(AutomaticNPTimerContextPropTest.class.getName());
    private final static String SERVLET = "AutoNPTimerContextPropWeb/AutoNPTimerServiceConfigServlet";

    @Server("AutoNPTimerContextPropServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("AutoNPTimerContextPropServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("AutoNPTimerContextPropServer")).andWith(new JakartaEE9Action().fullFATOnly().forServers("AutoNPTimerContextPropServer"));

    @BeforeClass
    public static void setup() throws Exception {
        // cleanup from prior repeat actions
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the Ears & Wars

        //#################### AutoNPTimerContextPropApp.ear
        JavaArchive AutoNPTimerContextPropEJB = ShrinkHelper.buildJavaArchive("AutoNPTimerContextPropEJB.jar", "com.ibm.ws.ejbcontainer.timer.auto.npTimer.context.ejb.");
        WebArchive AutoNPTimerContextPropWeb = ShrinkHelper.buildDefaultApp("AutoNPTimerContextPropWeb.war", "com.ibm.ws.ejbcontainer.timer.auto.npTimer.context.web.");

        EnterpriseArchive AutoNPTimerContextPropApp = ShrinkWrap.create(EnterpriseArchive.class, "AutoNPTimerContextPropApp.ear");
        AutoNPTimerContextPropApp = (EnterpriseArchive) ShrinkHelper.addDirectory(AutoNPTimerContextPropApp, "test-applications/AutoNPTimerContextPropApp.ear/resources");
        AutoNPTimerContextPropApp.addAsModule(AutoNPTimerContextPropEJB).addAsModule(AutoNPTimerContextPropWeb);

        ShrinkHelper.exportAppToServer(server, AutoNPTimerContextPropApp, DeployOptions.SERVER_ONLY);
        server.addInstalledAppForValidation("AutoNPTimerContextPropApp");
    }

    @Before
    public void beforeTest() throws Exception {
        server.startServer();

        // verify the appSecurity-2.0 feature is ready
        assertNotNull("Security service did not report it was ready", server.waitForStringInLogUsingMark("CWWKS0008I"));
        assertNotNull("LTPA configuration did not report it was ready", server.waitForStringInLogUsingMark("CWWKS4105I"));
    }

    @After
    public void afterTest() throws Exception {
        server.stopServer();
    }

    protected void runTest() throws Exception {
        FATServletClient.runTest(server, SERVLET, getTestMethodSimpleName());
    }

    /**
     * Test security context propagates for NP EJB timer when the timerService
     * nonPersistentContextServiceRef is set to point to a contextService with
     * the security context configured
     *
     * @throws Exception
     */
    @Test
    public void testNPAutoTimerSecurityContextPropagates() throws Exception {
        setTimerContextServiceConfigValue("EJBTimerSecurityContextService");
        runTest();
    }

    /**
     * Test that no contexts will propagate for NP EJB timer when the timerService
     * nonPersistentContextServiceRef is set to point to a contextService with
     * no contexts configured. This ensures the default behavior of NP timer creation.
     *
     * @throws Exception
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testNPAutoTimerNoContextPropagates() throws Exception {
        setTimerContextServiceConfigValue("NoEJBTimerContextService");
        runTest();
    }

    /**
     * Helper method to set the nonPersistentContextServiceRef in timerService in the
     * server.xml
     *
     * @param contextServiceValue
     * @throws Exception
     */
    private void setTimerContextServiceConfigValue(String contextServiceValue) throws Exception {
        String testMethodName = testName.getMethodName();
        ServerConfiguration config = server.getServerConfiguration();
        EJBContainerElement ejbContainer = config.getEJBContainer();
        EJBTimerServiceElement currentTimerService = ejbContainer.getTimerService();

        if (contextServiceValue == null) {
            if (currentTimerService == null) {
                logger.info(testMethodName + " : timerService config does not exist");
            }
            logger.info(testMethodName + " : removing context service element");
            ejbContainer.setTimerService(null);
        } else {
            if (currentTimerService == null) {
                logger.info(testMethodName + " creating timerService element");
                currentTimerService = new EJBTimerServiceElement();
                currentTimerService.setId("timerServiceTest1");
                ejbContainer.setTimerService(currentTimerService);

            }
            String currentContextService = currentTimerService.getNonPersistentContextServiceRef();
            // if contextServiceValue empty --> set nonPersistentContextServiceRef to null
            String newContextService = "".equals(contextServiceValue) ? null : contextServiceValue;

            if (currentContextService == newContextService ||
                (currentContextService != null && currentContextService.equals(newContextService))) {
                logger.info(testMethodName + " : timerService:nonPersistentContextServiceRef config already set to " + newContextService);
                return;
            } else {
                logger.info(testMethodName + " : setting timerService:nonPersistentContextServiceRef config to " + newContextService);
                currentTimerService.setNonPersistentContextServiceRef(newContextService);
            }

        }
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("AutoNPTimerContextPropApp"));
    }
}
