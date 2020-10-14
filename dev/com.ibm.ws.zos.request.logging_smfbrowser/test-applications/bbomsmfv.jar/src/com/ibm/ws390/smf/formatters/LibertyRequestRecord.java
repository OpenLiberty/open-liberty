/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.smf.formatters;

import java.io.UnsupportedEncodingException;

import com.ibm.ws390.sm.smfview.ClassificationDataSection;
import com.ibm.ws390.sm.smfview.LibertyNetworkDataSection;
import com.ibm.ws390.sm.smfview.LibertyRequestInfoSection;
import com.ibm.ws390.sm.smfview.SmfPrintStream;
import com.ibm.ws390.sm.smfview.SmfRecord;
import com.ibm.ws390.sm.smfview.Triplet;
import com.ibm.ws390.sm.smfview.UnsupportedVersionException;
import com.ibm.ws390.sm.smfview.UserDataSection;

/**
 *
 * Formats the SMF 120.11 Liberty Request Record
 */
public class LibertyRequestRecord extends SmfRecord {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 3;

    /** Subtype Version number */
    public int m_subtypeVersion;

    /** Number of triplets contained in this instance. */
    public int m_tripletN;

    /** index number of this record */
    public int m_recordIndex;

    /** total number of records */
    public int m_totalNumberOfRecords;

    /** record continuation token */
    public byte m_recordContinuationToken[];

    /** Platform Neutral Server Info section triplet */
    public Triplet m_LibertyServerInfoSectionTriplet;

    /** Triplet for user data from the request dispatch */
    public Triplet m_userDataTriplet;

    public Triplet m_requestDataTriplet;
    public Triplet m_classificationDataTriplet;
    public Triplet m_networkDataTriplet;

    /** The Server Information Section */
    public LibertyServerInfoSection m_libertyServerInfoSection;

    /** User data section(s) */
    public UserDataSection[] m_userDataSection;
    public LibertyRequestInfoSection m_libertyRequestInfoSection;
    public LibertyNetworkDataSection m_libertyNetworkDataSection;
    public ClassificationDataSection[] m_classificationDataSection;

    /**
     * Create an SMF 120.11 (Liberty Request Record) object.
     *
     * @param aSmfRecord The SMF record
     * @throws UnsupportedVersionException  unknown version
     * @throws UnsupportedEncodingException problems with text string encoding
     */
    public LibertyRequestRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aSmfRecord); // pushes the print stream once

        m_subtypeVersion = m_stream.getInteger(4);

        m_tripletN = m_stream.getInteger(4);

        m_recordIndex = m_stream.getInteger(4);

        m_totalNumberOfRecords = m_stream.getInteger(4);

        m_recordContinuationToken = m_stream.getByteBuffer(8);

        m_LibertyServerInfoSectionTriplet = new Triplet(m_stream);

        m_userDataTriplet = new Triplet(m_stream);

        if (m_subtypeVersion == 1) {
            m_libertyServerInfoSection = new LibertyServerInfoSection(m_stream);

            if (m_userDataTriplet.count() > 0) {
                m_userDataSection = new UserDataSection[m_userDataTriplet.count()];
                for (int i = 0; i < m_userDataTriplet.count(); i++) {
                    m_userDataSection[i] = UserDataSection.loadUserDataFormatter(m_stream, 11);
                }
            }
        } else {
            if (m_subtypeVersion == 2 || m_subtypeVersion == 3) {
                m_requestDataTriplet = new Triplet(m_stream);
                m_classificationDataTriplet = new Triplet(m_stream);
                m_networkDataTriplet = new Triplet(m_stream);
                m_libertyServerInfoSection = new LibertyServerInfoSection(m_stream);

                if (m_userDataTriplet.count() > 0) {
                    m_userDataSection = new UserDataSection[m_userDataTriplet.count()];
                    for (int i = 0; i < m_userDataTriplet.count(); i++) {
                        m_userDataSection[i] = UserDataSection.loadUserDataFormatter(m_stream, 11);
                    }
                }
                // get request data
                m_libertyRequestInfoSection = new LibertyRequestInfoSection(m_stream);
                // get classification data
                if (m_classificationDataTriplet.count() > 0) {
                    m_classificationDataSection = new ClassificationDataSection[m_classificationDataTriplet.count()];
                    for (int i = 0; i < m_classificationDataTriplet.count(); i++) {
                        m_classificationDataSection[i] = new ClassificationDataSection(m_stream);
                    }
                }
                // get network data
                m_libertyNetworkDataSection = new LibertyNetworkDataSection(m_stream);
            }
        }
    }

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     *
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    }

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
        aPrintStream.printlnKeyValue("#Subtype Version", m_subtypeVersion);
        aPrintStream.printlnKeyValue("Index of this record", m_recordIndex);
        aPrintStream.printlnKeyValue("Total number of records", m_totalNumberOfRecords);
        aPrintStream.printlnKeyValue("record continuation token", m_recordContinuationToken, null);
        aPrintStream.printlnKeyValue("#Triplets", m_tripletN);
        m_LibertyServerInfoSectionTriplet.dump(aPrintStream, 1);
        m_userDataTriplet.dump(aPrintStream, 2);
        if (m_subtypeVersion == 2 || m_subtypeVersion == 3) {
            m_requestDataTriplet.dump(aPrintStream, 3);
            m_classificationDataTriplet.dump(aPrintStream, 4);
            m_networkDataTriplet.dump(aPrintStream, 5);
        }
        m_libertyServerInfoSection.dump(aPrintStream, 1);
        if (m_userDataTriplet.count() > 0) {
            for (int i = 0; i < m_userDataTriplet.count(); i++) {
                m_userDataSection[i].dump(aPrintStream, 2);
            }
        }
        if (m_subtypeVersion == 2 || m_subtypeVersion == 3) {
            // dump request data
            m_libertyRequestInfoSection.dump(aPrintStream, 3);
            // dump classification data
            for (int i = 0; i < m_classificationDataTriplet.count(); i++) {
                m_classificationDataSection[i].dump(aPrintStream, 4);
            }
            // dump network data
            m_libertyNetworkDataSection.dump(aPrintStream, 5);
        }
        aPrintStream.pop();
    }

}
