/*******************************************************************************
 * Copyright (c) 2001 IBM Corporation and others.
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
/** Data container for SMF data related to a communication session. */
public class CommSessionSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 4; // @LI4369-4C

    /** Length of version 2 of this section */
    public final static int s_secondLength = 100; // @MD20733A

    /** Local optimization enum value. */
    public static final int LocalOptimization = 1;

    /** Remote optimization enum value. */
    public static final int RemoteOptimization = 2;

    /** RemoteEncypted optimization enum value. */
    public static final int RemoteEncryptedOptimization = 3;

    /** Sysplex optimization enum value. */
    public static final int SysplexOptimization = 4; // @MD17014 A

    /** Http session optimization enum value. */
    public static final int HttpOptimization = 5; // @MD17014 A

    /** Http encrypted session optimization enum value. */
    public static final int HttpEncryptedOptimization = 6; // @MD17014 A

    /** message driven bean optimization enum value. */
    public static final int MdbOptimization = 7; // @MD20733A

    /** SIP session optimization enum value. */
    public static final int SipOptimization = 8; // @LI4369-4A

    /** SIP encrypted session optimization enum value. */
    public static final int SipEncryptedOptimization = 9; // @LI4369-4A

    /** handle associated with this comm session. */
    public byte m_commSessionHandle[];

    /** address associated with this comm session. */
    public String m_commSessionAddress;

    /** Optimization customized for this comm session. */
    public int m_commSessionOptimization;

    /** Number of bytes received on this comm session. */
    public int m_dataReceived;

    /** Number of bytes written on this comm session. */
    public int m_dataTransferred;

    /** Number of bytes received on this comm session. */
    public long m_dataReceived64; /* @MD20733A */

    /** Number of bytes written on this comm session. */
    public long m_dataTransferred64; /* @MD20733A */

    /** Length of the section */
    public int m_sectionLength; /* @MD20733A */

    //----------------------------------------------------------------------------
    /**
     * CommSessionSection constructor from SmfSream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of CommSessionSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @param aSectionLength    length of the section
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public CommSessionSection(SmfStream aSmfStream, int aRequestedVersion, int aSectionLength) // @MD20733C
        throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_commSessionHandle = aSmfStream.getByteBuffer(8);

        m_commSessionAddress = aSmfStream.getString(64, SmfUtil.EBCDIC);

        m_commSessionOptimization = aSmfStream.getInteger(4); //    @L2C

        m_dataReceived = aSmfStream.getInteger(4); //    @L2C

        m_dataTransferred = aSmfStream.getInteger(4); //    @L2C

        m_sectionLength = aSectionLength; // @MD20733A

        if (s_secondLength == m_sectionLength) { // @MD20733A
            m_dataReceived64 = aSmfStream.getLong(); // @MD20733A
            m_dataTransferred64 = aSmfStream.getLong(); // @MD20733A
        } // @MD20733A
        if (m_sectionLength > s_secondLength) { // @MD20733A
            //byte extraStuff[] = aSmfStream.getByteBuffer(m_sectionLength-s_secondLength); // @MD20733A
            aSmfStream.skip(m_sectionLength - s_secondLength);
        }

    } // CommSessionSection.CommSessionSection(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // CommSessionSection.supportedVersion()

    //----------------------------------------------------------------------------
    /**
     * Returns the optimization value as a string.
     * 
     * @return optimization value as a string.
     */
    private String optimizationToString() {
        switch (m_commSessionOptimization) {
            case LocalOptimization:
                return "local optimization";
            case RemoteOptimization:
                return "remote optimization";
            case RemoteEncryptedOptimization:
                return "remoteEncrypted optimization";
            case SysplexOptimization: //@MD17014 A
                return "sysplex optimization"; //@MD17014 A
            case HttpOptimization: //@MD17014 A
                return "HTTP session optimization"; //@MD17014 A
            case HttpEncryptedOptimization: //@MD17014 A
                return "HTTP encrypted session optimization";//@MD17014 A
            case MdbOptimization: // @MD20733A
                return "message driven bean optimization"; // @MD20733A
            case SipOptimization: //@LI4369-4A
                return "SIP session optimization"; //@LI4369-4A
            case SipEncryptedOptimization: //@LI4369-4A
                return "SIP encrypted session optimization";//@LI4369-4A
            default:
                return "unknown optimization";
        }
    } // CommSessionSection.optimizationToString()

    //----------------------------------------------------------------------------
    /**
     * Dumps the object into a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of CommSessionSection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "CommSessionSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("CommSessionHandle", m_commSessionHandle, null);
        aPrintStream.printlnKeyValue("CommSessionAddress", m_commSessionAddress);
        aPrintStream.printlnKeyValueString(
                                           "CommSessionOptimization", m_commSessionOptimization, optimizationToString());
        aPrintStream.printKeyValue("DataReceived", m_dataReceived);
        aPrintStream.printlnKeyValue("DataTransferred", m_dataTransferred);

        if (s_secondLength == m_sectionLength) { // @MD20733A
            aPrintStream.printlnKeyValue("DataReceived 8 byte field", m_dataReceived64); // @MD20733A
            aPrintStream.printlnKeyValue("DataTransferred 8 byte field", m_dataTransferred64); // @MD20733A
        } // @MD20733A

        aPrintStream.pop();

        // Write # bytes transfered & received                       //@SUa
        // PerformanceSummary.writeNewLine();                        //@SVa
        // PerformanceSummary.writeString("  ", 20);                 //@SVa 
        PerformanceSummary.writeString("             ", 12); //@SU99
        PerformanceSummary.writeInt(m_dataReceived, 9); //@SUa
        // PerformanceSummary.writeString(" ", 1);                     //@SU99
        PerformanceSummary.writeInt(m_dataTransferred, 8); //@SU99
        if ((m_dataReceived + m_dataTransferred) == 0) { //@SVa
            PerformanceSummary.clearBuf(); //@SVa
        }

    } //CommSessionSection.dump()

} // CommSessionSection