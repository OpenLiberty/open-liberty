/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
/** Data container for SMF data related to a request activity Smf record. */
public class RequestActivitySmfRecord extends SmfRecord {

    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 1;

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
    public Triplet m_platformNeutralSectionTriplet;

    /** z/OS server info section triplet */
    public Triplet m_zosServerInfoTriplet;

    /** Triplet for platform neutral information about the request */
    public Triplet m_platformNeutralRequestInfoTriplet;
    /** Triplet for z/OS specific information about the request */
    public Triplet m_zosRequestInfoTriplet;
    /** Triplet for formatted timestamp section */
    public Triplet m_timeStampTriplet;
    /** Triplet for network data about the request */
    public Triplet m_networkDataTriplet;
    /** Triplet for classification data used for the request */
    public Triplet m_classificationDataTriplet;
    /** Triplet for security data */
    public Triplet m_securityDataTriplet;
    /** Triplet for CPU usage information from the request dispatch */
    public Triplet m_cpuUsageTriplet;
    /** Triplet for user data from the request dispatch */
    public Triplet m_userDataTriplet;
    /** Triplet for async work data section */
    public Triplet m_asyncWorkDataTriplet;

    /** Platform Neutral server data section */
    public PlatformNeutralSection m_platformNeutralSection;
    /** z/OS specific server data section */
    public ZosServerInfoSection m_zosServerInfoSection;
    /** Platform Neutral request information section */
    public PlatformNeutralRequestInfoSection m_platformNeutralRequestInfoSection;
    /** z/OS specific request information section */
    public ZosRequestInfoSection m_zosRequestInfoSection;
    /** Time stamp section */
    public TimeStampSection[] m_timeStampSection;
    /** Network data section(s) */
    public NetworkDataSection[] m_networkDataSection;
    /** Classification data section(s) */
    public ClassificationDataSection[] m_classificationDataSection;
    /** Security data section(s) */
    public SecurityDataSection[] m_securityDataSection;
    /** CPU usage section(s) */
    public CpuUsageSection[] m_cpuUsageSection;
    /** User data section(s) */
    public UserDataSection[] m_userDataSection;
    /** Async work data section */
    public AsyncWorkDataSection[] m_asyncWorkDataSection;

