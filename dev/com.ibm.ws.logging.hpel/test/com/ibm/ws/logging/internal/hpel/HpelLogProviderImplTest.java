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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryReaderImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.logprovider.LogProvider;

/**
 *
 */
public class HpelLogProviderImplTest {
    LogProvider provider;

    @Before
    public void setUp() throws Exception {
        provider = new HpelLogProviderImpl();
    }

    @After
    public void tearDown() throws Exception {
        // Make sure all logs are closed
        provider.stop();
    }

    @Test
    public void testWritingMessages() throws IOException {

        // Clean up after previous tests
        CommonUtils.delDir(CommonUtils.LOG_DIR);

        // Can't use CapturedOutputHolder here since it replaces TraceService set by LogProvider
        Map<String, String> bootConfig = new HashMap<String, String>();
        bootConfig.put(HpelConstants.INTERNAL_SERVER_NAME, CommonUtils.SERVER_NAME);
        bootConfig.put(HpelConstants.BOOTPROP_PRODUCT_INFO, CommonUtils.PRODUCT_INFO);
        // Disable buffering to have log records on disk faster.
        bootConfig.put("com.ibm.hpel.log.bufferingEnabled", "false");
        bootConfig.put("com.ibm.hpel.trace.bufferingEnabled", "false");
        bootConfig.put("com.ibm.hpel.text.bufferingEnabled", "false");
        // Include trace into TextLog.
        bootConfig.put("com.ibm.hpel.text.includeTrace", "true");

        // Enable all the messages via bootstrap property
        bootConfig.put("com.ibm.ws.logging.trace.specification", "com.*=all:org.*=all");

        provider.configure(bootConfig, CommonUtils.LOG_DIR, CommonUtils.TEXT_FACTORY);

        // Enable TextLog
        HashMap<String, Object> newConfig = new HashMap<String, Object>();
        newConfig.put(HpelConstants.TEXT_LOG, new String[] { "textLog_reference" });
        newConfig.put("suppressSensitiveTrace", false);
        TrConfigurator.update(newConfig);

        TraceComponent tc = Tr.register(getClass());

        String[] msgs = {
                          "audit message",
                          "debug message",
                          "Dump: dump message",
                          "Entry ",
                          "error message",
                          "event message",
                          "Exit ",
                          "fatal message",
                          "info message"
        };
        Tr.audit(tc, msgs[0]);
        Tr.debug(tc, msgs[1]);
        Tr.dump(tc, "dump message");
        Tr.entry(tc, "method");
        Tr.error(tc, msgs[4]);
        Tr.event(tc, msgs[5]);
        Tr.exit(tc, "method");
        Tr.fatal(tc, msgs[7]);
        Tr.info(tc, msgs[8]);

        // Should see exactly one WBL file in logdata and one in tracedata directories.
        assertEquals("Unexpected number of WBL files found", 2, CommonUtils.listWbls(CommonUtils.LOG_DIR, null));

        // Verify that repository contains all the messages.
        RepositoryReaderImpl reader = new RepositoryReaderImpl(CommonUtils.LOG_DIR);
        int count = 0;
        for (RepositoryLogRecord record : reader.getLogListForCurrentServerInstance()) {
            String formattedMessage = record.getFormattedMessage();
            if (count > 0) { // First one is a message about trace specification change.
                if (count > msgs.length) {
                    fail("Unexpected message: " + formattedMessage);
                }
                assertEquals("Record has incorrect formatted message", msgs[count - 1], formattedMessage);
            }
            count++;
        }
        assertEquals("Different number of records were found in the repository", msgs.length + 1, count);

        // Cannot verify writing to System.out, System.err here since that would
        // require overwriting writeStreamOutput method on HpelBaseTraceService
        // and as a result not completely clean test of HpelLogProviderImpl.
        // Assume that test done by HpelBaseTraceServiceTest for this is enough.

        // Find TextLog file
        File textLog = null;
        for (File file : CommonUtils.LOG_DIR.listFiles()) {
            if (file.getName().startsWith("TextLog_")) {
                textLog = file;
            }
        }
        assertNull("Unexpectedly found TextLog file", textLog);
//        assertNotNull("Didn't find TextLog file", textLog);
//        assertTrue("Text file was not created using TextFileOutputStreamFactory", CommonUtils.textFileContainsLine(textLog, CommonUtils.STREAM_TAG));
//        for (String msg : msgs) {
//            assertTrue("Text file is missing \"" + msg + "\" record", CommonUtils.textFileContainsLine(textLog, msg));
//        }
    }
}
