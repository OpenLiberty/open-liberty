/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
/** Data container for SMF data related to a Smf Outbound Request Wola Type Specific Section. */
public class OutboundRequestWolaTypeSpecificSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;
    /** version of this section */
    public int m_version;
    /** register name */
    public String m_registerName;
    /** service name */
    public String m_serviceName;
    /** correlator Context */
    public String m_correlatorContext1;
    /** correlator Context part 2 */
    public byte m_correlatorContext2[];
    /** reserved space */
    public byte m_theblob[];

    //----------------------------------------------------------------------------
    /**
     * OutboundRequestWolaTypeSpecificSection constructor from a SmfStream.
     *
     * @param aSmfStream SmfStream to be used to build this OutboundRequestWolaTypeSpecificSection.
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public OutboundRequestWolaTypeSpecificSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_version = aSmfStream.getInteger(4);

        m_registerName = aSmfStream.getString(12, SmfUtil.EBCDIC);

        m_serviceName = aSmfStream.getString(256, SmfUtil.EBCDIC);

        m_correlatorContext1 = aSmfStream.getString(128, SmfUtil.EBCDIC);

        m_correlatorContext2 = aSmfStream.getByteBuffer(128);

        m_theblob = aSmfStream.getByteBuffer(24);

    } // OutboundRequestWolaTypeSpecificSection(..)

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
     * @param aTripletNumber The triplet number of this OutboundRequestWolaTypeSpecificSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "OutboundRequestWolaTypeSpecificSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Version                          ", m_version);
        aPrintStream.printlnKeyValue("Register name                    ", m_registerName);
        aPrintStream.printlnKeyValue("service name                     ", m_serviceName);
        aPrintStream.printlnKeyValue("WOLA correlator context part 1   ", m_correlatorContext1);
        aPrintStream.printlnKeyValue("WOLA correlator context part 2   ", m_correlatorContext2, null);
        aPrintStream.printlnKeyValue("Reserved                         ", m_theblob, null);

        aPrintStream.pop();

    } // dump()

} // OutboundRequestWolaTypeSpecificSection
