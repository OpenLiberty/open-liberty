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
import com.ibm.ws390.sm.smfview.SmfStream;
import com.ibm.ws390.sm.smfview.Triplet;
import com.ibm.ws390.sm.smfview.UserDataSection;
import com.ibm.ws390.smf.formatters.LibertyRequestRecord;
import com.ibm.ws390.smf.formatters.LibertyServerInfoSection;

public class TestZosRequestLoggingSmfUserDataBytes5DifferentIdenfifiers implements SMFFilter {

    public TestZosRequestLoggingSmfUserDataBytes5DifferentIdenfifiers() {
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

        SMFType120SubType11BasicValidation basicValidation = new SMFType120SubType11BasicValidation(rec, smf_printstream);
        int rc = basicValidation.validateRecord();
        Triplet userDataTriplet = rec.m_userDataTriplet;
        int userDataCount = userDataTriplet.count();
        UserDataSection UDS[] = null;
        if (userDataCount == 5) {
            UDS = rec.m_userDataSection;
            int looksGoodcount = 0;
            for (int i = 0; i <= userDataCount - 1; i++) {
                UserDataSection uds = UDS[i];
                if (uds.m_dataType == (i + 11)) {
                    if (uds.m_dataLength == 4) {
                        SmfStream data = new SmfStream(uds.m_data);
                        int firstInt = data.getInteger(4);
                        if (firstInt == (i + 11)) {
                            looksGoodcount = looksGoodcount + 1;
                        } else {
                            smf_printstream.println("TestZosRequestLoggingSmfUserDataBytes5DifferentIdenfifiers userdata is wrong. expected int to be " + (i + 11) + " but int="
                                                    + firstInt);
                        }
                    } else {
                        smf_printstream.println("TestZosRequestLoggingSmfUserDataBytes5DifferentIdenfifiers userdata length is wrong. expected length=4 But length="
                                                + uds.m_dataLength);
                    }
                } else {
                    smf_printstream.println("TestZosRequestLoggingSmfUserDataBytes5DifferentIdenfifiers userdata type is not " + (i + 11) + " User data type=" + uds.m_dataType);
                }
            }
            if (looksGoodcount == 5) {
                if (rc == 0) {
                    looksGood = 1;
                }
            }
        } else {
            smf_printstream.println("TestZosRequestLoggingSmfUserDataBytes5DifferentIdenfifiers userdata count is not 5. User data count=" + userDataCount);
        }
    }

    @Override
    public void processingComplete() {
        if (looksGood == 1)
            smf_printstream.println("TestZosRequestLoggingSmfUserDataBytes5DifferentIdenfifiers looks good.");
    }

    private SmfPrintStream smf_printstream;
    private int looksGood;
}
