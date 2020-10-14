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
/** Data container for SMF data related to a server interval Smf record. */
public class ServerIntervalSmfRecord extends SmfRecord {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 4; /* @LI4369-4C */

    /** Number of triplets contained in this instance. */
    public int m_tripletN;

    /** Product section triplet. */
    public Triplet m_productSectionTriplet;

    /** Server interval section triplet. */
    public Triplet m_serverIntervalSectionTriplet;

    /** Server region section triplet. */
    public Triplet[] m_serverRegionSectionTriplets; // @MD17014 A

    /** Containted product section. */
    public ProductSection m_productSection;

    /** Server interval section triplet. */
    public ServerIntervalSection m_serverIntervalSection;

    /** Server Region Sections. */
    public ServerRegionSection[] m_serverRegionSections; // @MD17014 A

    //----------------------------------------------------------------------------
    /**
     * Constructs a ServerIntervalSmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input
     * stream. ServerActivitySmfRecord continues to parse the stream
     * for its specific attributes.
     * 
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ServerIntervalSmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aSmfRecord);

        m_tripletN = m_stream.getInteger(4); //    @L2C

        m_productSectionTriplet = new Triplet(m_stream);

        m_serverIntervalSectionTriplet = new Triplet(m_stream);

        m_serverRegionSectionTriplets = new Triplet[m_tripletN - 2]; // @MD17014 A

        for (int bX = 0; bX < m_tripletN - 2; bX++) { // @MD17014 A
            m_serverRegionSectionTriplets[bX] = new Triplet(m_stream); // @MD17014 A
        } // @MD17014 A

        m_productSection = new ProductSection(m_stream);

        // Now after the product section was processed
        // the required version number is known.
        // This might throw an UnsupportedVersionException.
        setVersion(m_productSection.version());

        m_serverIntervalSection = new ServerIntervalSection(m_stream, version(), m_serverIntervalSectionTriplet.length()); // @MD20733C

        m_serverRegionSections = new ServerRegionSection[m_tripletN - 2]; // @MD17014 A
        for (int bX = 0; bX < m_tripletN - 2; bX++) { // @MD17014 A
            m_serverRegionSections[bX] = new ServerRegionSection(m_stream, version());// @MD17014 A
        } // @MD17014 A

    } // ServerIntervalSmfRecord(...)

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
     * @param aPrintStream print stream to dump to.
     */
    @Override
    public void dump(SmfPrintStream aPrintStream) {

        super.dump(aPrintStream);

        aPrintStream.push();

        aPrintStream.println("");
        aPrintStream.printlnKeyValue("#Triplets", m_tripletN);
        m_productSectionTriplet.dump(aPrintStream, 1);
        m_serverIntervalSectionTriplet.dump(aPrintStream, 2);
        // @MD17014 10 A
        if (m_serverRegionSectionTriplets != null) {
            for (int i = 0; i < m_serverRegionSectionTriplets.length; i++) {
                m_serverRegionSectionTriplets[i].dump(aPrintStream, i + 3);
            }
        }
        m_productSection.dump(aPrintStream, 1);
        m_serverIntervalSection.dump(aPrintStream, 2);
        for (int bX = 0; bX < m_tripletN - 2; ++bX) {
            m_serverRegionSections[bX].dump(aPrintStream, bX + 3);
        }
        aPrintStream.pop();

    } // dump(...)

} // ServerIntervalFixed