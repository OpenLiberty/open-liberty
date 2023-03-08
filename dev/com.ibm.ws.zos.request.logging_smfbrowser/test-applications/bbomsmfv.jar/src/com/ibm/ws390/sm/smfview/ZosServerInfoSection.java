/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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

//------------------------------------------------------------------------------
/** Data container for SMF data related to a Smf record product section. */
public class ZosServerInfoSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2;

    /** Bit Masks */
    private final static int MASK_CPU_UsageOverflow = 0x80;
    private final static int MASK_CEEGMTO_Failed = 0x40;

    /** section version. */
    public int m_version;

    /** The system name within the interpreted record. */
    public String m_systemName;

    /** The sysplex name within the interpreted record. */
    public String m_sysplexName;

    /** The controller job name within the interpreted record. */
    public String m_controllerJobName;

    /** The controller job ID within the interpreted record. */
    public String m_controllerJobId;

    /** The controller STOKEN within the interpreted record. */
    public byte m_controllerStoken[];

    /** The controller ASID within the interpreted record. */
    public byte m_controllerAsid[];

    /** Bit Flags. - only version 2 and up */
    public byte[] m_bitFlags = new byte[2];

    /** The cluster UUID within the interpreted record. */
    public byte[] m_clusterUuid = new byte[20];

    /** The server UUID within the interpreted record. */
    public byte[] m_serverUuid = new byte[20];

    /** The daemon group name within the interpreted record. */
    public String m_daemonGroupName;

    /** The LE GMT offset (hours) within the interpreted record. */
    public int m_leGmtOffsetHours;

    /** The LE GMT offset (minutes) within the interpreted record. */
    public int m_leGmtOffsetMin;

    /** The LE GMT offset (seconds) within the interpreted record. */
    public long m_leGmtOffsetSec;

    /** The system GMT offset within the interpreted record. */
    public byte m_systemGmtOffset[];

    /** The maintenance level within the interpreted record. */
    public String m_maintenanceLevel;

    /** Reserved area within the interpreted record for future use. */
    public byte m_theblob[];

    //----------------------------------------------------------------------------
    /**
     * ZosServerInfoSection constructor from a SmfStream.
     *
     * @param aSmfStream SmfStream to be used to build this ZosServerInfoSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public ZosServerInfoSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_version = aSmfStream.getInteger(4);

        m_systemName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_sysplexName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_controllerJobName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_controllerJobId = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_controllerStoken = aSmfStream.getByteBuffer(8);

        m_controllerAsid = aSmfStream.getByteBuffer(2);

        m_bitFlags = aSmfStream.getByteBuffer(2); // only version 2 and up, prior just reserved

        m_clusterUuid = aSmfStream.getByteBuffer(20);

        m_serverUuid = aSmfStream.getByteBuffer(20);

        m_daemonGroupName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_leGmtOffsetHours = aSmfStream.getInteger(4);

        m_leGmtOffsetMin = aSmfStream.getInteger(4);

        m_leGmtOffsetSec = aSmfStream.getLong();

        m_systemGmtOffset = aSmfStream.getByteBuffer(8);

        if (m_version >= 2) //13@v8A
        {
            m_maintenanceLevel = aSmfStream.getString(16, SmfUtil.EBCDIC);
            m_theblob = aSmfStream.getByteBuffer(20);
        } else {
            m_maintenanceLevel = aSmfStream.getString(8, SmfUtil.EBCDIC);
            m_theblob = aSmfStream.getByteBuffer(28);
        }

    } // ZosServerInfoSection(..)

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

    //----------------------------------------------------------------------------
    /**
     * Dumps the fields of this object to a print stream.
     *
     * @param aPrintStream   The stream to print to.
     * @param aTripletNumber The triplet number of this ZosServerInfoSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ZosServerInfoSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Server Info Version                 ", m_version);
        aPrintStream.printlnKeyValue("System Name (CVTSNAME)              ", m_systemName);
        aPrintStream.printlnKeyValue("Sysplex Name                        ", m_sysplexName);
        aPrintStream.printlnKeyValue("Controller Name                     ", m_controllerJobName);
        aPrintStream.printlnKeyValue("Controller Job ID                   ", m_controllerJobId);
        aPrintStream.printlnKeyValue("Controller STOKEN                   ", m_controllerStoken, null);
        aPrintStream.printlnKeyValue("Controller ASID (HEX)               ", m_controllerAsid, null);

        if (m_version >= 2) //13@v8A
        {
            /*
             * // Get the defined bit flags in the first byte and convert to int preserving the first 8 bits.
             * // Currently only the first 2 high-order bits in the first byte are defined in V8. The other
             * // 14 remaining bits are reserved for future flags.
             */
            int firstByte = (0x000000FF & (m_bitFlags[0]));
            int booleanValue;
            booleanValue = ((firstByte & MASK_CPU_UsageOverflow) == MASK_CPU_UsageOverflow) ? 1 : 0;
            aPrintStream.printlnKeyValue("CPU Usage Overflow                  ", booleanValue);
            booleanValue = ((firstByte & MASK_CEEGMTO_Failed) == MASK_CEEGMTO_Failed) ? 1 : 0;
            aPrintStream.printlnKeyValue("CEEGMTO failed/unavailable          ", booleanValue);
        }

        aPrintStream.printlnKeyValue("Cluster UUID                        ", m_clusterUuid, null);
        aPrintStream.printlnKeyValue("Server UUID                         ", m_serverUuid, null);
        aPrintStream.printlnKeyValue("Daemon Group Name                   ", m_daemonGroupName);
        aPrintStream.printlnKeyValue("LE GMT Offset (Hours) from CEEGMTO  ", m_leGmtOffsetHours);
        aPrintStream.printlnKeyValue("LE GMT Offset (Minutes) from CEEGMTO", m_leGmtOffsetMin);
        aPrintStream.printlnKeyValue("LE GMT Offset (Seconds) from CEEGMTO", m_leGmtOffsetSec);
        aPrintStream.printlnKeyValue("System GMT Offset from CVTLDTO (HEX)", m_systemGmtOffset, null);
        aPrintStream.printlnKeyValue("Maintenance Level                   ", m_maintenanceLevel);
        aPrintStream.printlnKeyValue("Reserved                            ", m_theblob, null);

        aPrintStream.pop();

        // Write Server Jobname 					                  //@SU9
        PerformanceSummary.writeString(" ", 1); //@SU9
        PerformanceSummary.writeString(m_controllerJobName, 7); //@SU9
        PerformanceSummary.writeString(" ", 1); //@SU9
        PerformanceSummary.writeString(m_controllerJobId, 8); //@SU9

    } // dump()

} // ZosServerInfoSection
