/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.couchdb.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.couchdb.fat.tests.TestCouchDbWar;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;
import componenttest.topology.database.container.CouchDBContainer;

@RunWith(Suite.class)
@SuiteClasses({ TestCouchDbWar.class })
public class FATSuite {

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    // The Dockerfile for 'kyleaure/couchdb-ssl:1.0' can be found/rebuilt in the cloudant_fat project
    @ClassRule
    public static CouchDBContainer couchdb = new CouchDBContainer()
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "couchdb"));

}