    //----------------------------------------------------------------------------
    /**
     * Constructs a RequestActivitySmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input
     * stream. RequestActivitySmfRecord continues to parse the stream
     * for its specific attributes.
     * 
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public RequestActivitySmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException, SkipFilteredRecord {

        super(aSmfRecord); // pushes the print stream once

        m_subtypeVersion = m_stream.getInteger(4);

        m_tripletN = m_stream.getInteger(4); //   @L2C

        m_recordIndex = m_stream.getInteger(4);

        m_totalNumberOfRecords = m_stream.getInteger(4);

        m_recordContinuationToken = m_stream.getByteBuffer(8);

        m_platformNeutralSectionTriplet = new Triplet(m_stream);

        m_zosServerInfoTriplet = new Triplet(m_stream);

        m_platformNeutralRequestInfoTriplet = new Triplet(m_stream);

        m_zosRequestInfoTriplet = new Triplet(m_stream);

        m_timeStampTriplet = new Triplet(m_stream);

        m_networkDataTriplet = new Triplet(m_stream);

        m_classificationDataTriplet = new Triplet(m_stream);

        m_securityDataTriplet = new Triplet(m_stream);

        m_cpuUsageTriplet = new Triplet(m_stream);

        m_userDataTriplet = new Triplet(m_stream);

        m_asyncWorkDataTriplet = new Triplet(m_stream);

        // Skip 24 bytes here, reserved for future triplets
        m_stream.skip(24);

        m_platformNeutralSection = new PlatformNeutralSection(m_stream);

        String s = System.getProperty("com.ibm.ws390.smf.smf1209.MatchServer");
        if ((s != null) && (!s.equalsIgnoreCase(m_platformNeutralSection.m_serverShortName))) {
            throw new SkipFilteredRecord();
        }

        m_zosServerInfoSection = new ZosServerInfoSection(m_stream);

        s = System.getProperty("com.ibm.ws390.smf.smf1209.MatchSystem");
        if ((s != null) && (!s.equalsIgnoreCase(m_zosServerInfoSection.m_systemName))) {
            throw new SkipFilteredRecord();
        }

        if (m_platformNeutralRequestInfoTriplet.count() > 0) // 0 or 1 cardinality
        {
            m_platformNeutralRequestInfoSection = new PlatformNeutralRequestInfoSection(m_stream);

            s = System.getProperty("com.ibm.ws390.smf.smf1209.ExcludeInternal");
            if ((s != null) && (s.equalsIgnoreCase("TRUE"))) {
                int reqType = m_platformNeutralRequestInfoSection.m_requestType;
                if ((reqType == PlatformNeutralRequestInfoSection.TypeUnknown) |
                    (reqType == PlatformNeutralRequestInfoSection.TypeMBean) |
                    (reqType == PlatformNeutralRequestInfoSection.TypeOTS) |
                    (reqType == PlatformNeutralRequestInfoSection.TypeOther))
                    throw new SkipFilteredRecord();

            }
        }

        if (m_zosRequestInfoTriplet.count() > 0) // 0 or 1 cardinality
        {
            m_zosRequestInfoSection = new ZosRequestInfoSection(m_stream);
        }

        if (m_timeStampTriplet.count() > 0) {
            m_timeStampSection = new TimeStampSection[m_timeStampTriplet.count()];
            for (int i = 0; i < m_timeStampTriplet.count(); i++) {
                m_timeStampSection[i] = new TimeStampSection(m_stream);
            }
        }

        if (m_networkDataTriplet.count() > 0) // SGD: I think we can only have one of these, but it doesnt hurt to check the count...
        {
            m_networkDataSection = new NetworkDataSection[m_networkDataTriplet.count()];
            for (int i = 0; i < m_networkDataTriplet.count(); i++) {
                m_networkDataSection[i] = new NetworkDataSection(m_stream);
            }
        }

        if (m_classificationDataTriplet.count() > 0) {
            m_classificationDataSection = new ClassificationDataSection[m_classificationDataTriplet.count()];
            for (int i = 0; i < m_classificationDataTriplet.count(); i++) {
                m_classificationDataSection[i] = new ClassificationDataSection(m_stream);
            }
        }

        if (m_securityDataTriplet.count() > 0) {
            m_securityDataSection = new SecurityDataSection[m_securityDataTriplet.count()];
            for (int i = 0; i < m_securityDataTriplet.count(); i++) {
                m_securityDataSection[i] = new SecurityDataSection(m_stream);
            }
        }

        if (m_cpuUsageTriplet.count() > 0) {
            m_cpuUsageSection = new CpuUsageSection[m_cpuUsageTriplet.count()];
            for (int i = 0; i < m_cpuUsageTriplet.count(); i++) {
                m_cpuUsageSection[i] = new CpuUsageSection(m_stream);
            }
        }

        if (m_userDataTriplet.count() > 0) {
            m_userDataSection = new UserDataSection[m_userDataTriplet.count()];
            for (int i = 0; i < m_userDataTriplet.count(); i++) {
                m_userDataSection[i] = UserDataSection.loadUserDataFormatter(m_stream, 9);
            }
        }

        if (m_asyncWorkDataTriplet.count() > 0) {
            m_asyncWorkDataSection = new AsyncWorkDataSection[m_asyncWorkDataTriplet.count()];
            for (int i = 0; i < m_asyncWorkDataTriplet.count(); i++) {
                m_asyncWorkDataSection[i] = new AsyncWorkDataSection(m_stream);
            }
        }

    } // RequestActivitySmfRecord.RequestActivitySmfRecord(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // RequestActivitySmfRecord.supportedVersion()

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
        m_platformNeutralSectionTriplet.dump(aPrintStream, 1);
        m_zosServerInfoTriplet.dump(aPrintStream, 2);
        m_platformNeutralRequestInfoTriplet.dump(aPrintStream, 3);
        m_zosRequestInfoTriplet.dump(aPrintStream, 4);
        m_timeStampTriplet.dump(aPrintStream, 5);
        m_networkDataTriplet.dump(aPrintStream, 6);
        m_classificationDataTriplet.dump(aPrintStream, 7);
        m_securityDataTriplet.dump(aPrintStream, 8);
        m_cpuUsageTriplet.dump(aPrintStream, 9);
        m_userDataTriplet.dump(aPrintStream, 10);
        m_asyncWorkDataTriplet.dump(aPrintStream, 11);

        m_platformNeutralSection.dump(aPrintStream, 1);

        m_zosServerInfoSection.dump(aPrintStream, 2);

        if (m_platformNeutralRequestInfoTriplet.count() > 0)
            m_platformNeutralRequestInfoSection.dump(aPrintStream, 3);

        if (m_zosRequestInfoTriplet.count() > 0)
            m_zosRequestInfoSection.dump(aPrintStream, 4);

//  SGD: Don't need these ifs, for loop wont let us in if count is 0 since i starts at 0 so instantly i !< 0
        if (m_timeStampTriplet.count() > 0) {
            for (int i = 0; i < m_timeStampTriplet.count(); i++) {
                m_timeStampSection[i].dump(aPrintStream, 5);
            }
        }

        if (m_networkDataTriplet.count() > 0) {
            for (int i = 0; i < m_networkDataTriplet.count(); i++) {
                m_networkDataSection[i].dump(aPrintStream, 6);
            }
        }

        if (m_classificationDataTriplet.count() > 0) {
            for (int i = 0; i < m_classificationDataTriplet.count(); i++) {
                m_classificationDataSection[i].dump(aPrintStream, 7);
            }
        }

        if (m_securityDataTriplet.count() > 0) {
            for (int i = 0; i < m_securityDataTriplet.count(); i++) {
                m_securityDataSection[i].dump(aPrintStream, 8);
            }
        }

        if (m_cpuUsageTriplet.count() > 0) {
            for (int i = 0; i < m_cpuUsageTriplet.count(); i++) {
                m_cpuUsageSection[i].dump(aPrintStream, 9);
            }
        }

        if (m_userDataTriplet.count() > 0) {
            for (int i = 0; i < m_userDataTriplet.count(); i++) {
                m_userDataSection[i].dump(aPrintStream, 10);
            }
        }

        if (m_asyncWorkDataTriplet.count() > 0) {
            for (int i = 0; i < m_asyncWorkDataTriplet.count(); i++) {
                m_asyncWorkDataSection[i].dump(aPrintStream, 11);
            }
        }

        aPrintStream.pop();

    } // RequestActivitySmfRecord.dump()

} // RequestActivitySmfRecord