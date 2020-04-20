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

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.shaded.org.bouncycastle.util.test.SimpleTest;

import com.ibm.ws.security.acme.docker.boulder.BoulderContainer;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Same as {@link SimpleTest}, but uses Boulder instead of Pebble
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AcmeBoulderSimpleTest extends AcmeSimpleTest {

	/**
	 * This overrides beforeClass in AcmeSimpleTest
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {
	    ORIGINAL_CONFIG = server.getServerConfiguration();
        caContainer = new BoulderContainer();
		AcmeFatUtils.checkPortOpen(caContainer.getHttpPort(), 60000);
	}
	
}
