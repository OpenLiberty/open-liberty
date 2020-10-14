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

public class LibertyBatchCompletionSection extends SmfEntity {

    /** Supported version of this class. */
    public final static int s_supportedVersion = 1;

    /** version of section. */
    public int m_version;

    /** batch Status */
    public int m_batchStatus;

    /** exit Status */
    public String m_exitStatus;

    /** flags */
    public long m_flags;

    /** partition plan */
    public int m_partitionPlan;

    /** partition count */
    public int m_partitionCount;

    /** metrics */
    public long m_readCount;
    public long m_writeCount;
    public long m_commitCount;
    public long m_rollbackCount;
    public long m_readSkipCount;
    public long m_processSkipCount;
    public long m_filterCount;
    public long m_writeSkipCount;

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

    public LibertyBatchCompletionSection(SmfStream aSmfStream) throws UnsupportedVersionException, UnsupportedEncodingException {

        super(s_supportedVersion);
        m_version = aSmfStream.getInteger(4);

        m_batchStatus = aSmfStream.getInteger(4);
        m_exitStatus = aSmfStream.getString(128, SmfUtil.ASCII);
        m_flags = aSmfStream.getLong();
        m_partitionPlan = aSmfStream.getInteger(4);
        m_partitionCount = aSmfStream.getInteger(4);
        m_readCount = aSmfStream.getLong();
        m_writeCount = aSmfStream.getLong();
        m_commitCount = aSmfStream.getLong();
        m_rollbackCount = aSmfStream.getLong();
        m_readSkipCount = aSmfStream.getLong();
        m_processSkipCount = aSmfStream.getLong();
        m_filterCount = aSmfStream.getLong();
        m_writeSkipCount = aSmfStream.getLong();
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
        aPrintStream.printlnKeyValue("Type", "LibertyBatchCompletionSection");

        aPrintStream.push();
        aPrintStream.printlnKeyValue("Completion Section Version             ", m_version);

        aPrintStream.printlnKeyValue("batch status                          ", m_batchStatus);
        aPrintStream.printlnKeyValue("exit status                           ", m_exitStatus);
        /* flags when we have them */
        aPrintStream.printlnKeyValue("partition plan                        ", m_partitionPlan);
        aPrintStream.printlnKeyValue("partition count                       ", m_partitionCount);
        aPrintStream.printlnKeyValue("read count                            ", m_readCount);
        aPrintStream.printlnKeyValue("write count                           ", m_writeCount);
        aPrintStream.printlnKeyValue("commit count                          ", m_commitCount);
        aPrintStream.printlnKeyValue("rollback count                        ", m_rollbackCount);
        aPrintStream.printlnKeyValue("read skip count                       ", m_readSkipCount);
        aPrintStream.printlnKeyValue("process skip count                    ", m_processSkipCount);
        aPrintStream.printlnKeyValue("filter count                          ", m_filterCount);
        aPrintStream.printlnKeyValue("write skip count                      ", m_writeSkipCount);

        aPrintStream.pop();
    }
}
