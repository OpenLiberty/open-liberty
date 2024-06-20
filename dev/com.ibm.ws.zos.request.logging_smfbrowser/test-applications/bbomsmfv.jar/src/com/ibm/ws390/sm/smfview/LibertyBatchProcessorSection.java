/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

public class LibertyBatchProcessorSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;

    /** version of section. */
    public int m_version;

    /** Total CPU at start */
    public long m_totalCPUStart;

    /** Total CPU at end */
    public long m_totalCPUEnd;

    /** CP Time at start */
    public long m_CPStart;

    /** CP Time at end */
    public long m_CPEnd;

    /** Offload at start */
    public long m_offloadStart;

    /** Offload at end */
    public long m_offloadEnd;

    /** offload on CP at start */
    public long m_offloadOnCpStart;

    /** offload on CP at end */
    public long m_offloadOnCpEnd;

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

    public LibertyBatchProcessorSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        m_version = aSmfStream.getInteger(4);

        m_totalCPUStart = aSmfStream.getLong();
        m_totalCPUEnd = aSmfStream.getLong();
        m_CPStart = aSmfStream.getLong();
        m_CPEnd = aSmfStream.getLong();
        m_offloadStart = aSmfStream.getLong();
        m_offloadEnd = aSmfStream.getLong();
        m_offloadOnCpStart = aSmfStream.getLong();
        m_offloadOnCpEnd = aSmfStream.getLong();

    }

    //----------------------------------------------------------------------------
    /**
     * Dumps the fields of this object to a print stream.
     *
     * @param aPrintStream   The stream to print to.
     * @param aTripletNumber The triplet number of this LibertyRequestInfoSection.
     */
    public void dump(SmfPrintStream aPrintStream, int aTripletNumber) {

        aPrintStream.println("");
        aPrintStream.printKeyValue("Triplet #", aTripletNumber);
        aPrintStream.printlnKeyValue("Type", "LibertyBatchProcessorSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Processor Section Version             ", m_version);

        aPrintStream.printlnKeyValue("Total CPU start                       ", m_totalCPUStart);
        aPrintStream.printlnKeyValue("Total CPU end                         ", m_totalCPUEnd);
        aPrintStream.printlnKeyValue("Time on CP start                      ", m_CPStart);
        aPrintStream.printlnKeyValue("Time on CP end                        ", m_CPEnd);
        aPrintStream.printlnKeyValue("Offload start                         ", m_offloadStart);
        aPrintStream.printlnKeyValue("Offload end                           ", m_offloadEnd);
        aPrintStream.printlnKeyValue("Offload on GP start                   ", m_offloadOnCpStart);
        aPrintStream.printlnKeyValue("Offload on GP end                     ", m_offloadOnCpEnd);

        aPrintStream.pop();
    }

}
