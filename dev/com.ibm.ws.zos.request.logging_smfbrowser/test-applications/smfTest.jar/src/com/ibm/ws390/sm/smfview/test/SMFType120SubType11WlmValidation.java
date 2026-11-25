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

/**
 * Validates WLM related data of a SMF 120 subtype 11 record.
 */
public class SMFType120SubType11WlmValidation {

    /** Liberty request record object. */
    private final LibertyRequestRecord libertyRequestRecord;

    /** Print stream. */
    private final SmfPrintStream smf_printstream;

    /**
     * Cosntructor.
     *
     * @param libertyRequestRecord Liberty request record instance.
     * @param smf_printstream      The print stream.
     */
    public SMFType120SubType11WlmValidation(LibertyRequestRecord libertyRequestRecord, SmfPrintStream smf_printstream) {
        this.libertyRequestRecord = libertyRequestRecord;
        this.smf_printstream = smf_printstream;
    }

    /**
     * Validates WLM related data
     *
     * @return True if the validation was successful. False, otherwise.
     */
    public boolean validateRecord() {
        if (!validateLibertyRequestInfoSectionWlmData(libertyRequestRecord.m_libertyRequestInfoSection)) {
            return false;
        }

        if (!validateClassificationData()) {
            return false;
        }

        return true;

    }

    /**
     * Validates WLM related data in the record's request info section.
     *
     * @param libertyRequestInfoSection The record's request info section.
     * @return True if the validation was successful. False, otherwise.
     */
    private boolean validateLibertyRequestInfoSectionWlmData(LibertyRequestInfoSection libertyRequestInfoSection) {
        try {
            String tranClass = libertyRequestInfoSection.m_wlmTransactionClass;
            if (tranClass == null || tranClass.length() == 0) {
                smf_printstream.println("ERROR Request Info Section (WLM). The transaction class is null or empty.");
                return false;
            }
            if (libertyRequestInfoSection.m_enclaveDeleteCPU == 0) {
                smf_printstream.println("ERROR Request Info Section (WLM). The enclave delete CPU is 0.");
                return false;
            }
            if (libertyRequestInfoSection.m_enclaveDeleteCpuService == 0) {
                smf_printstream.println("ERROR Request Info Section (WLM). The enclave delete CPU service is 0.");
                return false;
            }
            if (libertyRequestInfoSection.m_enclaveDeletezAAPNorm == 0) {
                smf_printstream.println("ERROR Request Info Section (WLM). The enclave delete zAAP norm is 0.");
                return false;
            }

            byte[] enclaveToken = libertyRequestInfoSection.m_enclaveToken;
            if (enclaveToken == null || enclaveToken.length == 0) {
                smf_printstream.println("ERROR Request Info Section (WLM). The enclave token is null.");
                return false;
            }

            SmfStream aStream = new SmfStream(enclaveToken);
            if (aStream.getLong() == 0) {
                smf_printstream.println("ERROR Request Info Section (WLM). The enclave token is 0.");
                aStream.close();
                return false;
            }
            aStream.close();
        } catch (Throwable t) {
            smf_printstream.println("ERROR Request Info Section (WLM). Exception caught during data validation: " + t.getCause());
        }

        return true;
    }

    /**
     * Validates classification data in the SMF record.
     *
     * @return True if the validation was successful. False, otherwise.
     */
    private boolean validateClassificationData() {
        int classificationDataCount = libertyRequestRecord.m_classificationDataTriplet.count();
        for (int i = 0; i < classificationDataCount; i++) {
            ClassificationDataSection cds = libertyRequestRecord.m_classificationDataSection[i];
            if (cds.m_version == 1) {
                if (cds.m_dataType == ClassificationDataSection.TypeURI) {
                    smf_printstream.println("Classification Data Section. URI type found.");
                    if (cds.m_dataLength == 0) {
                        smf_printstream.println("ERROR Classification Data Section. The URI length is 0.");
                        return false;
                    }
                }

                if (cds.m_dataType == ClassificationDataSection.TypeHostname) {
                    smf_printstream.println("Classification Data Section. Host name type found.");
                    if (cds.m_dataLength == 0) {
                        smf_printstream.println("ERROR Classification Data Section. The host name is 0.");
                        return false;
                    }
                }

                if (cds.m_dataType == ClassificationDataSection.TypePort) {
                    smf_printstream.println("Classification Data Section. Port type found.");
                    if (cds.m_dataLength == 0) {
                        smf_printstream.println("ERROR Classification Data Section. The port length is 0.");
                        return false;
                    }
                }
            }
        }

        return true;

    }
}
