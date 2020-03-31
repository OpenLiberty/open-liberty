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
package com.ibm.ws.security.acme.fat;

import org.junit.runner.RunWith;
import org.testcontainers.shaded.org.bouncycastle.util.test.SimpleTest;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Same as {@link SimpleTest}, but uses the acme://* URI instead of an HTTPS
 * URI.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AcmeURISimpleTest extends AcmeSimpleTest {

	@Override
	protected void configureAcmeCA(LibertyServer server, ServerConfiguration originalConfig, String... domains)
			throws Exception {
		/*
		 * Always request an acme://* URI.
		 */
		AcmeFatUtils.configureAcmeCA(server, originalConfig, true, domains);
	}
}
