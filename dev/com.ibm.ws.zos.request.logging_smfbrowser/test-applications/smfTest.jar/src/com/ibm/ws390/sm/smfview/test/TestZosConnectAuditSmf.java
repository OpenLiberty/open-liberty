/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import com.ibm.ws390.smf.formatters.SMFType120SubType11UserDataType102;

/**
 * z/OS Connect Audit interceptor SMF data validator.
 */
public class TestZosConnectAuditSmf implements SMFFilter {
    /** Output stream. */
    private SmfPrintStream smf_printstream;

    /** Validation success indicator. */
    private boolean looksGood;

    /** {@inheritDoc} */
    @Override
    public boolean initialize(String parms) {
        boolean return_value = true;
        smf_printstream = DefaultFilter.commonInitialize(parms);
        if (smf_printstream == null)
            return_value = false;
        return return_value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean preParse(SmfRecord record) {
        boolean ok_to_process = false;
        if (record.type() == 120 && record.subtype() == 11)
            ok_to_process = true;
        return ok_to_process;
    }

    /** {@inheritDoc} */
    @Override
    public SmfRecord parse(SmfRecord record) {
        return DefaultFilter.commonParse(record);
    }

    /** {@inheritDoc} */
    @Override
    public void processRecord(SmfRecord record) {
        LibertyRequestRecord rec = (LibertyRequestRecord) record;

        // Version 1, contains the auditInterceptor original data. Logged when running with features:
        // a. zosConnect-1.* (ASYNC) + requestLogging-1.*
        // b. zosConnect-1.* (ASYNC/SYNC)
        if (rec.m_subtypeVersion == 1) {
            // Log the fact that a version 1 record was logged.
            smf_printstream.println("TestZosConnectAuditSmf. Found version 1 record.");

            // Validate the server information section.
            LibertyServerInfoSection serverInfo = rec.m_libertyServerInfoSection;
            if (!validateServerInfo(serverInfo)) {
                return;
            }

            // Validate basic entries in the user data information section.
            Triplet userDataTriplet = rec.m_userDataTriplet;
            int userDataCount = userDataTriplet.count();
            if (userDataCount > 0) {
                UserDataSection[] uds = rec.m_userDataSection;
                for (int i = 0; i < userDataCount; i++) {
                    if (!validateUserDataSection(uds[i])) {
                        return;
                    }
                }
            }
        }

        // Version 2, contains the requestLogging gathered data. Logged when running with features:
        // a. zosConnect-1.* (SYNC) + requestLogging-1.*
        if (rec.m_subtypeVersion == 2 || rec.m_subtypeVersion == 3) {
            // Log the fact that a version 1 record was logged.
            smf_printstream.println("TestZosConnectAuditSmf. Found version 2 record.");

            // Basic validation.
            SMFType120SubType11BasicValidation basicValidation = new SMFType120SubType11BasicValidation(rec, smf_printstream);
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
            String expectedConfigDir = "usr/servers/com.ibm.ws.zos.connect";
            if (configDir == null || configDir.length() == 0 || !configDir.contains(expectedConfigDir)) {
                smf_printstream.println("ERROR Server Info Section. The server config directory should have been" + expectedConfigDir + ", but it was: " + configDir);
                return;
            }
        }

        looksGood = true;
    }

    /** {@inheritDoc} */
    @Override
    public void processingComplete() {
        if (looksGood) {
            smf_printstream.println("TestZosConnectAuditSmf looks good.");
        }
    }

    /**
     * Validates the server information section of a z/OS Connect written record.
     *
     * @param uds The server information section object.
     *
     * @return True if the validation completed successfully. False, otherwise.
     */
    private boolean validateServerInfo(LibertyServerInfoSection serverInfo) {
        try {
            int version = serverInfo.m_version;
            if (version != 1) {
                smf_printstream.println("ERROR Server Info Section. The version should be 1. It was: " + version);
                return false;
            }

            String sysName = serverInfo.m_systemName;
            if (sysName == null || sysName.length() == 0) {
                smf_printstream.println("ERROR Server Info Section. The system name should have a meanigful string. It was: " + sysName);
                return false;
            }

            String sysplexName = serverInfo.m_sysplexName;
            if (sysplexName == null || sysplexName.length() == 0) {
                smf_printstream.println("ERROR Server Info Section. The sysplex name should have a meanigful string. It was: " + sysplexName);
                return false;
            }

            String jobId = serverInfo.m_jobId;
            if (jobId == null || jobId.length() == 0 || !jobId.startsWith("STC")) {
                smf_printstream.println("ERROR Server Info Section. The job id should have a meanigful STC* string. It was: " + jobId);
                return false;
            }

            String jobName = serverInfo.m_jobName;
            if (jobName == null || jobName.length() == 0) {
                smf_printstream.println("ERROR Server Info Section. The job name should have a meanigful string. It was: " + jobName);
                return false;
            }

            byte[] stoken = serverInfo.m_server_stoken;
            SmfStream stream = null;
            if (stoken == null || stoken.length == 0 || (stream = new SmfStream(stoken)).getLong() == 0) {
                smf_printstream.println("ERROR Server Info Section. The stoken should have a meanigful byte array. It was: " + stoken);
            }
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable t) {
            smf_printstream.println("ERROR Server Info Section. Exception caught during data validation: " + t.getCause());
        }

        return true;
    }

    /**
     * Validates the user data section of a z/OS Connect written record.
     *
     * @param uds The user data section object.
     *
     * @return True if the validation completed successfully. False, otherwise.
     */
    private boolean validateUserDataSection(UserDataSection uds) {
        try {
            int dataType = uds.m_dataType;
            if (uds.m_dataType != 102) {
                smf_printstream.println("ERROR UserData Section. The data type should have been 102. It was: " + dataType);
                return false;
            }

            SMFType120SubType11UserDataType102 userDataType102 = new SMFType120SubType11UserDataType102(uds);
            String userid = userDataType102.m_userid;
            if (!userid.equals("testuser")) {
                smf_printstream.println("ERROR UserData Section. The user id should have been testuser. It was: " + userid);
                return false;
            }

            String mappedUserid = userDataType102.m_mappedUserid;
            if (!mappedUserid.equals("MYUSR1")) {
                smf_printstream.println("ERROR UserData Section. The mapped user id should have been MYUSER. It was: " + mappedUserid);
                return false;
            }

            int dataVersion = userDataType102.m_dataVersion;
            if (dataVersion != 3) {
                smf_printstream.println("ERROR UserData Section. The data version should have been 3. It was: " + dataVersion);
                return false;
            }

            byte[] arrivalTime = userDataType102.m_arrivalTime;
            if (arrivalTime == null || arrivalTime.length == 0) {
                smf_printstream.println("ERROR UserData Section. The arrival time should have been a meaningful byte array. It was: " + arrivalTime);
                return false;
            }

            byte[] completionTime = userDataType102.m_completionTime;
            if (completionTime == null || completionTime.length == 0) {
                smf_printstream.println("ERROR UserData Section. The completion time should have been a meaningful byte array. It was: " + completionTime);
                return false;
            }

            String methodName = userDataType102.m_methodName;
            if (!methodName.equalsIgnoreCase("GET") && !methodName.equalsIgnoreCase("PUT") && !methodName.equalsIgnoreCase("POST") && !methodName.equalsIgnoreCase("DELETE")
                && !methodName.equalsIgnoreCase("OPTIONS") && !methodName.equalsIgnoreCase("HEAD")) {
                smf_printstream.println("ERROR UserData Section. The method name should have been GET/PUT/POST/DELETE/OPTIONS/HEAD. It was: " + methodName);
                return false;
            }

            int responseLength = userDataType102.m_responseLength;
            if (responseLength == 0) {
                smf_printstream.println("ERROR UserData Section. The response length should not have been zero: " + responseLength);
                return false;
            }
        } catch (Throwable t) {
            smf_printstream.println("ERROR UserData Section. Exception caught during data validation: " + t.getCause());
        }

        return true;
    }
}
