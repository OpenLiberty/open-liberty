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

package com.ibm.ws.security.acme.internal.util;

public class AcmeConstants {

	public static final String ACME_CONFIG_PID = "com.ibm.ws.security.acme.config";
	public static final String ACME_CONTEXT_ROOT = "/.well-known/acme-challenge";

	/*
	 * Constants that match the metatype fields
	 */
	public static final String DIR_URI = "directoryURI";
	public static final String DOMAIN = "domain";
	public static final String VALID_FOR = "validFor";
	public static final String COUNTRY = "country";
	public static final String LOCALITY = "locality";
	public static final String STATE = "state";
	public static final String ORG = "organization";
	public static final String OU = "organizationalUnit";

	// Challenge and order related fields.
	public static final String CHALL_RETRIES = "challengeRetries";
	public static final String CHALL_RETRY_WAIT = "challengeRetryWait";
	public static final String ORDER_RETRIES = "orderRetries";
	public static final String ORDER_RETRY_WAIT = "orderRetryWait";

	// ACME account related fields.
	public static final String ACCOUNT_KEY_FILE = "accountKeyFile";
	public static final String ACCOUNT_CONTACT = "accountContact";
	public static final String ACCEPT_TERMS = "acceptTermsOfService";
	public static final String DOMAIN_KEY_FILE = "domainKeyFile";

	/*
	 * End constants that match the metatype fields
	 */

	/**
	 * Key size for generated domain and account key pairs.
	 */
	public static final int KEY_SIZE = 2048;
	
	public static final String DEFAULT_KEY_STORE = "defaultKeyStore";
	public static final String DEFAULT_ALIAS = "default";
	public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
	
	public static final String ACCOUNT_TYPE = "account";
	public static final String DOMAIN_TYPE = "domain";
}
