/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({ AcmeClientTest.class, 
	AcmeSimpleTest.class,
	AcmeURISimpleTest.class,
	AcmeCaRestHandlerTest.class,
	AcmeSwapDirectoriesTest.class,
	AcmeValidityAndRenewTest.class,
	AcmeDisableTriggerSimpleTest.class,
	AcmeConfigVariationsTest.class,
	AcmeURIConfigVariationsTest.class,
	AcmeRevocationTest.class
})
public class FATSuite {
    /*
     * Repeat with EE9. Since most, if not all, servers don't have an EE feature enabled, we
     * will add servlet-5.0 for the EE9 repeats to test that the acmeCA-2.0 feature supports
     * EE9 since there are no EE features for the JakartaEE9Action to replace in the server 
     * XMLs.
     */
    @ClassRule
	public static RepeatTests repeat = RepeatTests.with(new EmptyAction())
			.andWith(new JakartaEE9Action().alwaysAddFeature("servlet-5.0"));
    
    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
        
        // Filter out any external docker servers in the 'libhpike' cluster
        ExternalTestServiceDockerClientStrategy.serviceFilter = (svc) -> {
                return !svc.getAddress().contains("libhpike-dockerengine");
        };
    }
}
