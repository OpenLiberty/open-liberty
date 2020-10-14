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
/** Data container for SMF data related to a server interval. */
public class ServerIntervalSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 4; // @LI4369-4C

    /** Length of version two of section */
    public final static int s_secondLength = 308; // @MD20733A

    /** Name of the host related to this container activity. */
    public String m_hostName;

    /** Name of the server related to this container activity. */
    public String m_serverName;

    /** Name of the server instance related to this container activity. */
    public String m_serverInstanceName;

    /** Interval start time. */
    public byte m_sampleStartTime[];

    /** Interval start time. */
    public byte m_sampleStopTime[];

    /** Number of global transactions during interval. */
    public int m_numberOfGlobalTransactions;

    /** Number of local transactions during interval. */
    public int m_numberOfLocalTransactions;

    /** Number of communication sessions at time of record creation. */
    public int m_numberOfCommSessions;

    /** Number of active communication session at time of record creation. */
    public int m_numberOfActiveCommSessions;

    /** Number of local communication session at time of record creation. */
    public int m_numberOfLocalCommSessions;

    /** Number of active local communication session at time of record creation. */
    public int m_numberOfActiveLocalCommSessions;

    /** Number of remote communication session at time of record creation. */
    public int m_numberOfRemoteCommSessions;

    /** Number of active remote communication session at time of record creation. */
    public int m_numberOfActiveRemoteCommSessions;

    /** Number of bytes transferred to server during interval. */
    public int m_totalBytesToServer;

    /** Number of bytes transferred from server during interval. */
    public int m_totalBytesFromServer;

    /** Number of bytes transferred locally to server during interval. */
    public int m_totalBytesLocalToServer;

    /** Number of bytes transferred locally from server during interval. */
    public int m_totalBytesLocalFromServer;

    /** Number of bytes transferred remotely to server during interval. */
    public int m_totalBytesRemoteToServer;

    /** Number of bytes transferred remotely from server during interval. */
    public int m_totalBytesRemoteFromServer;

    /** Server type (MOFW or J2ee). */
    public int m_serverType = SmfUtil.MofwServerType;

    /** Server cell name. */
    public String m_cellName; // @MD17014 A

    /** Server node name. */
    public String m_nodeName; // @MD17014 A

    /** Number of Http Communcations sessions at the end of the interval. */
    public int m_endHttpCommSessions; // @MD17014 A

    /**
     * Number of Http Communication sessions that have been attached and
     * active during the interval.
     */
    public int m_activeHttpCommSessions; // @MD17014 A

    /**
     * Number of SIP Communication sessions that have been attached and
     * active during the interval.
     */
    public int m_activeSipCommSessions; // @LI4369-4A

    /**
     * Number of bytes that have been tranferred to the server from all
     * http attached clients.
     */
    public int m_bytesTransferredIn; // @MD17014 A

    /**
     * NUmber of bytes that have need transferred from the server to all
     * http attached clients.
     */
    public int m_bytesTransferredOut; // @MD17014 A

    // Begin @LI4369-4A
    /**
     * Number of bytes that have been tranferred to the server from all
     * SIP attached clients.
     */
    public int m_SIPbytesTransferredIn;

    /**
     * Number of bytes that have been transferred from the server to all
     * SIP attached clients.
     */
    public int m_SIPbytesTransferredOut;
    // End   @LI4369-4A

    /** Total Wlm enclave CPU time. */
    public long m_wlmEnclaveCpuTime; // @MD17014 A

    /** Number of bytes transferred to server during interval. */
    public long m_totalBytesToServer64; //@MD20733A

    /** Number of bytes transferred from server during interval. */
    public long m_totalBytesFromServer64; //@MD20733A

    /** Number of bytes transferred locally to server during interval. */
    public long m_totalBytesLocalToServer64; //@MD20733A

    /** Number of bytes transferred locally from server during interval. */
    public long m_totalBytesLocalFromServer64; //@MD20733A

    /** Number of bytes transferred remotely to server during interval. */
    public long m_totalBytesRemoteToServer64; //@MD20733A

    /** Number of bytes transferred remotely from server during interval. */
    public long m_totalBytesRemoteFromServer64; //@MD20733A

    /**
     * Number of bytes that have been tranferred to the server from all
     * http attached clients.
     */
    public long m_bytesTransferredIn64; //@MD20733A

    /**
     * NUmber of bytes that have need transferred from the server to all
     * http attached clients.
     */
    public long m_bytesTransferredOut64; // @MD20733A

    // Begin @LI4369-4A
    /**
     * Number of bytes that have been tranferred to the server from all
     * SIP attached clients.
     */
    public long m_SIPbytesTransferredIn64;

    /**
     * Number of bytes that have been transferred from the server to all
     * SIP attached clients.
     */
    public long m_SIPbytesTransferredOut64;
    // End   @LI4369-4A

    /** Length of the section */
    public int m_sectionLength; // @MD20733A

    //----------------------------------------------------------------------------
    /**
     * ServerIntervalSection constructor from SmfStream.
     * The instance is filled from the provided SmfStream.
     *
     * @param aSmfStream        Smf stream to create this instance of ServeIntervalSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @param aSectionLength    length of the section
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ServerIntervalSection(SmfStream aSmfStream, int aRequestedVersion, int aSectionLength) // @MD20733C
        throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_hostName = aSmfStream.getString(64, SmfUtil.EBCDIC);

        m_serverName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_serverInstanceName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_sampleStartTime = aSmfStream.getByteBuffer(16);

        m_sampleStopTime = aSmfStream.getByteBuffer(16);

        m_numberOfGlobalTransactions = aSmfStream.getInteger(4); //    @L2C

        m_numberOfLocalTransactions = aSmfStream.getInteger(4); //    @L2C

        m_numberOfCommSessions = aSmfStream.getInteger(4); //    @L2C

        m_numberOfActiveCommSessions = aSmfStream.getInteger(4); //    @L2C

        m_numberOfLocalCommSessions = aSmfStream.getInteger(4); //    @L2C

        m_numberOfActiveLocalCommSessions = aSmfStream.getInteger(4); //    @L2C

        m_numberOfRemoteCommSessions = aSmfStream.getInteger(4); //    @L2C

        m_numberOfActiveRemoteCommSessions = aSmfStream.getInteger(4); //    @L2C

        m_totalBytesToServer = aSmfStream.getInteger(4); //    @L2C

        m_totalBytesFromServer = aSmfStream.getInteger(4); //    @L2C

        m_totalBytesLocalToServer = aSmfStream.getInteger(4); //    @L2C

        m_totalBytesLocalFromServer = aSmfStream.getInteger(4); //    @L2C

        m_totalBytesRemoteToServer = aSmfStream.getInteger(4); //    @L2C

        m_totalBytesRemoteFromServer = aSmfStream.getInteger(4); //    @L2C

        if (version() >= 2) {

            m_serverType = aSmfStream.getInteger(4); //    @L2C

            if (version() >= 3) { // @MD17014 A

                m_cellName = aSmfStream.getString(8, SmfUtil.EBCDIC); // @MD17014 A

                m_nodeName = aSmfStream.getString(8, SmfUtil.EBCDIC); // @MD17014 A

                m_endHttpCommSessions = aSmfStream.getInteger(4); // @MD17014 A

                m_activeHttpCommSessions = aSmfStream.getInteger(4); // @MD17014 A

                m_bytesTransferredIn = aSmfStream.getInteger(4); // @MD17014 A

                m_bytesTransferredOut = aSmfStream.getInteger(4); // @MD17014 A

                m_wlmEnclaveCpuTime = aSmfStream.getLong() >> 12; // @MD17014 A

                m_sectionLength = aSectionLength; // @MD20733A

                if (s_secondLength == m_sectionLength) { // @MD20733A

                    m_totalBytesToServer64 = aSmfStream.getLong(); // @MD20733A

                    m_totalBytesFromServer64 = aSmfStream.getLong(); // @MD20733A

                    m_totalBytesLocalToServer64 = aSmfStream.getLong(); // @MD20733A

                    m_totalBytesLocalFromServer64 = aSmfStream.getLong(); // @MD20733A

                    m_totalBytesRemoteToServer64 = aSmfStream.getLong(); // @MD20733A

                    m_totalBytesRemoteFromServer64 = aSmfStream.getLong(); // @MD20733A

                    m_bytesTransferredIn64 = aSmfStream.getLong(); // @MD20733A

                    m_bytesTransferredOut64 = aSmfStream.getLong(); // @MD20733A
                    // Begin @LI4369-4A
                    if (version() >= 4) {

                        m_SIPbytesTransferredIn64 = aSmfStream.getLong();

                        m_SIPbytesTransferredOut64 = aSmfStream.getLong();

                        m_activeSipCommSessions = aSmfStream.getInteger(4); // @MD17014 A

                        m_SIPbytesTransferredIn = aSmfStream.getInteger(4);

                        m_SIPbytesTransferredOut = aSmfStream.getInteger(4);
                        //String thegarb = aSmfStream.getString(4,SmfUtil.EBCDIC);
                        aSmfStream.getString(4, SmfUtil.EBCDIC); //==.skip(4)?
                    } else {
                        // End   @LI4369-4A
                        //String thegarb = aSmfStream.getString(32,SmfUtil.EBCDIC);         // @MD20733A
                        aSmfStream.getString(32, SmfUtil.EBCDIC); //==.skip(32)?
                    }
                } // @MD20733A
                if (m_sectionLength > s_secondLength) { // @MD20733A
                    //byte extraStuff[] = aSmfStream.getByteBuffer(m_sectionLength-s_secondLength); // @MD20733A
                    aSmfStream.skip(m_sectionLength - s_secondLength);
                }

            } // if ... version >= 3
        } // if ... version >= 2

    } // ServerIntervalSection(...)

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
     * Dumps the object into a print stream.
     *
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of ContainerIntervalSection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        String serverType = (m_serverType == SmfUtil.J2eeServerType) ? "J2EE Server" : "MOFW Server";

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ServerIntervalSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("HostName          ", m_hostName);
        aPrintStream.printlnKeyValue("ServerName        ", m_serverName);
        aPrintStream.printlnKeyValue("ServerInstanceName", m_serverInstanceName);
        aPrintStream.printlnKeyValue("ServerType        ", serverType);
        // @MD17014 4 A
        if (version() >= 3) {
            aPrintStream.printlnKeyValue("CellName          ", m_cellName);
            aPrintStream.printlnKeyValue("NodeName          ", m_nodeName);
        }

        aPrintStream.printlnKeyValue("SampleStartTime", m_sampleStartTime, null);
        aPrintStream.printlnKeyValue("SampleStopTime", m_sampleStopTime, null);

        aPrintStream.printKeyValue("#GlobalTransactions", m_numberOfGlobalTransactions);
        aPrintStream.printlnKeyValue("#LocalTransactions", m_numberOfLocalTransactions);
        aPrintStream.printlnKeyValue("#Active CS", m_numberOfActiveCommSessions);
        aPrintStream.printlnKeyValue("#ActiveLocal  CS", m_numberOfActiveLocalCommSessions);
        aPrintStream.printlnKeyValue("#ActiveRemote CS", m_numberOfActiveRemoteCommSessions);

        aPrintStream.println("# Bytes transferred 4 byte fields"); // @MD20733C
        aPrintStream.push();
        aPrintStream.printKeyValue("ToServer", m_totalBytesToServer);
        aPrintStream.printlnKeyValue("FromServer", m_totalBytesFromServer);
        aPrintStream.printKeyValue("LocalToServer", m_totalBytesLocalToServer);
        aPrintStream.printlnKeyValue("LocalFromServer", m_totalBytesLocalFromServer);
        aPrintStream.printKeyValue("RemoteToServer", m_totalBytesRemoteToServer);
        aPrintStream.printlnKeyValue("RemoteFromServer", m_totalBytesRemoteFromServer);
        // @MD17014 9 A
        if (version() >= 3) {
            aPrintStream.printlnKeyValue("Transferred to Server from http clients", m_bytesTransferredIn);
            aPrintStream.printlnKeyValue("Transferred from Server to http clients", m_bytesTransferredOut);
            // Begin @LI4369-4A
            if (version() >= 4) {
                aPrintStream.printlnKeyValue("Transferred to Server from SIP clients", m_SIPbytesTransferredIn);
                aPrintStream.printlnKeyValue("Transferred from Server to SIP clients", m_SIPbytesTransferredOut);
            }
            // End   @LI4369-4A
            aPrintStream.pop();
            aPrintStream.printlnKeyValue("#Http Communication Sessions attached and active during interval", m_activeHttpCommSessions);
            // Begin @LI4369-4A
            if (version() >= 4) {
                aPrintStream.printlnKeyValue("#SIP Communication Sessions attached and active during interval", m_activeSipCommSessions);
            }
            // End   @LI4369-4A
            aPrintStream.printlnKeyValue("Total WLM enclave CPU time", m_wlmEnclaveCpuTime);
            if (s_secondLength == m_sectionLength) { // @MD20733A
                aPrintStream.println("# Bytes transferred 8 byte fields"); // @MD20733A
                aPrintStream.printlnKeyValue("  ToServer", m_totalBytesToServer64); // @MD20733A
                aPrintStream.printlnKeyValue("  FromServer", m_totalBytesFromServer64); // @MD20733A
                aPrintStream.printlnKeyValue("  LocalToServer", m_totalBytesLocalToServer64); // @MD20733A
                aPrintStream.printlnKeyValue("  LocalFromServer", m_totalBytesLocalFromServer64); // @MD20733A
                aPrintStream.printlnKeyValue("  RemoteToServer", m_totalBytesRemoteToServer64); // @MD20733A
                aPrintStream.printlnKeyValue("  RemoteFromServer", m_totalBytesRemoteFromServer64); // @MD20733A
                aPrintStream.printlnKeyValue("  Transferred to Server from http clients", m_bytesTransferredIn64); // @MD20733A
                aPrintStream.printlnKeyValue("  Transferred from Server to http clients", m_bytesTransferredOut64); // @MD20733A
                // Begin @LI4369-4A
                if (version() >= 4) {
                    aPrintStream.printlnKeyValue("  Transferred to Server from SIP clients", m_SIPbytesTransferredIn64);
                    aPrintStream.printlnKeyValue("  Transferred from Server to SIP clients", m_SIPbytesTransferredOut64);
                }
                // End   @LI4369-4A
            } // @MD20733A

        } else {
            aPrintStream.pop();
        }

        aPrintStream.pop();

        // Write Server InstanceName out to summaryReport.               //@SUa

        PerformanceSummary.writeString(" " + m_serverInstanceName, 9); //@SUa

        // Write # bytes transfered & received                           //@SUa
        // PerformanceSummary.writeNewLine();                            //@SVa
        // PerformanceSummary.writeString("  ", 20);                     //@SVa
        PerformanceSummary.writeString("             ", 15); //@SUa
        PerformanceSummary.writeInt(m_totalBytesToServer, 9); //@SUa
        PerformanceSummary.writeString(" ", 1); //@SUa
        PerformanceSummary.writeInt(m_totalBytesFromServer, 9); //@SUa
        if ((m_totalBytesToServer + m_totalBytesFromServer) == 0) { //@SVa
            PerformanceSummary.writeString("(no Activity)", 15); //@SUa
            PerformanceSummary.clearBuf();
        } //@SUa
        if (version() >= 3) { //@SW
            if (m_wlmEnclaveCpuTime > 0) { //@SW
                PerformanceSummary.writeString(" ", 12); //@SW
                PerformanceSummary.writeLong(m_wlmEnclaveCpuTime, 10); //@SW
            } // endif m_wlmEnclaveCpuTime >0
        } // endif  version () >= 3
    } // dump(...)

} // ServerIntervalSection