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
/** Data container for SMF activity data related to a WebApplication. */
public class WebApplicationActivitySection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Name of the WebApplication. */
    public String m_name;

    /** Number of loaded servlets. */
    public int m_servletN;

    /** Triplets associated with this set of servlets. */
    public Triplet[] m_servletTriplets;

    /** Servlet activity sections loaded with this WebApplication. */
    public ServletActivitySection[] m_servletActivitySections;

    /**
     * WebApplicationActivitySection constructor from Smf stream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of BeanSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public WebApplicationActivitySection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_name = aSmfStream.getString(256, SmfUtil.UNICODE);

        m_servletN = aSmfStream.getInteger(4);

        m_servletTriplets = new Triplet[m_servletN];
        for (int sX = 0; sX < m_servletN; ++sX) {
            m_servletTriplets[sX] = new Triplet(aSmfStream);
        }

        m_servletActivitySections = new ServletActivitySection[m_servletN];
        for (int sX = 0; sX < m_servletN; ++sX) {
            m_servletActivitySections[sX] = new ServletActivitySection(aSmfStream, aRequestedVersion);
        }

    } // WebApplicationActivitySection(...)

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
     * Dumps the object to a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of WebApplicationActivitySection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "WebApplicationActivitySection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Name", m_name);

        aPrintStream.println("");
        aPrintStream.printlnKeyValue("# Servlets", m_servletN);

        // Servlet triplets and methods
        for (int sX = 0; sX < m_servletN; ++sX) {
            m_servletTriplets[sX].dump(aPrintStream, aTripletNumber, sX + 1); // @L2C
        } // for

        for (int sX = 0; sX < m_servletN; ++sX) {
            m_servletActivitySections[sX].dump(aPrintStream, aTripletNumber, sX + 1); // @L2C
        } // for

        aPrintStream.pop();

        // Write WebApplication Name out to summaryReport file.              //@SUa   

        if (aTripletNumber > 3) {
            PerformanceSummary.writeNewLine(); //@SUa
            PerformanceSummary.writeString("  ", 29); //@SUa PAD
        }
        PerformanceSummary.writeString(" ", 1); //@SUa  
        PerformanceSummary.writeString(m_name, m_name.length()); //@SUa

    } // dump()

} // WebApplicationActivitySection
