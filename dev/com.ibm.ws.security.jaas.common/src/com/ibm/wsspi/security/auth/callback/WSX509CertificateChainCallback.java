/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.auth.callback;

import java.security.cert.X509Certificate;

import javax.security.auth.callback.Callback;

import com.ibm.ws.security.jaas.common.callback.AuthenticationHelper;

/**
 * JAAS Callback for certificates for WebSphere Application Server
 * <p>
 * The <code>WSX509CertificateChainCallback</code> allows a certificate chain to be
 * gathered by a <code>CallbackHandler</code> and passed to a <code>LoginModule</code>
 * stack.
 */
public class WSX509CertificateChainCallback implements Callback {
    private X509Certificate[] certificateChain;
    private final String prompt;

    /**
     * Construct a <code>WSX509CertificateChainCallback</code> object with a prompt hint.
     * 
     * @param prompt A String prompt hint.
     */
    public WSX509CertificateChainCallback(String prompt) {
        this.prompt = prompt;
    }

    /**
     * <p>
     * Construct a <code>WSX509CertificateChainCallback</code> object with a prompt hint and
     * a java.security.cert.X509Certificate[].
     * </p>
     * 
     * @param prompt A String prompt hint.
     * @param certChain An array of java.security.cert.X509Certificate
     */
    public WSX509CertificateChainCallback(String prompt, java.security.cert.X509Certificate[] certChain) {
        this.prompt = prompt;
        certificateChain = AuthenticationHelper.copyCertChain(certChain);
    }

    /**
     * Set the certificate chain
     * 
     * @param certChain an array of java.security.cert.X509Certificate[]
     */
    public void setX509CertificateChain(X509Certificate[] certChain) {
        certificateChain = AuthenticationHelper.copyCertChain(certChain);
    }

    /**
     * Get the certificate chain
     * 
     * @return an array of java.security.cert.X509Certificate[]
     */
    public X509Certificate[] getX509CertificateChain() {
        return AuthenticationHelper.copyCertChain(certificateChain);
    }

    /**
     * 
     * @return the String prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * <p>
     * Returns the name of the Callback. Typically, it is the name of the class.
     * </p>
     * 
     * @return The name of the Callback.
     */
    @Override
    public String toString() {
        return getClass().getName();
    }
}
