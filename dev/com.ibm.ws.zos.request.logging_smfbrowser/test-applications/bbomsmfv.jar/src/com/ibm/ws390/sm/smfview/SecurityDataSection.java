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
public class SecurityDataSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;
    /** version of this section */
    public int m_version;
    /** type of data in this section (1=original, 2=received, 3=invocation) */
    public int m_dataType;
    /** Length of data */
    public int m_dataLength;
    /** identity string */
    public String m_id;

    //----------------------------------------------------------------------------
    /**
     * SecurityDataSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this SecurityDataSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public SecurityDataSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_version = aSmfStream.getInteger(4);

        m_dataType = aSmfStream.getInteger(4);

        m_dataLength = aSmfStream.getInteger(4);

        m_id = aSmfStream.getString(m_dataLength, SmfUtil.EBCDIC);

        // Skip past remaining area since we have space for 64 chars
        // even if we used less than that
        aSmfStream.getString(64 - m_dataLength, SmfUtil.EBCDIC);

    } // SecurityDataSection(..)

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
     * @param aTripletNumber The triplet number of this SecurityDataSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        // Append a user readable type string as a description
        // instead of just the number for the identity types
        String identityTypeString = "";
        switch (m_dataType) {
            case 1:
                identityTypeString = "Original Identity";
                break;
            case 2:
                identityTypeString = "Received Identity";
                break;
            case 3:
                identityTypeString = "Invocation Identity";
                break;
        }

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "SecurityDataSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Version        ", m_version);
        aPrintStream.printlnKeyValueString("Identity Type  ", m_dataType, identityTypeString);
        aPrintStream.printlnKeyValue("Identity Length", m_dataLength);
        aPrintStream.printlnKeyValueString("Identity   ", m_id, "EBCDIC");

        aPrintStream.pop();

        if (m_dataType == 1) {
            PerformanceSummary.writeString(".Sec", 4); //@SU9 Just let them know a Security Section is present
        } // endif
// Write Security IDs to Summary Report		(Not Now)            //@SU9
// PerformanceSummary.writeString("\n          9S: ", 15);          //@SU9
// PerformanceSummary.writeString(identityTypeString, 19);            //@SU9
// PerformanceSummary.writeString("=", 1);                            //@SU99
// PerformanceSummary.writeString(m_id, m_dataLength);                //@SU9
// ++PerformanceSummary.lineNumber;                // Increment Line #  //@SU9
    } // dump()

} // SecurityDataSection
