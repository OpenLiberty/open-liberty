/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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

/**
 * RawSmfRecord data. This class is used when the subtype is unrecognized.
 */
public class RawSmfRecord extends SmfRecord {
    /** Supported version of this implementation. */
    public final static int s_supportedVersion = 1;

    /**
     * Constructs a RawSmfRecord from a generic SmfRecord.
     * The generic record already parsed the generic part of the input stream.
     *
     * @param aSmfRecord SmfRecord to parse input from.
     * @throws UnsupportedVersionException  Exception thrown when the requested version is higher than the supported version.
     * @throws UnsupportedEncodingException Exception thrown when an unsupported encoding is encountered during Smf stream parse.
     */
    public RawSmfRecord(SmfRecord aSmfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {
        super(aSmfRecord);
    }

    /**
     * Returns the supported version of this class.
     *
     * @return supported version of this class.
     */
    @Override
    public int supportedVersion() {
        return s_supportedVersion;
    }

    /**
     * Dumps the raw record data into a print stream.
     *
     * @param aPrintStream print stream to dump to.
     */
    @Override
    public void dump(SmfPrintStream aPrintStream) {
        super.dump(aPrintStream);

        // Dump the raw record (minus the header, which we just printed).
        int len = m_stream.available();
        byte[] b = new byte[len];
        m_stream.read(b, 0, len);

        aPrintStream.printlnKeyValue("RawRecordData", b, SmfUtil.EBCDIC);
    }
}