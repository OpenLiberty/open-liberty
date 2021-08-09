/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryReaderImpl;
import com.ibm.websphere.ras.TraceComponent;

import test.common.SharedOutputManager;

/**
 *
 */
public class HpelBaseTraceServiceTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(CommonUtils.UNITTEST_LOGS);
    Map<String, String> bootConfig = new HashMap<String, String>();

    @Rule
    public TestRule outputRule = outputMgr;

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
        CommonUtils.delDir(CommonUtils.LOG_DIR);
        bootConfig.clear();
        bootConfig.put(HpelConstants.INTERNAL_SERVER_NAME, CommonUtils.SERVER_NAME);
        bootConfig.put(HpelConstants.BOOTPROP_PRODUCT_INFO, CommonUtils.PRODUCT_INFO);
    }

    public static void setTraceSpec(TraceComponent tc, String spec) throws Exception {
        Method m = TraceComponent.class.getDeclaredMethod("setTraceSpec", String.class);
        m.setAccessible(true);

        m.invoke(tc, spec);
    }

    @Test
    public void testWritingMessages() throws Exception {
        HpelBaseTraceService service = new HpelBaseTraceService();
        try {
            // Do stuff here. The outputMgr catches all output issued to stdout or stderr
            // unless/until an unexpected exception occurs. failWithThrowable will copy
            // all captured output back to the original streams before failing
            // the testcase.

            // Disable buffering to have log records on disk faster.
            bootConfig.put("com.ibm.hpel.log.bufferingEnabled", "false");
            bootConfig.put("com.ibm.hpel.trace.bufferingEnabled", "false");
            bootConfig.put("com.ibm.hpel.text.bufferingEnabled", "false");
            HpelTraceServiceConfig config = new HpelTraceServiceConfig(bootConfig, CommonUtils.LOG_DIR, CommonUtils.TEXT_FACTORY);

            // Enable TextLog
            HashMap<String, Object> newConfig = new HashMap<String, Object>();
            newConfig.put(HpelConstants.TEXT_LOG, new String[] { "textLog_reference" });
            config.update(newConfig);

            service.init(config);

            // No need to register TraceComponent since Tr is not initialized.
            TraceComponent tc = new TraceComponent(getClass()) {};
            // Need to set trace spec directly since Tr is not initialized.
            setTraceSpec(tc, "*=all");

            File[] files = CommonUtils.LOG_DIR.listFiles();
            StringBuilder fileMessage = new StringBuilder("Number of created logs files is unexpected: ");
            for (File file : files) {
                fileMessage.append(file.getName()).append(" ");
            }
            // Should see just 'logdata', 'tracedata', // and "TextLog_*"
            assertEquals(fileMessage.toString(), 2, files.length);
            assertEquals("Unexpectedly found WBL files before recording any record", 0, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));

            String[] msgs = {
                              "audit message",
                              "debug message",
                              "Dump: dump message",
                              "Entry ",
                              "error message",
                              "event message",
                              "Exit ",
                              "fatal message",
                              "info message",
                              "System.out message",
                              "System.err message",
                              "Not-copied System.out",
                              "Not-copied System.err"
            };
            service.audit(tc, msgs[0]);
            service.debug(tc, msgs[1]);
            service.dump(tc, "dump message");
            service.entry(tc, "method");
            service.error(tc, msgs[4]);
            service.event(tc, msgs[5]);
            service.exit(tc, "method");
            service.fatal(tc, msgs[7]);
            service.info(tc, msgs[8]);
            System.out.println(msgs[9]);
            System.err.println(msgs[10]);

            // Verify that repository contains all the messages.
            RepositoryReaderImpl reader = new RepositoryReaderImpl(CommonUtils.LOG_DIR);
            int count = 0;
            for (RepositoryLogRecord record : reader.getLogListForCurrentServerInstance()) {
                String formattedMessage = record.getFormattedMessage();
                if (count >= msgs.length) {
                    fail("Unexpected message: " + formattedMessage);
                }
                assertEquals("Record has incorrect formatted message", msgs[count], formattedMessage);
                count++;
            }

            // Verify that System.out has AUDIT messages and whatever what directly sent to System.out
            assertTrue("Audit message is not found on System.out", outputMgr.checkForStandardOut(msgs[0]));
            assertTrue("Output to System.out should be found on System.out", outputMgr.checkForStandardOut(msgs[9]));

            // Others should not be there.
            assertFalse("Unexpectedly found debug message on System.out", outputMgr.checkForStandardOut(msgs[1]));
            assertFalse("Unexpectedly found dump message on System.out", outputMgr.checkForStandardOut(msgs[2]));
            assertFalse("Unexpectedly found entry message on System.out", outputMgr.checkForStandardOut(msgs[3]));
            assertFalse("Unexpectedly found error message on System.out", outputMgr.checkForStandardOut(msgs[4]));
            assertFalse("Unexpectedly found event message on System.out", outputMgr.checkForStandardOut(msgs[5]));
            assertFalse("Unexpectedly found exit message on System.out", outputMgr.checkForStandardOut(msgs[6]));
            assertFalse("Unexpectedly found fatal message on System.out", outputMgr.checkForStandardOut(msgs[7]));
            assertFalse("Unexpectedly found info message on System.out", outputMgr.checkForStandardOut(msgs[8]));
            assertFalse("Unexpectedly found output to System.err on System.out", outputMgr.checkForStandardOut(msgs[10]));

            // Verify that System.err has error and fatal messages as well as prints to System.err
            assertTrue("Error message is not found on System.err", outputMgr.checkForStandardErr(msgs[4]));
            assertTrue("Fatal message is not found on System.err", outputMgr.checkForStandardErr(msgs[7]));
            assertTrue("Output to System.err should be found on System.err", outputMgr.checkForStandardErr(msgs[10]));

            // Others should not be there.
            assertFalse("Unexpectedly found audit message on System.err", outputMgr.checkForStandardErr(msgs[0]));
            assertFalse("Unexpectedly found debug message on System.err", outputMgr.checkForStandardErr(msgs[1]));
            assertFalse("Unexpectedly found dump message on System.err", outputMgr.checkForStandardErr(msgs[2]));
            assertFalse("Unexpectedly found entry message on System.err", outputMgr.checkForStandardErr(msgs[3]));
            assertFalse("Unexpectedly found event message on System.err", outputMgr.checkForStandardErr(msgs[5]));
            assertFalse("Unexpectedly found exit message on System.err", outputMgr.checkForStandardErr(msgs[6]));
            assertFalse("Unexpectedly found info message on System.err", outputMgr.checkForStandardErr(msgs[8]));
            assertFalse("Unexpectedly found output to System.out on System.err", outputMgr.checkForStandardErr(msgs[9]));

            // Find TextLog file
            File textLog = null;
            for (File file : CommonUtils.LOG_DIR.listFiles()) {
                if (file.getName().startsWith("TextLog_")) {
                    textLog = file;
                }
            }
            assertNull("Unexpectedly found TextLog file", textLog);
//            assertNotNull("Didn't find TextLog file", textLog);
//            assertTrue("Text file was not created using TextFileOutputStreamFactory", CommonUtils.textFileContainsLine(textLog, CommonUtils.STREAM_TAG));
//
//            assertTrue("Audit message is not in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[0]));
//            assertTrue("Error message is not in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[4]));
//            assertTrue("Fatal message is not in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[7]));
//            assertTrue("Info message is not in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[8]));
//            assertTrue("Output to System.out should be in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[9]));
//            assertTrue("Output to System.err should be in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[10]));
//            assertFalse("Unexpectedly found Debug message in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[1]));
//            assertFalse("Unexpectedly found Dump message in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[2]));
//            assertFalse("Unexpectedly found Entry message in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[3]));
//            assertFalse("Unexpectedly found Event message in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[5]));
//            assertFalse("Unexpectedly found Exit message in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[6]));

            newConfig.put("copySystemStreams", "false");
            config.update(newConfig);
            service.update(config);

            System.out.println(msgs[11]);
            System.err.println(msgs[12]);
//            assertTrue("Output to System.out should be in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[11]));
            assertFalse("Output to System.out should not be found on system out", outputMgr.checkForStandardOut(msgs[11]));
//            assertTrue("Output to System.err should be in TextLog file", CommonUtils.textFileContainsLine(textLog, msgs[12]));
            assertFalse("Output to System.err should not have gone to system err", outputMgr.checkForStandardErr(msgs[12]));

        } finally {
            service.stop();
        }
    }

}
