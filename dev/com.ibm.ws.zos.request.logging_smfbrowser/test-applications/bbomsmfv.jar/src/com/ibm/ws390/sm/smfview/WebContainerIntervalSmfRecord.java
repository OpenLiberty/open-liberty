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
/** Data container for SMF data related to a web container interval Smf record. */
public class WebContainerIntervalSmfRecord extends SmfRecord {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Number of triplets contained in this instance. */
    public int m_tripletN;

    /** Product section triplet. */
    public Triplet m_productSectionTriplet;

    /** Web container interval section triplet. */
    public Triplet m_webContainerIntervalSectionTriplet;

    /** Web container interval section triplet. */
    public Triplet m_httpSessionManagerIntervalSectionTriplet;

    /** Contained web application triplets. */
    public Triplet[] m_webApplicationTriplets;

    /** Containted product section. */
    public ProductSection m_productSection;

    /** Contained web container interval section. */
    public WebContainerIntervalSection m_webContainerIntervalSection;

    /** Contained http session manager interval section. */
    public HttpSessionManagerIntervalSection m_httpSessionManagerIntervalSection;

    /** Contained web application sections. */
    public WebApplicationIntervalSection[] m_webApplicationIntervalSections;

    //----------------------------------------------------------------------------
    /**
     * Constructs a WebContainerIntervalSmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input
     * stream. WebContainerActivitySmfRecord continues to parse the stream
     * for its specific attributes.
     * 
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public WebContainerIntervalSmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aSmfRecord);

        m_tripletN = m_stream.getInteger(4);

        m_productSectionTriplet = new Triplet(m_stream);

        m_webContainerIntervalSectionTriplet = new Triplet(m_stream);

        m_httpSessionManagerIntervalSectionTriplet = new Triplet(m_stream);

        m_webApplicationTriplets = new Triplet[m_tripletN - 3];
        for (int aX = 0; aX < m_tripletN - 3; ++aX) {
            m_webApplicationTriplets[aX] = new Triplet(m_stream);
        }

        m_productSection = new ProductSection(m_stream);

        // Now after the product section was processed
        // the required version number is known.
        // This might throw an UnsupportedVersionException.
        setVersion(m_productSection.version());

        m_webContainerIntervalSection = new WebContainerIntervalSection(m_stream, version());

        m_httpSessionManagerIntervalSection = new HttpSessionManagerIntervalSection(m_stream, version());

        m_webApplicationIntervalSections = new WebApplicationIntervalSection[m_tripletN - 3];
        for (int wX = 0; wX < m_tripletN - 3; ++wX) {
            m_webApplicationIntervalSections[wX] = new WebApplicationIntervalSection(m_stream, version());
        }

    } // WebContainerIntervalSmfRecord(...)

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
        aPrintStream.printlnKeyValue("# Triplets", m_tripletN);
        m_productSectionTriplet.dump(aPrintStream, 1);
        m_webContainerIntervalSectionTriplet.dump(aPrintStream, 2);
        m_httpSessionManagerIntervalSectionTriplet.dump(aPrintStream, 3); //     $P0A
        for (int tX = 0; tX < m_webApplicationTriplets.length; ++tX) {
            m_webApplicationTriplets[tX].dump(aPrintStream, tX + 4); //     $P0C
        } // for

        m_productSection.dump(aPrintStream, 1);

        m_webContainerIntervalSection.dump(aPrintStream, 2);

        m_httpSessionManagerIntervalSection.dump(aPrintStream, 3);

        for (int sX = 0; sX < m_webApplicationIntervalSections.length; ++sX) {
            m_webApplicationIntervalSections[sX].dump(aPrintStream, sX + 4);
        } // for

        aPrintStream.pop();

    } // dump(...)

} // WebContainerIntervalSmfRecord
