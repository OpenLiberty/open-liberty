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

import java.util.Date;

import com.ibm.ws390.sm.smfview.LibertyBatchCompletionSection;
import com.ibm.ws390.sm.smfview.LibertyBatchIdentificationSection;
import com.ibm.ws390.sm.smfview.LibertyBatchProcessorSection;
import com.ibm.ws390.sm.smfview.LibertyBatchRecord;
import com.ibm.ws390.sm.smfview.LibertyBatchSubsystemSection;
import com.ibm.ws390.sm.smfview.LibertyBatchUssSection;
import com.ibm.ws390.sm.smfview.SmfPrintStream;

public class SMFType120SubType12BasicValidation {

    private final LibertyBatchRecord libertyBatchRecord;

    private final SmfPrintStream smf_printstream;

    private final int recordType;

    private static final int FLOW_ENDED_TYPE = 4;

    public SMFType120SubType12BasicValidation(LibertyBatchRecord libertyBatchRecord, SmfPrintStream smf_printstream) {
        this.libertyBatchRecord = libertyBatchRecord;
        this.smf_printstream = smf_printstream;
        this.recordType = libertyBatchRecord.m_libertyBatchSubsystemSection.m_batch_record_type;
    }

    public int validateRecord() {
        int rc = 0;
        rc = validateBatchSubsystemSection(libertyBatchRecord.m_libertyBatchSubsystemSection);
        if (rc == 0) {
            rc = validateBatchIdentificationSection(libertyBatchRecord.m_identificationSection);
        }
        if (rc == 0) {
            rc = validateBatchCompletionSection(libertyBatchRecord.m_completionSection);
        }
        if (rc == 0) {
            rc = validateBatchProcessorSection(libertyBatchRecord.m_processorSection);
        }
        if (rc == 0) {
            rc = validateBatchUssSection(libertyBatchRecord.m_ussSection);
        }
        return rc;
    }

    private int validateBatchIdentificationSection(LibertyBatchIdentificationSection libertyBatchIdentificationSection) {
        int rc = 0;
        if (libertyBatchIdentificationSection.m_version != 2) {

            smf_printstream.println("ERROR Batch Identification Section version is not 2. version=" + libertyBatchIdentificationSection.m_version);
            rc = 1;
        }

        // Check that dates are not in the future
        if (libertyBatchIdentificationSection.m_createTimeDate.after(new Date())) {

            smf_printstream.println("ERROR Batch Identification Section createTimeDate is not a valid date. createTimeDate=" + libertyBatchIdentificationSection.m_createTimeDate);
            rc = 1;
        }
        if (libertyBatchIdentificationSection.m_startTimeDate.after(new Date())) {

            smf_printstream.println("ERROR Batch Identification Section startTimeDate is not a valid date. startTimeDate=" + libertyBatchIdentificationSection.m_startTimeDate);
            rc = 1;
        }
        if (libertyBatchIdentificationSection.m_endTimeDate.after(new Date())) {

            smf_printstream.println("ERROR Batch Identification Section endTimeDate is not a valid date. endTimeDate=" + libertyBatchIdentificationSection.m_endTimeDate);
            rc = 1;
        }

        //Check that start time is after create time
        if (libertyBatchIdentificationSection.m_startTimeDate.before(libertyBatchIdentificationSection.m_createTimeDate)) {

            smf_printstream.println("ERROR Batch Identification Section startTimeDate is before createTimeDate. createTimeDate="
                                    + libertyBatchIdentificationSection.m_createTimeDate + ". startTimeDate=" + libertyBatchIdentificationSection.m_startTimeDate);
            rc = 1;
        }
        //Check that end time is after start time
        if (libertyBatchIdentificationSection.m_endTimeDate.before(libertyBatchIdentificationSection.m_startTimeDate)) {

            smf_printstream.println("ERROR Batch Identification Section endTimeDate is before startTimeDate. endTimeDate=" + libertyBatchIdentificationSection.m_endTimeDate
                                    + ". startTimeDate=" + libertyBatchIdentificationSection.m_startTimeDate);
            rc = 1;
        }

        return rc;
    }

    private int validateBatchCompletionSection(LibertyBatchCompletionSection libertyBatchCompletionSection) {
        int rc = 0;
        if (libertyBatchCompletionSection.m_version != 2) {

            smf_printstream.println("ERROR Batch Completion Section version is not 2. version=" + libertyBatchCompletionSection.m_version);
            rc = 1;
        }
        return rc;
    }

    private int validateBatchProcessorSection(LibertyBatchProcessorSection libertyBatchProcessorSection) {
        int rc = 0;

        // It is valid to have no processor section for flow type record
        if ((libertyBatchProcessorSection == null && recordType != FLOW_ENDED_TYPE)
            && libertyBatchProcessorSection.m_version != 1) {

            smf_printstream.println("ERROR Batch Processor Section version is not 1. version=" + libertyBatchProcessorSection.m_version);
            rc = 1;
        }
        return rc;
    }

    private int validateBatchUssSection(LibertyBatchUssSection libertyBatchUssSection) {
        int rc = 0;
        if (libertyBatchUssSection.m_version != 2) {

            smf_printstream.println("ERROR Batch USS Section version is not 2. version=" + libertyBatchUssSection.m_version);
            rc = 1;
        }
        return rc;
    }

    private int validateBatchSubsystemSection(LibertyBatchSubsystemSection libertyBatchSubsystemSection) {
        int rc = 0;
        if (libertyBatchSubsystemSection.m_version != 3) {
            smf_printstream.println("ERROR Batch Subsystem Section version is not 3. version=" + libertyBatchSubsystemSection.m_version);
            rc = 1;
        }

        // Ensure all bits in the flag word are zero other than the first high order bit for Cvtzcbp
        int firstByteOtherThanCvtzcbp = (0x0000007F & (libertyBatchSubsystemSection.m_bitFlags[0]));
        if (firstByteOtherThanCvtzcbp != 0) {
            smf_printstream.println("ERROR Batch Subsystem Section first byte of the flag word has bits on that should be off. First byte = " + firstByteOtherThanCvtzcbp);
            rc = 2;
        }

        int secondByte = (0x000000FF & (libertyBatchSubsystemSection.m_bitFlags[1]));
        if (secondByte != 0) {
            smf_printstream.println("ERROR Batch Subsystem Section second byte of the flag word is not 0.");
            rc = 3;
        }

        int thirdByte = (0x000000FF & (libertyBatchSubsystemSection.m_bitFlags[2]));
        if (thirdByte != 0) {
            smf_printstream.println("ERROR Batch Subsystem Section third byte of the flag word is not 0.");
            rc = 4;
        }

        int fourthByte = (0x000000FF & (libertyBatchSubsystemSection.m_bitFlags[3]));
        if (fourthByte != 0) {
            smf_printstream.println("ERROR Batch Subsystem Section fourth byte of the flag word is not 0.");
            rc = 5;
        }

        return rc;
    }
}
