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
public class PlatformNeutralSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;

    /** version of section. */
    public int m_version;

    /** The cell short name within the interpreted record. */
    public String m_cellShortName;

    /** The node short name within the interpreted record. */
    public String m_nodeShortName;

    /** The cluster short name within the interpreted record. */
    public String m_clusterShortName;

    /** The server short name within the interpreted record. */
    public String m_serverShortName;

    /** The Server/Controller PID within the interpreted record. */
    public int m_serverControllerPid;

    /** The WAS Release (e.g. V7) within the interpreted record. */
    public int m_wasRelease;

    /** The WAS Release x of .x.y.z within the interpreted record.. */
    public int m_wasReleaseX;

    /** The WAS Release y of .x.y.z within the interpreted record.. */
    public int m_wasReleaseY;

    /** The WAS Release z of .x.y.z within the interpreted record.. */
    public int m_wasReleaseZ;

    /** Reserved area within the interpreted record for future use. */
    public byte m_theblob[];

    //----------------------------------------------------------------------------
    /**
     * PlatformNeutralSection constructor from a SmfStream.
     *
     * @param aSmfStream SmfStream to be used to build this PlatformNeutralSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public PlatformNeutralSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_version = aSmfStream.getInteger(4);

        m_cellShortName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_nodeShortName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_clusterShortName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_serverShortName = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_serverControllerPid = aSmfStream.getInteger(4);

        m_wasRelease = aSmfStream.getInteger(1);

        m_wasReleaseX = aSmfStream.getInteger(1);

        m_wasReleaseY = aSmfStream.getInteger(1);

        m_wasReleaseZ = aSmfStream.getInteger(1);

        m_theblob = aSmfStream.getByteBuffer(32); // @TJ, was 28, fixed now?

    } // PlatformNeutralSection(..)

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
     * @param aTripletNumber The triplet number of this PlatformNeutralSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "PlatformNeutralSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Server Info Version    ", m_version);
        aPrintStream.printlnKeyValue("Cell Short Name        ", m_cellShortName);
        aPrintStream.printlnKeyValue("Node Short Name        ", m_nodeShortName);
        aPrintStream.printlnKeyValue("Cluster Short Name     ", m_clusterShortName);
        aPrintStream.printlnKeyValue("Server Short Name      ", m_serverShortName);
        aPrintStream.printlnKeyValue("Server/Controller PID  ", m_serverControllerPid);
        aPrintStream.printlnKeyValue("WAS Release            ", m_wasRelease);
        aPrintStream.printlnKeyValue("WAS Release x of .x.y.z", m_wasReleaseX);
        aPrintStream.printlnKeyValue("WAS Release y of .x.y.z", m_wasReleaseY);
        aPrintStream.printlnKeyValue("WAS Release z of .x.y.z", m_wasReleaseZ);
        aPrintStream.printlnKeyValue("Reserved               ", m_theblob, null);

        aPrintStream.pop();

    } // dump()

} // PlatformNeutralSection
