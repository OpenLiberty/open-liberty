/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
/** Data container for SMF data related to a outbound request Smf record. */
public class OutboundRequestSmfRecord extends SmfRecord {

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

    /** Triplet for outbound Request Info */
    public Triplet m_outboundRequestInfoTriplet;
    /** Triplet for WOLA outbound request type specific section */
    public Triplet m_WolaOutboundRequestTypeSpecificTriplet;
    /** Triplet for outbound request transaction context */
    public Triplet m_outboundRequestTtransactionContextTriplet;
    /** Triplet for outbound request security context */
    public Triplet m_outboundRequestSecurityContextTriplet;
    /** Triplet for outbound request CICS context */
    public Triplet m_outboundRequestCicsContextTriplet;
    /** Triplet for OTMA outbound request type specific section */
    public Triplet m_OtmaOutboundRequestTypeSpecificTriplet;

    /** Platform Neutral server data section */
    public PlatformNeutralSection m_platformNeutralSection;
    /** z/OS specific server data section */
    public ZosServerInfoSection m_zosServerInfoSection;
    /** outbound Request Info section */
    public OutboundRequestInfoSection m_outboundRequestInfoSection;
    /** WOLA outbound request type specific section */
    public OutboundRequestWolaTypeSpecificSection m_OutboundRequestWolaTypeSpecificSection;
    /** outbound request transaction context */
    public OutboundRequestTransactionContextSection m_OutboundRequestTransactionContextSection;
    /** outbound request security context */
    public OutboundRequestSecurityContextSection m_outboundRequestSecurityContextSection;
    /** outbound request CICS context */
    public OutboundRequestCicsContextSection m_outboundRequestCicsContextSection;
    /** OTMA outbound request type specific section */
    public OutboundRequestOtmaTypeSpecificSection m_OutboundRequestOtmaTypeSpecificSection;

    //----------------------------------------------------------------------------
    /**
     * Constructs a OutboundRequestSmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input
     * stream. OutboundRequestSmfRecord continues to parse the stream
     * for its specific attributes.
     *
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public OutboundRequestSmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aSmfRecord); // pushes the print stream once

        m_subtypeVersion = m_stream.getInteger(4);

        m_tripletN = m_stream.getInteger(4);

        m_recordIndex = m_stream.getInteger(4);

        m_totalNumberOfRecords = m_stream.getInteger(4);

        m_recordContinuationToken = m_stream.getByteBuffer(8);

        m_platformNeutralSectionTriplet = new Triplet(m_stream);

        m_zosServerInfoTriplet = new Triplet(m_stream);

        m_outboundRequestInfoTriplet = new Triplet(m_stream);

        m_WolaOutboundRequestTypeSpecificTriplet = new Triplet(m_stream);

        m_outboundRequestTtransactionContextTriplet = new Triplet(m_stream);

        m_outboundRequestSecurityContextTriplet = new Triplet(m_stream);

        m_outboundRequestCicsContextTriplet = new Triplet(m_stream);

        m_OtmaOutboundRequestTypeSpecificTriplet = new Triplet(m_stream);

        // Skip 60 bytes here, reserved for future triplets
        m_stream.skip(60);

        m_platformNeutralSection = new PlatformNeutralSection(m_stream);

        m_zosServerInfoSection = new ZosServerInfoSection(m_stream);

        m_outboundRequestInfoSection = new OutboundRequestInfoSection(m_stream);

        if (m_WolaOutboundRequestTypeSpecificTriplet.count() > 0) {
            m_OutboundRequestWolaTypeSpecificSection = new OutboundRequestWolaTypeSpecificSection(m_stream);
        }

        if (m_OtmaOutboundRequestTypeSpecificTriplet.count() > 0) {
            m_OutboundRequestOtmaTypeSpecificSection = new OutboundRequestOtmaTypeSpecificSection(m_stream);
        }

        if (m_outboundRequestTtransactionContextTriplet.count() > 0) {
            m_OutboundRequestTransactionContextSection = new OutboundRequestTransactionContextSection(m_stream);
        }

        if (m_outboundRequestSecurityContextTriplet.count() > 0) {
            m_outboundRequestSecurityContextSection = new OutboundRequestSecurityContextSection(m_stream);
        }

        if (m_outboundRequestCicsContextTriplet.count() > 0) {
            m_outboundRequestCicsContextSection = new OutboundRequestCicsContextSection(m_stream);
        }

    } // OutboundRequestSmfRecord.OutboundRequestSmfRecord(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     *
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // OutboundRequestSmfRecord.supportedVersion()

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

        m_outboundRequestInfoTriplet.dump(aPrintStream, 3);
        m_WolaOutboundRequestTypeSpecificTriplet.dump(aPrintStream, 4);
        m_outboundRequestTtransactionContextTriplet.dump(aPrintStream, 5);
        m_outboundRequestSecurityContextTriplet.dump(aPrintStream, 6);
        m_outboundRequestCicsContextTriplet.dump(aPrintStream, 7);
        m_OtmaOutboundRequestTypeSpecificTriplet.dump(aPrintStream, 8);

        m_platformNeutralSection.dump(aPrintStream, 1);

        m_zosServerInfoSection.dump(aPrintStream, 2);

        m_outboundRequestInfoSection.dump(aPrintStream, 3);

        if (m_WolaOutboundRequestTypeSpecificTriplet.count() > 0) {
            m_OutboundRequestWolaTypeSpecificSection.dump(aPrintStream, 4);
        }

        if (m_outboundRequestTtransactionContextTriplet.count() > 0) {
            m_OutboundRequestTransactionContextSection.dump(aPrintStream, 5);
        }

        if (m_outboundRequestSecurityContextTriplet.count() > 0) {
            m_outboundRequestSecurityContextSection.dump(aPrintStream, 6);
        }

        if (m_outboundRequestCicsContextTriplet.count() > 0) {
            m_outboundRequestCicsContextSection.dump(aPrintStream, 7);
        }

        if (m_OtmaOutboundRequestTypeSpecificTriplet.count() > 0) {
            m_OutboundRequestOtmaTypeSpecificSection.dump(aPrintStream, 8);
        }

        aPrintStream.pop();

    } // OutboundRequestSmfRecord.dump()

} // OutboundRequestSmfRecord