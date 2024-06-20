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
/** Data container for SMF data related to a server activity. */
public class ServerActivitySection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 4; // @LI4369-4C

    /** Method request enum value. */
    public static final int MethodRequest = 1;

    /** Transaction enum value. */
    public static final int Transaction = 2;

    /** Activity group enum value. */
    public static final int ActivityGroup = 3;

    /** Name of the host related to this container activity. */
    public String m_hostName;

    /** Name of the server related to this container activity. */
    public String m_serverName;

    /** Name of the server instance related to this container activity. */
    public String m_serverInstanceName;

    /** Number of associated server regions. */
    public int m_numberOfServerRegions;

    /** Address space id of the 1st associated server region. */
    public int m_serverRegionASID1;

    /** Address space id of the 2nd associated server region. */
    public int m_serverRegionASID2;

    /** Address space id of the 3rd associated server region. */
    public int m_serverRegionASID3;

    /** Address space id of the 4th associated server region. */
    public int m_serverRegionASID4;

    /** Address space id of the 5th associated server region. */
    public int m_serverRegionASID5;

    /** User credentials. */
    public String m_userCredentials;

    /** Activity type of the monitored server activity. */
    public int m_activityType;

    /** Activity id of the monitored server activity. */
    public byte m_activityID[];

    /** Work load manager enclave token associated with the server activity. */
    public byte m_wlmEnclaveToken[];

    /** Activity start time. */
    public byte m_activityStartTime[];

    /** Activity stop time. */
    public byte m_activityStopTime[];

    /** Number of input method for this server activity. */
    public int m_numberOfInputMethods;

    /** Number of global transactions for this server activity. */
    public int m_numberOfGlobalTransactions;

    /** Number of local transactions for this server activity. */
    public int m_numberOfLocalTransactions;

    /** Server type (MOFW or J2ee). */
    public int m_serverType = SmfUtil.MofwServerType;

    /** WebSphere for z/OS transaction server cell name. */
    public String m_cellName; // @MD17014 A              

    /** WebSphere for z/OS transaction server node name. */
    public String m_nodeName; // @MD17014 A

    /** WLM enclave cpu time. */
    public long m_wlmEnclaveCpuTime; // @MD17014 A

    //----------------------------------------------------------------------------
    /**
     * ServerActivitySection constructor from SmfStream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of ServerActivitySection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ServerActivitySection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_hostName = aSmfStream.getString(64, SmfUtil.EBCDIC);

        m_serverName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_serverInstanceName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_numberOfServerRegions = aSmfStream.getInteger(4); //   @L2C

        m_serverRegionASID1 = aSmfStream.getInteger(4); //   @L2C

        m_serverRegionASID2 = aSmfStream.getInteger(4); //   @L2C

        m_serverRegionASID3 = aSmfStream.getInteger(4); //   @L2C

        m_serverRegionASID4 = aSmfStream.getInteger(4); //   @L2C

        m_serverRegionASID5 = aSmfStream.getInteger(4); //   @L2C

        m_userCredentials = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_activityType = aSmfStream.getInteger(4); //   @L2C

        m_activityID = aSmfStream.getByteBuffer(20);

        m_wlmEnclaveToken = aSmfStream.getByteBuffer(8);

        m_activityStartTime = aSmfStream.getByteBuffer(16);

        m_activityStopTime = aSmfStream.getByteBuffer(16);

        m_numberOfInputMethods = aSmfStream.getInteger(4); //   @L2C

        m_numberOfGlobalTransactions = aSmfStream.getInteger(4); //   @L2C

        m_numberOfLocalTransactions = aSmfStream.getInteger(4); //   @L2C

        if (version() >= 2) {
            m_serverType = aSmfStream.getInteger(4); //   @L2C

            if (version() >= 3) { // @MD17014 A

                m_cellName = aSmfStream.getString(8, SmfUtil.EBCDIC); // @MD17014 A

                m_nodeName = aSmfStream.getString(8, SmfUtil.EBCDIC); // @MD17014 A

                m_wlmEnclaveCpuTime = aSmfStream.getLong() >> 12; // @MD17014 A

            } // @MD17014 A

        } // if ... version >= 2

    } // ServerActivitySection(...)

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
     * Returns the activity type as a string.
     * 
     * @return activity type as a string.
     */
    private String activityTypeToString() {
        switch (m_activityType) {
            case MethodRequest:
                return "method request";
            case Transaction:
                return "transaction";
            case ActivityGroup:
                return "activity group";
            default:
                return "unknown activity type";
        }
    } // activityTypeToString()

    //----------------------------------------------------------------------------
    /**
     * Dumps the object into a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of ContainerActivitySection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        String serverType = (m_serverType == SmfUtil.J2eeServerType) ? "J2EE Server" : "MOFW Server";

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ServerActivitySection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("HostName          ", m_hostName);
        aPrintStream.printlnKeyValue("ServerName        ", m_serverName);
        aPrintStream.printlnKeyValue("ServerInstanceName", m_serverInstanceName);
        aPrintStream.printlnKeyValue("ServerType        ", serverType);
        if (version() >= 3) { // @MD17014 A
            aPrintStream.printlnKeyValue("CellName          ", m_cellName); // @MD17014 A
            aPrintStream.printlnKeyValue("NodeName          ", m_nodeName); // @MD17014 A
        } // @MD17014 A
        aPrintStream.printlnKeyValue("#ServerRegions", m_numberOfServerRegions);
        aPrintStream.printKeyValue("ASID1", m_serverRegionASID1);
        aPrintStream.printKeyValue("ASID2", m_serverRegionASID2);
        aPrintStream.printKeyValue("ASID3", m_serverRegionASID3);
        aPrintStream.printKeyValue("ASID4", m_serverRegionASID4);
        aPrintStream.printlnKeyValue("ASID5", m_serverRegionASID5);

        aPrintStream.printlnKeyValue("UserCredentials", m_userCredentials);
        aPrintStream.printlnKeyValueString(
                                           "ActivityType", m_activityType, activityTypeToString());
        aPrintStream.printlnKeyValue("ActivityID", m_activityID, null);
        aPrintStream.printlnKeyValue("WlmEnclaveToken", m_wlmEnclaveToken, null);
        aPrintStream.printlnKeyValue("ActivityStartTime", m_activityStartTime, null);
        aPrintStream.printlnKeyValue("ActivityStopTime", m_activityStopTime, null);
        aPrintStream.printlnKeyValue("#InputMethods      ", m_numberOfInputMethods);
        aPrintStream.printlnKeyValue("#GlobalTransactions", m_numberOfGlobalTransactions);
        aPrintStream.printlnKeyValue("#LocalTransactions ", m_numberOfLocalTransactions);
        if (version() >= 3) { // @MD17014 A
            aPrintStream.printlnKeyValue("WLM enclave CPU time", m_wlmEnclaveCpuTime); // @MD17014 A
        } // @MD17014 A
        aPrintStream.pop();

        //** Write Server InstanceName out if summaryReportSwitch is on.   //@SUa   

        PerformanceSummary.writeString(" " + m_serverInstanceName, 9); //@SUa

        if (version() >= 3) { //@SW
            PerformanceSummary.writeString(" ", 46); //@SW9

            PerformanceSummary.writeLong(m_wlmEnclaveCpuTime, 10); //@SW
            // PerformanceSummary.writeString(" -TotSvr", 8);                //@SU99
            PerformanceSummary.writeString("\n ", 32); //@SW
            ++PerformanceSummary.lineNumber; // Increment Line #  //@SU9
        } //@SUa                                                        // @MD17014 A

    } // dump(...)

} // ServerActivitySection