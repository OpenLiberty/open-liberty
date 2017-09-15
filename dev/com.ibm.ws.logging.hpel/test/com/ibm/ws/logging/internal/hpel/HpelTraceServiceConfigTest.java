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
package com.ibm.ws.logging.internal.hpel;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class HpelTraceServiceConfigTest {
    private static SharedOutputManager outputMgr;
    Map<String, String> bootConfig = new HashMap<String, String>();

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(CommonUtils.UNITTEST_LOGS);
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Before
    public void setUp() throws Exception {
        bootConfig.clear();
        bootConfig.put(HpelConstants.INTERNAL_SERVER_NAME, CommonUtils.SERVER_NAME);
        bootConfig.put(HpelConstants.BOOTPROP_PRODUCT_INFO, CommonUtils.PRODUCT_INFO);
    }

    @Test
    public void testDefaultValues() {
        final String m = "testDefaultValues";
        try {
            // Do stuff here. The outputMgr catches all output issued to stdout or stderr
            // unless/until an unexpected exception occurs. failWithThrowable will copy
            // all captured output back to the original streams before failing
            // the testcase.
            HpelTraceServiceConfig config = new HpelTraceServiceConfig(bootConfig, null, CommonUtils.TEXT_FACTORY);

            // Calculate the expected default state of the config.
            String dataDirectory = new File("").getAbsolutePath(); // Current location.
            StringBuilder expected = new StringBuilder("HpelTraceServiceConfig[log.dataDirectory=");
            expected.append(dataDirectory);
            expected.append(",log.purgeMaxSize=-1,log.purgeMinTime=-1,log.outOfSpaceAction=StopLogging,");
            expected.append("log.bufferingEnabled=true,log.fileSwitchTime=-1,trace.dataDirectory=");
            expected.append(dataDirectory);
            expected.append(",trace.purgeMaxSize=-1,trace.purgeMinTime=-1,trace.outOfSpaceAction=PurgeOld,");
            expected.append("trace.bufferingEnabled=true,trace.fileSwitchTime=-1,trace.memoryBufferSize=-1]");

            assertEquals("Unexpected default values", expected.toString(), config.toString());
            assertEquals("Default value mismatch for serverName", CommonUtils.SERVER_NAME, config.ivServerName);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testReadingBootstrapValues() {
        final String m = "testReadingBootstrapValues";
        try {
            File tmpLog = File.createTempFile("log", "");

            // Fill bootConfig with bootstrap values
            // note that we use com.ibm.ws.logging.log.directory since 
            // we decided not to expose com.ibm.hpel.log/trace.dataDirectory in the product
            bootConfig.put("com.ibm.ws.logging.log.directory", tmpLog.getAbsolutePath());
            bootConfig.put("com.ibm.hpel.log.purgeMaxSize", "37");
            bootConfig.put("com.ibm.hpel.log.purgeMinTime", "13");
            bootConfig.put("com.ibm.hpel.log.outOfSpaceAction", "StopServer");
            bootConfig.put("com.ibm.hpel.log.bufferingEnabled", "false");
            bootConfig.put("com.ibm.hpel.log.fileSwitchTime", "7");
            bootConfig.put("com.ibm.hpel.trace.purgeMaxSize", "23");
            bootConfig.put("com.ibm.hpel.trace.purgeMinTime", "17");
            bootConfig.put("com.ibm.hpel.trace.outOfSpaceAction", "StopLogging");
            bootConfig.put("com.ibm.hpel.trace.bufferingEnabled", "true");
            bootConfig.put("com.ibm.hpel.trace.fileSwitchTime", "3");
            bootConfig.put("com.ibm.hpel.trace.memoryBufferSize", "87");

            HpelTraceServiceConfig config = new HpelTraceServiceConfig(bootConfig, null, CommonUtils.TEXT_FACTORY);
            // Enabled text to see its attributes in toString()
            HashMap<String, Object> newConfig = new HashMap<String, Object>();
            newConfig.put(HpelConstants.TEXT_LOG, new String[] { "textLog_reference" });
            config.update(newConfig);

            // Calculate the expected read state of the config.
            StringBuilder expected = new StringBuilder("HpelTraceServiceConfig[log.dataDirectory=");
            expected.append(tmpLog.getAbsolutePath());
            expected.append(",log.purgeMaxSize=37,log.purgeMinTime=13,log.outOfSpaceAction=StopServer,");
            expected.append("log.bufferingEnabled=false,log.fileSwitchTime=7,trace.dataDirectory=");
            expected.append(tmpLog.getAbsolutePath());
            expected.append(",trace.purgeMaxSize=23,trace.purgeMinTime=17,trace.outOfSpaceAction=StopLogging,");
            expected.append("trace.bufferingEnabled=true,trace.fileSwitchTime=3,trace.memoryBufferSize=87");
            expected.append("]");
            assertEquals("Bootstrap values were read incorrectly", expected.toString(), config.toString());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testGetterMethods() {
        final String m = "testReadingBootstrapValues";
        try {
            File tmpRepo = File.createTempFile("repository", "");

            // Fill bootConfig with bootstrap values
            bootConfig.put("com.ibm.ws.logging.log.directory", tmpRepo.getAbsolutePath());
            bootConfig.put("com.ibm.hpel.log.purgeMaxSize", "43");
            bootConfig.put("com.ibm.hpel.log.purgeMinTime", "19");
            bootConfig.put("com.ibm.hpel.trace.purgeMaxSize", "31");
            bootConfig.put("com.ibm.hpel.trace.purgeMinTime", "11");
            bootConfig.put("com.ibm.hpel.trace.memoryBufferSize", "91");

            HpelTraceServiceConfig config = new HpelTraceServiceConfig(bootConfig, null, CommonUtils.TEXT_FACTORY);

            assertEquals("ivLog.getLocation() mismatch", new File(tmpRepo, "logdata"), config.ivLog.getLocation());
            assertEquals("ivLog.getPurgeMaxSize() mismatch", 43 * 1024 * 1024L, config.ivLog.getPurgeMaxSize());
            assertEquals("ivLog.getPurgeMinTime() mismatch", 19 * 60 * 60 * 1000L, config.ivLog.getPurgeMinTime());
            assertEquals("ivTrace.getLocation() mismatch", new File(tmpRepo, "tracedata"), config.ivTrace.getLocation());
            assertEquals("ivTrace.getPurgeMaxSize() mismatch", 31 * 1024 * 1024L, config.ivTrace.getPurgeMaxSize());
            assertEquals("ivTrace.getPurgeMinTime() mismatch", 11 * 60 * 60 * 1000L, config.ivTrace.getPurgeMinTime());
            assertEquals("ivTrace.getMemoryBufferSize() mismatch", 91 * 1024 * 1024L, config.ivTrace.getMemoryBufferSize());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
