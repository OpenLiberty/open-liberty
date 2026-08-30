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
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.bind.DatatypeConverter;

import com.ibm.ws390.sm.smfview.DefaultFilter;
import com.ibm.ws390.sm.smfview.LibertyRequestInfoSection;
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
public class TestZosRequestLoggingSmfUserDataStringFilterAndForward implements SMFFilter {

    public static String c = "TestZosRequestLoggingSmfUserDataStringFilterAndForward";
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
            String mainServletFilterData = "MAIN SERVLET FILTER USER DATA";
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
            String mainServletData = "MAIN SERVLET USER DATA";
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

            // Validate that the total request time is equal or greater than 5000 milliseconds.
            // That is because the associated servlet filter will sleep for 5 seconds.
            // If, for some reason, the test takes 5 or more seconds to complete, then this
            // test is either no-op (ran on a super fast machine) or it is working just fine.
            // If the test takes less than 5 seconds to complete, then we know that the filter
            // execution is not being taken into account or the processing time is somehow being
            // overwritten.
            // Note that without any injected delays, requests to the associated servlet only take
            // milliseconds to complete.
            boolean requestTimeValid = false;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            LibertyRequestInfoSection lris = rec.m_libertyRequestInfoSection;

            byte[] startStck = lris.m_startStck;
            Calendar requestStartDate = stckToDate(startStck);
            long startTimeMillis = requestStartDate.getTimeInMillis();

            byte[] endStck = lris.m_endStck;
            Calendar requestEndDate = stckToDate(endStck);
            long endTimeMillis = requestEndDate.getTimeInMillis();

            long requestExecTimeMillis = endTimeMillis - startTimeMillis;
            if (requestExecTimeMillis >= 5000) {
                requestTimeValid = true;
            } else {
                smf_printstream.println(c + ". ERROR. The total request took less than 5 seconds. It should have taken more than 5 seconds. Request Execution Time: "
                                        + requestExecTimeMillis + " milliseconds. Request Start Date: " + sdf.format(requestStartDate.getTime()) + ". Request end Date: "
                                        + sdf.format(requestEndDate.getTime()));
            }

            // Validate that the URI is the one from the original servlet request and not the one from the downstream servlet request.
            boolean uriValid = false;
            String requestUri = lris.m_requestUri;
            String expectedReqUri = "/MultiStepServletWithFilter/addUserDataAndForward";
            if (expectedReqUri.equals(requestUri)) {
                uriValid = true;
            } else {
                smf_printstream.println(c + ". ERROR. The recorded URI is unexpected. Expected (original) URI: " + expectedReqUri + ". Found URI: " + requestUri + ".");
            }

            // Put it all together.
            if (filterDataValid && mainDataValid && downStrmDataValid && requestTimeValid && uriValid) {
                outcomeValid = true;
                smf_printstream.println(c + ". INFO:\nRequest Execution Time: " + requestExecTimeMillis + " milliseconds. Request Start Date: "
                                        + sdf.format(requestStartDate.getTime()) + ". Request end Date: " + sdf.format(requestEndDate.getTime()));
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

    /**
     * Convert a native STCK to a Calendar Object.
     *
     * @param stck The STCK byte array.
     *
     * @return A Calendar object representing the given STCK.
     */
    private Calendar stckToDate(byte[] stck) {
        // Convert bytes to hex string.
        String hexString = DatatypeConverter.printHexBinary(stck);

        // Take the 3 last chars out. The time is in microseconds.
        BigInteger time = new BigInteger(hexString.substring(0, hexString.length() - 3), 16);

        // Wrap a STCK that starts on 1/1/1970 (7D91 048B CA00 0000). Take the last 3 chars out.
        BigInteger startDate1970 = new BigInteger("7D91048BCA000", 16);

        // Calculate the time with 1/1/1970 as the starting point.
        BigInteger microSecsTime = time.subtract(startDate1970);

        // Reduce the time to milliseconds.
        long millisTime = microSecsTime.divide(new BigInteger("1000")).longValue();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millisTime);

        return calendar;
    }
}
