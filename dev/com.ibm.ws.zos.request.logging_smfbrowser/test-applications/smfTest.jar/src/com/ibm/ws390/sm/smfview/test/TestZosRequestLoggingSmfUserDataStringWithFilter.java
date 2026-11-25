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

import java.io.UnsupportedEncodingException;

import com.ibm.ws390.sm.smfview.DefaultFilter;
import com.ibm.ws390.sm.smfview.SMFFilter;
import com.ibm.ws390.sm.smfview.SmfPrintStream;
import com.ibm.ws390.sm.smfview.SmfRecord;
import com.ibm.ws390.sm.smfview.SmfStream;
import com.ibm.ws390.sm.smfview.SmfUtil;
import com.ibm.ws390.sm.smfview.Triplet;
import com.ibm.ws390.sm.smfview.UserDataSection;
import com.ibm.ws390.smf.formatters.LibertyRequestRecord;
import com.ibm.ws390.smf.formatters.LibertyServerInfoSection;

public class TestZosRequestLoggingSmfUserDataStringWithFilter implements SMFFilter {

    public TestZosRequestLoggingSmfUserDataStringWithFilter() {
        smf_printstream = null;
        looksGood = 0;
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

        // Custom server info validation.
        LibertyServerInfoSection serverInfo = rec.m_libertyServerInfoSection;
        String configDir = serverInfo.m_serverConfigDir;
        String expectedConfigDir = "usr/servers/com.ibm.ws.zos.request.logging";
        if (configDir == null || configDir.length() == 0 || !configDir.contains(expectedConfigDir)) {
            smf_printstream.println("ERROR Server Info Section. The server config directory should have been" + expectedConfigDir + ", but it was: " + configDir);
            return;
        }

        int looksGood1 = 0;
        int looksGood2 = 0;
        SMFType120SubType11BasicValidation basicValidation = new SMFType120SubType11BasicValidation(rec, smf_printstream);
        int rc = basicValidation.validateRecord();

        Triplet userDataTriplet = rec.m_userDataTriplet;
        int userDataCount = userDataTriplet.count();
        UserDataSection UDS[] = null;
        if (userDataCount == 2) {
            UDS = rec.m_userDataSection;

            UserDataSection uds0 = UDS[0];
            String userDataBytes = "USER DATA 1 USER DATA 1";
            if (uds0.m_dataType == 1) {
                if (uds0.m_dataLength == userDataBytes.length()) {
                    SmfStream data = new SmfStream(uds0.m_data);
                    try {

                        String stringData = data.getString(uds0.m_dataLength, SmfUtil.EBCDIC);
                        if (stringData.equals(userDataBytes)) {
                            if (rc == 0) {
                                looksGood1 = 1;
                            }
                        } else {
                            smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter userdata is wrong. expected data=" + userDataBytes + "But data="
                                                    + stringData);
                        }
                    } catch (UnsupportedEncodingException e) {
                        smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter formatting error." + e.getStackTrace());
                    }
                } else {
                    smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter userdata length is wrong. expected length=" + userDataBytes.length()
                                            + "But length="
                                            + uds0.m_dataLength);
                }
            } else {
                smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter userdata type is not 1. User data type=" + uds0.m_dataType);
            }

            UserDataSection uds1 = UDS[1];
            String userDataBytes1 = "FILTER USERDATA";
            if (uds1.m_dataType == 98) {
                if (uds1.m_dataLength == userDataBytes1.length()) {
                    SmfStream data = new SmfStream(uds1.m_data);
                    try {

                        String stringData = data.getString(uds1.m_dataLength, SmfUtil.EBCDIC);
                        if (stringData.equals(userDataBytes1)) {
                            if (rc == 0) {
                                looksGood2 = 1;
                            }
                        } else {
                            smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter userdata is wrong. expected data=" + userDataBytes1 + "But data="
                                                    + stringData);
                        }
                    } catch (UnsupportedEncodingException e) {
                        smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter formatting error." + e.getStackTrace());
                    }
                } else {
                    smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter userdata length is wrong. expected length=" + userDataBytes1.length() + "But length="
                                            + uds1.m_dataLength);
                }
            } else {
                smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter userdata type is not 98. User data type=" + uds1.m_dataType);
            }

            if ((looksGood1 == 1) && (looksGood2 == 1)) {
                looksGood = 1;
            }
        } else {
            smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter userdata count is not 2. User data count=" + userDataCount);
        }
    }

    @Override
    public void processingComplete() {
        if (looksGood == 1)
            smf_printstream.println("TestZosRequestLoggingSmfUserDataStringWithFilter looks good.");
    }

    private SmfPrintStream smf_printstream;
    private int looksGood;
}
