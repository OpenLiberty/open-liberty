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
package com.ibm.ws.security.acme.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({ AcmeClientTest.class, 
	AcmeSimpleTest.class,
	AcmeURISimpleTest.class,
	AcmeCaRestHandlerTest.class,
	AcmeSwapDirectoriesTest.class,
	AcmeValidityAndRenewTest.class,
	AcmeRevocationTest.class,
	AcmeDisableTriggerSimpleTest.class
	 })
public class FATSuite {

	/*
	 * This static block should be the first static initialization in this class
	 * so that the testcontainers config is cleared before we start our new
	 * testcontainers.
	 */
	static {
		ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
	}

}
