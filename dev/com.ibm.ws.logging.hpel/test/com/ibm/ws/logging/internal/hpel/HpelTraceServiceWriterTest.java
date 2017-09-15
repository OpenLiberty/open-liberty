/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class HpelTraceServiceWriterTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(CommonUtils.UNITTEST_LOGS);

    @Rule
    public TestRule output = outputMgr;

    Map<String, String> bootConfig = new HashMap<String, String>();
    HpelTraceServiceWriter writer;

    @Before
    public void setUp() throws Exception {
        writer = new HpelTraceServiceWriter(null);
        CommonUtils.delDir(CommonUtils.LOG_DIR);
        bootConfig.clear();
        bootConfig.put(HpelConstants.INTERNAL_SERVER_NAME, CommonUtils.SERVER_NAME);
        bootConfig.put(HpelConstants.BOOTPROP_PRODUCT_INFO, CommonUtils.PRODUCT_INFO);
    }

    @After
    public void tearDown() throws Exception {
        writer.stop();
    }

    @Test
    public void testWritingMessagesDirectly() {
        // Disable buffering to have log records on disk faster.
        bootConfig.put("com.ibm.hpel.log.bufferingEnabled", "false");
        bootConfig.put("com.ibm.hpel.trace.bufferingEnabled", "false");
        HpelTraceServiceConfig config = new HpelTraceServiceConfig(bootConfig, CommonUtils.LOG_DIR, CommonUtils.TEXT_FACTORY);
        writer.configure(config);
        File[] files = CommonUtils.LOG_DIR.listFiles();
        StringBuilder fileMessage = new StringBuilder("Number of created logs files is unexpected: ");
        for (File file : files) {
            fileMessage.append(file.getName()).append(" ");
        }
        // Should see just 'logdata' and 'tracedata' directories
        assertEquals(fileMessage.toString(), 2, files.length);
        assertEquals("Unexpectedly found WBL files before recording any record", 0, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));

        LogRecord record = new LogRecord(Level.INFO, "First message");
        writer.repositoryPublish(record);
        // Should find one in logdata directory
        assertEquals("Unexpectedly found wrong number of WBL files after first INFO record", 1, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));
        assertEquals("Unexpectedly found lock files in logdata directory", 0, new File(CommonUtils.LOG_DIR, "logdata").list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".lock");
            }
        }).length);

        record = new LogRecord(Level.FINE, "Second message");
        writer.repositoryPublish(record);
        // Should find on in logdata and one in tracedata directories.
        assertEquals("Unexpectedly found wrong number of WBL files after first FINE record", 2, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));
        assertEquals("Unexpectedly found lock files in tracedata directory", 0, new File(CommonUtils.LOG_DIR, "tracedata").list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".lock");
            }
        }).length);

        // Ensure that using the same configuration data does not result WBL file switch.
        writer.configure(config);
        record = new LogRecord(Level.WARNING, "Third message");
        writer.repositoryPublish(record);
        // Should still find just 2 WBL files.
        assertEquals("Unexpectedly found wrong number of WBL files after second configuration call", 2, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));

        // Ensure that using configuration with the same location does not result in WBL file switch.
        // Don't enable buffering here since it will make Unit test longer by 10 seconds (default buffering wait)
        config.ivLog.ivPurgeMaxSize = 100;
        config.ivLog.ivPurgeMinTime = 24;
        config.ivLog.ivOutOfSpaceAction = HpelTraceServiceConfig.OutOfSpaceAction.StopServer;
        // Don't change fileSwitchTime yet since HPEL has problem with doing it on the fly.
        //config.ivLog.ivFileSwitchTime = 1;
        config.ivTrace.ivPurgeMaxSize = 200;
        config.ivTrace.ivPurgeMinTime = 48;
        config.ivTrace.ivOutOfSpaceAction = HpelTraceServiceConfig.OutOfSpaceAction.StopServer;
        // Don't change fileSwitchTime yet since HPEL has problem with doing it on the fly.
        //config.ivTrace.ivFileSwitchTime = 2;
        writer.configure(config);
        record = new LogRecord(Level.FINEST, "Fourth message");
        writer.repositoryPublish(record);
        // Should still find just 2 WBL files.
        assertEquals("Unexpectedly found wrong number of WBL files after third configuration call with the same location", 2, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));
    }

    @Test
    public void testWritingMessageUsingLogger() {
        Handler handler = null;
        try {
            // Disable buffering to have log records on disk faster.
            bootConfig.put("com.ibm.hpel.log.bufferingEnabled", "false");
            bootConfig.put("com.ibm.hpel.trace.bufferingEnabled", "false");
            HpelTraceServiceConfig config = new HpelTraceServiceConfig(bootConfig, CommonUtils.LOG_DIR, CommonUtils.TEXT_FACTORY);
            writer.configure(config);
            handler = writer.getHandler();
            Logger.getLogger("").addHandler(handler);

            Logger logger = Logger.getLogger(getClass().getName());
            logger.setLevel(Level.ALL); // Allow all messages to go through.

            logger.info("First message");
            // Should find one in logdata directory
            assertEquals("Unexpectedly found wrong number of WBL files after first INFO message", 1, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));

            logger.fine("Second message");
            // Should find on in logdata and one in tracedata directories.
            assertEquals("Unexpectedly found wrong number of WBL files after first FINE message", 2, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));
        } finally {
            if (handler != null) {
                Logger.getLogger("").removeHandler(handler);
            }
        }
    }

    @Test
    public void testTextCopy() throws IOException {
        // Disable buffering to have log records on disk faster.
        bootConfig.put("com.ibm.hpel.log.bufferingEnabled", "false");
        bootConfig.put("com.ibm.hpel.trace.bufferingEnabled", "false");
        bootConfig.put("com.ibm.hpel.text.bufferingEnabled", "false");
        HpelTraceServiceConfig config = new HpelTraceServiceConfig(bootConfig, CommonUtils.LOG_DIR, CommonUtils.TEXT_FACTORY);
        // Enable Text Copy
        HashMap<String, Object> newConfig = new HashMap<String, Object>();
        newConfig.put(HpelConstants.TEXT_LOG, new String[] { "textLog_reference" });
        config.update(newConfig);
        // Configure writer
        writer.configure(config);

        File[] files = CommonUtils.LOG_DIR.listFiles();
        StringBuilder fileMessage = new StringBuilder("Number of created logs files is unexpected: ");
        File textLog = null;
        for (File file : files) {
            fileMessage.append(file.getName()).append(" ");
            if (file.getName().startsWith("TextLog_")) {
                textLog = file;
            }
        }
        // Should see just 'logdata', 'tracedata' //, and 'TextLog_*'
        assertEquals(fileMessage.toString(), 2, files.length);
        assertEquals("Unexpectedly found WBL files before recording any record", 0, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));
        assertNull("Unexpectedly found TextLog file", textLog);
