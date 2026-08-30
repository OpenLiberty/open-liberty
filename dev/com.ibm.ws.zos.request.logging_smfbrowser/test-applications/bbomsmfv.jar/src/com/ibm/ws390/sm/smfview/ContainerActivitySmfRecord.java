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
/** Data container for SMF data related to a container activity Smf record. */
public class ContainerActivitySmfRecord extends SmfRecord {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 2;

    /** Number of triplets contained in this instance. */
    public int m_tripletN;

    /** Product section triplet. */
    public Triplet m_productSectionTriplet;

    /** Container activity section triplet. */
    public Triplet m_containerActivitySectionTriplet;

    /** Contained class triplets. */
    public Triplet[] m_classTriplets;

    /** Containted product section. */
    public ProductSection m_productSection;

    /** Contained container activity section. */
    public ContainerActivitySection m_containerActivitySection;

    /** Contained class sections. */
    public ClassSection[] m_classSections;

    //----------------------------------------------------------------------------
    /**
     * Constructs a ContainerActivitySmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input
     * stream. ContainerActivitySmfRecord continues to parse the stream
     * for its specific attributes.
     * 
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ContainerActivitySmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aSmfRecord);

        m_tripletN = m_stream.getInteger(4); //    @L3C 

        m_productSectionTriplet = new Triplet(m_stream);

        m_containerActivitySectionTriplet = new Triplet(m_stream);

        m_classTriplets = new Triplet[m_tripletN - 2];
        for (int cX = 0; cX < m_tripletN - 2; cX++) {
            m_classTriplets[cX] = new Triplet(m_stream);
        }

        m_productSection = new ProductSection(m_stream);

        // Now after the product section was processed
        // the required version number is known.
        // This might throw an UnsupportedVersionException.
        setVersion(m_productSection.version());

        m_containerActivitySection = new ContainerActivitySection(m_stream, version());

        m_classSections = new ClassSection[m_tripletN - 2];
        for (int cX = 0; cX < m_tripletN - 2; cX++) {
            m_classSections[cX] = new ClassSection(m_stream, version());
        }

    } // ContainerActivitySmfRecord.ContainerActivitySmfRecord(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // ContainerActivitySmfRecord.supportedVersion()

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
        m_containerActivitySectionTriplet.dump(aPrintStream, 2);
        for (int cX = 0; cX < m_classTriplets.length; ++cX) {
            m_classTriplets[cX].dump(aPrintStream, cX + 3);
        }

        m_productSection.dump(aPrintStream, 1);
        m_containerActivitySection.dump(aPrintStream, 2);

        // print ClassSections
        for (int cX = 0; cX < m_classSections.length; ++cX) {
            m_classSections[cX].dump(aPrintStream, cX + 3);
        } // for

        aPrintStream.pop();

    } // ContainerActivitySmfRecord.dump(...)

} // ContainerActivitySmfRecord