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

import java.util.concurrent.TimeUnit;

public class AcmeConstants {

	public static final String ACME_CONFIG_PID = "com.ibm.ws.security.acme.config";
	public static final String ACME_CONTEXT_ROOT = "/.well-known/acme-challenge";

	/*
	 * Constants that match the metatype fields
	 */
	public static final String DIR_URI = "directoryURI";
	public static final String DOMAIN = "domain";
	public static final String VALID_FOR = "validFor";
	public static final String SUBJECT_DN = "subjectDN";

	// Challenge and order related fields.
	public static final String CHALL_POLL_TIMEOUT = "challengePollTimeout";
	public static final String ORDER_POLL_TIMEOUT = "orderPollTimeout";

	// ACME account related fields.
	public static final String ACCOUNT_KEY_FILE = "accountKeyFile";
	public static final String ACCOUNT_CONTACT = "accountContact";
	public static final String DOMAIN_KEY_FILE = "domainKeyFile";

	// Transport configuration.
	public static final String TRANSPORT_CONFIG = "acmeTransportConfig";
	public static final String TRANSPORT_PROTOCOL = "protocol";
	public static final String TRANSPORT_TRUST_STORE = "trustStore";
	public static final String TRANSPORT_TRUST_STORE_PASSWORD = "trustStorePassword";
	public static final String TRANSPORT_TRUST_STORE_TYPE = "trustStoreType";
	
	// Renewal configuration options
	public static final String RENEW_BEFORE_EXPIRATION = "renewBeforeExpiration";

	// Revocation checker configuration.
	public static final String REVOCATION_CHECKER = "acmeRevocationChecker";
	public static final String REVOCATION_CHECKER_ENABLED = "enabled";
	public static final String REVOCATION_OCSP_RESPONDER_URL = "ocspResponderUrl";
	public static final String REVOCATION_PREFER_CRLS = "preferCRLs";
	public static final String REVOCATION_DISABLE_FALLBACK = "disableFallback";
	
	// Certificate checker configuration options, currently intended to be internal only
	public static final String CERT_CHECKER_SCHEDULE = "certCheckerSchedule";
	public static final String CERT_CHECKER_ERROR_SCHEDULE = "certCheckerErrorSchedule";
	
	// Allow immediate requests for certificate renewal
	public static final String DISABLE_MIN_RENEW_WINDOW = "disableMinRenewWindow";

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
	
	public static final long RENEW_CERT_MIN = 15000L; // Minimum allowed time to check for expiration
	public static final Long RENEW_CERT_MIN_WARN_LEVEL = 60000L; // The renew time that we'll put out a warning that you've picked a very low renew time
	public static final Long RENEW_DEFAULT_MS = TimeUnit.DAYS.toMillis(7L);  // 604800000L; 
	public static final double RENEW_DIVISOR = .5;
	public static final long CHALLENGE_POLL_DEFAULT = 120000l;
	public static final long ORDER_POLL_DEFAULT = 120000l;


	public static final Long SCHEDULER_MS = TimeUnit.HOURS.toMillis(24L);
	public static final Long SCHEDULER_ERROR_MS = TimeUnit.HOURS.toMillis(1L);

}