//        assertNotNull("Didn't find TextLog file", textLog);
//        assertTrue("Text file was not created using TextFileOutputStreamFactory", CommonUtils.textFileContainsLine(textLog, CommonUtils.STREAM_TAG));

//        LogRecord record = new LogRecord(Level.INFO, "First message");
//        writer.repositoryPublish(record);
//        // Should find the log record in textLog
//        assertTrue("Text file does not contain the log record", CommonUtils.textFileContainsLine(textLog, "First message"));
//
//        record = new LogRecord(Level.FINE, "Second message");
//        writer.repositoryPublish(record);
//        // Should NOT find the trace record in textLog since they are not included by default
//        assertFalse("Text file incorrectly contains the trace record", CommonUtils.textFileContainsLine(textLog, "Second message"));
    }

    @Test
    public void testStopEvents() {
        // Disable buffering to have log records on disk faster.
        bootConfig.put("com.ibm.hpel.log.bufferingEnabled", "false");
        bootConfig.put("com.ibm.hpel.trace.bufferingEnabled", "false");
        HpelTraceServiceConfig config = new HpelTraceServiceConfig(bootConfig, CommonUtils.LOG_DIR, CommonUtils.TEXT_FACTORY);
        writer.configure(config);

        // Force removal of handlers as during shutdown.
        LogManager.getLogManager().reset();

        LogRecord record = new LogRecord(Level.INFO, "First message");
        writer.repositoryPublish(record);
        // Should find one in logdata directory since close of the handler should not cause the close of the writer
        assertEquals("Unexpectedly found wrong number of WBL files after first INFO message after LogManager.reset()", 1, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));

        writer.stop();

        record = new LogRecord(Level.FINE, "Second message");
        writer.repositoryPublish(record);
        // Should still find just one in logdata directory since we already closed writer.
        assertEquals("Unexpectedly found wrong number of WBL files after first FINE message after writer.stop()", 1, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));
    }

}
