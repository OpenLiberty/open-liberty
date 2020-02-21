/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.oracle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({
                OracleTest.class,
                OracleUCPTest.class
})
public class FATSuite {
	
    //TODO replace this container with the official oracle-xe container if/when it is available without a license
    static OracleContainer oracle = new OracleContainer("oracleinanutshell/oracle-xe-11g").withLogConsumer(FATSuite::log);
    
    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(FATSuite.class, "oracle", msg);
    }
	
    @BeforeClass
    public static void beforeSuite() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client. 
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
        
        oracle.start();
    }
    
    @AfterClass
    public static void afterSuite() {
    	oracle.stop();
    }
}
