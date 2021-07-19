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
public class HttpSessionManagerIntervalSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Number of http sessions that were created. */
    public int m_httpSessionCreatedN;

    /** Number of http sessions that were invalidated. */
    public int m_httpSessionInvalidatedN;

    /** Number of http sessions that are active at interval end. */
    public int m_httpSessionActiveN;

    /** Minimum number of http sessions that were active. */
    public int m_minHttpSessionActiveN;

    /** Maximum number of http sessions that were active. */
    public int m_maxHttpSessionActiveN;

    /** Average session life time. */
    public int m_averageSessionLifeTime;

    /** Average session invalidation time. */
    public int m_averageSessionInvalidateTime;

    /** Number of http sessions that were finalized. */
    public int m_httpSessionFinalizedN;

    /** Number of http sessions that were tracked. */
    public int m_httpSessionTotalN;

    /** Minimum number of http sessions that were tracked. */
    public int m_minHttpSessionN;

    /** Maximum number of http sessions that were tracked. */
    public int m_maxHttpSessionN;

    //----------------------------------------------------------------------------
    /**
     * HttpSessionManagerIntervalSection constructor from SmfStream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of HpptSessionManagerIntervalSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public HttpSessionManagerIntervalSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_httpSessionCreatedN = aSmfStream.getInteger(4);
        m_httpSessionInvalidatedN = aSmfStream.getInteger(4);
        m_httpSessionActiveN = aSmfStream.getInteger(4);
        m_minHttpSessionActiveN = aSmfStream.getInteger(4);
        m_maxHttpSessionActiveN = aSmfStream.getInteger(4);
        m_averageSessionLifeTime = aSmfStream.getInteger(4);
        m_averageSessionInvalidateTime = aSmfStream.getInteger(4);
        m_httpSessionFinalizedN = aSmfStream.getInteger(4);
        m_httpSessionTotalN = aSmfStream.getInteger(4);
        m_minHttpSessionN = aSmfStream.getInteger(4);
        m_maxHttpSessionN = aSmfStream.getInteger(4);

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
     *                           where this instance of HttpSessionManagerIntervalSection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "HttpSessionManagerIntervalSection");

        aPrintStream.push();

        aPrintStream.printKeyValue("http sessions #created", m_httpSessionCreatedN);
        aPrintStream.printlnKeyValue("#invalidated", m_httpSessionInvalidatedN);
        aPrintStream.printKeyValue("http sessions #active", m_httpSessionActiveN);
        aPrintStream.printKeyValue("Min #active", m_minHttpSessionActiveN);
        aPrintStream.printlnKeyValue("Max #active", m_maxHttpSessionActiveN);
        aPrintStream.printlnKeyValue("Average session life time", m_averageSessionLifeTime);
        aPrintStream.printlnKeyValue("Average session invalidate time", m_averageSessionInvalidateTime);
        aPrintStream.printKeyValue("http sessions #finalized", m_httpSessionFinalizedN);
        aPrintStream.printlnKeyValue("#tracked", m_httpSessionTotalN);
        aPrintStream.printKeyValue("http sessions #min live", m_minHttpSessionN);
        aPrintStream.printlnKeyValue("#max live", m_maxHttpSessionN);

        aPrintStream.pop();

    } // dump()

} // WebContainerIntervalSection
