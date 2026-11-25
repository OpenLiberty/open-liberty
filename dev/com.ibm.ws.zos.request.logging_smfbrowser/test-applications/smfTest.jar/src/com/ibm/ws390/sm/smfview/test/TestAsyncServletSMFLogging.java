/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.sm.smfview.test;

import com.ibm.ws390.sm.smfview.DefaultFilter;
import com.ibm.ws390.sm.smfview.SMFFilter;
import com.ibm.ws390.sm.smfview.SmfPrintStream;
import com.ibm.ws390.sm.smfview.SmfRecord;
import com.ibm.ws390.smf.formatters.LibertyRequestRecord;
import com.ibm.ws390.smf.formatters.LibertyServerInfoSection;

public class TestAsyncServletSMFLogging implements SMFFilter {
    /** Output stream. */
    private SmfPrintStream smf_printstream;

    /** Validation success indicator. */
    private boolean looksGood;

    /** Minimum number of bytes that the response can contain. */
    private final int minResponseBytes;

    /** String that we print in the success message. */
    private final String successString;

    public TestAsyncServletSMFLogging() {
        minResponseBytes = 7;
        successString = "TestAsyncServletSMFLogging";
    }

    protected TestAsyncServletSMFLogging(int minResponseBytes, String successString) {
        this.minResponseBytes = minResponseBytes;
        this.successString = successString;
    }

    @Override
    public boolean initialize(String parms) {
        boolean return_value = true;
        smf_printstream = DefaultFilter.commonInitialize(parms);
        if (smf_printstream == null)
            return_value = false;
        return return_value;
    }

    @Override
    public boolean preParse(SmfRecord record) {
        boolean ok_to_process = false;
        if (record.type() == 120 && record.subtype() == 11)
            ok_to_process = true;
        return ok_to_process;
    }

    @Override
    public SmfRecord parse(SmfRecord record) {
        return DefaultFilter.commonParse(record);
    }

    @Override
    public void processRecord(SmfRecord record) {
        LibertyRequestRecord rec = (LibertyRequestRecord) record;
        SMFType120SubType11BasicValidation basicValidation = new SMFType120SubType11BasicValidation(rec, smf_printstream, minResponseBytes);

        // Basic validation.
        if (basicValidation.validateRecord() != 0) {
            smf_printstream.println("ERROR Failed basic SMF validation.");
            return;
        }

        SMFType120SubType11WlmValidation wlmValidation = new SMFType120SubType11WlmValidation(rec, smf_printstream);
        if (!wlmValidation.validateRecord()) {
            smf_printstream.println("ERROR Failed WLM SMF validation.");
            return;
        }

        // Custom validation.
        LibertyServerInfoSection serverInfo = rec.m_libertyServerInfoSection;
        String configDir = serverInfo.m_serverConfigDir;
        String expectedServerName = System.getProperty("liberty.server.name", "com.ibm.ws.zos.wlm.context.servlet.async.zfat");
        String expectedConfigDir = "usr/servers/" + expectedServerName;
        if (configDir == null || configDir.length() == 0 || !configDir.contains(expectedConfigDir)) {
            smf_printstream.println("ERROR Server Info Section. The server config directory should have been" + expectedConfigDir + ", but it was: " + configDir);
            return;
        }

        looksGood = true;
    }

    @Override
    public void processingComplete() {
        if (looksGood) {
            smf_printstream.println(successString + " looks good.");
        }
    }
}
