/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip;

import java.util.HashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.ws.sip.stack.transaction.transport.connections.tls.SSLRepertoire;

/**
 * creates channel-framework chains using standalone configuration. in the past,
 * this class used to implement SIPConnectionFactory and create channel chains
 * when running in standalone mode with channel framework enabled. currently,
 * SIPConnectionFactoryImplWs is the only connection factory that supports
 * channel framework, and is used either in standalone or under WAS. currently
 * the only thing this class does is create the chain group when running in
 * standalone mode, using custom config properties as the source of information
 * for listening points.
 * 
 * @author ran
 */
public class SIPConnectionFactoryImpl
{
	/** class logger */
	private static final TraceComponent tc = Tr.register(SIPConnectionFactoryImpl.class);
	
	/**
	 * chain counter. needed for creating unique chain names in case of multiple
	 * listening points on the same transport
	 */ 
	private static int s_chains = 0;


	/**
	 * translates tls properties from stack config to channel config
	 */
	private static HashMap getTlsProperties() {
		SSLRepertoire repertoire =
			com.ibm.ws.sip.stack.transaction.transport.connections.tls.SIPConnectionFactoryImpl.createSSLRepertoire();
		HashMap tlsProperties = new HashMap();

		String protocol = repertoire.getProtocol();
		if (protocol != null) {
			tlsProperties.put(Constants.SSLPROP_SSLTYPE, protocol);
		}
		String keyManager = repertoire.getKeyManagerName();
		if (keyManager != null) {
			tlsProperties.put(Constants.SSLPROP_KEY_MANAGER, keyManager);
		}
		String keyStoreType = repertoire.getKeyStoreType();
		if (keyStoreType != null) {
			tlsProperties.put(Constants.SSLPROP_KEY_STORE_TYPE, keyStoreType);
		}
		String keyStoreProvider = repertoire.getKeyStoreProvider();
		if (keyStoreProvider != null) {
			tlsProperties.put(Constants.SSLPROP_KEY_STORE_PROVIDER, keyStoreProvider);
		}
		String keyStoreFile = repertoire.getKeyStoreFile();
		if (keyStoreFile != null) {
			tlsProperties.put(Constants.SSLPROP_KEY_STORE, keyStoreFile);
		}
		String keyStorePassword = repertoire.getKeyStorePassword();
		if (keyStorePassword != null) {
			tlsProperties.put(Constants.SSLPROP_KEY_STORE_PASSWORD, keyStorePassword);
		}
		String trustManagerName = repertoire.getTrustManagerName();
		if (trustManagerName != null) {
			tlsProperties.put(Constants.SSLPROP_TRUST_MANAGER, trustManagerName);
		}
		String trustStoreType = repertoire.getTrustStoreType();
		if (trustStoreType != null) {
			tlsProperties.put(Constants.SSLPROP_TRUST_STORE_TYPE, trustStoreType);
		}
		String trustStoreProvider = repertoire.getTrustStoreProvider();
		if (trustStoreProvider != null) {
			tlsProperties.put(Constants.SSLPROP_TRUST_STORE_PROVIDER, trustStoreProvider);
		}
		String trustStoreFile = repertoire.getTrustStoreFile();
		if (trustStoreFile != null) {
			tlsProperties.put(Constants.SSLPROP_TRUST_STORE, trustStoreFile);
		}
		String trustStorePassword = repertoire.getTrustStorePassword();
		if (trustStorePassword != null) {
			tlsProperties.put(Constants.SSLPROP_TRUST_STORE_PASSWORD, trustStorePassword);
		}
		String contextProvider = repertoire.getContextProvider();
		if (contextProvider != null) {
			tlsProperties.put(Constants.SSLPROP_CONTEXT_PROVIDER, contextProvider);
		}

		boolean clientAuthentication = repertoire.isClientAuthenticationEnabled();
		tlsProperties.put(Constants.SSLPROP_CLIENT_AUTHENTICATION, Boolean.toString(clientAuthentication));

		return tlsProperties;
	}
}
