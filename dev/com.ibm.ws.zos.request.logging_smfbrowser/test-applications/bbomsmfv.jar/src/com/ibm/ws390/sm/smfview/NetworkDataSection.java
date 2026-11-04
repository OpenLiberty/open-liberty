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

//  ------------------------------------------------------------------------------
/** Data container for SMF data related to a Smf record product section. */
public class NetworkDataSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;
    /** version of this section */
    public int m_version;
    /** number of bytes received for this request */
    public long m_bytesReceived;
    /** number of bytes sent as response for this request */
    public long m_bytesSent;
    /** target port number for this request */
    public int m_targetPort;
    /** length of origin string */
    public int m_lengthOriginString;
    /** origin host/port of this request (or jobname/asid for local clients) */
    public String m_originstring;

    /** reserved space */
    public byte m_theblob[];

    //----------------------------------------------------------------------------
    /**
     * NetworkDataSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this NetworkDataSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public NetworkDataSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        // 188 total
        m_version = aSmfStream.getInteger(4);
        m_bytesReceived = aSmfStream.getLong();
        m_bytesSent = aSmfStream.getLong();
        m_targetPort = aSmfStream.getInteger(4);
        m_lengthOriginString = aSmfStream.getInteger(4);
        m_originstring = aSmfStream.getString(128, SmfUtil.EBCDIC);

        m_theblob = aSmfStream.getByteBuffer(32);

    } // NetworkDataSection(..)

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
     * @param aTripletNumber The triplet number of this NetworkDataSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "NetworkDataSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Version             ", m_version);

        aPrintStream.printlnKeyValue("Bytes Received      ", m_bytesReceived);

        aPrintStream.printlnKeyValue("Bytes Sent          ", m_bytesSent);

        aPrintStream.printlnKeyValue("Target Port         ", m_targetPort);

        aPrintStream.printlnKeyValue("Origin String Length", m_lengthOriginString);

        aPrintStream.printlnKeyValue("Origin String       ", m_originstring);

        aPrintStream.printlnKeyValue("Reserved            ", m_theblob, null);

        aPrintStream.pop();

        // Show bytes received by server, and sent to server  (Debug)    //@SU9
        PerformanceSummary.writeString("\n         .9N", 14); //@SU9 
        PerformanceSummary.writeString(m_originstring, 31); //@SU9
        PerformanceSummary.writeLong(m_bytesReceived, 8); //@SU9 
        PerformanceSummary.writeLong(m_bytesSent, 8); //@SU9 
        PerformanceSummary.writeString(" ", 36); //@SU99 

        PerformanceSummary.TotalBytesRecvd = PerformanceSummary.TotalBytesRecvd + m_bytesReceived; //@SU99
        PerformanceSummary.TotalBytesSent = PerformanceSummary.TotalBytesSent + m_bytesSent; //@SU99

        //PerformanceSummary.writeString(".9Net", 5);   //@SU9 Just let them know a Net Section is present
        ++PerformanceSummary.lineNumber; // Increment Line #  //@SU9

    } // dump()

} // NetworkDataSection
