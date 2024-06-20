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

/** Data container for SMF data related to a Smf record product section. */
public class AsyncWorkDataSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;

    /** m_flags bit masks */
    static int m_flags_enclaveJoinedCreatedMask = 0x80;
    static int m_flags_schedWithIsDaemonMask = 0x40;

    /** Version of this section */
    public int m_Version;
    /** Time Execution Context Created */
    public byte m_timeExecutionContextCreated[];
    /** Execution Start time */
    public byte m_executionStartTime[];
    /** Time Execution Completed */
    public byte m_executionCompleteTime[];
    /** Servant PID */
    public int m_servantPID;
    /** Servant Job Name */
    public String m_servantJobName;
    /** Servant Job Id */
    public String m_servantJobId;
    /** Servant Stoken */
    public byte m_servantStoken[];
    /** Servant ASID */
    public byte m_servantAsid[];
    /** Reserved */
    public byte m_reserved1[];
    /** Execution context task id */
    public byte m_executionContextTaskId[];
    /** Execution context TCB Address */
    public byte m_executionContextTcbAddress[];
    /** Execution context TCB token */
    public byte m_executionContextTcbToken[];
    /** Dispatch TaskId */
    public byte m_dispatchTaskId[];
    /** Dispatch TCB Address */
    public byte m_dispatchTcbAddress[];
    /** Dispatch TCB token */
    public byte m_dispatchTcbToken[];
    /** Execution context enclave token */
    public byte m_executionContextEnclaveToken[];
    /** Dispatch enclave token */
    public byte m_dispatchEnclaveToken[];
    /** Enclave tran class */
    public String m_enclaveTranClass;
    /** Flags */
    public byte m_flags[];
    /** Enclave CPU so far */
    public long m_enclaveCpuSoFar;
    /** Enclave zAAP CPU so far */
    public long m_enclaveZaapCpuSoFar;
    /** Enclave zAAP eligible on CP */
    public long m_enclaveZaapEligibleOnCp;
    /** Enclave zIIP on CP so far */
    public long m_enclaveZiipOnCPSoFar;
    /** Enclave zIIP qual time so far */
    public long m_enclaveZiipQualTimeSoFar;
    /** Enclave zIIP CPU so far */
    public long m_enclaveZiipCpuSoFar;
    /** zAAP normalization factor */
    public int m_zAAPNormalizationFactor;
    /** reserved */
    public byte m_reserved2[];
    /** Dispatch TCB CPU */
    public long m_dispatchTcbCpu;
    /** Dispatch CPU Offload */
    public long m_dispatchCpuOffloadNonStd;
    /** Work class Name length */
    public int m_workClassNameLength;
    /** Work package / class name */
    public String m_workClassName;
    /** Work Manager Name length */
    public int m_workMgrNameLength;
    /** Work Manager name */
    public String m_workMgrName;
    /** Identity length */
    public int m_identityLength;
    /** Identity */
    public String m_identity;
    /** reserved */
    public byte m_reserved3[];

    //----------------------------------------------------------------------------
    /**
     * AsyncWorkDataSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this AsyncWorkDataSection.
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public AsyncWorkDataSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_Version = aSmfStream.getInteger(4);
        m_timeExecutionContextCreated = aSmfStream.getByteBuffer(16);
        m_executionStartTime = aSmfStream.getByteBuffer(16);
        m_executionCompleteTime = aSmfStream.getByteBuffer(16);
        m_servantPID = aSmfStream.getInteger(4);
        m_servantJobName = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_servantJobId = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_servantStoken = aSmfStream.getByteBuffer(8);
        m_servantAsid = aSmfStream.getByteBuffer(2);
        m_reserved1 = aSmfStream.getByteBuffer(2);
        m_executionContextTaskId = aSmfStream.getByteBuffer(8);
        m_executionContextTcbAddress = aSmfStream.getByteBuffer(4);
        m_executionContextTcbToken = aSmfStream.getByteBuffer(16);
        m_dispatchTaskId = aSmfStream.getByteBuffer(8);
        m_dispatchTcbAddress = aSmfStream.getByteBuffer(4);
        m_dispatchTcbToken = aSmfStream.getByteBuffer(16);
        m_executionContextEnclaveToken = aSmfStream.getByteBuffer(8);
        m_dispatchEnclaveToken = aSmfStream.getByteBuffer(8);
        m_enclaveTranClass = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_flags = aSmfStream.getByteBuffer(4);
        m_enclaveCpuSoFar = aSmfStream.getLong();
        m_enclaveZaapCpuSoFar = aSmfStream.getLong();
        m_enclaveZaapEligibleOnCp = aSmfStream.getLong();
        m_enclaveZiipOnCPSoFar = aSmfStream.getLong();
        m_enclaveZiipQualTimeSoFar = aSmfStream.getLong();
        m_enclaveZiipCpuSoFar = aSmfStream.getLong();
        m_zAAPNormalizationFactor = aSmfStream.getInteger(4);
        m_reserved2 = aSmfStream.getByteBuffer(4);
        m_dispatchTcbCpu = aSmfStream.getLong();
        m_dispatchCpuOffloadNonStd = aSmfStream.getLong();
        m_workClassNameLength = aSmfStream.getInteger(4);
        m_workClassName = aSmfStream.getString(128, SmfUtil.EBCDIC);
        m_workMgrNameLength = aSmfStream.getInteger(4);
        m_workMgrName = aSmfStream.getString(128, SmfUtil.EBCDIC);
        m_identityLength = aSmfStream.getInteger(4);
        m_identity = aSmfStream.getString(64, SmfUtil.EBCDIC);
        m_reserved3 = aSmfStream.getByteBuffer(16);

    }

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;
    }

    //----------------------------------------------------------------------------
    /**
     * Dumps the fields of this object to a print stream.
     * 
     * @param aPrintStream   The stream to print to.
     * @param aTripletNumber The triplet number of this AsyncWorkDataSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "AsyncWorkDataSection");
        aPrintStream.push();

        aPrintStream.printlnKeyValue("Version                         ", m_Version);
        aPrintStream.printlnKeyValue("Time execution context created  ", m_timeExecutionContextCreated, null);
        aPrintStream.printlnKeyValue("Execution start time            ", m_executionStartTime, null);
        aPrintStream.printlnKeyValue("Execution completion time       ", m_executionCompleteTime, null);
        aPrintStream.printlnKeyValue("Servant PID                     ", m_servantPID);
        aPrintStream.printlnKeyValue("Servant Job Name                ", m_servantJobName);
        aPrintStream.printlnKeyValue("Servant Job Id                  ", m_servantJobId);
        aPrintStream.printlnKeyValue("Servant Stoken                  ", m_servantStoken, null);
        aPrintStream.printlnKeyValue("Servant ASID (HEX)              ", m_servantAsid, null);
        aPrintStream.printlnKeyValue("Reserved1                       ", m_reserved1, null);
        aPrintStream.printlnKeyValue("Execution context task id       ", m_executionContextTaskId, null);
        aPrintStream.printlnKeyValue("Execution context TCB address   ", m_executionContextTcbAddress, null);
        aPrintStream.printlnKeyValue("Execution context TCB token     ", m_executionContextTcbToken, null);
        aPrintStream.printlnKeyValue("Dispatch task Id                ", m_dispatchTaskId, null);
        aPrintStream.printlnKeyValue("Dispatch TCB address            ", m_dispatchTcbAddress, null);
        aPrintStream.printlnKeyValue("Dispatch TCB token              ", m_dispatchTcbToken, null);
        aPrintStream.printlnKeyValue("Execution context enclave token ", m_executionContextEnclaveToken, null);
        aPrintStream.printlnKeyValue("Dispatch enclave token          ", m_dispatchEnclaveToken, null);
        aPrintStream.printlnKeyValue("Enclave transaction class       ", m_enclaveTranClass);

        /*
         * // Get the defined bit flags in the first byte and convert to int preserving the first 8 bits.
         * // Currently only the first 2 high-order bits in the first byte are defined in V8. The other
         * // 30 remaining bits are reserved for future flags.
         */
        int firstByte = (0x000000FF & (m_flags[0]));
        int booleanValue;
        booleanValue = ((firstByte & m_flags_enclaveJoinedCreatedMask) == m_flags_enclaveJoinedCreatedMask) ? 1 : 0;
        aPrintStream.printlnKeyValue("Enclave joined or created       ", booleanValue);
        booleanValue = ((firstByte & m_flags_schedWithIsDaemonMask) == m_flags_schedWithIsDaemonMask) ? 1 : 0;
        aPrintStream.printlnKeyValue("Scheduled with isDaemon true    ", booleanValue);

        aPrintStream.printlnKeyValue("Enclave CPU so far              ", m_enclaveCpuSoFar);
        aPrintStream.printlnKeyValue("Enclave zAAP CPU So Far         ", m_enclaveZaapCpuSoFar);
        aPrintStream.printlnKeyValue("Enclave zAAP eligible on CP     ", m_enclaveZaapEligibleOnCp);
        aPrintStream.printlnKeyValue("zIIP on CP so far               ", m_enclaveZiipOnCPSoFar);
        aPrintStream.printlnKeyValue("Enclave zIIP qual time so far   ", m_enclaveZiipQualTimeSoFar);
        aPrintStream.printlnKeyValue("Enclave zIIP CPU so far         ", m_enclaveZiipCpuSoFar);
        aPrintStream.printlnKeyValue("zAAP normalization factor       ", m_zAAPNormalizationFactor);
        aPrintStream.printlnKeyValue("Reserved2                       ", m_reserved2, null);
        aPrintStream.printlnKeyValue("Dispatch TCB CPU                ", m_dispatchTcbCpu);
        aPrintStream.printlnKeyValue("Dispatch CPU Offload Non-Std    ", m_dispatchCpuOffloadNonStd);
        aPrintStream.printlnKeyValue("Work class name length          ", m_workClassNameLength);
        aPrintStream.printlnKeyValue("Work class name                 ", m_workClassName);
        aPrintStream.printlnKeyValue("Work manager name length        ", m_workMgrNameLength);
        aPrintStream.printlnKeyValue("Work manager name               ", m_workMgrName);
        aPrintStream.printlnKeyValue("Identity length                 ", m_identityLength);
        aPrintStream.printlnKeyValue("Identity                        ", m_identity);
        aPrintStream.printlnKeyValue("Reserved3                       ", m_reserved3, null);

        aPrintStream.pop();

        return;
    }

} // AsyncWorkDataSection