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
/** Data container for SMF data related to a web container activity Smf record. */
public class WebContainerActivitySmfRecord extends SmfRecord {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Number of triplets contained in this instance. */
    public int m_tripletN;

    /** Product section triplet. */
    public Triplet m_productSectionTriplet;

    /** Web container activity section triplet. */
    public Triplet m_webContainerActivitySectionTriplet;

    /** Web container activity section triplet. */
    public Triplet m_httpSessionManagerActivitySectionTriplet;

    /** Contained web application triplets. */
    public Triplet[] m_webApplicationTriplets;

    /** Containted product section. */
    public ProductSection m_productSection;

    /** Contained web container activity section. */
    public WebContainerActivitySection m_webContainerActivitySection;

    /** Contained http session manager activity section. */
    public HttpSessionManagerActivitySection m_httpSessionManagerActivitySection;

    /** Contained web application sections. */
    public WebApplicationActivitySection[] m_webApplicationActivitySections;

    //----------------------------------------------------------------------------
    /**
     * Constructs a WebContainerActivitySmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input
     * stream. WebContainerActivitySmfRecord continues to parse the stream
     * for its specific attributes.
     * 
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public WebContainerActivitySmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aSmfRecord);

        m_tripletN = m_stream.getInteger(4);

        m_productSectionTriplet = new Triplet(m_stream);

        m_webContainerActivitySectionTriplet = new Triplet(m_stream);

        m_httpSessionManagerActivitySectionTriplet = new Triplet(m_stream);

        m_webApplicationTriplets = new Triplet[m_tripletN - 3];
        for (int aX = 0; aX < m_tripletN - 3; ++aX) {
            m_webApplicationTriplets[aX] = new Triplet(m_stream);
        }

        m_productSection = new ProductSection(m_stream);

        // Now after the product section was processed
        // the required version number is known.
        // This might throw an UnsupportedVersionException.
        setVersion(m_productSection.version());

        m_webContainerActivitySection = new WebContainerActivitySection(m_stream, version());

        m_httpSessionManagerActivitySection = new HttpSessionManagerActivitySection(m_stream, version());

        m_webApplicationActivitySections = new WebApplicationActivitySection[m_tripletN - 3];
        for (int wX = 0; wX < m_tripletN - 3; ++wX) {
            m_webApplicationActivitySections[wX] = new WebApplicationActivitySection(m_stream, version());
        }

    } // WebContainerActivitySmfRecord(...)

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
        m_webContainerActivitySectionTriplet.dump(aPrintStream, 2);
        m_httpSessionManagerActivitySectionTriplet.dump(aPrintStream, 3); //     $P0A
        for (int tX = 0; tX < m_webApplicationTriplets.length; ++tX) {
            m_webApplicationTriplets[tX].dump(aPrintStream, tX + 4); //     $P0C
        } // for

        m_productSection.dump(aPrintStream, 1);

        m_webContainerActivitySection.dump(aPrintStream, 2);

        m_httpSessionManagerActivitySection.dump(aPrintStream, 3);

        for (int sX = 0; sX < m_webApplicationActivitySections.length; ++sX) {
            m_webApplicationActivitySections[sX].dump(aPrintStream, sX + 4);
        } // for

        aPrintStream.pop();

    } // dump(...)

} // WebContainerActivitySmfRecord
