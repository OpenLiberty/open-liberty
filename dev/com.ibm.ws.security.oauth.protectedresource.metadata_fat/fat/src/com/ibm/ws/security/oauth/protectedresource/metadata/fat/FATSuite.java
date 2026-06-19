/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth.protectedresource.metadata.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Collection of all FAT tests for OAuth 2.0 Protected Resource Metadata (RFC 8707).
 * 
 * <p>
 * This suite runs tests that verify the metadata endpoint correctly serves
 * metadata for protected resources, including resource URLs and authorization
 * server identifiers.
 * </p>
 */
@RunWith(Suite.class)
@SuiteClasses({
    OAuthProtectedResourceMetadataFATTest.class
})
public class FATSuite {
}

// Made with Bob
