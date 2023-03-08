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

public class LibertyBatchUssSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;

    /** version of section. */
    public int m_version;

    /** process ID */
    public int m_pid;

    /** thread ID */
    public byte[] m_threadId;

    /** java Thread ID */
    public byte[] m_javaThreadId;

    public int m_uid;

    public int m_gid;

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

    public LibertyBatchUssSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        m_version = aSmfStream.getInteger(4);
        m_pid = aSmfStream.getInteger(4);
        m_threadId = aSmfStream.getByteBuffer(8);
        m_javaThreadId = aSmfStream.getByteBuffer(8);
        m_uid = aSmfStream.getInteger(4);
        m_gid = aSmfStream.getInteger(4);

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
        aPrintStream.printlnKeyValue("Type", "LibertyUSSSection");

        aPrintStream.push();

        aPrintStream.printlnKeyValue("USS Info Version                ", m_version);
        aPrintStream.printlnKeyValue("Server pid                      ", m_pid);
        aPrintStream.printlnKeyValue("Thread id                       ", m_threadId, null);
        aPrintStream.printlnKeyValue("Java Thread id                  ", m_javaThreadId, null);
        aPrintStream.printlnKeyValue("Submitter uid                   ", m_uid);
        aPrintStream.printlnKeyValue("Submitter gid                   ", m_gid);

        aPrintStream.pop();

    }
}
