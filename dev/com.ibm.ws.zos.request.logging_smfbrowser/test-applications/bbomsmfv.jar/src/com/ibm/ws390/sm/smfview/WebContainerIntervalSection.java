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
/** Data container for SMF data related to a web container interval. */
public class WebContainerIntervalSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Name of the transaction host related to this container activity. */
    public String m_hostName;

    /** Name of the server related to this container activity. */
    public String m_serverName;

    /** Name of the server instance related to this container activity. */
    public String m_serverInstanceName;

    /** Interval start time. */
    public byte[] m_sampleStartTime;

    /** Interval stop time. */
    public byte[] m_sampleStopTime;

    /** Cell Name. */
    public String m_cellName; // @MD17014A

    /** Node Name. */
    public String m_nodeName; // @MD17014 A

    //----------------------------------------------------------------------------
    /**
     * WebContainerIntervalSection constructor from SmfStream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of ContainerActivitySection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public WebContainerIntervalSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_hostName = aSmfStream.getString(64, SmfUtil.EBCDIC);

        m_serverName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_serverInstanceName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_sampleStartTime = aSmfStream.getByteBuffer(16);

        m_sampleStopTime = aSmfStream.getByteBuffer(16);

        if (version() >= 2) { // @MD17014 A

            m_cellName = aSmfStream.getString(8, SmfUtil.EBCDIC);// @MD17014 A

            m_nodeName = aSmfStream.getString(8, SmfUtil.EBCDIC);// @MD17014 A

        } // @MD17014 A

    } // WebContainerIntervalSection(...)

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
     *                           where this instance of WebContainerIntervalSection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "WebContainerIntervalSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("HostName          ", m_hostName);
        aPrintStream.printlnKeyValue("ServerName        ", m_serverName);
        aPrintStream.printlnKeyValue("ServerInstanceName", m_serverInstanceName);
        // @MD17014 4 A
        if (version() >= 2) {
            aPrintStream.printlnKeyValue("CellName          ", m_cellName);
            aPrintStream.printlnKeyValue("NodeName          ", m_nodeName);
        }
        aPrintStream.printlnKeyValue("SampleStartTime", m_sampleStartTime, null);
        aPrintStream.printlnKeyValue("SampleStopTime", m_sampleStopTime, null);

        aPrintStream.pop();

        // Write Server InstanceName out to summaryReport.                //@SUa   
        PerformanceSummary.writeString(" " + m_serverInstanceName, 9); //@SUa

    } // dump()

} // WebContainerIntervalSection
