/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.fat;

import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class TestMongoDbWar extends TestMongoDb {

    @BeforeClass
    public static void b() throws Exception {
        before("com.ibm.ws.mongo.fat.server");
    }

    @AfterClass
    public static void a() throws Exception {
        after("CWKKD0013E:.*mongo-lib-10",
              "CWKKD0013E:.*mongo-lib-2-9-3",
              "SRVE0777E:.*MongoServlet\\.getDB");
    }

    @Override
    protected void updateApplication(ServerConfiguration sc, String libName) {
        for (Application app : sc.getApplications()) {
            if ((AbstractMongoTestCase.APP_NAME + ".war").equals(app.getLocation())) {
                ClassloaderElement cl = app.getClassloader();
                Set<String> refs = cl.getCommonLibraryRefs();
                refs.clear();
                refs.add(libName);
            }
        }
    }
}
