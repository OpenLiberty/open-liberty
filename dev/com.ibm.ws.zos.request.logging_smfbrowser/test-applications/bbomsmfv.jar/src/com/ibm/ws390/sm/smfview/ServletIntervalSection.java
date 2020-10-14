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
import java.util.Date;

//------------------------------------------------------------------------------
/** Data container for SMF interval data related to a servlet. */
public class ServletIntervalSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Name of the servlet. */
    public String m_name;

    /** Number of total requests. */
    public int m_totalRequestN;

    /** Average response time. */
    public int m_averageResponseTime;

    /** Minimum response time. */
    public int m_minResponseTime;

    /** Maximum response time. */
    public int m_maxResponseTime;

    /** Number of errors during servlet execution. */
    public int m_errorN;

    /** Time when the servlet was loaded. */
    public String m_loadedSince;

    /** Average CPU Time. */
    public long m_avgCpuTime; // @MD17014 A

    /** Minimum CPU Time. */
    public long m_minCpuTime; // @MD17014 A

    /** Maximum CPU Time . */
    public long m_maxCpuTime; // @MD17014 A

    //----------------------------------------------------------------------------
    /**
     * BeanSection constructor from Smf stream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of BeanSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public ServletIntervalSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_name = aSmfStream.getString(256, SmfUtil.UNICODE);

        m_totalRequestN = aSmfStream.getInteger(4);

        m_averageResponseTime = aSmfStream.getInteger(4);

        m_minResponseTime = aSmfStream.getInteger(4);

        m_maxResponseTime = aSmfStream.getInteger(4);

        m_errorN = aSmfStream.getInteger(4);

        /* get the servlet load time. */
        m_loadedSince = aSmfStream.getString(16, SmfUtil.EBCDIC);

        if (version() >= 2) { // @MD17014 A

            m_avgCpuTime = aSmfStream.getLong(); // @MD17014 A

            m_minCpuTime = aSmfStream.getLong(); // @MD17014 A

            m_maxCpuTime = aSmfStream.getLong(); // @MD17014 A

        } // @MD17014 A

    } // ServletIntervalSection(...)

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
     * Dumps the object to a print stream.
     * 
     * @param aPrintStream       The stream to print to
     * @param aBaseTripletNumber The triplet number of the base item.
     * @param aTripletNumber     The triplet number of this BeanMethodInfo
     */
    public void dump(
                     SmfPrintStream aPrintStream,
                     int aBaseTripletNumber,
                     int aTripletNumber) {

        aPrintStream.println("");
        String tripletId = Integer.toString(aBaseTripletNumber) // @L2C
                           + "." + Integer.toString(aTripletNumber); // @L2C
        aPrintStream.printKeyValue("Triplet #", tripletId); // @L2C
        aPrintStream.printlnKeyValue("Type", "ServletIntervalSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Name", m_name);
        aPrintStream.printlnKeyValue("# requests", m_totalRequestN);
        aPrintStream.printlnTimeMills("AverageResponseTime", m_averageResponseTime);
        aPrintStream.printlnTimeMills("MinimumResponseTime", m_minResponseTime);
        aPrintStream.printlnTimeMills("MaximumResponseTime", m_maxResponseTime);
        aPrintStream.printlnKeyValue("# errors", m_errorN);
        aPrintStream.printlnKeyValue("Loaded since (raw)", m_loadedSince);
        String safeLoadedSince = "0";
        if ((m_loadedSince != null) && (m_loadedSince.length() > 0)) {
            safeLoadedSince = m_loadedSince;
        }
        long decimalLoadedSince = Long.parseLong(safeLoadedSince, 16);
        Date dateLoadedSince = new Date(decimalLoadedSince);
        aPrintStream.printlnKeyValue("Loaded since", dateLoadedSince.toString());
        // @MD17014 5 A
        if (version() >= 2) {
            aPrintStream.printlnKeyValue("Average CPU Time", m_avgCpuTime);
            aPrintStream.printlnKeyValue("Minimum CPU Time", m_minCpuTime);
            aPrintStream.printlnKeyValue("Maximum CPU Time", m_maxCpuTime);
        }

        aPrintStream.pop();

        // Write Response Time & CPU Time				           	//@SW   
        if (m_totalRequestN > 0) { //@SW  
            PerformanceSummary.writeString("\n ", 32); //@SW
            PerformanceSummary.writeString(m_name, 30); //@SW
            PerformanceSummary.writeInt(m_totalRequestN, 5); //@SW9 
            PerformanceSummary.writeString(" ", 1); //@SW9
            PerformanceSummary.writeInt(m_averageResponseTime, 8); //@SW9            

            PerformanceSummary.writeLong(m_avgCpuTime, 10); //@SW99 
            PerformanceSummary.writeString(" -Av", 4); //@SW99

            ++PerformanceSummary.lineNumber; // Increment Line #  //@SU9
        } // endif m_totalRequestN > 0

    } // dump()

} // ServletIntervalSection
