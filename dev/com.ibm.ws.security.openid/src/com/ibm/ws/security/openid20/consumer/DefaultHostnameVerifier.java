/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.consumer;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.X509HostnameVerifier;

/**
 *
 */
public class DefaultHostnameVerifier implements X509HostnameVerifier {

    /** {@inheritDoc} */
    @Override
    public void verify(String host, SSLSocket ssl) throws IOException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void verify(String host, X509Certificate cert) throws SSLException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public boolean verify(String hostame, SSLSession session) {
        // TODO Auto-generated method stub
        return true;
    }

}
