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
/** Data container for SMF data related to a Container Activity. */
public class ContainerActivitySection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2;

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

    /** Address space id of the associated server region. */
    public int m_serverRegionASID;

    /** Name of the container. */
    public String m_containerName;

    /** Transaction policy customized for the container. */
    public int m_containerTransactionPolicy;

    /** Security policy customized for the container. */
    public int m_containerSecurityPolicy;

    /** Work load manager enclave token associated with the container activity. */
    public byte m_wlmEnclaveToken[];

    /** Activity type of the monitored container activity. */
    public int m_activityType;

    /** Id of the monitored container activity. */
    public byte m_activityID[];

    //----------------------------------------------------------------------------
    /**
     * ContainerActivitySection constructor from SmfStream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of ContainerActivitySection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ContainerActivitySection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_hostName = aSmfStream.getString(64, SmfUtil.EBCDIC);

        m_serverName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_serverInstanceName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_serverRegionASID = aSmfStream.getInteger(4); //    @L2C

        m_containerName = aSmfStream.getString(256, SmfUtil.EBCDIC);

        m_containerTransactionPolicy = aSmfStream.getInteger(4); //    @L2C

        m_containerSecurityPolicy = aSmfStream.getInteger(4); //    @L2C

        m_wlmEnclaveToken = aSmfStream.getByteBuffer(8);

        m_activityType = aSmfStream.getInteger(4); //    @L2C

        m_activityID = aSmfStream.getByteBuffer(20);

    } // ContainerActivitySection.ContainerActivitySection(..)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // ContainerActivitySection.supportedVersion()

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
        } // switch(...)
    } // ContainerActivitySection.activityTypeToString()

    //----------------------------------------------------------------------------
    /**
     * Dumps the object into a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of ContainerActivitySection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ContainerActivity");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("HostName          ", m_hostName);
        aPrintStream.printlnKeyValue("ServerName        ", m_serverName);
        aPrintStream.printlnKeyValue("ServerInstanceName", m_serverInstanceName);
        aPrintStream.printlnKeyValue("ServerRegionASID  ", m_serverRegionASID);
        aPrintStream.printlnKeyValue("ContainerName     ", m_containerName);
        aPrintStream.printlnKeyValue("ContainerTransactionPolicy", m_containerTransactionPolicy);
        aPrintStream.printlnKeyValueString(
                                           "ContainerSecurityPolicy", m_containerSecurityPolicy, SmfSecurityPolicy.policies(m_containerSecurityPolicy));
        aPrintStream.printlnKeyValue("WlmEnclaveToken", m_wlmEnclaveToken, SmfUtil.EBCDIC);
        aPrintStream.printlnKeyValueString(
                                           "ActivityType", m_activityType, activityTypeToString());
        aPrintStream.printlnKeyValue("ActivityID", m_activityID, SmfUtil.EBCDIC);

        aPrintStream.pop();

        // Write Server InstanceName out to summaryReport.                //@SUa   

        PerformanceSummary.writeString(" " + m_serverInstanceName, 9); //@SUa

    } // ContainerActivitySection.show()

} // ContainerActivitySection