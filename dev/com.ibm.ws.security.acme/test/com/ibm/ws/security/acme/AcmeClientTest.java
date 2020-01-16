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

package com.ibm.ws.security.acme;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ibm.ws.security.acme.internal.AcmeClient;

/**
 * Unit tests for the {@link AcmeClient} class. These tests are limited to those
 * that do not interact with an ACME CA service.
 */
// TODO Invalid key files.
// TODO Unreadable key files.
// TODO Unwriteable key files.
public class AcmeClientTest {

	private static final String VALID_URI = "some_path";
	private static final String VALID_USER_KEY_PATH = "userKey.pem";
	private static final String VALID_DOMAIN_KEY_PATH = "domainKey.pem";

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void constructor_NullURI() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("The ACME CA's directory URI must be a valid URI.");

		new AcmeClient(null, VALID_USER_KEY_PATH, VALID_DOMAIN_KEY_PATH, null);
	}

	@Test
	public void constructor_EmptyURI() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("The ACME CA's directory URI must be a valid URI.");

		new AcmeClient("", VALID_USER_KEY_PATH, VALID_DOMAIN_KEY_PATH, null);
	}

	@Test
	public void constructor_NullAccountKeyPath() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("The account key file path must be valid.");

		new AcmeClient(VALID_URI, null, VALID_DOMAIN_KEY_PATH, null);
	}

	@Test
	public void constructor_EmptyAccountKeyPath() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("The account key file path must be valid.");

		new AcmeClient(VALID_URI, "", VALID_DOMAIN_KEY_PATH, null);
	}

	@Test
	public void constructor_NullDomainKeyPath() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("The domain key file path must be valid.");

		new AcmeClient(VALID_URI, VALID_DOMAIN_KEY_PATH, null, null);
	}

	@Test
	public void constructor_EmptyDomainKeyPath() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("The domain key file path must be valid.");

		new AcmeClient(VALID_URI, VALID_DOMAIN_KEY_PATH, "", null);
	}
}
