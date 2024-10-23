/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.server.transport.http2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.rules.TestName;

public class Utils {

    public static Properties localProps = new Properties();
    public static String TEST_DIR;

    // Begin of stress test parameters
    //
    // Parameters used in test.server.transport.http2.MultiSessionTests.java
    // and in http2.test.driver.war.servlets.H2FATDriverServlet for stress testing
    public static final int STRESS_CONNECTIONS = 5; // Parallel H2 Connections
    public static final int STREAM_INSTANCES = 6; // Parallel stream instances per H2 Connection
    public static final int FIRST_STREAM_WEIGHT = 16;
    public static final int WEIGHT_INCREMENT_PER_STREAM = 16;

    // times are in milliseconds
    public static final int STRESS_DELAY_BETWEEN_CONN_STARTS = 2000;
    public static final int STRESS_DELAY_BETWEEN_STREAM_STARTS = 50;

    public static final int STRESS_TEST_TIMEOUT_testMultipleConnectionStress = 10 * 60000;

    // initial (and only) size that the connection window update gets set to.
    // so make it really big, basically all the DATA payload bytes that will be sent for the entire test.
    public static final int STRESS_CONNECTION_WINDOW_UPDATE = 10000000 * Utils.STREAM_INSTANCES * Utils.STRESS_CONNECTIONS;

    // initial size that the connection window update gets set to for each stream.
    // value will increment by the value based in com.ibm.ws.http2.test.Constants
    // at the interval defined in http2.test.war.servlets.H2MultiDataFrame
    public static final int STRESS_STREAM_WINDOW_UPDATE_START = 1000000;
    //
    //End of stress test parameters

    static {
        try {
            FileInputStream in = new FileInputStream(System.getProperty("local.properties"));
            localProps.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TEST_DIR = localProps.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport" + File.separator + "http2"
                   + File.separator + "buckets";
    }

    public static class CustomTestName extends TestName {
        @Override
        public String getMethodName() {
            // TODO Auto-generated method stub
            String name = super.getMethodName();
            if (name.contains("_"))
                return name.substring(0, name.indexOf("_"));
            return name;
        }
    }

}
