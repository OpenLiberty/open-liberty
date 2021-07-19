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

/** Data container for SMF data related to a Smf record product section. */
public class LibertyRequestInfoSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;

    /** version of section. */
    public int m_version;
    /** TCB address where the request was dispatched */
    public byte m_tcbAddress[];
    /** TTOKEN of the TCB where the request was dispatched */
    public byte m_ttoken[];
    /** Thread id of the request was dispatched */
    public byte m_threadId[];
    /** The system GMT offset within the interpreted record. */
    public byte m_systemGmtOffset[];
    /** Thread id of the request was dispatched. Thread.currentThread().getId() */
    public byte m_threadIdCurrentThread[];
    /** Request id */
    public byte m_requestId[];
    /** reserved */
    public byte m_reserved[];
    /** STCK when request was received */
    public byte m_startStck[];
    /** STCK when request ended */
    public byte m_endStck[];
    /** WLM transaction class used for classification */
    public String m_wlmTransactionClass;
    /** Timeused when request started */
    public byte m_timeusedStart[];
    /** Timeused when request started */
    public byte m_timeusedEnd[];
    /** CPU time for the enclave at enclave delete */
    public long m_enclaveDeleteCPU;
    /** CPU usage in service units */
    public long m_enclaveDeleteCpuService;
    /** zAAP CPU time for the enclave at enclave delete */
    public long m_enclaveDeletezAAPCPU;
    /** zAAP CPU usage in service units */
    public long m_enclaveDeletezAAPService;
    /** zIIP CPU */
    public long m_enclaveDeletezIIPCPU;
    /** zIIP CPU usage in service units */
    public long m_enclaveDeletezIIPService;
    /** WLM zAAP normalization factor */
    public int m_enclaveDeletezAAPNorm;
    /** WLM enclave response time ratio */
    public int m_enclaveDeleteRespTimeRatio;
    /** Enclave token for the request */
    public byte m_enclaveToken[];
    /** User id */
    public String m_userid;
    /** mapped User id */
    public String m_mappedUserid;
    /** length of request URI string */
    public int m_lengthRequestUri;
    /** request URI of this request */
    public String m_requestUri;

    //----------------------------------------------------------------------------
    /**
     * LibertyRequestInfoSection constructor from a SmfStream.
     *
     * @param aSmfStream SmfStream to be used to build this LibertyRequestInfoSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public LibertyRequestInfoSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        m_version = aSmfStream.getInteger(4);
        // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        m_tcbAddress = aSmfStream.getByteBuffer(4);
        m_ttoken = aSmfStream.getByteBuffer(16);
        m_threadId = aSmfStream.getByteBuffer(8);
        m_systemGmtOffset = aSmfStream.getByteBuffer(8);
        m_threadIdCurrentThread = aSmfStream.getByteBuffer(8);
        m_requestId = aSmfStream.getByteBuffer(23);
        m_reserved = aSmfStream.getByteBuffer(1);
        m_startStck = aSmfStream.getByteBuffer(8);
        m_endStck = aSmfStream.getByteBuffer(8);
        m_wlmTransactionClass = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_timeusedStart = aSmfStream.getByteBuffer(16);
        m_timeusedEnd = aSmfStream.getByteBuffer(16);

        m_enclaveDeleteCPU = aSmfStream.getLong();
        m_enclaveDeleteCpuService = aSmfStream.getLong();
        m_enclaveDeletezAAPCPU = aSmfStream.getLong();
        m_enclaveDeletezAAPService = aSmfStream.getLong();
        m_enclaveDeletezIIPCPU = aSmfStream.getLong();
        m_enclaveDeletezIIPService = aSmfStream.getLong();
        m_enclaveDeletezAAPNorm = aSmfStream.getInteger(4);
        m_enclaveDeleteRespTimeRatio = aSmfStream.getInteger(4);
        m_enclaveToken = aSmfStream.getByteBuffer(8);

        m_userid = aSmfStream.getString(64, SmfUtil.EBCDIC);
        m_mappedUserid = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_lengthRequestUri = aSmfStream.getInteger(4);
        m_requestUri = aSmfStream.getString(128, SmfUtil.EBCDIC);

    } // LibertyRequestInfoSection(..)

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
     * @param aTripletNumber The triplet number of this LibertyRequestInfoSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "LibertyRequestInfoSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Request Info Version            ", m_version);
        aPrintStream.printlnKeyValue("Tcb Address                     ", m_tcbAddress, null);
        aPrintStream.printlnKeyValue("TToken                          ", m_ttoken, null);
        aPrintStream.printlnKeyValue("Thread id                       ", m_threadId, null);
        aPrintStream.printlnKeyValue("System GMT Offset from CVTLDTO (HEX)", m_systemGmtOffset, null);
        aPrintStream.printlnKeyValue("Thread.currentThread().getId()  ", m_threadIdCurrentThread, null);
        aPrintStream.printlnKeyValue("Request id                      ", m_requestId, null);
        aPrintStream.printlnKeyValue("Reserved for alignment          ", m_reserved, null);
        aPrintStream.printlnKeyValue("Start STCK                      ", m_startStck, null);
        aPrintStream.printlnKeyValue("End STCK                        ", m_endStck, null);
        aPrintStream.printlnKeyValue("Transaction Class               ", m_wlmTransactionClass);
        aPrintStream.printlnKeyValue("Timeused start                  ", m_timeusedStart, null);
        aPrintStream.printlnKeyValue("Timeused end                    ", m_timeusedEnd, null);
        aPrintStream.printlnKeyValue("Enclave Delete CPU              ", m_enclaveDeleteCPU);
        aPrintStream.printlnKeyValue("Enclave Delete CPU  Service     ", m_enclaveDeleteCpuService);
        aPrintStream.printlnKeyValue("Enclave Delete zAAP CPU         ", m_enclaveDeletezAAPCPU);
        aPrintStream.printlnKeyValue("Enclave Delete zAAP Service     ", m_enclaveDeletezAAPService);
        aPrintStream.printlnKeyValue("Enclave Delete zIIP CPU         ", m_enclaveDeletezIIPCPU);
        aPrintStream.printlnKeyValue("Enclave Delete zIIP Service     ", m_enclaveDeletezIIPService);
        aPrintStream.printlnKeyValue("Enclave Delete zAAP Norm        ", m_enclaveDeletezAAPNorm);
        aPrintStream.printlnKeyValue("Enclave Delete Resp Time Ratio  ", m_enclaveDeleteRespTimeRatio);
        aPrintStream.printlnKeyValue("Enclave Token                   ", m_enclaveToken, null);

        aPrintStream.printlnKeyValue("User Id                         ", m_userid);
        aPrintStream.printlnKeyValue("Mapped User Id                  ", m_mappedUserid);

        aPrintStream.printlnKeyValue("Request URI length              ", m_lengthRequestUri);
        aPrintStream.printlnKeyValue("Request URI                     ", m_requestUri);

        aPrintStream.pop();

        // PerformanceSummary stuff

    } // dump()

} // LibertyRequestInfoSection
