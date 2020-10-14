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

public class LibertyBatchSubsystemSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 3;

    /** version of section. */
    public int m_version;

    /** Type of record */
    public int m_batch_record_type;
    /** Record types */
    public static final int STEP_ENDED_TYPE = 1;
    public static final int JOB_ENDED_TYPE = 2;
    public static final int PARTITION_ENDED_TYPE = 3;
    public static final int FLOW_ENDED_TYPE = 4;
    public static final int DECIDER_ENDED_TYPE = 5;
    public static final String[] typeString = { "", "Step Ended", "Job Ended", "Partition Ended", "Flow Ended", "Decider Ended" };

    /** The system name within the interpreted record. */
    public String m_systemName;

    /** The sysplex name within the interpreted record. */
    public String m_sysplexName;

    /** The system GMT offset within the interpreted record. */
    public byte m_systemGmtOffset[];

    /** The Java time zone string */
    public String m_javaTimezone;

    /** The controller job ID within the interpreted record. */
    public String m_jobId;

    /** The controller job name within the interpreted record. */
    public String m_jobName;

    /** Server stoken */
    public byte m_server_stoken[];

    /** asid */
    public int m_asid;

    /** server config directory */
    public String m_serverConfigDir;

    /** product version */
    public String m_productVersion;

    public int m_rctpcpua;

    public int m_rmctadjc;

    public String m_repositoryType;

    public String m_jobStoreRefId;

    /** flag word */
    public byte[] m_bitFlags = new byte[4];

    /** Bit Masks */
    private final static int MASK_CVTZCBP = 0x80;

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

    public LibertyBatchSubsystemSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        m_version = aSmfStream.getInteger(4);
        m_batch_record_type = aSmfStream.getInteger(4);

        m_systemName = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_sysplexName = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_systemGmtOffset = aSmfStream.getByteBuffer(8);
        m_javaTimezone = aSmfStream.getString(32, SmfUtil.EBCDIC);
        m_jobId = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_jobName = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_server_stoken = aSmfStream.getByteBuffer(8);
        m_asid = aSmfStream.getInteger(4);
        m_serverConfigDir = aSmfStream.getString(128, SmfUtil.EBCDIC);
        m_productVersion = aSmfStream.getString(16, SmfUtil.EBCDIC);
        m_rctpcpua = aSmfStream.getInteger(4);
        m_rmctadjc = aSmfStream.getInteger(4);
        m_repositoryType = aSmfStream.getString(4, SmfUtil.EBCDIC);
        m_jobStoreRefId = aSmfStream.getString(16, SmfUtil.EBCDIC);
        if (m_version >= 3) {
            m_bitFlags = aSmfStream.getByteBuffer(4);
        }

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
        aPrintStream.printlnKeyValue("Type", "LibertyBatchSubsystemSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Subsystem Section Version           ", m_version);
        aPrintStream.printlnKeyValue("Record Type                         ", m_batch_record_type);
        if ((m_batch_record_type >= STEP_ENDED_TYPE) && (m_batch_record_type <= DECIDER_ENDED_TYPE)) {
            aPrintStream.printlnKeyValue("                                    ", typeString[m_batch_record_type]);
        }

        aPrintStream.printlnKeyValue("System Name (CVTSNAME)              ", m_systemName);
        aPrintStream.printlnKeyValue("Sysplex Name                        ", m_sysplexName);
        aPrintStream.printlnKeyValue("System GMT Offset from CVTLDTO (HEX)", m_systemGmtOffset, null);
        aPrintStream.printlnKeyValue("Java Time Zone                      ", m_javaTimezone);
        aPrintStream.printlnKeyValue("JobID                               ", m_jobId);
        aPrintStream.printlnKeyValue("JobName                             ", m_jobName);
        aPrintStream.printlnKeyValue("Stoken                              ", m_server_stoken, null);
        aPrintStream.printlnKeyValue("Server asid                     ", m_asid);
        aPrintStream.printlnKeyValue("Server config directory         ", m_serverConfigDir);
        aPrintStream.printlnKeyValue("Product version                 ", m_productVersion);
        aPrintStream.printlnKeyValue("RCTPCPUA                        ", m_rctpcpua);
        aPrintStream.printlnKeyValue("RMCTADJC                        ", m_rmctadjc);
        aPrintStream.printlnKeyValue("Repository type                 ", m_repositoryType);
        aPrintStream.printlnKeyValue("Job store ref                   ", m_jobStoreRefId);
        if (m_version >= 3) {
            aPrintStream.printlnKeyValue("Flag word                           ", m_bitFlags, null);
            int firstByte = (0x000000FF & (m_bitFlags[0]));
            int booleanValue = ((firstByte & MASK_CVTZCBP) == MASK_CVTZCBP) ? 1 : 0;
            aPrintStream.printlnKeyValue("  CVTZCBP                           ", booleanValue);
        }
        aPrintStream.pop();
    }
}
