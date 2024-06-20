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
/** Data container for SMF data related to a Container Activity Smf Record. */
public class J2eeContainerActivitySmfRecord extends SmfRecord {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Number of triplets contained in this instance. */
    public int m_tripletN;

    /** Product section triplet. */
    public Triplet m_productSectionTriplet;

    /** Container activity section triplet. */
    public Triplet m_j2eeContainerActivitySectionTriplet;

    /** Contained bean triplets. */
    public Triplet[] m_beanTriplets;

    /** Containted product section. */
    public ProductSection m_productSection;

    /** Contained J2ee container activity section. */
    public J2eeContainerActivitySection m_j2eeContainerActivitySection;

    /** Contained bean sections. */
    public BeanSection[] m_beanSections;

    //----------------------------------------------------------------------------
    /**
     * Constructs a J2eeContainerActivitySmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input
     * stream. J2eeContainerActivitySmfRecord continues to parse the stream
     * for its specific attributes.
     * 
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public J2eeContainerActivitySmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aSmfRecord);

        m_tripletN = m_stream.getInteger(4); //    @L2C 

        m_productSectionTriplet = new Triplet(m_stream);

        m_j2eeContainerActivitySectionTriplet = new Triplet(m_stream);

        m_beanTriplets = new Triplet[m_tripletN - 2];
        for (int cX = 0; cX < m_tripletN - 2; ++cX) {
            m_beanTriplets[cX] = new Triplet(m_stream);
        }

        m_productSection = new ProductSection(m_stream);

        // Now after the product section was processed
        // the required version number is known.
        // This might throw an UnsupportedVersionException.
        setVersion(m_productSection.version());

        m_j2eeContainerActivitySection = new J2eeContainerActivitySection(m_stream, version());

        m_beanSections = new BeanSection[m_tripletN - 2];
        for (int bX = 0; bX < m_tripletN - 2; ++bX) {
            m_beanSections[bX] = new BeanSection(m_stream, version());
        }

    } // J2eeContainerActivitySmfRecord.J2eeContainerActivitySmfRecord(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // J2eeContainerActivitySmfRecord.supportedVersion()

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
        m_j2eeContainerActivitySectionTriplet.dump(aPrintStream, 2);
        for (int bX = 0; bX < m_beanTriplets.length; bX++) {
            m_beanTriplets[bX].dump(aPrintStream, bX + 3);
        }

        m_productSection.dump(aPrintStream, 1);
        m_j2eeContainerActivitySection.dump(aPrintStream, 2);

        // print BeanSections
        for (int bX = 0; bX < m_beanSections.length; ++bX) {
            m_beanSections[bX].dump(aPrintStream, bX + 3);
        } // for

        aPrintStream.pop();

    } // J2eeContaineActivitySmfRecord.dump()

} // J2eeContainerActivitySmfRecord
