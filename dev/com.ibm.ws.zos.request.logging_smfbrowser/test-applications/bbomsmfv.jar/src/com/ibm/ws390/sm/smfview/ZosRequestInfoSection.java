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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

//import java.nio.ByteBuffer;             //@SU99

//  ------------------------------------------------------------------------------
/** Data container for SMF data related to a Smf record product section. */
public class ZosRequestInfoSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;

    /** version of section. */
    public int m_version;
    /** timestamp when request was received in the server */
    public byte m_received[];
    /** timestamp when request was placed on the queue for a servant to pick up */
    public byte m_queued[];
    /** timestamp when request was dispatched in a servant */
    public byte m_dispatched[];
    /** timestamp when dispatch in a servant completed */
    public byte m_dispatchcomplete[];
    /** timestamp when controller processing for the request completed (e.g. response sent) */
    public byte m_complete[];
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
    /** CPU time on non-standard processors (e.g. zAAP/zIIP) */
    public long m_dispatchServantCpuOffload;
    /** Enclave token for the request */
    public byte m_dispatchServantEnclaveToken[];
    /** CPU time for the enclave */
    public long m_dispatchServantEnclaveCpu;
    /** zAAP CPU time for the enclave */
    public long m_dispatchServantZaapCpu;
    /** zAAP eligible time for the enclave which ran on regular GPs */
    public long m_dispatchServantzAAPEligibleonCP;
    /** reserved */
    public byte m_reservedAlignment2[];
    /** zIIP eligible time for the enclave which ran on regular GPs */
    public long m_dispatchServantzIIPonCPUsofar;
    /** zIIP qualified time */
    public long m_dispatchServantzIIPQualTimeSoFar;
    /** zIIP CPU time for the enclave */
    public long m_dispatchServantzIIPCPUSoFar;
    /** WLM zAAP normalization factor */
    public int m_dispatchServantzAAPNormalizationFactor;
    /** CPU time for the enclave at enclave delete */
    public long m_EnclaveDeleteCPU;
    /** zAAP CPU time for the enclave at enclave delete */
    public long m_EnclaveDeletezAAPCPU;
    /** WLM zAAP normalization factor */
    public int m_EnclaveDeletezAAPNorm;
    /** reserved */
    public byte m_reservedAlignment3[];
    /** zIIP CPU time normalized */
    public long m_EnclaveDeletezIIPCPUNormalized;
    /** zIIP CPU usage in service units */
    public long m_EnclaveDeletezIIPService;
    /** zAAP CPU usage in service units */
    public long m_EnclaveDeletezAAPService;
    /** CPU usage in service units */
    public long m_EnclaveDeleteCpuService;
    /** WLM enclave response time ration */
    public int m_EnclaveDeleteRespTimeRatio;
    /** reserved */
    public byte m_reservedAlignment4[];
    /** global transaction id */
    public byte m_gtid[];
    /** reserved */
    public byte m_reservedAlignment5[];
    /** dispatch timeout value used */
    public int m_dispatchTimeout;
    /** WLM transaction class used for classification */
    public String m_tranClass;
    /** flags */
    public byte m_flags[];

    //static int m_flags_create_enclave_mask               = 0x40000000;
    //static int m_flags_timeout_odr                       = 0x20000000;
    //static int m_flags_tran_class_odr_mask               = 0x10000000;
    //static int m_flags_one_way_mask                      = 0x8000000;
    //static int m_flags_cpu_usage_overflow_mask           = 0x4000000;
    //static int m_flags_request_queued_with_affinity_mask = 0x2000000;
    //static int m_flags_CEEGMTO_not_available             = 0x1000000;
    //static int m_flags_reserved                          = 0x3FFFFFF; //26 bits remaining

    /** Stalled Thread dump action */
    public int m_stalled_thread_dump_action; //@v8A
    /** CPU Time Used Dump Action */
    public int m_cputimeused_dump_action; //@v8A
    /** DPM Dump Action */
    public int m_dpm_dump_action; //@v8A
    /** Timeout Recovery */
    public int m_timeout_recovery; //@v8A
    /** dispatch timeout */
    public int m_dispatch_timeout; //@v8A
    /** Queue Timeout Percent */
    public int m_queue_timeout; //@v8A
    /** Request Timeout */
    public int m_request_timeout; //@v8A
    /** CPU Time Used Limit */
    public int m_cputimeused_limit; //@v8A
    /** DPM Interval */
    public int m_dpm_interval; //@v8A
    /** Message Tag */
    public String m_message_tag; //@v8A
    /** Obtained Affinity length */
    public int m_obtainedAffinityLength; //@v8A
    /** Obtained Affinity length */
    public byte m_obtainedAffinity[]; //@v8A
    /** Routing Affinity length */
    public int m_routingAffinityLength; //@v8A
    /** Routing Affinity length */
    public byte m_routingAffinity[]; //@v8A

    /** reserved */
    public byte m_reserved[];

    //----------------------------------------------------------------------------
    /**
     * ZosRequestInfoSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this ZosRequestInfoSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public ZosRequestInfoSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        m_version = aSmfStream.getInteger(4);

        m_received = aSmfStream.getByteBuffer(16);
        m_queued = aSmfStream.getByteBuffer(16);
        m_dispatched = aSmfStream.getByteBuffer(16);
        m_dispatchcomplete = aSmfStream.getByteBuffer(16);
        m_complete = aSmfStream.getByteBuffer(16);

        m_dispatchServantJobname = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_dispatchServantJobId = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_dispatchServantStoken = aSmfStream.getByteBuffer(8);
        m_dispatchServantAsid = aSmfStream.getByteBuffer(2);
        m_reservedAlignment1 = aSmfStream.getByteBuffer(2);
        m_dispatchServantTcbAddress = aSmfStream.getByteBuffer(4);
        m_dispatchServantTtoken = aSmfStream.getByteBuffer(16);
        m_dispatchServantCpuOffload = aSmfStream.getLong();
        m_dispatchServantEnclaveToken = aSmfStream.getByteBuffer(8);
        m_reservedAlignment2 = aSmfStream.getByteBuffer(32);

        m_dispatchServantEnclaveCpu = aSmfStream.getLong();
        m_dispatchServantZaapCpu = aSmfStream.getLong();
        m_dispatchServantzAAPEligibleonCP = aSmfStream.getLong();
        m_dispatchServantzIIPonCPUsofar = aSmfStream.getLong();
        m_dispatchServantzIIPQualTimeSoFar = aSmfStream.getLong();
        m_dispatchServantzIIPCPUSoFar = aSmfStream.getLong();
        m_dispatchServantzAAPNormalizationFactor = aSmfStream.getInteger(4);
        m_EnclaveDeleteCPU = aSmfStream.getLong();
        m_EnclaveDeletezAAPCPU = aSmfStream.getLong();
        m_EnclaveDeletezAAPNorm = aSmfStream.getInteger(4);
        m_reservedAlignment3 = aSmfStream.getByteBuffer(4);
        m_EnclaveDeletezIIPCPUNormalized = aSmfStream.getLong();
        m_EnclaveDeletezIIPService = aSmfStream.getLong();
        m_EnclaveDeletezAAPService = aSmfStream.getLong();
        m_EnclaveDeleteCpuService = aSmfStream.getLong();
        m_EnclaveDeleteRespTimeRatio = aSmfStream.getInteger(4);
        m_reservedAlignment4 = aSmfStream.getByteBuffer(12);

        m_gtid = aSmfStream.getByteBuffer(73);
        m_reservedAlignment5 = aSmfStream.getByteBuffer(3);

        m_dispatchTimeout = aSmfStream.getInteger(4);
        m_tranClass = aSmfStream.getString(8, SmfUtil.EBCDIC);
        m_flags = aSmfStream.getByteBuffer(4);
        //If breaking out individual flags, will just mask m_flags in dump method

        m_reserved = aSmfStream.getByteBuffer(32);
        if (m_version >= 2) //13@v8A    
        {
            m_stalled_thread_dump_action = aSmfStream.getInteger(4);
            m_cputimeused_dump_action = aSmfStream.getInteger(4);
            m_dpm_dump_action = aSmfStream.getInteger(4);
            m_timeout_recovery = aSmfStream.getInteger(4);
            m_dispatch_timeout = aSmfStream.getInteger(4);
            m_queue_timeout = aSmfStream.getInteger(4);
            m_request_timeout = aSmfStream.getInteger(4);
            m_cputimeused_limit = aSmfStream.getInteger(4);
            m_dpm_interval = aSmfStream.getInteger(4);
            m_message_tag = aSmfStream.getString(8, SmfUtil.EBCDIC);
            m_obtainedAffinityLength = aSmfStream.getInteger(4);
            // Changed the affinity tokens from Strings to byte-arrays since they aren't really printable (just a hex value)
            m_obtainedAffinity = aSmfStream.getByteBuffer(128);
            m_routingAffinityLength = aSmfStream.getInteger(4);
            m_routingAffinity = aSmfStream.getByteBuffer(128);
        }
    } // ZosRequestInfoSection(..)

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
     * @param aTripletNumber The triplet number of this ZosRequestInfoSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ZosRequestInfoSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Server Info Version            ", m_version);

        aPrintStream.printlnKeyValue("Time Received                  ", m_received, null);
        aPrintStream.printlnKeyValue("Time Queued                    ", m_queued, null);
        aPrintStream.printlnKeyValue("Time Dispatched                ", m_dispatched, null);
        aPrintStream.printlnKeyValue("Time Dispatch Complete         ", m_dispatchcomplete, null);
        aPrintStream.printlnKeyValue("Time Complete                  ", m_complete, null);

        aPrintStream.printlnKeyValue("Servant Job Name               ", m_dispatchServantJobname);
        aPrintStream.printlnKeyValue("Servant Job ID                 ", m_dispatchServantJobId);
        aPrintStream.printlnKeyValue("Servant SToken                 ", m_dispatchServantStoken, null);
        aPrintStream.printlnKeyValue("Servant ASID (HEX)             ", m_dispatchServantAsid, null);
        aPrintStream.printlnKeyValue("Reserved for alignment         ", m_reservedAlignment1, null);
        aPrintStream.printlnKeyValue("Servant Tcb Address            ", m_dispatchServantTcbAddress, null);
        aPrintStream.printlnKeyValue("Servant TToken                 ", m_dispatchServantTtoken, null);
        aPrintStream.printlnKeyValue("CPU Offload                    ", m_dispatchServantCpuOffload);
        aPrintStream.printlnKeyValue("Servant Enclave Token          ", m_dispatchServantEnclaveToken, null);
        aPrintStream.printlnKeyValue("Reserved                       ", m_reservedAlignment2, null);

        aPrintStream.printlnKeyValue("Enclave CPU So Far             ", m_dispatchServantEnclaveCpu);
        aPrintStream.printlnKeyValue("zAAP CPU So Far                ", m_dispatchServantZaapCpu); //@SU99
        aPrintStream.printlnKeyValue("zAAP Eligible on CP            ", m_dispatchServantzAAPEligibleonCP); //@SU99
        aPrintStream.printlnKeyValue("zIIP on CPU So Far             ", m_dispatchServantzIIPonCPUsofar);
        aPrintStream.printlnKeyValue("zIIP Qual Time So Far          ", m_dispatchServantzIIPQualTimeSoFar);
        aPrintStream.printlnKeyValue("zIIP CPU So Far                ", m_dispatchServantzIIPCPUSoFar);
        aPrintStream.printlnKeyValue("zAAP Normalization Factor      ", m_dispatchServantzAAPNormalizationFactor);
        aPrintStream.printlnKeyValue("Enclave Delete CPU             ", m_EnclaveDeleteCPU);
        aPrintStream.printlnKeyValue("Enclave Delete zAAP CPU        ", m_EnclaveDeletezAAPCPU);
        aPrintStream.printlnKeyValue("Enclave Delete zAAP Norm       ", m_EnclaveDeletezAAPNorm);
        aPrintStream.printlnKeyValue("Reserved                       ", m_reservedAlignment3, null);
        aPrintStream.printlnKeyValue("Enclave Delete zIIP Norm       ", m_EnclaveDeletezIIPCPUNormalized);
        aPrintStream.printlnKeyValue("Enclave Delete zIIP Service    ", m_EnclaveDeletezIIPService);
        aPrintStream.printlnKeyValue("Enclave Delete zAAP Service    ", m_EnclaveDeletezAAPService);
        aPrintStream.printlnKeyValue("Enclave Delete CPU  Service    ", m_EnclaveDeleteCpuService);
        aPrintStream.printlnKeyValue("Enclave Delete Resp Time Ratio ", m_EnclaveDeleteRespTimeRatio);
        aPrintStream.printlnKeyValue("Reserved for alignment         ", m_reservedAlignment4, null);

        aPrintStream.printlnKeyValue("GTID                           ", m_gtid, null);
        aPrintStream.printlnKeyValue("Reserved for alignment         ", m_reservedAlignment5, null);

        aPrintStream.printlnKeyValue("Dispatch Timeout               ", m_dispatchTimeout);
        aPrintStream.printlnKeyValue("Transaction Class              ", m_tranClass);
        aPrintStream.printlnKeyValue("Flags                          ", m_flags, null);
        // Break out flags here?
        //aPrintStream.printlnKeyValue("- Created Enclave              ",(m_flags & m_flags_create_enclave_mask));
        //aPrintStream.printlnKeyValue("Timeout from ODR               ",(m_flags & m_flags_timeout_odr));
        //aPrintStream.printlnKeyValue("Tran Class from ODR            ",(m_flags & m_flags_tran_class_odr_mask));
        //aPrintStream.printlnKeyValue("One-way                        ",(m_flags & m_flags_one_way_mask));
        //aPrintStream.printlnKeyValue("CPU Usage Overflow             ",(m_flags & m_flags_cpu_usage_overflow_mask));
        //aPrintStream.printlnKeyValue("Request Queued w/Affinity      ",(m_flags & m_flags_request_queued_with_affinity_mask));
        //aPrintStream.printlnKeyValue("CEEGMTO not available          ",(m_flags & m_flags_CEEGMTO_not_available));
        //aPringStream.printlnKeyValue("Reserved                       ",(m_flags & m_flags_reserved));

        aPrintStream.printlnKeyValue("Reserved                       ", m_reserved, null);

        if (m_version >= 2) // 14@v8A
        {
            aPrintStream.printlnKeyValue("Classification attributes", "");
            aPrintStream.printlnKeyValue("Stalled thread dump action     ", m_stalled_thread_dump_action);
            aPrintStream.printlnKeyValue("CPU time used dump action      ", m_cputimeused_dump_action);
            aPrintStream.printlnKeyValue("DPM dump action                ", m_dpm_dump_action);
            aPrintStream.printlnKeyValue("Timeout recovery               ", m_timeout_recovery);
            aPrintStream.printlnKeyValue("Dispatch timeout               ", m_dispatch_timeout);
            aPrintStream.printlnKeyValue("Queue timeout                  ", m_queue_timeout);
            aPrintStream.printlnKeyValue("Request timeout                ", m_request_timeout);
            aPrintStream.printlnKeyValue("CPU time used limit            ", m_cputimeused_limit);
            aPrintStream.printlnKeyValue("DPM interval                   ", m_dpm_interval);
            aPrintStream.printlnKeyValue("Message Tag                    ", m_message_tag);
            aPrintStream.printlnKeyValue("Obtained affinity              ", m_obtainedAffinity, null);
            aPrintStream.printlnKeyValue("Routing affinity               ", m_routingAffinity, null);
        }

        aPrintStream.pop();

        // Write Elapsed Time, TranClass, CPU Times                             //@SU99 
        // This is a Java 1.4.2+ way, for reference
        /*
         * ByteBuffer m_queuedBB = ByteBuffer.wrap(m_queued);
         * long m_queuedAsLong = m_queuedBB.getLong();
         * ByteBuffer m_completeBB = ByteBuffer.wrap(m_complete);
         * long m_completeAsLong = m_completeBB.getLong();
         */
        // To be Java 1.3.1 compatable, can't use ByteBuffers...gotta do it the "old" way
        long m_queuedAsLong = 0;
        long m_completeAsLong = 0;
        try {
            // Use m_queued (m_received is often a stale TOD stamp) ...               @SU99
            ByteArrayInputStream bis = new ByteArrayInputStream(m_queued);
            DataInputStream dis = new DataInputStream(bis);
            m_queuedAsLong = dis.readLong();
            dis.close();

            bis = new ByteArrayInputStream(m_complete);
            dis = new DataInputStream(bis);
            m_completeAsLong = dis.readLong();
            dis.close();
        } catch (EOFException eofe) {
            // Likely Cause: Failed to close the DataInputStream
            // Result: GC will clean it up for us
            // Write message to the Perf Summary area and move on
            PerformanceSummary.writeString("Exception trying to read time queued or complete as a long" + eofe, 1);
        } catch (IOException ioe) {
            // Likely Cause: Hit end of DataInputStream before had enough data for a long
            // Result: Values/calculations may be wrong in summary
            // Write message to the Perf Summary area and move on
            PerformanceSummary.writeString("Exception trying to close DataInputStream of time queued or complete" + ioe, 1);
        }

        long m_queuedUsec = m_queuedAsLong >>> 12; // Shift STCKE right 12 bits @SU99
        long m_completeUsec = m_completeAsLong >>> 12; // to get Microseconds  @SU99
        long m_elapsedUsec = m_completeUsec - m_queuedUsec; //@SU99
        long m_elapsedTimeMsec = m_elapsedUsec / 1000; // Millesec = Usec/1000 @SU99

        // Now work with CPU/zAAP/zIIP times which are STCKE shifted left 12 bits:
        long m_dispatchServantEnclaveUsec = m_dispatchServantEnclaveCpu / 4096; //to get Microsec.@SU99

        PerformanceSummary.writeString("/TC=", 1); //@SU99 
        PerformanceSummary.writeString(m_tranClass, 8); //@SU99 
        PerformanceSummary.writeString(" ", 16); //@SU99
        // if (m_elapsedTimeMsec > 1) {
        PerformanceSummary.writeLong(m_elapsedTimeMsec, 6); //@SU99
        // }
        // else { 
        //	  PerformanceSummary.writeString(" 0.", 3);
        //	  PerformanceSummary.writeLong(m_elapsedUsec, 3); 
        // }
        PerformanceSummary.writeLong(m_dispatchServantEnclaveUsec, 10); //@SU9
        // PerformanceSummary.writeString(" ", 1);                             //@SU99 
        PerformanceSummary.writeLong(m_dispatchServantZaapCpu / 4096, 10); //@SU9
        // PerformanceSummary.writeString(" ", 1);                             //@SU9 
        //PerformanceSummary.writeLong(m_dispatchServantzIIPCPUSoFar / 4096, 10);//@SU99

        PerformanceSummary.TotalTranCount = PerformanceSummary.TotalTranCount + 1;
        PerformanceSummary.TotalElapsedTime = PerformanceSummary.TotalElapsedTime + m_elapsedTimeMsec; //@SU99
        PerformanceSummary.TotalElapsedUsec = PerformanceSummary.TotalElapsedUsec + m_elapsedUsec; //@SU99

        PerformanceSummary.TotalCPUTime = PerformanceSummary.TotalCPUTime + m_dispatchServantEnclaveUsec; //@SU99
        PerformanceSummary.TotalZaapTime = PerformanceSummary.TotalZaapTime + (m_dispatchServantZaapCpu / 4096); //@SU99
        PerformanceSummary.TotalZiipTime = PerformanceSummary.TotalZiipTime + (m_dispatchServantzIIPCPUSoFar / 4096); //@SU99

    } // dump()

} // ZosRequestInfoSection
