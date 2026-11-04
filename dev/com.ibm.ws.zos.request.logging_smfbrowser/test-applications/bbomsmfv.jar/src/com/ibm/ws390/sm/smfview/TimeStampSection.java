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
public class TimeStampSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;
    /** Time the request was received */
    public String m_received;
    /** Time the request was placed on the queue to be processed by a servant */
    public String m_queued;
    /** Time the request was picked up by a servant */
    public String m_dispatched;
    /** Time dispatch completed */
    public String m_dispatchcomplete;
    /** Time processing for the request was completed in the controller (e.g. response sent) */
    public String m_complete;
    /** reserved */
    public byte m_theblob[];

    //----------------------------------------------------------------------------
    /**
     * TimeStampSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this TimeStampSection.
     *                       The requested version is currently set in the Platform Neutral Section
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public TimeStampSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        //132
        m_received = aSmfStream.getString(26, SmfUtil.EBCDIC);
        m_queued = aSmfStream.getString(26, SmfUtil.EBCDIC);
        m_dispatched = aSmfStream.getString(26, SmfUtil.EBCDIC);
        m_dispatchcomplete = aSmfStream.getString(26, SmfUtil.EBCDIC);
        m_complete = aSmfStream.getString(26, SmfUtil.EBCDIC);

        m_theblob = aSmfStream.getByteBuffer(2);

    } // TimeStampSection(..)

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
     * @param aTripletNumber The triplet number of this TimeStampSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "TimeStampSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Time Received         ", m_received);
        aPrintStream.printlnKeyValue("Time Queued           ", m_queued);
        aPrintStream.printlnKeyValue("Time Dispatched       ", m_dispatched);
        aPrintStream.printlnKeyValue("Time Dispatch Complete", m_dispatchcomplete);
        aPrintStream.printlnKeyValue("Time Complete         ", m_complete);
        aPrintStream.printlnKeyValue("Reserved for alignment", m_theblob, null);

        aPrintStream.pop();

        // Write Timestamps to Summary Report			 //@SU9   
        PerformanceSummary.writeString("\n         .9Ts: ", 17); //@SU9 
        PerformanceSummary.writeString(m_received, 26); //@SU9
        PerformanceSummary.writeString(" Received", 17); //@SU9 

        PerformanceSummary.writeString("\n         .9Ts: ", 17); //@SU9 
        PerformanceSummary.writeString(m_queued, 26); //@SU9
        PerformanceSummary.writeString(" Queued", 17); //@SU9 

        PerformanceSummary.writeString("\n         .9Ts: ", 17); //@SU9 
        PerformanceSummary.writeString(m_dispatched, 26); //@SU9
        PerformanceSummary.writeString(" Dispatched", 17); //@SU9 

        PerformanceSummary.writeString("\n         .9Ts: ", 17); //@SU9 
        PerformanceSummary.writeString(m_dispatchcomplete, 26); //@SU9
        PerformanceSummary.writeString(" dispatchComplete", 19); //@SU9

        PerformanceSummary.writeString("\n         .9Ts: ", 17); //@SU9 
        PerformanceSummary.writeString(m_complete, 26); //@SU9
        PerformanceSummary.writeString(" Complete", 17); //@SU9 

        //Increment Line #        
        PerformanceSummary.lineNumber = PerformanceSummary.lineNumber + 8; //@SU9

    } // dump()

} // TimeStampSection  
