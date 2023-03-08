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

package com.ibm.ws390.sm.smfview;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LibertyBatchIdentificationSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;

    /** version of section. */
    public int m_version;

    /** Job Instance ID */
    public long m_instanceId;

    /** Job execution ID */
    public long m_executionId;

    /** execution number */
    public long m_executionNumber;

    /** step id */
    public long m_stepExecutionId;

    /** partition number */
    public long m_partitionNumber;

    /** job name */
    public String m_jobName;

    /** amc name */
    public String m_AmcName;

    /** xml name */
    public String m_XMLName;

    /** step name */
    public String m_stepName;

    /** split name */
    public String m_splitName;

    /** flow name */
    public String m_flowName;

    /** create time */
    public long m_createTime;
    public Date m_createTimeDate;

    /** start time */
    public long m_startTime;
    public Date m_startTimeDate;

    /** end time */
    public long m_endTime;
    public Date m_endTimeDate;

    /** submitter */
    public String m_submitter;

    /** JES job name */
    public String m_jesJobName;

    /** JES Job ID */
    public String m_jesJobId;

    /** TCB address */
    public byte m_tcbAddress[];

    /** TTOKEN of the TCB */
    public byte m_ttoken[];

    /** flags set for the identification record */
    public byte m_flags[];

    /** From JSL step start limit */
    public int m_stepStartLimit;

    /** From JSL for chunk policy */
    public int m_chunkStepCheckpointPolicy;

    /** From JSL for chunk item count */
    public int m_chunkStepItemCount;

    /** From JSL for chunk time limit */
    public int m_chunkStepTimeLimit;

    /** From JSL for chunk skip limit */
    public int m_chunkStepSkipLimit;

    /** From JSL for chunk retry limit */
    public int m_chunkStepRetryLimit;

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     *
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // supportedVersion()

    public LibertyBatchIdentificationSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        m_version = aSmfStream.getInteger(4);

        m_instanceId = aSmfStream.getLong();
        m_executionId = aSmfStream.getLong();
        m_executionNumber = aSmfStream.getLong();
        m_stepExecutionId = aSmfStream.getLong();
        m_partitionNumber = aSmfStream.getLong();

        m_jobName = aSmfStream.getString(32, SmfUtil.EBCDIC);
        m_AmcName = aSmfStream.getString(32, SmfUtil.EBCDIC);
        m_XMLName = aSmfStream.getString(32, SmfUtil.EBCDIC);
        m_stepName = aSmfStream.getString(32, SmfUtil.EBCDIC);
        m_splitName = aSmfStream.getString(32, SmfUtil.EBCDIC);
        m_flowName = aSmfStream.getString(32, SmfUtil.EBCDIC);

        m_createTime = aSmfStream.getLong();
        m_createTimeDate = new Date(m_createTime);
        m_startTime = aSmfStream.getLong();
        m_startTimeDate = new Date(m_startTime);
        m_endTime = aSmfStream.getLong();
        m_endTimeDate = new Date(m_endTime);

        m_submitter = aSmfStream.getString(32, SmfUtil.EBCDIC);
        m_jesJobName = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_jesJobId = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_tcbAddress = aSmfStream.getByteBuffer(4);
        m_ttoken = aSmfStream.getByteBuffer(16);

        m_flags = aSmfStream.getByteBuffer(8);
        m_stepStartLimit = aSmfStream.getInteger(4);
        m_chunkStepCheckpointPolicy = aSmfStream.getInteger(4);
        m_chunkStepItemCount = aSmfStream.getInteger(4);
        m_chunkStepTimeLimit = aSmfStream.getInteger(4);
        m_chunkStepSkipLimit = aSmfStream.getInteger(4);
        m_chunkStepRetryLimit = aSmfStream.getInteger(4);

    }

    //----------------------------------------------------------------------------
    /**
     * Dumps the fields of this object to a print stream.
     *
     * @param aPrintStream   The stream to print to.
     * @param aTripletNumber The triplet number of this LibertyRequestInfoSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "LibertyBatchIdentificationSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Identification Section Version             ", m_version);

        aPrintStream.printlnKeyValue("Instance ID                                ", m_instanceId);
        aPrintStream.printlnKeyValue("Execution ID                               ", m_executionId);
        aPrintStream.printlnKeyValue("Execution Number                           ", m_executionNumber);
        aPrintStream.printlnKeyValue("Step Execution ID                          ", m_stepExecutionId);
        aPrintStream.printlnKeyValue("Partition Number                           ", m_partitionNumber);

        aPrintStream.printlnKeyValue("Job name                                   ", m_jobName);
        aPrintStream.printlnKeyValue("AMC Name                                   ", m_AmcName);
        aPrintStream.printlnKeyValue("XML File Name                              ", m_XMLName);
        aPrintStream.printlnKeyValue("Step Name                                  ", m_stepName);
        aPrintStream.printlnKeyValue("Split Name                                 ", m_splitName);
        aPrintStream.printlnKeyValue("Flow Name                                  ", m_flowName);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        aPrintStream.printlnKeyValue("Create time (value)                        ", m_createTime);
        aPrintStream.printlnKeyValue("Create time                                ", sdf.format(m_createTimeDate));

        aPrintStream.printlnKeyValue("Start time (value)                         ", m_startTime);
        aPrintStream.printlnKeyValue("Start time                                 ", sdf.format(m_startTimeDate));

        aPrintStream.printlnKeyValue("End time (value)                           ", m_createTime);
        aPrintStream.printlnKeyValue("End time                                   ", sdf.format(m_endTimeDate));

        aPrintStream.printlnKeyValue("Submitter                                  ", m_submitter);
        aPrintStream.printlnKeyValue("JES Job name                               ", m_jesJobName);
        aPrintStream.printlnKeyValue("JES Job ID                                 ", m_jesJobId);

        aPrintStream.printlnKeyValue("Tcb Address                                ", m_tcbAddress, null);
        aPrintStream.printlnKeyValue("TToken                                     ", m_ttoken, null);
        aPrintStream.printlnKeyValue("Flags                                      ", m_flags, null);
        aPrintStream.printlnKeyValue("stepStartLimit:                            ", m_stepStartLimit);
        aPrintStream.printlnKeyValue("chunkStepCheckpointPolicy                  ", m_chunkStepCheckpointPolicy);
        aPrintStream.printlnKeyValue("chunkStepItemCount                         ", m_chunkStepItemCount);
        aPrintStream.printlnKeyValue("chunkStepTimeLimit,                        ", m_chunkStepTimeLimit);
        aPrintStream.printlnKeyValue("chunkStepSkipLimit                         ", m_chunkStepSkipLimit);
        aPrintStream.printlnKeyValue("chunkStepRetryLimit                        ", m_chunkStepRetryLimit);

        aPrintStream.pop();
    }

}
