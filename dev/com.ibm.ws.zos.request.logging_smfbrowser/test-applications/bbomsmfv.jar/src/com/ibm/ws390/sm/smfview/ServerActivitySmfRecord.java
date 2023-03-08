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
/** Data container for SMF data related to a server activity Smf record. */
public class ServerActivitySmfRecord extends SmfRecord {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 4; //@LI4369-4C

    /** Number of triplets contained in this instance. */
    public int m_tripletN;

    /** Product section triplet. */
    public Triplet m_productSectionTriplet;

    /** Server activity section triplet. */
    public Triplet m_serverActivityTriplet;

    /** Communication session triplet. */
    public Triplet m_commSTriplet;

    /** JVM Heap Section Triplet */
    public Triplet m_jvmHeapSTriplet; //@MD17014 A

    /** Containted product section. */
    public ProductSection m_productSection;

    /** Server activity section triplet. */
    public ServerActivitySection m_serverActivitySection;

    /** Communication session sections. */
    public CommSessionSection[] m_commSessionSections;

    /** JVM Heap Sections. */
    public JvmHeapSection[] m_jvmHeapSections; // @MD17014 A

    //----------------------------------------------------------------------------
    /**
     * Constructs a ServerActivitySmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input
     * stream. ServerActivitySmfRecord continues to parse the stream
     * for its specific attributes.
     *
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ServerActivitySmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aSmfRecord); // pushes the print stream once

        m_tripletN = m_stream.getInteger(4); //   @L2C

        m_productSectionTriplet = new Triplet(m_stream);

        m_serverActivityTriplet = new Triplet(m_stream);

        m_commSTriplet = new Triplet(m_stream);
        // @MD17014 6A
        if (m_tripletN > 3) { //if there is a triplet for heap sections
            m_jvmHeapSTriplet = new Triplet(m_stream);
        } else {
            m_jvmHeapSTriplet = null;
        }

        m_productSection = new ProductSection(m_stream);

        // Now after the product section was processed
        // the required version number is known.
        // This might throw an UnsupportedVersionException.
        setVersion(m_productSection.version());

        m_serverActivitySection = new ServerActivitySection(m_stream, version());

        m_commSessionSections = new CommSessionSection[m_commSTriplet.count()];
        for (int cX = 0; cX < m_commSTriplet.count(); cX++) {
            m_commSessionSections[cX] = new CommSessionSection(m_stream, version(), m_commSTriplet.length()); // @MD20733C
        }
        // @MD17014 9 A
        if (m_jvmHeapSTriplet != null) {
            m_jvmHeapSections = new JvmHeapSection[m_jvmHeapSTriplet.count()];
            for (int jX = 0; jX < m_jvmHeapSTriplet.count(); jX++) {
                m_jvmHeapSections[jX] = new JvmHeapSection(m_stream, version());
            }
        } else {
            m_jvmHeapSections = new JvmHeapSection[0];
        }

    } // ServerActivitySmfRecord.ServerActivitySmfRecord(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     *
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // ServerActivitySmfRecord.supportedVersion()

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
        m_serverActivityTriplet.dump(aPrintStream, 2);
        m_commSTriplet.dump(aPrintStream, 3);
        // @MD17014 3 A
        if (m_jvmHeapSTriplet != null) {
            m_jvmHeapSTriplet.dump(aPrintStream, 4);
        }
        m_productSection.dump(aPrintStream, 1);
        m_serverActivitySection.dump(aPrintStream, 2);

        int nSections = 3; // @MD17014 A
        for (int cX = 0; cX < m_commSessionSections.length; ++cX) {
            m_commSessionSections[cX].dump(aPrintStream, nSections++); // @MD17014 C
        }
        // @MD17014 3 A
        for (int jX = 0; jX < m_jvmHeapSections.length; ++jX) {
            m_jvmHeapSections[jX].dump(aPrintStream, nSections++);
        }

        aPrintStream.pop();

    } // ServerActivitySmfRecord.dump()

} // ServerActivitySmfRecord