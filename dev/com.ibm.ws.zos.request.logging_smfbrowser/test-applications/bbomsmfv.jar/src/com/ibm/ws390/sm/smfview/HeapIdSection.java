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
/** Data container for SMF data HeapID. */
public class HeapIdSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 4; /* @LI4369-4C */

    /** Heap ID. */
    public byte m_heapID[];

    /** Garbage collection count. */
    public long m_garbageCollectionCount;

    /** Minimum total storage. */
    public long m_minTotalStorage;

    /** Maximum total storage. */
    public long m_maxTotalStorage;

    /** Average total storage. */
    public long m_avgTotalStorage;

    /** Minimum Free storage. */
    public long m_minFreeStorage;

    /** Maximum Free storage. */
    public long m_maxFreeStorage;

    /** Average Free storage. */
    public long m_avgFreeStorage;

    //----------------------------------------------------------------------------
    /**
     * HeapIdSection constructor from a SmfStream.
     * 
     * @param aSmfStream        SmfStream to be used to build this HeapIdSection.
     * @param aRequestedVersion Version as required by the SmfRecord.
     *                              The requested version is currently set in the product section.
     * @throws UnsupportedVersionException  Exception to be thrown when version is not supported
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is detected.
     */
    public HeapIdSection(SmfStream aSmfStream, int aRequestedVersion) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(aRequestedVersion);

        m_heapID = aSmfStream.getByteBuffer(4);

        m_garbageCollectionCount = aSmfStream.getInteger(4);

        m_minTotalStorage = aSmfStream.getLong();

        m_maxTotalStorage = aSmfStream.getLong();

        m_avgTotalStorage = aSmfStream.getLong();

        m_minFreeStorage = aSmfStream.getLong();

        m_maxFreeStorage = aSmfStream.getLong();

        m_avgFreeStorage = aSmfStream.getLong();

    } // HeapIdSection(...)

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
     * @param aPrintStream   The stream to print to
     * @param aTripletNumber The triplet number of this BeanMethodInfo
     */
    public void dump(
                     SmfPrintStream aPrintStream,
                     int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "HeapIdSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("HeapID", m_heapID, null);
        aPrintStream.printlnKeyValue("Total Garbage Collection Count", m_garbageCollectionCount);
        aPrintStream.printlnKeyValue("Minimum Total Storage", m_minTotalStorage);
        aPrintStream.printlnKeyValue("Maximum Total Storage", m_maxTotalStorage);
        aPrintStream.printlnKeyValue("Average Total Storage", m_avgTotalStorage);
        aPrintStream.printlnKeyValue("Minimum Free Storage ", m_minFreeStorage);
        aPrintStream.printlnKeyValue("Maximum Free Storage ", m_maxFreeStorage);
        aPrintStream.printlnKeyValue("Average Free Storage ", m_avgFreeStorage);

        aPrintStream.pop();

    } // dump(...)

} // HeapIdSection
