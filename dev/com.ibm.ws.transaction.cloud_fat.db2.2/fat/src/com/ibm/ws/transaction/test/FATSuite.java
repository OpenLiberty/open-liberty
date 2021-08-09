/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.test.dbrotationtests.DBRotationTest;
import com.ibm.ws.transaction.test.dbrotationtests.DualServerDynamicDBRotationTest2;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;

@RunWith(Suite.class)
@SuiteClasses({
                DualServerDynamicDBRotationTest2.class,
})
public class FATSuite {

    // Using the RepeatTests @ClassRule will cause all tests to be run three times.
    // First without any modifications, then again with all features upgraded to
    // their EE8 equivalents and finally with the Jakarta EE9 features.
    //
    // In this test we allow one of the flavours of supported database to be selected either through
    // specifying the fat.bucket.db.type property or it is chosen based on the date. That database is
    // used in all 3 runs of the tests against the different version of EE.
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
    .andWith(FeatureReplacementAction.EE8_FEATURES())
    .andWith(new JakartaEE9Action());

    public static DatabaseContainerType type = DatabaseContainerType.DB2;
    public static JdbcDatabaseContainer<?> testContainer;

    @BeforeClass
    public static void beforeSuite() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client.
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
        testContainer = DatabaseContainerFactory.createType(type);
        Log.info(FATSuite.class, "beforeSuite", "start test container of type: " + type);
        testContainer.start();
    }

    @AfterClass
    public static void afterSuite() {
        Log.info(FATSuite.class, "afterSuite", "stop test container");
        testContainer.stop();
    }

    //Private Method: used to setup logging for containers to this class.
    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(FATSuite.class, "dbrotation", msg);
    }
}
