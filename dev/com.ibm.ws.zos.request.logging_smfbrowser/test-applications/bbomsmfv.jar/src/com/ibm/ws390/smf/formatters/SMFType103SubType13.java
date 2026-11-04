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

package com.ibm.ws390.smf.formatters;

import java.io.UnsupportedEncodingException;

import com.ibm.ws390.sm.smfview.SmfPrintStream;
import com.ibm.ws390.sm.smfview.SmfRecord;
import com.ibm.ws390.sm.smfview.UnsupportedVersionException;

/**
 * Formats the SMF 103 Subtype 13 record
 *
 */
public class SMFType103SubType13 extends SmfRecord {

    /**
     * Constructor
     *
     * @param smfRecord
     *                      The SMF record to be contained by this object
     * @throws UnsupportedVersionException
     *                                          bad version
     * @throws UnsupportedEncodingException
     *                                          bad encoding
     */
    public SMFType103SubType13(SmfRecord smfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {
        super(smfRecord);
    }

    @Override
    public String subtypeToString() {
        return "IHS Thread Info";
    }

    private int SERVERNAME_LEN_OFFSET = 52;

    @Override
    public void dump(SmfPrintStream aPrintStream) {
        String server_name = null;

        String[] thread_keys = new String[] { "ready", "busy", "reading", "writing", "logging", "dns", "closing", "keepalive", null };
        Integer[] threads = new Integer[thread_keys.length];

        super.dump(aPrintStream);

        /* Gather data from record -- don't re-order these calls! */
        int ppid = m_stream.getInteger(4);
        for (int i = 0; thread_keys[i] != null; i++) {
            threads[i] = new Integer(m_stream.getInteger(4));
        }

        long bytes = m_stream.getLong();
        long requests = m_stream.getLong();

        /* Get the variable width server name */
        int namelen = m_stream.getInteger(4);
        try {
            server_name = m_stream.getString(namelen, "IBM1047");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        /* Start writing to the output stream */

        if (server_name != null) {
            aPrintStream.printKeyValue("ServerName", server_name);
        }
        aPrintStream.printlnKeyValue("PID", ppid);

        aPrintStream.print("Thread Details: [ ");
        for (int i = 0; thread_keys[i] != null; i++) {
            aPrintStream.printKeyValue(thread_keys[i], threads[i].intValue());
        }

        aPrintStream.println("]");

        aPrintStream.print("Cumulative: [ ");
        aPrintStream.printKeyValue("kbytes", Long.toString(bytes));
        aPrintStream.printKeyValue("requests", Long.toString(requests));
        aPrintStream.println("]");
    }

}
