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
import com.ibm.ws390.sm.smfview.LibertyBatchIdentificationSection;
import com.ibm.ws390.sm.smfview.LibertyBatchRecord;
import com.ibm.ws390.sm.smfview.SMFFilter;
import com.ibm.ws390.sm.smfview.SmfPrintStream;
import com.ibm.ws390.sm.smfview.SmfRecord;

public class TestBatchSMFLoggingBasic implements SMFFilter {

    public TestBatchSMFLoggingBasic() {
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
        if (record.type() == 120 && record.subtype() == 12)
            ok_to_process = true;
        return ok_to_process;
    }

    @Override
    public SmfRecord parse(SmfRecord record) {
        return DefaultFilter.commonParse(record);
    }

    @Override
    public void processRecord(SmfRecord record) {
        LibertyBatchRecord rec = (LibertyBatchRecord) record;
        int rc = 0;
        SMFType120SubType12BasicValidation basicValidation = new SMFType120SubType12BasicValidation(rec, smf_printstream);

        rc = basicValidation.validateRecord();

        LibertyBatchIdentificationSection libertyBatchIdentificationSection = rec.m_identificationSection;

        //Check that the job name in the id section is "BonusPayoutJob"
        //Not a good check, we're not always using BonusPayoutJob now
        /*
         * if (!libertyBatchIdentificationSection.m_jobName.equals("BonusPayoutJob")) {
         *
         * smf_printstream.println("ERROR Batch Identification Section job name is incorrect. Expecting BonusPayoutJob but got " + libertyBatchIdentificationSection.m_jobName);
         * rc = 1;
         * }
         */

        if (rc == 0) {
            looksGood = 1;
        }
    }

    @Override
    public void processingComplete() {
        if (looksGood == 1)
            smf_printstream.println("TestBatchSMFLoggingBasic looks good.");
    }

    private SmfPrintStream smf_printstream;
    private int looksGood;
}
