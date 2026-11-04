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
/** Data container for SMF data related to a JVM Heap. */
public class JvmHeapSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 4; /* @LI4369-4C */

    /** Server Region ASID. */
    public int m_serverRegionASID;

    /** Heap Type. */
    public int m_heapType;

    /** Garbage collection count. */
    public int m_garbageCollectionCount;

    /** Free storage. */
    public long m_freeStorage;

    /** Total storage. */
    public long m_totalStorage;

    //----------------------------------------------------------------------------
    /**
     * JvmHeapSection constructor from SmfSream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream        Smf stream to create this instance of JvmHeapSection from.
     * @param aRequestedVersion Version as required by the SmfRecord.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public JvmHeapSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_serverRegionASID = aSmfStream.getInteger(4);

        m_heapType = aSmfStream.getInteger(4);

        m_garbageCollectionCount = aSmfStream.getInteger(4);

        m_freeStorage = aSmfStream.getLong();

        m_totalStorage = aSmfStream.getLong();

    } // JvmHeapSection.JvmHeapSection(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the supported version of this class.
     * 
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {

        return s_supportedVersion;

    } // JvmHeapSection.supportedVersion()

    //----------------------------------------------------------------------------
    /**
     * Dumps the object into a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet
     *                           where this instance of JvmHeapSection originates from.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "JvmHeapSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("Server Region ASID", m_serverRegionASID);
        aPrintStream.printlnKeyValue("Heap Type", m_heapType);
        aPrintStream.printlnKeyValue(
                                     "Garbage Collection Count", m_garbageCollectionCount);
        aPrintStream.printlnKeyValue("free Storage ", m_freeStorage);
        aPrintStream.printlnKeyValue("total Storage", m_totalStorage);

        aPrintStream.pop();

    } //JvmHeapSection.dump()

} // JvmHeapSection
