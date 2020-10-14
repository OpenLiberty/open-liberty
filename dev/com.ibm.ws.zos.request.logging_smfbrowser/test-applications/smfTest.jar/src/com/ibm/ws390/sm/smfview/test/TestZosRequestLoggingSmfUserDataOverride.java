/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

/**
 * SMF validation class associated with MultiStepServletWithFilter.war under
 * com.ibm.ws.zos.request.logging_zfat
 */
public class TestZosRequestLoggingSmfUserDataOverride implements SMFFilter {

    public static String c = "TestZosRequestLoggingSmfUserDataOverride";
    private SmfPrintStream smf_printstream;
    private boolean outcomeValid;

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
        smf_printstream.println("");
        LibertyRequestRecord rec = (LibertyRequestRecord) record;

        // Custom server info validation.
        LibertyServerInfoSection serverInfo = rec.m_libertyServerInfoSection;
        String configDir = serverInfo.m_serverConfigDir;
        String expectedConfigDir = "usr/servers/com.ibm.ws.zos.request.logging";
        if (configDir == null || configDir.length() == 0 || !configDir.contains(expectedConfigDir)) {
            smf_printstream.println(c + ". ERROR: The recorded config DIR is incorrect. Expected server config directory:" + expectedConfigDir + ". Recorded config dir: "
                                    + configDir);
        }

        // User data validation.
        SMFType120SubType11BasicValidation basicValidation = new SMFType120SubType11BasicValidation(rec, smf_printstream);
        int rc = basicValidation.validateRecord();
        Triplet userDataTriplet = rec.m_userDataTriplet;
        int userDataCount = userDataTriplet.count();
        UserDataSection UDS[] = null;
        if (userDataCount == 3) {
            boolean filterDataValid = false;
            UDS = rec.m_userDataSection;

            // Validate the data added by the filter.
            UserDataSection uds1 = UDS[2];
            String mainServletFilterData = "MAIN SERVLET USER DATA 65535 OVERRRIDE";
            if (uds1.m_dataType == 65535) {
                if (uds1.m_dataLength == mainServletFilterData.length()) {
                    SmfStream data = new SmfStream(uds1.m_data);
                    try {

                        String stringData = data.getString(uds1.m_dataLength, SmfUtil.EBCDIC);
                        if (stringData.equals(mainServletFilterData)) {
                            if (rc == 0) {
                                filterDataValid = true;
                            }
                        } else {
                            smf_printstream.println(c + ". ERROR: The recorded user data of: " + stringData + " does not match the expected data of: " + mainServletFilterData);
                        }
                    } catch (UnsupportedEncodingException e) {
                        smf_printstream.println(c + ". ERROR: Formatting error." + e.getStackTrace());
                    } finally {
                        try {
                            data.close();
                        } catch (Exception e) {
                            // This is a test. Taking a best effort approach.
                        }
                    }
                } else {
                    smf_printstream.println(c + ". ERROR: User Data Type " + uds1.m_dataType + ": The recorded userdata's length of " + uds1.m_dataLength
                                            + " does not match the expected length of: " + mainServletFilterData.length());
                }
            } else {
                smf_printstream.println(c + ". ERROR: The recorded user data with type " + uds1.m_dataType + " does not match the expected type of 65535.");
            }

            // Validate the user data added by the main servlet.
            boolean mainDataValid = false;
            UserDataSection uds2 = UDS[1];
            String mainServletData = "DOWNSTREAM SERVLET USER DATA 65536 OVERRIDE";
            if (uds2.m_dataType == 65536) {
                if (uds2.m_dataLength == mainServletData.length()) {
                    SmfStream data = new SmfStream(uds2.m_data);
                    try {

                        String stringData = data.getString(uds2.m_dataLength, SmfUtil.EBCDIC);
                        if (stringData.equals(mainServletData)) {
                            if (rc == 0) {
                                mainDataValid = true;
                            }
                        } else {
                            smf_printstream.println(c + ". ERROR. The recorded user data of: " + stringData + " does not match the expected data of: " + mainServletData);
                        }
                    } catch (UnsupportedEncodingException e) {
                        smf_printstream.println(c + ". ERROR. Formatting error." + e.getStackTrace());
                    } finally {
                        try {
                            data.close();
                        } catch (Exception e) {
                            // This is a test. Taking a best effort approach.
                        }
                    }
                } else {
                    smf_printstream.println(c + ". ERROR. User Data Type " + uds2.m_dataType + ": The recorded userdata's length of " + uds2.m_dataLength
                                            + " does not match the expected length of: " + mainServletData.length());
                }
            } else {
                smf_printstream.println(c + ". ERROR. The recorded user data with type " + uds2.m_dataType + " does not match the expected type of 65536.");
            }

            // Validate the user data added by the downstream (forwarded) servlet.
            boolean downStrmDataValid = false;
            UserDataSection uds3 = UDS[0];
            String downStreamServletData = "DOWNSTREAM SERVLET USER DATA";
            if (uds3.m_dataType == 65537) {
                if (uds3.m_dataLength == downStreamServletData.length()) {
                    SmfStream data = new SmfStream(uds3.m_data);
                    try {

                        String stringData = data.getString(uds3.m_dataLength, SmfUtil.EBCDIC);
                        if (stringData.equals(downStreamServletData)) {
                            if (rc == 0) {
                                downStrmDataValid = true;
                            }
                        } else {
                            smf_printstream.println(c + ". ERROR. The recorded user data of: " + stringData + " does not match the expected data of: " + downStreamServletData);
                        }
                    } catch (UnsupportedEncodingException e) {
                        smf_printstream.println(c + ". Formatting error." + e.getStackTrace());
                    } finally {
                        try {
                            data.close();
                        } catch (Exception e) {
                            // This is a test. Taking a best effort approach.
                        }
                    }
                } else {
                    smf_printstream.println(c + ". ERROR. User Data Type " + uds3.m_dataType + ": The recorded userdata's length of " + uds3.m_dataLength
                                            + " does not match the expected length of: " + downStreamServletData.length());
                }
            } else {
                smf_printstream.println(c + ". ERROR. The recorded user data with type " + uds3.m_dataType + " does not match the expected type of 65537.");
            }

            if (filterDataValid && mainDataValid && downStrmDataValid) {
                outcomeValid = true;
            }
        } else {
            smf_printstream.println(c + ". ERROR. The amount of user data entries recorded: " + userDataCount + ", does not match the expected value of 3.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processingComplete() {
        smf_printstream.println("");
        smf_printstream.println(c + ". OUTCOME.");
        if (outcomeValid) {
            smf_printstream.println(c + " looks good.");
        } else {
            smf_printstream.println(c + " FAILED.");
        }
    }
}
