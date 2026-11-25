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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.ibm.jzos.ZFile;

//------------------------------------------------------------------------------
/** Implementation for ISmfFile to access a local Smf file. */
public class JzOSSmfFile implements ISmfFile {

    /** Record file. */
    private ZFile m_file = null;

    private byte[] m_buffer = null;

    //----------------------------------------------------------------------------
    /** SmfFile constructor. */
    public JzOSSmfFile() {
    }

    //----------------------------------------------------------------------------
    /**
     * Open local Smf file.
     * 
     * @param aName Name of Smf file. If it starts DD: then we assume its a DD name, otherwise we assume
     *                  its a fully qualified dataset name.
     * @throws IOException in case of IO errors.
     */
    @Override
    public void open(String aName) throws IOException {

        try {
            if (!aName.startsWith("DD:")) {
                aName = "'" + aName + "'";
            }
            m_file = new ZFile("//" + aName, "rb,type=record");
            m_buffer = new byte[m_file.getLrecl()];
        } catch (Exception e) {
            e.printStackTrace();
        }

    } // SmfFile(...)

    //----------------------------------------------------------------------------
    /**
     * Close local Smf file.
     * 
     * @throws IOException in case of IO errors.
     */
    @Override
    public void close() throws IOException {

        if (m_file != null)
            m_file.close();

        m_file = null;

    } // close()

    //----------------------------------------------------------------------------
    /**
     * Read a record.
     * 
     * @return byte array containing the record read.
     * @throws java.io.IOException in case of IO errors.
     */
    @Override
    public byte[] read() throws IOException {

        if (m_file == null)
            return null;

        int byteN = m_file.read(m_buffer);

        if ((byteN == -1) | (byteN == 0))
            return null;
        byte[] record = new byte[byteN];

        ByteArrayInputStream s = new ByteArrayInputStream(m_buffer, 0, byteN);

        s.read(record, 0, byteN);

        return record;

    } // read(...)

}