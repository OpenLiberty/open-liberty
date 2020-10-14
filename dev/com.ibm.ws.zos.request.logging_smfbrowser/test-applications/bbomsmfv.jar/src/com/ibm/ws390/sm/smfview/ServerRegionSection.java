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
/** Data container for SMF data related to a Server Region. */
public class ServerRegionSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 4; /* @LI4369-4C */

    /** ASID. */
    public byte m_Asid[];

    /** Number of heap id triplets */
    public int m_nHeapIdSections;

    /** Triplets associated with this set HeapIdSections. */
    public Triplet m_heapIdSectionsTriplets[];

    /** HeapIdSections. */
    public HeapIdSection m_HeapIdSections[];

    //----------------------------------------------------------------------------
    /**
     * ServerRegionSection constructor from Smf stream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of ServerRegionSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ServerRegionSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);
        m_Asid = aSmfStream.getByteBuffer(4);

        m_nHeapIdSections = aSmfStream.getInteger(4); //    @L3C

        m_heapIdSectionsTriplets = new Triplet[m_nHeapIdSections];
        for (int cX = 0; cX < m_nHeapIdSections; ++cX) {
            m_heapIdSectionsTriplets[cX] = new Triplet(aSmfStream);
        }

        m_HeapIdSections = new HeapIdSection[m_nHeapIdSections];
        for (int cX = 0; cX < m_nHeapIdSections; ++cX) {
            m_HeapIdSections[cX] = new HeapIdSection(aSmfStream, aRequestedVersion);
        }

    } // ServerRegionSection.ServerRegionSection(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // ServerRegionSection.supportedVersion()

    //----------------------------------------------------------------------------
    /**
     * Dump the object to a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of ServerRegionSection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.push();

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ServerRegionSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("ASID", m_Asid, null);
        aPrintStream.println("");
        aPrintStream.printlnKeyValue("#HeapIdSections", m_nHeapIdSections);

        for (int i = 0; i < m_heapIdSectionsTriplets.length; ++i) {
            m_heapIdSectionsTriplets[i].dump(aPrintStream, aTripletNumber, i + 1);
        }
        // Method triplets and methods
        for (int mX = 0; mX < m_nHeapIdSections; ++mX) {
            this.m_HeapIdSections[mX].dump(aPrintStream, mX + 1);
        }
        aPrintStream.pop();

        aPrintStream.pop();

    } // ServerRegionSection.dump()

} // ServerRegionSection
