/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.config.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.EJBTimerServiceElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class NpTimerConfigRetryTest extends FATServletClient {
    private final static Logger logger = Logger.getLogger(NpTimerConfigRetryTest.class.getName());
    private static final String RETRY_SERVLET = "NpTimerConfigRetryWeb/NpTimerConfigRetryServlet";
    private static final String LATE_MSG_SERVLET = "NpTimerConfigLateWarningWeb/NpTimerLateWarningServlet";

    @Server("com.ibm.ws.ejbcontainer.timer.np.config.NpTimerConfigServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.timer.np.config.NpTimerConfigServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.np.config.NpTimerConfigServer")).andWith(FeatureReplacementAction.EE9_FEATURES().forServers("com.ibm.ws.ejbcontainer.timer.np.config.NpTimerConfigServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### NpTimerConfigRetryApp.ear
        JavaArchive NpTimerConfigLateWarningEJB = ShrinkHelper.buildJavaArchive("NpTimerConfigLateWarningEJB.jar", "com.ibm.ws.ejbcontainer.timer.np.config.late.ejb.");
        WebArchive NpTimerConfigLateWarningWeb = ShrinkHelper.buildDefaultApp("NpTimerConfigLateWarningWeb.war", "com.ibm.ws.ejbcontainer.timer.np.config.late.web.");
        JavaArchive NpTimerConfigRetryEJB = ShrinkHelper.buildJavaArchive("NpTimerConfigRetryEJB.jar", "com.ibm.ws.ejbcontainer.timer.np.config.retry.ejb.");
        WebArchive NpTimerConfigRetryWeb = ShrinkHelper.buildDefaultApp("NpTimerConfigRetryWeb.war", "com.ibm.ws.ejbcontainer.timer.np.config.retry.web.");
        EnterpriseArchive NpTimerConfigRetryApp = ShrinkWrap.create(EnterpriseArchive.class, "NpTimerConfigRetryApp.ear");
        NpTimerConfigRetryApp.addAsModule(NpTimerConfigLateWarningEJB).addAsModule(NpTimerConfigLateWarningWeb);
        NpTimerConfigRetryApp.addAsModule(NpTimerConfigRetryEJB).addAsModule(NpTimerConfigRetryWeb);

        ShrinkHelper.exportDropinAppToServer(server, NpTimerConfigRetryApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CNTR0020E: EJB threw an unexpected (non-declared) exception
        // CNTR0179W: Non-persistent timer maximum number of retries x was reached.
        // CNTR0333W: EJB timer ... started later than expected.
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0020E", "CNTR0179W", "CNTR0333W");
        }
    }

    private void runTest() throws Exception {
        FATServletClient.runTest(server, RETRY_SERVLET, getTestMethodSimpleName());
    }

    private static Long RETRY_INTERVAL_NORMAL = Long.valueOf(1);
    private static Integer RETRY_COUNT_NORMAL = Integer.valueOf(4);
    private static Long RETRY_INTERVAL_ZERO = Long.valueOf(0);
    private static Long RETRY_INTERVAL_LONG = Long.valueOf(10);
    private static Integer RETRY_COUNT_ZERO = Integer.valueOf(0);
    private static Integer RETRY_COUNT_TWO = Integer.valueOf(2);

    /**
     * Purpose:
     * 1) We get an automatic retry
     * 2) It happens immediately (ie, it doesn't wait for the scheduled retry interval)
     */
    @Test
    @ExpectedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testForceImmediateRetry() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_NORMAL, RETRY_COUNT_NORMAL);
        runTest();
    }

    /**
     * Purpose:
     * 1) Verify that we get the correct number of retries.
     * 2) Verify that all retries (after the first, immediate retry) wait for the configured interval
     */
    @Test
    @AllowedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testRetryCountAndIntervalHonored() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_NORMAL, RETRY_COUNT_NORMAL);
        server.setMarkToEndOfLog();
        runTest();
        assertEquals("Max retries 4 was not reached (CNTR0179W)", 1, server.findStringsInLogsUsingMark("CNTR0179W:.*4", server.getDefaultLogFile()).size());
    }

    /**
     * Purpose:
     * 1) Verify that when a timer is invoked as part of a retry sequence, and is successful, that the retryCount is reset, so the next regularly scheduled
     * timer invocation also gets the benefit of the full retry sequence.
     */
    @Test
    @AllowedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testRetryLimitGetsResetUponSuccess() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_NORMAL, RETRY_COUNT_NORMAL);
        runTest();
    }

    /**
     * Purpose:
     * 1) Verify that we do not get any retry (not even the required automatic one) when the user configured to have zero retries.
     */
    @Test
    @AllowedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testConfiguredForZeroRetries() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_NORMAL, RETRY_COUNT_ZERO);
        server.setMarkToEndOfLog();
        runTest();
        assertEquals("Max retries 0 was not reached (CNTR0179W)", 1, server.findStringsInLogsUsingMark("CNTR0179W:.*0", server.getDefaultLogFile()).size());
    }

    /**
     * Purpose:
     * 1) Verify that an interval timer that hits max retries will continue on and fire next interval.
     */
    @Test
    @AllowedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testConfiguredForZeroRetriesTimerReschedules() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_NORMAL, RETRY_COUNT_ZERO);
        server.setMarkToEndOfLog();
        runTest();
        assertEquals("Max retries 0 was not reached (CNTR0179W)", 2, server.waitForMultipleStringsInLogUsingMark(2, "CNTR0179W:.*0"));
    }

    /**
     * Purpose:
     * 1) Verify that an calendar timer that hits max retries will continue on and fire next interval.
     */
    @Test
    @AllowedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testConfiguredForTwoRetriesTimerReschedulesCalendar() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_NORMAL, RETRY_COUNT_TWO);
        server.setMarkToEndOfLog();
        runTest();
        assertEquals("Max retries 2 was not reached (CNTR0179W)", 2, server.waitForMultipleStringsInLogUsingMark(2, "CNTR0179W:.*2"));
    }

    /**
     * Purpose:
     * 1) Verify that an interval timer that hits max retries will have it's retry count reset for next interval.
     */
    @Test
    @AllowedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testConfiguredForTwoRetriesTimerReschedules() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_NORMAL, RETRY_COUNT_TWO);
        server.setMarkToEndOfLog();
        runTest();
        assertEquals("Max retries 2 was not reached (CNTR0179W)", 2, server.waitForMultipleStringsInLogUsingMark(2, "CNTR0179W:.*2"));
    }

    /**
     * Purpose:
     * 1) Verify that we retry endlessly when the user omits the retry count configuration attribute.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    @AllowedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testConfiguredForEndlessRetry() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_NORMAL, null);
        runTest();
    }

    /**
     * Purpose:
     * 1) Verify that we attempt all retries immediately, because the user configured an interval of zero.
     */
    @Test
    @AllowedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testConfiguredForImmediateRetry() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_ZERO, RETRY_INTERVAL_LONG.intValue());
        server.setMarkToEndOfLog();
        runTest();
        assertEquals("Max retries 10 was not reached (CNTR0179W)", 1, server.findStringsInLogsUsingMark("CNTR0179W:.*10", server.getDefaultLogFile()).size());
    }

    /**
     * Purpose:
     * 1) Verify that we attempt all retries (after the first immediate, automatic retry) on 5 minute intervals, because that is the default interval
     * that the user should get when the interval attribute is omitted from the configuration.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    // Full because test sleeps for over 5 minutes; repeat for only one action
    @SkipForRepeat({ EE7FeatureReplacementAction.ID, JakartaEE9Action.ID })
    @AllowedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testConfiguredForDefaultRetryInterval() throws Exception {
        setNonPersistentTimerRetryConfiguration(null, RETRY_COUNT_NORMAL);
        runTest();
    }

    /**
     * Purpose:
     * 1) Verify that when retry attempts and regularly scheduled attempts overlap, that we handle this correctly.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    @ExpectedFFDC("javax.ejb.TransactionRolledbackLocalException")
    public void testOverlappingRetriesAndRegularlyScheduled() throws Exception {
        setNonPersistentTimerRetryConfiguration(RETRY_INTERVAL_LONG, RETRY_COUNT_NORMAL);
        runTest();
    }

    /**
     * Change the setting of the asynchronous method ContextService to the specified value;
     * nothing is done if the specified value is the existing value.
     *
     * @param asyncContextService the name of the context service to use; null indicates no
     *            asynchronous element present; "" indicates no contextServiceRef attribute.
     */
    private void setNonPersistentTimerRetryConfiguration(Long retryInterval, Integer retryCount) throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        EJBContainerElement ejbContainer = config.getEJBContainer();
        EJBTimerServiceElement currentTimerService = ejbContainer.getTimerService();
        boolean configChanged = false;

        if (retryInterval == null && retryCount == null) {
            if (currentTimerService == null) {
                logger.info(testName.getMethodName() + " : timerService config already does not exist");
            } else {
                logger.info(testName.getMethodName() + " : removing timerService element");
                ejbContainer.setTimerService(null);
                configChanged = true;
            }
        } else {
            if (currentTimerService == null) {
                currentTimerService = new EJBTimerServiceElement();
                ejbContainer.setTimerService(currentTimerService);
                configChanged = true;
            }

            Long currentRetryInterval = currentTimerService.getNonPersistentRetryInterval();
            if ((retryInterval != null && !retryInterval.equals(currentRetryInterval)) ||
                (currentRetryInterval != null && !currentRetryInterval.equals(retryInterval))) {
                currentTimerService.setNonPersistentRetryInterval(retryInterval);
                configChanged = true;
            }

            Integer currentRetryCount = currentTimerService.getNonPersistentMaxRetries();
            if ((retryCount != null && !retryCount.equals(currentRetryCount)) ||
                (currentRetryCount != null && !currentRetryCount.equals(retryCount))) {
                currentTimerService.setNonPersistentMaxRetries(retryCount);
                configChanged = true;
            }
        }

        if (configChanged) {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            assertNotNull(server.waitForConfigUpdateInLogUsingMark(null));
        }
    }

    /**
     * Test non-persistent timer logging a warning message when a Timer is starting
     * later than the default lateTimerThreshold of 5 minutes
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    // Full because test sleeps for over 5 minutes; repeat for only one action
    @SkipForRepeat({ EE7FeatureReplacementAction.ID, JakartaEE9Action.ID })
    public void testDefaultLateTimerMessage() throws Exception {
        String warningRegExp = "CNTR0333W(?=.*LateWarningBean)(?=.*NpTimerConfigLateWarningEJB.jar)(?=.*NpTimerConfigRetryApp)";
        setLateTimerThresholdConfiguration(null);
        server.setMarkToEndOfLog();
        runTest(server, LATE_MSG_SERVLET, "testDefaultLateWarningMessageSetup");
        assertNotNull("Did not receive expected message in log 'CNTR0333W:'", server.waitForStringInLogUsingMark(warningRegExp, 6 * 60 * 1000));
        runTest(server, LATE_MSG_SERVLET, "testLateWarningMessageTearDown");
    }

    /**
     * Test non-persistent timer logging a warning message when a Timer is starting
     * later than the configured lateTimerThreshold of 1 minute.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    // Full because test sleeps for over 1 minutes; repeat for only one action
    @SkipForRepeat({ EE7FeatureReplacementAction.ID, JakartaEE9Action.ID })
    public void testConfiguredLateTimerMessage() throws Exception {
        String warningRegExp = "CNTR0333W(?=.*LateWarningBean)(?=.*NpTimerConfigLateWarningEJB.jar)(?=.*NpTimerConfigRetryApp)";
        setLateTimerThresholdConfiguration(1L);
        server.setMarkToEndOfLog();
        runTest(server, LATE_MSG_SERVLET, "testConfiguredLateWarningMessageSetup");
        assertNotNull("Did not receive expected message in log 'CNTR0333W:'", server.waitForStringInLogUsingMark(warningRegExp, 2 * 60 * 1000));
        runTest(server, LATE_MSG_SERVLET, "testLateWarningMessageTearDown");
        setLateTimerThresholdConfiguration(null);
    }

    /**
     * Change the setting of the timerService lateTimerThreshold to the specified value;
     * nothing is done if the specified value is the existing value.
     *
     * @param lateTimerThreshold late timer threshold in minutes, or null.
     */
    private static void setLateTimerThresholdConfiguration(Long lateTimerThreshold) throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        EJBContainerElement ejbContainer = config.getEJBContainer();
        EJBTimerServiceElement timerService = ejbContainer.getTimerService();
        Long currentLateTimerThreshold = timerService.getLateTimerThreshold();

        if (lateTimerThreshold != currentLateTimerThreshold) {
            timerService.setLateTimerThreshold(lateTimerThreshold);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            assertNotNull(server.waitForConfigUpdateInLogUsingMark(null));
        }
    }
}
