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

package com.ibm.ws.security.acme.internal;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Unit tests for the {@link AcmeConfig} class.
 */
public class AcmeConfigTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void constructor_accountKeyFile_empty() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2027E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "");
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_accountKeyFile_null() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2027E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, null);
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_accountKeyFile_unreadable() throws Exception {

		Assume.assumeTrue(!System.getProperty("os.name", "unknown").toLowerCase().contains("windows")); // windows
																										// not
																										// enforcing
																										// the
																										// setReadable

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2021E");

		File file = File.createTempFile("unreadable", "key");
		file.setReadable(false);
		file.deleteOnExit();

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, file.getAbsolutePath());
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_accountKeyFile_unwritable() throws Exception {

		Assume.assumeTrue(!System.getProperty("os.name", "unknown").toLowerCase().contains("windows")); // windows
																										// not
																										// enforcing
																										// the
																										// setWritable

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2023E");

		File dir = Files.createTempDirectory("unwritable").toFile();
		dir.setWritable(false);
		dir.deleteOnExit();

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, dir.getAbsolutePath() + "/account.key");
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_directoryURI_missing() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2008E");

		Map<String, Object> properties = new HashMap<String, Object>();
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_domain_missing() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2037E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_domainKeyFile_empty() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2027E");

		File file = File.createTempFile("unreadable", "key");
		file.setReadable(false);
		file.deleteOnExit();

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "account.key");
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, "");
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_domainKeyFile_null() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2027E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "account.key");
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, null);
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_domainKeyFile_unreadable() throws Exception {

		Assume.assumeTrue(!System.getProperty("os.name", "unknown").toLowerCase().contains("windows")); // windows
																										// not
																										// enforcing
																										// the
																										// setReadable

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2020E");

		File file = File.createTempFile("unreadable", "key");
		file.setReadable(false);
		file.deleteOnExit();

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "account.key");
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, file.getAbsolutePath());
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_domainKeyFile_unwritable() throws Exception {

		Assume.assumeTrue(!System.getProperty("os.name", "unknown").toLowerCase().contains("windows")); // windows
																										// not
																										// enforcing
																										// the
																										// setWritable

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2022E");

		File dir = Files.createTempDirectory("unwritable").toFile();
		dir.setWritable(false);
		dir.deleteOnExit();

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "account.key");
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, dir.getAbsolutePath() + "/domain.key");
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_domains_empty() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2037E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "" });
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_subjectDN_cnNotDomain() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2039E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.SUBJECT_DN, "cn=baddomain.com");
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_subjectDN_domain_reorder() throws Exception {

		/*
		 * Create a properties map.
		 */
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "account.key");
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, "domain.key");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain1.com", "domain2.com" });
		properties.put(AcmeConstants.SUBJECT_DN, "cn=domain2.com,ou=liberty,o=ibm.com");

		/*
		 * Instantiate the ACME configuration.
		 */
		AcmeConfig acmeConfig = new AcmeConfig(properties);

		/*
		 * Verify values. Domain1 and domain2 should have switched order.
		 */
		assertEquals("domain2.com", acmeConfig.getDomains().get(0));
		assertEquals("domain1.com", acmeConfig.getDomains().get(1));

		assertEquals("cn=domain2.com", acmeConfig.getSubjectDN().get(0).toString());
		assertEquals("ou=liberty", acmeConfig.getSubjectDN().get(1).toString());
		assertEquals("o=ibm.com", acmeConfig.getSubjectDN().get(2).toString());
	}

	@Test
	public void constructor_subjectDN_invalidRdnType() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2041E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.SUBJECT_DN, "cn=domain.com,type=invalidtype");
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_subjectDN_isNotDN() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2042E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.SUBJECT_DN, "invalidDN");
		new AcmeConfig(properties);
	}

	// @Test Wasn't able to get this to fail. Leaving in case I think of a case.
	public void constructor_subjectDN_noSubjectDN_domainFormsInvalidRDN() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2043E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { ";domainthatmakesbadrdn;" });
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_subjectDN_multipleCN() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2040E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain.com" });
		properties.put(AcmeConstants.SUBJECT_DN, "cn=domain.com,cn=anothercn.com");
		new AcmeConfig(properties);
	}

	@Test
	public void constructor_validConfig() throws Exception {

		/*
		 * Create a properties map.
		 */
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.ACCOUNT_CONTACT, new String[] { "mailto://pacman@mail.com" });
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "account.key");
		properties.put(AcmeConstants.CHALL_RETRIES, 1);
		properties.put(AcmeConstants.CHALL_RETRY_WAIT, 2L);
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, "domain.key");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain1.com", "domain2.com" });
		properties.put(AcmeConstants.ORDER_RETRIES, 3);
		properties.put(AcmeConstants.ORDER_RETRY_WAIT, 4L);
		properties.put(AcmeConstants.SUBJECT_DN, "cn=domain1.com,ou=liberty,o=ibm.com");
		properties.put(AcmeConstants.TRANSPORT_CONFIG + ".0." + AcmeConstants.TRANSPORT_PROTOCOL, "SSL");
		properties.put(AcmeConstants.TRANSPORT_CONFIG + ".0." + AcmeConstants.TRANSPORT_TRUST_STORE, "truststore.p12");
		properties.put(AcmeConstants.TRANSPORT_CONFIG + ".0." + AcmeConstants.TRANSPORT_TRUST_STORE_PASSWORD,
				new SerializableProtectedString("acmepassword".toCharArray()));
		properties.put(AcmeConstants.TRANSPORT_CONFIG + ".0." + AcmeConstants.TRANSPORT_TRUST_STORE_TYPE, "PKCS12");
		properties.put(AcmeConstants.REVOCATION_CHECKER + ".0." + AcmeConstants.REVOCATION_CHECKER_ENABLED, false);
		properties.put(AcmeConstants.REVOCATION_CHECKER + ".0." + AcmeConstants.REVOCATION_DISABLE_FALLBACK, true);
		properties.put(AcmeConstants.REVOCATION_CHECKER + ".0." + AcmeConstants.REVOCATION_OCSP_RESPONDER_URL,
				"http://localhost:4001");
		properties.put(AcmeConstants.REVOCATION_CHECKER + ".0." + AcmeConstants.REVOCATION_PREFER_CRLS, true);
		properties.put(AcmeConstants.VALID_FOR, 5L);
		properties.put(AcmeConstants.RENEW_BEFORE_EXPIRATION, 691200000L); // 8 days

		/*
		 * Instantiate the ACME configuration.
		 */
		AcmeConfig acmeConfig = new AcmeConfig(properties);

		/*
		 * Verify values.
		 */
		assertEquals("mailto://pacman@mail.com", acmeConfig.getAccountContacts().get(0));
		assertEquals("account.key", acmeConfig.getAccountKeyFile());
		assertEquals(1, acmeConfig.getChallengeRetries().intValue());
		assertEquals(2L, acmeConfig.getChallengeRetryWaitMs().longValue());
		assertEquals("https://localhost:443/dir", acmeConfig.getDirectoryURI());
		assertEquals("domain.key", acmeConfig.getDomainKeyFile());
		assertEquals("domain1.com", acmeConfig.getDomains().get(0));
		assertEquals("domain2.com", acmeConfig.getDomains().get(1));
		assertEquals(3, acmeConfig.getOrderRetries().intValue());
		assertEquals(4L, acmeConfig.getOrderRetryWaitMs().intValue());

		SSLConfig sslConfig = acmeConfig.getSSLConfig();
		assertEquals("SSL", sslConfig.getProperty(Constants.SSLPROP_PROTOCOL));
		assertEquals("truststore.p12", sslConfig.getProperty(Constants.SSLPROP_TRUST_STORE));
		assertEquals("acmepassword", sslConfig.get(Constants.SSLPROP_TRUST_STORE_PASSWORD));
		assertEquals("PKCS12", sslConfig.getProperty(Constants.SSLPROP_TRUST_STORE_TYPE));

		assertEquals("cn=domain1.com", acmeConfig.getSubjectDN().get(0).toString());
		assertEquals("ou=liberty", acmeConfig.getSubjectDN().get(1).toString());
		assertEquals("o=ibm.com", acmeConfig.getSubjectDN().get(2).toString());
		assertEquals(5L, acmeConfig.getValidForMs().longValue());
		
		assertEquals(691200000L, acmeConfig.getRenewBeforeExpirationMs().longValue());
		assertTrue("Auto-renewal should be enabled", acmeConfig.isAutoRenewOnExpiration());

		assertTrue(acmeConfig.isDisableFallback());
		assertEquals(Boolean.FALSE, acmeConfig.isRevocationCheckerEnabled());
		assertEquals(URI.create("http://localhost:4001"), acmeConfig.getOcspResponderUrl());
		assertTrue(acmeConfig.isPreferCrls());
	}
	
	@Test
	public void constructor_validConfig_disableRenew() throws Exception {

		/*
		 * Create a properties map.
		 */
		Map<String, Object> properties = getBasicConfig();

		properties.put(AcmeConstants.RENEW_BEFORE_EXPIRATION, -1L);

		/*
		 * Instantiate the ACME configuration.
		 */
		AcmeConfig acmeConfig = new AcmeConfig(properties);

		/*
		 * Verify values. A negative or zero renew disables auto-renew
		 */
		
		assertEquals(0L, acmeConfig.getRenewBeforeExpirationMs().longValue());
		assertFalse("Auto-renewal should be disabled", acmeConfig.isAutoRenewOnExpiration());
	}

	@Test
	public void constructor_invalid_values() throws Exception {

		/*
		 * Create a properties map.
		 */
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "account.key");
		properties.put(AcmeConstants.CHALL_RETRIES, -1);
		properties.put(AcmeConstants.CHALL_RETRY_WAIT, -2L);
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, "domain.key");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain1.com", "domain2.com" });
		properties.put(AcmeConstants.ORDER_RETRIES, -3);
		properties.put(AcmeConstants.ORDER_RETRY_WAIT, -4L);
		properties.put(AcmeConstants.VALID_FOR, -5L);
		properties.put(AcmeConstants.RENEW_BEFORE_EXPIRATION, AcmeConstants.RENEW_CERT_MIN - 10);

		/*
		 * Instantiate the ACME configuration.
		 */
		AcmeConfig acmeConfig = new AcmeConfig(properties);

		/*
		 * Verify values.
		 */
		assertEquals(10, acmeConfig.getChallengeRetries().intValue());
		assertEquals(5000L, acmeConfig.getChallengeRetryWaitMs().longValue());
		assertEquals(10, acmeConfig.getOrderRetries().intValue());
		assertEquals(3000L, acmeConfig.getOrderRetryWaitMs().intValue());
		assertEquals(null, acmeConfig.getValidForMs());
		assertEquals(AcmeConstants.RENEW_CERT_MIN, acmeConfig.getRenewBeforeExpirationMs().longValue());
		assertTrue("Auto-renewal should be enabled", acmeConfig.isAutoRenewOnExpiration());
	}
	
	private Map<String, Object> getBasicConfig() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "account.key");
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, "domain.key");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain1.com", "domain2.com" });

		return properties;
	}

	@Test
	public void constructor_ocspReponderUrl_invalid() throws Exception {

		expectedException.expect(AcmeCaException.class);
		expectedException.expectMessage("CWPKI2062E");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(AcmeConstants.ACCOUNT_KEY_FILE, "account.key");
		properties.put(AcmeConstants.DIR_URI, "https://localhost:443/dir");
		properties.put(AcmeConstants.DOMAIN, new String[] { "domain1.com", "domain2.com" });
		properties.put(AcmeConstants.DOMAIN_KEY_FILE, "domain.key");
		properties.put(AcmeConstants.REVOCATION_CHECKER + ".0." + AcmeConstants.REVOCATION_OCSP_RESPONDER_URL, ":");
		new AcmeConfig(properties);
	}
}
