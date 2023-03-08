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

//------------------------------------------------------------------------------
/** Data container for SMF data related to a triplet. */
public class Triplet {

    /** Offset of data described by this triplet in SmfRecord. */
    private int m_offset;

    /** Length of data described by this triplet in SmfRecord. */
    private int m_length;

    /** Number of data sections described by this triplet in SmfRecord. */
    private int m_count;

    //----------------------------------------------------------------------------
    /**
     * Triplet constructor from SmfStream.
     * The instance is filled from the provided SmfStream.
     * 
     * @param aSmfStream Smf stream to create this instance of ServeIntervalSection from.
     */
    public Triplet(SmfStream aSmfStream) {

        m_offset = aSmfStream.getInteger(4);

        m_length = aSmfStream.getInteger(4);

        m_count = aSmfStream.getInteger(4);

    } // Triplet(...)

    //----------------------------------------------------------------------------
    /**
     * Returns the offset to the section from RDW.
     * 
     * @return Offset to the section from RDW.
     */
    public int offset() {
        return m_offset;
    } // offset()

    //----------------------------------------------------------------------------
    /**
     * Returns the length of the section.
     * 
     * @return Length of the section.
     */
    public int length() {
        return m_length;
    } // length()

    //----------------------------------------------------------------------------
    /**
     * Returns the number of sections.
     * 
     * @return Number of sections.
     */
    public int count() {
        return m_count;
    } // count()

    //----------------------------------------------------------------------------
    /**
     * Dumps the object into a print stream.
     * 
     * @param aPrintStream   print stream to dump to.
     * @param aTripletNumber number of the triplet.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {
        String Hex_offset = Integer.toHexString(m_offset); // @SU9
        String Hex_length = Integer.toHexString(m_length); // @SU9
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printKeyValue("offsetDec", m_offset);
        aPrintStream.printKeyValue("offsetHex", Hex_offset); // @SU9
        aPrintStream.printKeyValue("lengthDec", m_length);
        aPrintStream.printKeyValue("lengthHex", Hex_length); // @SU9
        aPrintStream.printlnKeyValue("count", m_count);

    } // dump(...)

    /* @L1 start */
    //----------------------------------------------------------------------------
    /**
     * Dumps the object into a print stream.
     * 
     * @param aPrintStream       Print stream to dump to.
     * @param aBaseTripletNumber Number of the containing triplet.
     * @param aTripletNumber     Number of the triplet.
     */
    public void dump(
                     SmfPrintStream aPrintStream,
                     int aBaseTripletNumber,
                     int aTripletNumber) {

        String Hex_offset = Integer.toHexString(m_offset); // @SU9
        String tripletId = Integer.toString(aBaseTripletNumber) + "." +
                           Integer.toString(aTripletNumber);
        aPrintStream.printKeyValue("Triplet #", tripletId);
        aPrintStream.printKeyValue("offsetDec", m_offset);
        aPrintStream.printKeyValue("offsetHex", Hex_offset); // @SU9
        aPrintStream.printKeyValue("length", m_length);
        aPrintStream.printlnKeyValue("count", m_count);

    } // dump(...)
    /* @L1 end */

} // Triplet