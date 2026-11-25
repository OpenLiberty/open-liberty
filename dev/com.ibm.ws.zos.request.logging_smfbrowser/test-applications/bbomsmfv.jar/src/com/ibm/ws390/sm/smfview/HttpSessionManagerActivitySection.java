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
/** Data container for SMF data related to a web container activity. */
public class HttpSessionManagerActivitySection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Number of http sessions that were created. */
    public int m_httpSessionCreatedN;

    /** Number of http sessions that were invalidated. */
    public int m_httpSessionInvalidatedN;

    /** Number of http sessions that are active at activity end. */
    public int m_httpSessionActiveN;

    /** Average session lifetime. */
    public int m_averageSessionLifeTime;

    //----------------------------------------------------------------------------
    /**
     * HttpSessionManagerActivitySection constructor from SmfStream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of HpptSessionManagerActivitySection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public HttpSessionManagerActivitySection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_httpSessionCreatedN = aSmfStream.getInteger(4);
        m_httpSessionInvalidatedN = aSmfStream.getInteger(4);
        m_httpSessionActiveN = aSmfStream.getInteger(4);
        m_averageSessionLifeTime = aSmfStream.getInteger(4);

    } // WebContainerActivitySection(...)

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
     *                           where this instance of HttpSessionManagerActivitySection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "HttpSessionManagerActivitySection");

        aPrintStream.push();

        aPrintStream.printKeyValue("# http sessions created", m_httpSessionCreatedN);
        aPrintStream.printKeyValue("# http sessions invalidated", m_httpSessionInvalidatedN);
        aPrintStream.printKeyValue("# http sessions active", m_httpSessionActiveN);
        aPrintStream.printlnTimeMills("Average session life time", m_averageSessionLifeTime);

        aPrintStream.pop();

    } // dump()

} // HttpSessionManagerActivitySection
