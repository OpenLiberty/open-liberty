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
/** Data container for SMF activity data related to a servlet. */
public class ServletActivitySection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2; // @MD17014 C

    /** Name of the servlet. */
    public String m_name;

    /** Response time. */
    public int m_responseTime;

    /** Number of errors during servlet execution. */
    public int m_errorN;

    /** Loaded flag. A 1 means that servlet was loaded as result of this request. */
    public int m_loadedFlag;

    /** Time when the servlet was loaded. */
    public String m_loadedSince;

    /** The Cpu time. */
    public long m_cpuTime; // @MD17014 A

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
    public ServletActivitySection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_name = aSmfStream.getString(256, SmfUtil.UNICODE);

        m_responseTime = aSmfStream.getInteger(4);

        m_errorN = aSmfStream.getInteger(4);

        m_loadedFlag = aSmfStream.getInteger(4);

        /* get the servlet load time. */
        m_loadedSince = aSmfStream.getString(16, SmfUtil.EBCDIC);

        if (version() >= 2) { // @MD17014 A

            m_cpuTime = aSmfStream.getLong(); // @MD17014 A  

        } // @MD17014 A

    } // ServletActivitySection(...)

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
        aPrintStream.printlnKeyValue("Type", "ServletActivitySection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Name", m_name);
        aPrintStream.printlnTimeMills("ResponseTime", m_responseTime);
        aPrintStream.printlnKeyValue("# errors", m_errorN);
        aPrintStream.printlnKeyValue("Loaded by this request", m_loadedFlag);
        aPrintStream.printlnKeyValue("Loaded since (raw)", m_loadedSince);
        String safeLoadedSince = "0";
        if ((m_loadedSince != null) && (m_loadedSince.length() > 0)) {
            safeLoadedSince = m_loadedSince;
        }
        long decimalLoadedSince = Long.parseLong(safeLoadedSince, 16);
        Date dateLoadedSince = new Date(decimalLoadedSince);
        aPrintStream.printlnKeyValue("Loaded since", dateLoadedSince.toString());
        // @MD17014 3 A
        if (version() >= 2) {
            aPrintStream.printlnKeyValue("CPU Time", m_cpuTime);
        }

        aPrintStream.pop();

        // Write Servlet name, Response Time & CPU Time				//@SW   
        PerformanceSummary.writeNewLine(); //@SUa
        PerformanceSummary.writeString("  ", 31); //@SW
        PerformanceSummary.writeString(m_name, 37); //@SW
        PerformanceSummary.writeInt(m_responseTime, 7); //@SW99                                                          	//@SUa
        PerformanceSummary.writeLong(m_cpuTime, 10); //@SW99

    } // dump()

} // ServletActivitySection
