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
/** Data container for SMF data related to a BOSS BO method. */
public class MethodSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 2;

    /** Name of the BOSS BO method. */
    public String m_methodName;

    /** Number of times the represented bean method was invoked. */
    public int m_numberOfInvocations;

    /** Number of exceptions thrown. */
    public int m_numberOfExceptionsThrown;

    /** Average method response time. */
    public int m_averageResponseTime;

    /** Maximum method response time. */
    public int m_maximumResponseTime;

    //----------------------------------------------------------------------------
    /**
     * MethodSection constructor from a SmfStream.
     * 
     * @param aSmfStream        SmfStream to be used to build this MethodSection.
     * @param aRequestedVersion Version as required by the SmfRecord.
     *                              The requested version is currently set in the product section.
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public MethodSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_methodName = aSmfStream.getString(256, SmfUtil.EBCDIC);

        m_numberOfInvocations = aSmfStream.getInteger(4); //    @L3C

        m_numberOfExceptionsThrown = aSmfStream.getInteger(4); //    @L3C

        m_averageResponseTime = aSmfStream.getInteger(4); //    @L3C

        m_maximumResponseTime = aSmfStream.getInteger(4); //    @L3C

    } // MethodSection(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the version supported by this class.
     * 
     * @return Version supported by this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // supportedVersion()

    //----------------------------------------------------------------------------
    /**
     * Dumps the fields of this object to a print stream.
     * 
     * @param aPrintStream       The stream to print to
     * @param aBaseTripletNumber The triplet number of the base item.
     * @param aTripletNumber     The triplet number of this BeanMethodInfo
     */
    public void dump(
                     SmfPrintStream aPrintStream,
                     int aBaseTripletNumber, // @L2C
                     int aTripletNumber) { // @L2A

        aPrintStream.println("");
        String tripletId = Integer.toString(aBaseTripletNumber) // @L2C 
                           + "." + Integer.toString(aTripletNumber); // @L2C
        aPrintStream.printKeyValue("Triplet #", tripletId); // @L2M
        aPrintStream.printlnKeyValue("Type", "MethodSection"); // @L2M

        aPrintStream.push();

        aPrintStream.printKeyValue("MethodName", m_methodName);
        aPrintStream.printKeyValue("Invocations", m_numberOfInvocations);
        aPrintStream.printlnKeyValue("ExceptionsThrown", m_numberOfExceptionsThrown);

        aPrintStream.printlnTime("AverageResponseTime", m_averageResponseTime);
        aPrintStream.printlnTime("MaximumResponseTime", m_maximumResponseTime);

        aPrintStream.pop();

    } // dump(...)

} // MethodSection