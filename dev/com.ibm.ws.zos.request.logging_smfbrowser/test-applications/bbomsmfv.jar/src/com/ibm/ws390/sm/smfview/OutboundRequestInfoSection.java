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
/** Data container for SMF data related to a Smf Outbound Request information Section. */
public class OutboundRequestInfoSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;
    /** version of this section */
    public int m_version;
    /** process of ID of the servant where the request was dispatched */
    public byte m_dispatchServantPID[];
    /** task id of the thread in the servant where the request was dispatched */
    public byte m_dispatchTaskId[];
    /** outbound request type */
    public int m_outboundRequestType;
    /** reserved space */
    public byte m_reservedAlignment0[];
    /** jobname of the servant where the request was dispatched */
    public String m_dispatchServantJobname;
    /** jobid of the servant where the request was dispatched */
    public String m_dispatchServantJobId;
    /** STOKEN of the servant where the request was dispatched */
    public byte m_dispatchServantStoken[];
    /** ASID of the servant where the request was dispatched */
    public byte m_dispatchServantAsid[];
    /** reserved */
    public byte m_reservedAlignment1[];
    /** TCB address where the request was dispatched */
    public byte m_dispatchServantTcbAddress[];
    /** TTOKEN of the TCB where the request was dispatched */
    public byte m_dispatchServantTtoken[];
    /** Enclave token for the request */
    public byte m_dispatchServantEnclaveToken[];
    /** number of bytes sent */
    public long m_bytesSent;
    /** number of response bytes */
    public long m_responseBytes;
    /** timestamp when request went outbound */
    public byte m_outboundStartTime[];
    /** timestamp when outbound request returned */
    public byte m_outboundEndTime[];
    /** reserved space */
    public byte m_theblob[];

    //----------------------------------------------------------------------------
    /**
     * OutboundRequestInfoSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this OutboundRequestInfoSection.
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public OutboundRequestInfoSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_version = aSmfStream.getInteger(4);
        m_dispatchServantPID = aSmfStream.getByteBuffer(4);
        m_dispatchTaskId = aSmfStream.getByteBuffer(8);
        m_outboundRequestType = aSmfStream.getInteger(4);
        m_reservedAlignment0 = aSmfStream.getByteBuffer(4);
        m_dispatchServantJobname = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_dispatchServantJobId = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_dispatchServantStoken = aSmfStream.getByteBuffer(8);
        m_dispatchServantAsid = aSmfStream.getByteBuffer(2);
        m_reservedAlignment1 = aSmfStream.getByteBuffer(2);
        m_dispatchServantTcbAddress = aSmfStream.getByteBuffer(4);
        m_dispatchServantTtoken = aSmfStream.getByteBuffer(16);
        m_dispatchServantEnclaveToken = aSmfStream.getByteBuffer(8);
        m_bytesSent = aSmfStream.getLong();
        m_responseBytes = aSmfStream.getLong();
        m_outboundStartTime = aSmfStream.getByteBuffer(16);
        m_outboundEndTime = aSmfStream.getByteBuffer(16);
        m_theblob = aSmfStream.getByteBuffer(32);

    } // OutboundRequestInfoSection(..)

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
     * @param aTripletNumber The triplet number of this OutboundRequestInfoSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "OutboundRequestInfoSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Version                        ", m_version);
        aPrintStream.printlnKeyValue("Dispatch Servant PID (HEX)     ", m_dispatchServantPID, null);
        aPrintStream.printlnKeyValue("Dispatch Task ID               ", m_dispatchTaskId, null);
        aPrintStream.printlnKeyValue("Outbound request type          ", m_outboundRequestType);
        aPrintStream.printlnKeyValue("Reserved                       ", m_reservedAlignment0, null);
        aPrintStream.printlnKeyValue("Servant Job Name               ", m_dispatchServantJobname);
        aPrintStream.printlnKeyValue("Servant Job ID                 ", m_dispatchServantJobId);
        aPrintStream.printlnKeyValue("Servant SToken                 ", m_dispatchServantStoken, null);
        aPrintStream.printlnKeyValue("Servant ASID (HEX)             ", m_dispatchServantAsid, null);
        aPrintStream.printlnKeyValue("Reserved for alignment         ", m_reservedAlignment1, null);
        aPrintStream.printlnKeyValue("Servant Tcb Address            ", m_dispatchServantTcbAddress, null);
        aPrintStream.printlnKeyValue("Servant TToken                 ", m_dispatchServantTtoken, null);
        aPrintStream.printlnKeyValue("Servant Enclave Token          ", m_dispatchServantEnclaveToken, null);
        aPrintStream.printlnKeyValue("Bytes sent                     ", m_bytesSent);
        aPrintStream.printlnKeyValue("Response bytes                 ", m_responseBytes);
        aPrintStream.printlnKeyValue("Time outbound request started  ", m_outboundStartTime, null);
        aPrintStream.printlnKeyValue("Time outbound request ended    ", m_outboundEndTime, null);
        aPrintStream.printlnKeyValue("Reserved                       ", m_theblob, null);

        aPrintStream.pop();

    } // dump()

} // OutboundRequestInfoSection
