/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.internal;

import static junit.framework.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;

/**
 * Unit tests for the {@link AcmeClient} class. These tests are limited to those
 * that do not interact with an ACME CA service.
 */
// TODO Invalid key files.
// TODO Unreadable key files.
// TODO Unwritable key files.
public class AcmeClientTest {

	private static final String VALID_URI = "some_path";
	private static final String VALID_USER_KEY_PATH = "userKey.pem";
	private static final String VALID_DOMAIN_KEY_PATH = "domainKey.pem";

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void constructor_NullURI() throws Exception {
		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2008E");

		new AcmeClient(getAcmeConfig(null, VALID_USER_KEY_PATH, VALID_DOMAIN_KEY_PATH));
	}

	@Test
	public void constructor_EmptyURI() throws Exception {
		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2008E");

		new AcmeClient(getAcmeConfig("", VALID_USER_KEY_PATH, VALID_DOMAIN_KEY_PATH));
	}

	@Test
	public void constructor_NullAccountKeyPath() throws Exception {
		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2027E");
		expectedException.expectMessage(AcmeConstants.ACCOUNT_TYPE);

		new AcmeClient(getAcmeConfig(VALID_URI, null, VALID_DOMAIN_KEY_PATH));
	}

	@Test
	public void constructor_EmptyAccountKeyPath() throws Exception {
		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2027E");
		expectedException.expectMessage(AcmeConstants.ACCOUNT_TYPE);

		new AcmeClient(getAcmeConfig(VALID_URI, "", VALID_DOMAIN_KEY_PATH));
	}

	@Test
	public void constructor_NullDomainKeyPath() throws Exception {
		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2027E");
		expectedException.expectMessage(AcmeConstants.DOMAIN_TYPE);

		new AcmeClient(getAcmeConfig(VALID_URI, VALID_DOMAIN_KEY_PATH, null));
	}

	@Test
	public void constructor_EmptyDomainKeyPath() throws Exception {
		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2027E");
		expectedException.expectMessage(AcmeConstants.DOMAIN_TYPE);

		new AcmeClient(getAcmeConfig(VALID_URI, VALID_DOMAIN_KEY_PATH, ""));
	}

	private static AcmeConfig getAcmeConfig(String acmeDirectoryURI, String accountFile, String domainFile)
			throws AcmeCaException {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.DIR_URI, acmeDirectoryURI);
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, accountFile);
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, domainFile);
		return new AcmeConfig(properties);
	}
	
	@Test
	public void validate_renewGreaterThanValidity() throws Exception {

		/*
		 * Instantiate the ACME configuration.
		 */
		AcmeConfig acmeConfig = getAcmeConfig(VALID_URI, VALID_USER_KEY_PATH, VALID_DOMAIN_KEY_PATH);
		acmeConfig.setRenewBeforeExpirationMs(AcmeConstants.RENEW_DEFAULT_MS * 3, true);

		/*
		 * If the fresh is longer than the duration, reset the renew to default
		 */
		Calendar cal = Calendar.getInstance();
		Date notBefore = cal.getTime();
		cal.add(Calendar.DAY_OF_MONTH, AcmeConstants.RENEW_DEFAULT_DAYS *2);
		Date notAfter = cal.getTime();
		
		AcmeClient acmeClient = new AcmeClient(acmeConfig);
		
		acmeClient.checkRenewTimeAgainstCertValidityPeriod(notBefore, notAfter, "123456");
		
		assertEquals(AcmeConstants.RENEW_DEFAULT_MS, acmeConfig.getRenewBeforeExpirationMs());
	}
	
	@Test
	public void validate_renewGreaterThanVadilityLessThanDefault() throws Exception {
		/*
		 * Instantiate the ACME configuration.
		 */
		AcmeConfig acmeConfig = getAcmeConfig(VALID_URI, VALID_USER_KEY_PATH, VALID_DOMAIN_KEY_PATH);
		acmeConfig.setRenewBeforeExpirationMs(AcmeConstants.RENEW_DEFAULT_MS * 3, true);
		
		long duration = 30000;

		acmeConfig.setRenewBeforeExpirationMs(duration + 10, true);
		
		/*
		 * Instantiate the ACME configuration.
		 */

		assertEquals(duration + 10, acmeConfig.getRenewBeforeExpirationMs().longValue());

		/*
		 * If the duration is under the default and the renewBeforeExpiration is longer, reset
		 */
		
		Calendar cal = Calendar.getInstance();
		Date notBefore = cal.getTime();
		cal.add(Calendar.MILLISECOND, Math.toIntExact(duration));
		Date notAfter = cal.getTime();
		
		AcmeClient acmeClient = new AcmeClient(acmeConfig);
		
		acmeClient.checkRenewTimeAgainstCertValidityPeriod(notBefore, notAfter, "123456");
		
		assertEquals(Math.round(duration * AcmeConstants.RENEW_DIVISOR), acmeConfig.getRenewBeforeExpirationMs().longValue());
	}
	
	@Test
	public void validate_renewGreaterThanVadilityLessThanMinimum() throws Exception {

		/*
		 * Instantiate the ACME configuration.
		 */
		AcmeConfig acmeConfig = getAcmeConfig(VALID_URI, VALID_USER_KEY_PATH, VALID_DOMAIN_KEY_PATH);

		long duration = AcmeConstants.RENEW_CERT_MIN - 10;

		acmeConfig.setRenewBeforeExpirationMs(AcmeConstants.RENEW_CERT_MIN + 10, true);

		/*
		 * If the duration is under the default and the renewBeforeExpiration is longer, reset
		 */
		
		Calendar cal = Calendar.getInstance();
		Date notBefore = cal.getTime();
		cal.add(Calendar.MILLISECOND, Math.toIntExact(duration));
		Date notAfter = cal.getTime();
		
		AcmeClient acmeClient = new AcmeClient(acmeConfig);
		
		acmeClient.checkRenewTimeAgainstCertValidityPeriod(notBefore, notAfter, "123456");
		
		assertEquals(AcmeConstants.RENEW_CERT_MIN, acmeConfig.getRenewBeforeExpirationMs().longValue());
	}
	

	
	@Test
	public void validate_validityLessThanMinRenew() throws Exception {

		/*
		 * Instantiate the ACME configuration.
		 */
		AcmeConfig acmeConfig = getAcmeConfig(VALID_URI, VALID_USER_KEY_PATH, VALID_DOMAIN_KEY_PATH);

		acmeConfig.setRenewBeforeExpirationMs(AcmeConstants.RENEW_CERT_MIN, true);

		/*
		 * If the duration is less than the min_renew and the renewBeforeExpiration is at the min, we don't modify it.
		 */
		int duration = 3;
		Calendar cal = Calendar.getInstance();
		Date notBefore = cal.getTime();
		cal.add(Calendar.SECOND, duration);
		Date notAfter = cal.getTime();

		AcmeClient acmeClient = new AcmeClient(acmeConfig);

		acmeClient.checkRenewTimeAgainstCertValidityPeriod(notBefore, notAfter, "123456");

		assertEquals(AcmeConstants.RENEW_CERT_MIN, acmeConfig.getRenewBeforeExpirationMs().longValue());

	}
}
