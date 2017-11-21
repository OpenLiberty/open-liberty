/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.SharedTr;
import com.ibm.ws.logging.internal.DisabledFFDCService;
import com.ibm.ws.logging.internal.impl.LoggingConstants.TraceFormat;
import com.ibm.wsspi.logprovider.FFDCFilterService;
import com.ibm.wsspi.logprovider.TrService;

/**
 *
 */
public class LogProviderConfigTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(TestConstants.BUILD_TMP);

    @Rule
    public TestRule outputRule = outputMgr;

    /**
     * Test method for {@link com.ibm.ws.logging.internal.impl.BaseTraceServiceWriter#initialize(java.util.Dictionary, java.lang.String, boolean)} .
     */
    @Test
    public void testDefaults() {
        String m = "testDefaults";

        // "FRESH values"
        Map<String, String> map = new HashMap<String, String>();
        LogProviderConfigImpl config = new LogProviderConfigImpl(map, TestConstants.BUILD_TMP, SharedTr.fileStreamFactory);

        assertEquals(m + "-1a: empty properties should default to default", TestConstants.BUILD_TMP, config.getLogDirectory());
        assertEquals(m + "-1b: empty properties should default to default", WsLevel.AUDIT, config.getConsoleLogLevel());
        assertEquals(m + "-1c: empty properties should default to default", 0, config.getMaxFiles());
        assertEquals(m + "-1d: empty properties should default to default", 0, config.getMaxFileBytes());
        assertEquals(m + "-1e: empty properties should default to default", "messages.log", config.getMessageFileName());
        assertEquals(m + "-1f: empty properties should default to default", "trace.log", config.getTraceFileName());
        assertEquals(m + "-1g: empty properties should default to default", "*=info", config.getTraceString());
        assertEquals(m + "-1h: empty properties should default to default", TraceFormat.ENHANCED, config.getTraceFormat());
    }

    @Test
    public void testGetLogLocation() throws IOException {
        final String m = "testGetLogLocation";

        File f = LoggingConfigUtils.getLogDirectory(null, null);
        assertNotNull(m + "-1: non-null return when a log location is not specified", f);

        f = LoggingConfigUtils.getLogDirectory(null, TestConstants.BUILD_TMP);
        assertEquals(m + "-2a: return the provided file", TestConstants.BUILD_TMP, f);

        f = LoggingConfigUtils.getLogDirectory(TestConstants.BUILD_TMP.getAbsolutePath(), null);
        assertEquals(m + "-2b: return the provided file", TestConstants.BUILD_TMP.getCanonicalFile(), f.getCanonicalFile());
    }

    @Test
    public void testGetStringValue() {
        final String m = "testGetStringValue";

        String s = LoggingConfigUtils.getStringValue(null, "default");
        assertEquals(m + "-1: Null parameter should preserve default value", "default", s);

        s = LoggingConfigUtils.getStringValue("letters", "default");
        assertEquals(m + "-2: Parameter should supplant the default value", "letters", s);
    }

    @Test
    public void testGetBooleanValue() {
        final String m = "testGetBooleanValue";

        boolean i = LoggingConfigUtils.getBooleanValue("letters", false);
        assertEquals(m + "-1: Exception should preserve original value", false, i);

        i = LoggingConfigUtils.getBooleanValue("true", false);
        assertEquals(m + "-2: well-formed number should replace old value", true, i);
    }

    @Test
    public void testGetIntValue() {
        final String m = "testGetIntValue";

        int i = LoggingConfigUtils.getIntValue("letters", 5);
        assertEquals(m + "-1: Exception should preserve original value", 5, i);

        i = LoggingConfigUtils.getIntValue("2", i);
        assertEquals(m + "-2: well-formed number should replace old value", 2, i);
    }

    @Test
    public void testGetLogLevel() {
        final String m = "testGetLogLevel";

        Level level = LoggingConfigUtils.getLogLevel(null, WsLevel.AUDIT);
        assertEquals(m + "-1: Null parameter should preserve default value", WsLevel.AUDIT, level);
        level = LoggingConfigUtils.getLogLevel(12, WsLevel.AUDIT);
        assertEquals(m + "-2: Non-string parameter should preserve default value", WsLevel.AUDIT, level);

        level = LoggingConfigUtils.getLogLevel("INFO", Level.OFF);
        assertEquals(m + "-3: Known string should supplant the default value", Level.INFO, level);
        level = LoggingConfigUtils.getLogLevel("AUDIT", Level.OFF);
        assertEquals(m + "-4: Known string should supplant the default value", WsLevel.AUDIT, level);
        level = LoggingConfigUtils.getLogLevel("WARNING", Level.OFF);
        assertEquals(m + "-5: Known string should supplant the default value", Level.WARNING, level);
        level = LoggingConfigUtils.getLogLevel("ERROR", Level.OFF);
        assertEquals(m + "-6: Known string should supplant the default value", WsLevel.ERROR, level);

        level = LoggingConfigUtils.getLogLevel("whoknows?", WsLevel.AUDIT);
        assertEquals(m + "-7: Bad string should revert to the default value", WsLevel.AUDIT, level);
    }

    @Test
    public void testGetLogFormat() {
        final String m = "testGetLogFormat";

        TraceFormat format = LoggingConfigUtils.getFormatValue(null, TraceFormat.ENHANCED);
        assertEquals(m + "-1: Null parameter should preserve default value", TraceFormat.ENHANCED, format);
        format = LoggingConfigUtils.getFormatValue(12, TraceFormat.ENHANCED);
        assertEquals(m + "-2: Non-string parameter should preserve default value", TraceFormat.ENHANCED, format);
        format = LoggingConfigUtils.getFormatValue("MadeUp", TraceFormat.ENHANCED);
        assertEquals(m + "-3: Unknown format should preserve default value", TraceFormat.ENHANCED, format);

        format = LoggingConfigUtils.getFormatValue("baSic", TraceFormat.ENHANCED);
        assertEquals(m + "-4: Known string should supplant the default value", TraceFormat.BASIC, format);
        format = LoggingConfigUtils.getFormatValue("ENHANCED", TraceFormat.BASIC);
        assertEquals(m + "-5: Known string should supplant the default value", TraceFormat.ENHANCED, format);
        format = LoggingConfigUtils.getFormatValue("Advanced", TraceFormat.ENHANCED);
        assertEquals(m + "-6: Known string should supplant the default value", TraceFormat.ADVANCED, format);
    }

    @Test
    public void testGetDelegate() {
        Object d = LoggingConfigUtils.getDelegate(FFDCFilterService.class, null, DisabledFFDCService.class.getName());
        assertNotNull("Default delegate should be created " + d, d);
        assertTrue("Delegate should be of the expected default type", d instanceof DisabledFFDCService);

        d = LoggingConfigUtils.getDelegate(FFDCFilterService.class, BaseFFDCService.class.getName(), DisabledFFDCService.class.getName());
        assertNotNull("Specific delegate should be created when the class is specified and reachable " + d, d);
        assertTrue("Delegate should be of the expected default type", d instanceof BaseFFDCService);
        assertTrue("Delegate should be of the expected default type", d instanceof FFDCFilterService);

        d = LoggingConfigUtils.getDelegate(TrService.class, null, DisabledFFDCService.class.getName());
        assertNull("Default delegate should not be created when there is a class mis-match", d);
    }

    @Test
    public void testGetLogHeader() {
        Map<String, String> config = new HashMap<String, String>();
        String logHeader;

        logHeader = LogProviderConfigImpl.getLogHeader(config);
        assertTrue("unexpected default header:" + System.getProperty("line.separator") + logHeader,
                   !logHeader.contains("null") &&
                                   !logHeader.contains("product = ") &&
                                   !logHeader.contains("wlp.install.dir = ") &&
                                   !logHeader.contains("server.config.dir = ") &&
                                   !logHeader.contains("server.output.dir = ") &&
                                   logHeader.contains("java.home = ") &&
                                   logHeader.contains("java.runtime = ") &&
                                   logHeader.contains("os = ") &&
                                   logHeader.contains("process = "));

        config.put("wlp.install.dir", "/wlp/install/dir/");
        logHeader = LogProviderConfigImpl.getLogHeader(config);
        assertTrue("unexpected wlp.install.dir:" + System.getProperty("line.separator") + logHeader,
                   !logHeader.contains("null") && logHeader.contains("wlp.install.dir = "));

        config.put("server.config.dir", "/wlp/server/config/dir/");
        logHeader = LogProviderConfigImpl.getLogHeader(config);
        assertTrue("unexpected server.config.dir:" + System.getProperty("line.separator") + logHeader,
                   !logHeader.contains("null") && logHeader.contains("server.config.dir = "));

        config.put("wlp.user.dir.isDefault", "true");
        logHeader = LogProviderConfigImpl.getLogHeader(config);
        assertTrue("unexpected server.config.dir:" + System.getProperty("line.separator") + logHeader,
                   !logHeader.contains("null") && !logHeader.contains("server.config.dir = "));

        config.put("server.output.dir", "/wlp/server/output/dir/");
        logHeader = LogProviderConfigImpl.getLogHeader(config);
        assertTrue("unexpected server.output.dir:" + System.getProperty("line.separator") + logHeader,
                   !logHeader.contains("null") && logHeader.contains("server.output.dir = "));

        config.put("server.output.dir", config.get("server.config.dir"));
        logHeader = LogProviderConfigImpl.getLogHeader(config);
        assertTrue("unexpected server.output.dir:" + System.getProperty("line.separator") + logHeader,
                   !logHeader.contains("null") && !logHeader.contains("server.output.dir = "));
    }
}
