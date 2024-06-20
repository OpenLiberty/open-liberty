/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
/** Data container for SMF data related to a Smf record network data section. */
public class LibertyNetworkDataSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;
    /** version of this section */
    public int m_version;
    public long m_reserved1;
    /** Bytes written */
    public long m_responseBytes;
    /** target port number for this request */
    public int m_targetPort;
    /** remote port number for this request */
    public int m_remotePort;
    /** length of remote addr string */
    public int m_lengthRemoteAddrString;
    /** remote addr of this request */
    public String m_remoteaddrstring;

    //----------------------------------------------------------------------------
    /**
     * LibertyNetworkDataSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this LibertyNetworkDataSection.
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public LibertyNetworkDataSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        m_version = aSmfStream.getInteger(4);
        // skip reserved
        m_reserved1 = aSmfStream.getLong();

        m_responseBytes = aSmfStream.getLong();
        m_targetPort = aSmfStream.getInteger(4);
        m_remotePort = aSmfStream.getInteger(4);
        m_lengthRemoteAddrString = aSmfStream.getInteger(4);
        m_remoteaddrstring = aSmfStream.getString(40, SmfUtil.EBCDIC);

    } // LibertyNetworkDataSection(..)

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
     * @param aTripletNumber The triplet number of this LibertyNetworkDataSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "LibertyNetworkDataSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Version             ", m_version);

        aPrintStream.printlnKeyValue("Reserved            ", m_reserved1);

        aPrintStream.printlnKeyValue("Bytes Written       ", m_responseBytes);

        aPrintStream.printlnKeyValue("Target Port         ", m_targetPort);

        aPrintStream.printlnKeyValue("Remote Port         ", m_remotePort);

        aPrintStream.printlnKeyValue("Remote addr String Length", m_lengthRemoteAddrString);

        aPrintStream.printlnKeyValue("Remote addr String  ", m_remoteaddrstring);

        aPrintStream.pop();

    } // dump()

} // LibertyNetworkDataSection
