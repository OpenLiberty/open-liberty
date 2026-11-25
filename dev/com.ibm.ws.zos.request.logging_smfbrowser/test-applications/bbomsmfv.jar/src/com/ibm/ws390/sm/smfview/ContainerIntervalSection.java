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
/** Data container for SMF data related to a container interval section. */
public class ContainerIntervalSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2;

    /** Name of the host related to this container activity. */
    public String m_hostName;

    /** Name of the server related to this container activity. */
    public String m_serverName;

    /** Name of the server instance related to this container activity. */
    public String m_serverInstanceName;

    /** Name of the container. */
    public String m_containerName;

    /** Transaction policy customized for the container. */
    public int m_containerTransactionPolicy;

    /** Security policy customized for the container. */
    public int m_containerSecurityPolicy;

    /** Interval start time. */
    public byte m_sampleStartTime[];

    /** Interval stop time. */
    public byte m_sampleStopTime[];

    //----------------------------------------------------------------------------
    /**
     * ContainerIntervalSection constructor from SmfStream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of ContainerIntervalSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ContainerIntervalSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_hostName = aSmfStream.getString(64, SmfUtil.EBCDIC);

        m_serverName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_serverInstanceName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_containerName = aSmfStream.getString(256, SmfUtil.EBCDIC);

        m_containerTransactionPolicy = aSmfStream.getInteger(4); //    @L2C

        m_containerSecurityPolicy = aSmfStream.getInteger(4); //    @L2C

        m_sampleStartTime = aSmfStream.getByteBuffer(16);

        m_sampleStopTime = aSmfStream.getByteBuffer(16);

    } // ContainerIntervalSection.ContainerIntervalSection(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // ContainerIntervalSection.supportedVersion()

    //----------------------------------------------------------------------------
    /**
     * Dumps the object into a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of ContainerIntervalSection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ContainerIntervalSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("HostName          ", m_hostName);
        aPrintStream.printlnKeyValue("ServerName        ", m_serverName);
        aPrintStream.printlnKeyValue("ServerInstanceName", m_serverInstanceName);
        aPrintStream.printlnKeyValue("ContainerName     ", m_containerName);
        aPrintStream.printlnKeyValue("ContainerTransactionPolicy", m_containerTransactionPolicy);
        aPrintStream.printlnKeyValueString(
                                           "ContainerSecurityPolicy", m_containerSecurityPolicy,
                                           SmfSecurityPolicy.policies(m_containerSecurityPolicy));
        aPrintStream.printlnKeyValue("SampleStartTime", m_sampleStartTime, null);
        aPrintStream.printlnKeyValue("SampleStopTime", m_sampleStopTime, null);

        aPrintStream.pop();

        // Write Server InstanceName out to summaryReport.                  //@SUa   

        PerformanceSummary.writeString(" " + m_serverInstanceName, 9); //@SUa

    } // ContainerIntervalSection.dump(...)

} // ContainerIntervalSection