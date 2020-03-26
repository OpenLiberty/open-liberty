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
package com.ibm.ws.jdbc.fat.sqlserver;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                SQLServerTest.class
})
public class FATSuite {

    static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2019-CU2-ubuntu-16.04").withLogConsumer(FATSuite::log);

    //Private Method: used to setup logging for containers to this class.
    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(FATSuite.class, "sqlserver", msg);
    }

    @BeforeClass
    public static void beforeSuite() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client.
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();

        sqlserver.start();
    }

    @AfterClass
    public static void afterSuite() {
        sqlserver.stop();
    }
}