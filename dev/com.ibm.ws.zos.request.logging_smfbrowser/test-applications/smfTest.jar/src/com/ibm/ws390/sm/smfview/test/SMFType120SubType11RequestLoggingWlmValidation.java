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

import com.ibm.ws390.sm.smfview.ClassificationDataSection;
import com.ibm.ws390.sm.smfview.LibertyRequestInfoSection;
import com.ibm.ws390.sm.smfview.SmfPrintStream;
import com.ibm.ws390.sm.smfview.SmfStream;
import com.ibm.ws390.smf.formatters.LibertyRequestRecord;

public class SMFType120SubType11RequestLoggingWlmValidation {

    private final LibertyRequestRecord libertyRequestRecord;
    private final SmfPrintStream smf_printstream;

    public SMFType120SubType11RequestLoggingWlmValidation(LibertyRequestRecord libertyRequestRecord, SmfPrintStream smf_printstream) {
        this.libertyRequestRecord = libertyRequestRecord;
        this.smf_printstream = smf_printstream;
    }

    public int validateRecord() {
        int rc = 0;
        rc = validateLibertyRequestInfoSectionWlmStuff(libertyRequestRecord.m_libertyRequestInfoSection);
        if (rc == 0) {
            int classificationdatcount = libertyRequestRecord.m_classificationDataTriplet.count();
            int expectedCount = 3;
            if (classificationdatcount != expectedCount) {
                smf_printstream.println("ERROR Classification data triplet count is not 3. count=" + classificationdatcount);
                rc = 1;
            } else {
                boolean typeUriFound = false;
                boolean typeHostnameFound = false;
                boolean typePortFound = false;
                for (int i = 0; i < expectedCount; i++) {
                    ClassificationDataSection cds = libertyRequestRecord.m_classificationDataSection[i];
                    if (cds.m_version == 1) {
                        if (cds.m_dataType == ClassificationDataSection.TypeURI) {
                            typeUriFound = true;
                            String uri = "/RequestLogging/RequestLogging_testUserData_add_string";
                            if (cds.m_dataLength == uri.length()) {
                                if (!(cds.m_theData.equals(uri))) {
                                    smf_printstream.println("ERROR Classification Data Section URI is incorrect. Expected " + uri + " but it was " + cds.m_theData);
                                    rc = 2;
                                }
                            } else {
                                smf_printstream.println("ERROR Classification Data Section URI length is incorrect. Expected " + uri.length() + " but URI length="
                                                        + cds.m_dataLength);
                                rc = 3;
                            }
                        }
                        if (cds.m_dataType == ClassificationDataSection.TypeHostname) {
                            typeHostnameFound = true;
                            String hostName = "localhost";
                            if (cds.m_dataLength == hostName.length()) {
                                if (!(cds.m_theData.equals(hostName))) {
                                    smf_printstream.println("ERROR Classification Data Section hostname is incorrect. Expected " + hostName + " but it was " + cds.m_theData);
                                    rc = 4;
                                }
                            } else {
                                smf_printstream.println("ERROR Classification Data Section host name is incorrect. Expected " + hostName.length() + " but host name length="
                                                        + cds.m_dataLength);
                                rc = 5;
                            }
                        }
                        if (cds.m_dataType == ClassificationDataSection.TypePort) {
                            typePortFound = true;
                            if (cds.m_dataLength != 0) {
                                if (cds.m_theData == null) {
                                    smf_printstream.println("ERROR Classification Data Section port is null. port length=" + cds.m_dataLength);
                                    rc = 6;
                                }
                            } else {
                                smf_printstream.println("ERROR Classification Data Section port length is 0.");
                                rc = 7;
                            }
                        }
                    } else {
                        smf_printstream.println("ERROR Classification Data Section version is not 1. version=" + cds.m_version);
                        rc = 8;
                    }
                }
                if (typeUriFound == false) {
                    smf_printstream.println("ERROR Classification Data Section URI type not found.");
                    rc = 9;
                }
                if (typeHostnameFound == false) {
                    smf_printstream.println("ERROR Classification Data Section hostname type not found.");
                    rc = 10;
                }
                if (typePortFound == false) {
                    smf_printstream.println("ERROR Classification Data Section port type not found.");
                    rc = 11;
                }
            }
        }
        return rc;
    }

    private int validateLibertyRequestInfoSectionWlmStuff(LibertyRequestInfoSection libertyRequestInfoSection) {
        int rc = 0;
        if (libertyRequestInfoSection.m_wlmTransactionClass == null) {
            smf_printstream.println("ERROR Request Info Section transaction class is null.");
            rc = 21;
        } else {
            if (!(libertyRequestInfoSection.m_wlmTransactionClass.equals("CLASS001"))) {
                smf_printstream.println("ERROR Request Info Section transaction class is not CLASS001. transaction class=" + libertyRequestInfoSection.m_wlmTransactionClass);
                rc = 22;
            }
        }
        if (libertyRequestInfoSection.m_enclaveDeleteCPU == 0) {
            smf_printstream.println("ERROR Request Info Section enclave delete CPU is 0.");
            rc = 23;
        }
        if (libertyRequestInfoSection.m_enclaveDeleteCpuService == 0) {
            smf_printstream.println("ERROR Request Info Section enclave delete CPU service is 0.");
            rc = 24;
        }
        if (libertyRequestInfoSection.m_enclaveDeletezAAPNorm == 0) {
            smf_printstream.println("ERROR Request Info Section enclave delete zAAP norm is 0.");
            rc = 25;
        }
        if (libertyRequestInfoSection.m_enclaveToken == null) {
            smf_printstream.println("ERROR Request Info Section enclave token is null.");
            rc = 26;
        } else {
            if (libertyRequestInfoSection.m_enclaveToken.length == 0) {
                smf_printstream.println("ERROR Request Info Section enclave token length is 0.");
                rc = 27;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_enclaveToken);
                if (aStream.getLong() == 0) {
                    smf_printstream.println("ERROR Request Info Section enclave token is 0.");
                    rc = 28;
                }
            }
        }
        return rc;
    }

}
