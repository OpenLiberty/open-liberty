/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.internal;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;

public class AcmeCertificate {

	/**
	 * The certificate returned from the ACME server.
	 */
	private final X509Certificate certificate;

	/**
	 * The certificate chain returned from the ACME server.
	 */
	private final List<X509Certificate> certificateChain;

	/**
	 * The certificate's public / private keypair.
	 */
	private final KeyPair keyPair;

	public AcmeCertificate(KeyPair keyPair, X509Certificate certificate, List<X509Certificate> certChain) {
		this.keyPair = keyPair;
		this.certificate = certificate;
		this.certificateChain = certChain;
	}

	public X509Certificate getCertificate() {
		return this.certificate;
	}

	public List<X509Certificate> getCertificateChain() {
		return this.certificateChain;
	}

	public KeyPair getKeyPair() {
		return this.keyPair;
	}
}
