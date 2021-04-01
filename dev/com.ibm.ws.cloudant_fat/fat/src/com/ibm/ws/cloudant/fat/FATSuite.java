/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cloudant.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;

@RunWith(Suite.class)
@SuiteClasses({
                CloudantDemoTest.class,
                CloudantTest.class,
                CloudantTestOutboundSSL.class,
                CloudantModifyConfigTest.class
})
public class FATSuite {

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    @ClassRule
    public static CouchDBContainer cloudant = new CouchDBContainer("gjwatts/couchdb-tls12:1.0")
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "cloudant"));

}