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
 * Formats the SMF 103 Subtype 14 record
 *
 */
public class SMFType103SubType14 extends SmfRecord {

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
    public SMFType103SubType14(SmfRecord smfRecord) throws UnsupportedVersionException, UnsupportedEncodingException {
        super(smfRecord);
    }

    @Override
    public String subtypeToString() {
        return "IHS Request Info";
    }

    @Override
    public void dump(SmfPrintStream aPrintStream) {
        String method = null, uri = null, host = null, rip = null;

        super.dump(aPrintStream);
        int pid = m_stream.getInteger(4);
        String cpu = null;

        int method_len = m_stream.getInteger(4);
        int domain_len = m_stream.getInteger(4);
        int uri_len = m_stream.getInteger(4);
        int remote_ip_len = m_stream.getInteger(4);
        try {
            cpu = m_stream.getString(8, "IBM1047");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        long elapsed = m_stream.getLong();

        try {
            method = m_stream.getString(method_len, "IBM1047"); //26
            host = m_stream.getString(domain_len, "IBM1047"); //126
            uri = m_stream.getString(uri_len, "IBM1047"); //376
            rip = m_stream.getString(remote_ip_len, "IBM1047"); //406
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        aPrintStream.println("pid=" + pid + " method=" + method + " host=" + host + " uri=" + uri + " rip = " + rip +
                             " elapsed= " + Long.toString(elapsed) + " cpu=" + cpu);
    }

}
