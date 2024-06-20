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
/** Data container for SMF data related to a Smf record product section. */
public class ProductSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 4; // @LI4369-4C

    /** Codeset used within the interpreted record. */
    public String m_codeset;

    /** Endian type used within the interpreted record. */
    public int m_endian;

    /** z/OS time stamp format used within the interpreted record. */
    public int m_timeStampFormat;

    /** Index of the interpreted record. */
    public int m_indexOfThisRecord;

    /** Total number of records. */
    public int m_totalNumberOfRecords;

    /** Total number of triplets. */
    public int m_totalNumberOfTriplets;

    //----------------------------------------------------------------------------
    /**
     * ProductSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this ProductSection.
     *                       The requested version is currently set in the product section.
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public ProductSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        int version = aSmfStream.getInteger(4); //    @L1C

        // Later other classes check against the product section version
        // whether they are capable to support it.
        setVersion(version);

        m_codeset = aSmfStream.getString(8, SmfUtil.EBCDIC);

        m_endian = aSmfStream.getInteger(4); //    @L1C    

        m_timeStampFormat = aSmfStream.getInteger(4); //    @L1C

        m_indexOfThisRecord = aSmfStream.getInteger(4); //    @L1C                          

        m_totalNumberOfRecords = aSmfStream.getInteger(4); //    @L1C  

        m_totalNumberOfTriplets = aSmfStream.getInteger(4); //    @L1C

    } // ProductSection(..)

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
     * Returns the timeStampFormat as a String.
     * 
     * @return timeStampFormat as a String.
     */
    private String timeStampFormatToString() {
        switch (m_timeStampFormat) {
            case 1:
                return "S390STCK64";
            default:
                return "unknown format";
        }
    } // timeStampFormatToString()

    //----------------------------------------------------------------------------
    /**
     * Dumps the fields of this object to a print stream.
     * 
     * @param aPrintStream   The stream to print to.
     * @param aTripletNumber The triplet number of this ProductSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "ProductSection");

        aPrintStream.push();

        aPrintStream.printKeyValue("Version", version());
        aPrintStream.printKeyValue("Codeset", m_codeset);
        aPrintStream.printKeyValue("Endian", m_endian);
        aPrintStream.printlnKeyValueString(
                                           "TimeStampFormat", m_timeStampFormat, timeStampFormatToString());

        aPrintStream.printKeyValue("IndexOfThisRecord", m_indexOfThisRecord);
        aPrintStream.printKeyValue("Total # records", m_totalNumberOfRecords);
        aPrintStream.printKeyValue("Total # triplets", m_totalNumberOfTriplets);

        aPrintStream.pop();

    } // dump()

} // ProductSection