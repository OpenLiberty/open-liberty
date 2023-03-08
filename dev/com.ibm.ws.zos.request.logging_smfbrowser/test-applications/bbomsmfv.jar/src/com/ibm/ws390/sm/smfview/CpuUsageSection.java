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

//  ------------------------------------------------------------------------------
/** Data container for SMF data related to a Smf record product section. */
public class CpuUsageSection extends SmfEntity {

    /** Supported version of this class. */
    private final static int s_supportedVersion = 1;

    /** version of this section */
    public int m_version;
    /** Type of data (1=EJB container, 2=Web Container) */
    public int m_dataType;
    /** cpu time for this item */
    public long m_cpuTime; //@SU99
    /** elapsed time for this item */
    public long m_elapsedTime; //@SU99
    /** count of calls to this item */
    public int m_invocationCount;
    /** length of first string identifying this items */
    public int m_string1Length;
    /** length of second string identifying this item */
    public int m_string2Length;
    /** first string identifying this item */
    public String m_String1; //@SU9
    /** second string identifying this item */
    public String m_String2; //@SU9

    //----------------------------------------------------------------------------
    /**
     * CpuUsageSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this CpuUsageSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public CpuUsageSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_version = aSmfStream.getInteger(4);

        m_dataType = aSmfStream.getInteger(4);

        m_cpuTime = aSmfStream.getLong(); //@SU99

        // m_elapsedTime = aSmfStream.getByteBuffer(8);
        m_elapsedTime = aSmfStream.getLong(); //@SU99

        m_invocationCount = aSmfStream.getInteger(4);

        m_string1Length = aSmfStream.getInteger(4);

        m_String1 = aSmfStream.getString(256, SmfUtil.EBCDIC);

        m_string2Length = aSmfStream.getInteger(4);

        m_String2 = aSmfStream.getString(256, SmfUtil.EBCDIC);

    } // CpuUsageSection(..)

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
     * Dumps the fields of this object to a print stream.
     * 
     * @param aPrintStream   The stream to print to.
     * @param aTripletNumber The triplet number of this CpuUsageSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        // Append a user readable type string as a description
        // instead of just the number for the data types
        String dataTypeString = "";
        switch (m_dataType) {
            case 1:
                dataTypeString = "EJB Container";
                break;
            case 2:
                dataTypeString = "Web Container";
                break;
        }

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "CpuUsageSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Version         ", m_version);
        aPrintStream.printlnKeyValue("Data Type       ", m_dataType);
        aPrintStream.printlnKeyValueString("Request Type    ", m_dataType, dataTypeString);
        aPrintStream.printlnKeyValue("CPU Time        ", m_cpuTime); //@SU99
        aPrintStream.printlnKeyValue("Elapsed Time    ", m_elapsedTime); //@SU99
        aPrintStream.printlnKeyValue("Invocation Count", m_invocationCount);
        aPrintStream.printlnKeyValue("String 1 Length ", m_string1Length);
        aPrintStream.printlnKeyValueString("String 1        ", m_string1Length, m_String1); //@SU99
        aPrintStream.printlnKeyValue("String 2 Length ", m_string2Length);
        aPrintStream.printlnKeyValueString("String 2        ", m_string2Length, m_String2); //@SU99

        aPrintStream.pop();

        // PerformanceSummary.writeString(".9CPU", 5);   //@SU9 Just let them know a CPU Section is present
        // Write CPU #s to Summary Report		              	           //@SU9   
        PerformanceSummary.writeString("\n          9CPU:", 16); //@SU99 
        // PerformanceSummary.writeInt(m_dataType, 1);                   //@SU9 
        PerformanceSummary.writeString(dataTypeString, 4); //@SU9 
        PerformanceSummary.writeString(m_String1, 20); //@SU9 
        PerformanceSummary.writeString("/", 1); //@SU9 
        PerformanceSummary.writeString(m_String2, 20); //@SU9 
        PerformanceSummary.writeInt(m_invocationCount, 6); //@SU9 
        PerformanceSummary.writeLong(m_elapsedTime, 9); //@SU99 
        PerformanceSummary.writeLong(m_cpuTime, 10);
        PerformanceSummary.writeString(" ", 11); //@SU9
        ++PerformanceSummary.lineNumber; // Increment Line #  //@SU9            //@SU9 

    } // dump()

} // CpuUsageSection
