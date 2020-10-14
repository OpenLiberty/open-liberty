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
/** Data container for SMF data related to a Container Interval Smf Record. */
public class ContainerIntervalSmfRecord extends SmfRecord {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 2;

    /** Number of triplets contained in this instance. */
    public int m_tripletN;

    /** Product section triplet. */
    public Triplet m_productSectionTriplet;

    /** Container activity section triplet. */
    public Triplet m_containerIntervalSectionTriplet;

    /** Contained class triplets. */
    public Triplet[] m_classTriplets;

    /** Containted product section. */
    public ProductSection m_productSection;

    /** Contained container interval section. */
    public ContainerIntervalSection m_containerIntervalSection;

    /** Contained class sections. */
    public ClassSection[] m_classSections;

    //----------------------------------------------------------------------------
    /**
     * Constructs a ContainerIntervalSmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input
     * stream. ContainerActivitySmfRecord continues to parse the stream
     * for its specific attributes.
     * 
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ContainerIntervalSmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aSmfRecord);

        m_tripletN = m_stream.getInteger(4); //   @L2C

        m_productSectionTriplet = new Triplet(m_stream);

        m_containerIntervalSectionTriplet = new Triplet(m_stream);

        m_classTriplets = new Triplet[m_tripletN - 2];
        for (int cX = 0; cX < m_tripletN - 2; cX++) {
            m_classTriplets[cX] = new Triplet(m_stream);
        }

        m_productSection = new ProductSection(m_stream);

        // Now after the product section was processed
        // the required version number is known.
        // This might throw an UnsupportedVersionException.
        setVersion(m_productSection.version());

        m_containerIntervalSection = new ContainerIntervalSection(m_stream, version());

        m_classSections = new ClassSection[m_tripletN - 2];
        for (int cX = 0; cX < m_tripletN - 2; cX++) {
            m_classSections[cX] = new ClassSection(m_stream, version());
        }

    } // ContainerIntervalSmfRecord.ContainerIntervalSmfRecord(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // ContainerIntervalSmfRecord.supportedVersion()

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
        m_containerIntervalSectionTriplet.dump(aPrintStream, 2);
        for (int cX = 0; cX < m_classTriplets.length; cX++) {
            m_classTriplets[cX].dump(aPrintStream, cX + 3);
        } // for ... dump class triplets

        m_productSection.dump(aPrintStream, 1);
        m_containerIntervalSection.dump(aPrintStream, 2);

        // print ClassSections
        for (int cX = 0; cX < m_classSections.length; cX++) {
            m_classSections[cX].dump(aPrintStream, cX + 3);
        } // for ... dump class sections

        aPrintStream.pop();

    } // ContainerIntervalSmfRecord.dump(...)

} // ContainerIntervalSmfRecord