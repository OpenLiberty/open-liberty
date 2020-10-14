/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

public class TestZosRequestLoggingSmfMappedUserid implements SMFFilter {

    public TestZosRequestLoggingSmfMappedUserid() {
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
        int basicRc = basicValidation.validateRecord();

        if (rec.m_libertyRequestInfoSection.m_userid.equals("testuser")) {
            if (rec.m_libertyRequestInfoSection.m_mappedUserid.equals("RSTUSR1")) {
                if (basicRc == 0) {
                    if (rec.m_libertyRequestInfoSection.m_lengthRequestUri == 57) {
                        if (rec.m_libertyRequestInfoSection.m_requestUri.equals("/mappedCredentialsSync/mapLdapIdentityToMvsUseridSyncTest")) {
                            looksGood = 1;
                        } else {
                            smf_printstream.println("TestZosRequestLoggingSmfUnauth request URI is not /mappedCredentialsSync/mapLdapIdentityToMvsUseridSyncTest. Request URI "
                                                    + rec.m_libertyRequestInfoSection.m_requestUri);
                        }
                    } else {
                        smf_printstream.println("TestZosRequestLoggingSmfUnauth request URI length is not 57. Request URI length "
                                                + rec.m_libertyRequestInfoSection.m_lengthRequestUri);
                    }
                } else {
                    smf_printstream.println("TestZosRequestLoggingSmfMappedUserid basic validation failed. rc=" + basicRc);
                }
            } else {
                smf_printstream.println("TestZosRequestLoggingSmfMappedUserid mapped user id not RSTUSR1 . " + rec.m_libertyRequestInfoSection.m_mappedUserid + " end");
            }

        } else {
            smf_printstream.println("TestZosRequestLoggingSmfMappedUserid user id not testuser. " + rec.m_libertyRequestInfoSection.m_userid + " end");
        }

    }

    @Override
    public void processingComplete() {
        if (looksGood == 1)
            smf_printstream.println("TestZosRequestLoggingSmfMappedUserid looks good.");
    }

    private SmfPrintStream smf_printstream;
    private int looksGood;
}
