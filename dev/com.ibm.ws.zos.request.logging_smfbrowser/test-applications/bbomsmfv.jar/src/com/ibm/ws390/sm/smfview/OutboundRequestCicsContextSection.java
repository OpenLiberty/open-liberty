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
/** Data container for SMF data related to a Smf Outbound Request CICS Context Section. */
public class OutboundRequestCicsContextSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;
    /** version of this section */
    public int m_version;
    /** CICS Context */
    public byte m_cicsContext[];
    /** reserved space */
    public byte m_theblob[];

    //----------------------------------------------------------------------------
    /**
     * OutboundRequestCicsContextSection constructor from a SmfStream.
     * 
     * @param aSmfStream SmfStream to be used to build this OutboundRequestCicsContextSection.
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public OutboundRequestCicsContextSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);

        m_version = aSmfStream.getInteger(4);

        m_cicsContext = aSmfStream.getByteBuffer(80);

        m_theblob = aSmfStream.getByteBuffer(28);

    } // OutboundRequestCicsContextSection(..)

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
     * @param aTripletNumber The triplet number of this OutboundRequestCicsContextSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "OutboundRequestCicsContextSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Version        ", m_version);
        aPrintStream.printlnKeyValue("CICS context   ", m_cicsContext, null);
        aPrintStream.printlnKeyValue("Reserved       ", m_theblob, null);

        aPrintStream.pop();

    } // dump()

} // OutboundRequestCicsContextSection
