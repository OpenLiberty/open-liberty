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
package com.ibm.ws.logging.hpel.config;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.Ignore;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.logging.internal.hpel.CommonUtils;
import com.ibm.ws.logging.internal.hpel.HpelConstants;
import com.ibm.ws.logging.internal.hpel.HpelTraceServiceConfig;

/**
 *
 */
 @Ignore
public class HpelConfiguratorTest {

    /**
     * Verifies that calling HpelConfigurator 'update*' methods correctly updates
     * HpelTraceServiceConfig instance.
     * 
     * @throws IOException
     */
    @Test
    public void testUpdateLogTraceText() throws IOException {
        Map<String, String> bootConfig = new HashMap<String, String>();
        bootConfig.put(HpelConstants.INTERNAL_SERVER_NAME, CommonUtils.SERVER_NAME);
        bootConfig.put(HpelConstants.BOOTPROP_PRODUCT_INFO, CommonUtils.PRODUCT_INFO);

        HpelTraceServiceConfig config = new HpelTraceServiceConfig(bootConfig, CommonUtils.LOG_DIR, CommonUtils.TEXT_FACTORY);

//        File tmpLogDir = File.createTempFile("log", "");
//        File tmpTraceDir = File.createTempFile("trace", "");
//        File tmpTextDir = File.createTempFile("text", "");

        // Need to call TrConfigurator.init() to initialize delegate.
        TrConfigurator.init(config);
        HpelConfigurator.init(config);

        // Update log attributes
        Map<String, Object> newConfig = new HashMap<String, Object>();
        newConfig.put("purgeMaxSize", Integer.valueOf(37));
        newConfig.put("purgeMinTime", Long.valueOf(13));
        newConfig.put("outOfSpaceAction", "StopServer");
        newConfig.put("bufferingEnabled", Boolean.FALSE);
        newConfig.put("fileSwitchTime", Integer.valueOf(7));
        HpelConfigurator.updateLog(newConfig);

        // Calculate the expected read state of the config.
        StringBuilder expected = new StringBuilder("HpelTraceServiceConfig[log.dataDirectory=");
        expected.append(CommonUtils.LOG_DIR.getAbsolutePath());
        expected.append(",log.purgeMaxSize=37,log.purgeMinTime=13,log.outOfSpaceAction=StopServer,");
        expected.append("log.bufferingEnabled=false,log.fileSwitchTime=7,trace.dataDirectory=");
        expected.append(CommonUtils.LOG_DIR.getAbsolutePath());
        expected.append(",trace.purgeMaxSize=-1,trace.purgeMinTime=-1,trace.outOfSpaceAction=PurgeOld,");
        expected.append("trace.bufferingEnabled=true,trace.fileSwitchTime=-1,trace.memoryBufferSize=-1]");
        assertEquals("Log runtime values were updated incorrectly", expected.toString(), config.toString());

        // Update trace attributes
        newConfig.clear();
        newConfig.put("purgeMaxSize", Integer.valueOf(23));
        newConfig.put("purgeMinTime", Long.valueOf(17));
        newConfig.put("outOfSpaceAction", "StopLogging");
        newConfig.put("bufferingEnabled", Boolean.TRUE);
        newConfig.put("fileSwitchTime", Integer.valueOf(3));
        newConfig.put("memoryBufferSize", Integer.valueOf(87));
        HpelConfigurator.updateTrace(newConfig);

        // Calculate the expected read state of the config.
        expected = new StringBuilder("HpelTraceServiceConfig[log.dataDirectory=");
        expected.append(CommonUtils.LOG_DIR.getAbsolutePath());
        expected.append(",log.purgeMaxSize=37,log.purgeMinTime=13,log.outOfSpaceAction=StopServer,");
        expected.append("log.bufferingEnabled=false,log.fileSwitchTime=7,trace.dataDirectory=");
        expected.append(CommonUtils.LOG_DIR.getAbsolutePath());
        expected.append(",trace.purgeMaxSize=23,trace.purgeMinTime=17,trace.outOfSpaceAction=StopLogging,");
        expected.append("trace.bufferingEnabled=true,trace.fileSwitchTime=3,trace.memoryBufferSize=87]");
//        expected.append(",text.disabled]");
        assertEquals("Trace runtime values were updated incorrectly", expected.toString(), config.toString());

//        // Update attributes on logging adding 'textLog' subelement
//        newConfig.clear();
//        newConfig.put("textLog", new String[] { "textLog_reference" });
//        newConfig.put("suppressSensitiveTrace", false);
//        TrConfigurator.update(newConfig);

//        // Update text copy attributes
//        newConfig.clear();
//        newConfig.put("dataDirectory", tmpTextDir.getAbsolutePath());
//        newConfig.put("purgeMaxSize", Integer.valueOf(41));
//        newConfig.put("purgeMinTime", Integer.valueOf(43));
//        newConfig.put("outOfSpaceAction", "StopServer");
//        newConfig.put("bufferingEnabled", Boolean.FALSE);
//        newConfig.put("fileSwitchTime", Integer.valueOf(11));
//        newConfig.put("outputFormat", "Advanced");
//        newConfig.put("includeTrace", Boolean.TRUE);
//        HpelConfigurator.updateText(newConfig);
//
//        // Calculate the expected read state of the config.
//        expected = new StringBuilder("HpelTraceServiceConfig[log.dataDirectory=");
//        expected.append(tmpLogDir.getAbsolutePath());
//        expected.append(",log.purgeMaxSize=37,log.purgeMinTime=13,log.outOfSpaceAction=StopServer,");
//        expected.append("log.bufferingEnabled=false,log.fileSwitchTime=7,trace.dataDirectory=");
//        expected.append(tmpTraceDir.getAbsolutePath());
//        expected.append(",trace.purgeMaxSize=23,trace.purgeMinTime=17,trace.outOfSpaceAction=StopLogging,");
//        expected.append("trace.bufferingEnabled=true,trace.fileSwitchTime=3,trace.memoryBufferSize=87,");
//        expected.append("text.dataDirectory=").append(tmpTextDir.getAbsolutePath());
//        expected.append(",text.purgeMaxSize=41,text.purgeMinTime=43,text.outOfSpaceAction=StopServer,");
//        expected.append("text.bufferingEnabled=false,text.fileSwitchTime=11,text.outputFormat=Advanced,text.includeTrace=true]");
//        assertEquals("Text runtime values were updated incorrectly", expected.toString(), config.toString());
    }

}
