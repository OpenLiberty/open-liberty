/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.couchdb.fat;

import java.io.File;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.couchdb.fat.tests.CouchDBContainer;
import com.ibm.ws.couchdb.fat.tests.TestCouchDbWar;

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({ TestCouchDbWar.class })
public class FATSuite {

    static {
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
    }

    @ClassRule
    public static CouchDBContainer couchdb = new CouchDBContainer(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from("couchdb:1.7")
                                    .copy("/opt/couchdb/etc/local.d/testcontainers_config.ini", "/opt/couchdb/etc/local.d/testcontainers_config.ini")
                                    .copy("/etc/couchdb/cert/couchdb.pem", "/etc/couchdb/cert/couchdb.pem")
                                    .copy("/etc/couchdb/cert/privkey.pem", "/etc/couchdb/cert/privkey.pem")
                                    .build())
                    .withFileFromFile("/opt/couchdb/etc/local.d/testcontainers_config.ini", new File("lib/LibertyFATTestFiles/couchdb-config/testcontainers_config.ini"), 644)
                    .withFileFromFile("/etc/couchdb/cert/couchdb.pem", new File("lib/LibertyFATTestFiles/ssl-certs/couchdb.pem"), 644)
                    .withFileFromFile("/etc/couchdb/cert/privkey.pem", new File("lib/LibertyFATTestFiles/ssl-certs/privkey.pem"), 644))
                                    .withLogConsumer(FATSuite::log);
    // public static CouchDBContainer couchdb = new CouchDBContainer("couchdb:1.7").withLogConsumer(FATSuite::log);

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(FATSuite.class, "couchdb", msg);
    }

}