/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.transport.http2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Utils {

    public static Properties localProps = new Properties();
    public static String TEST_DIR;

    public static final int STRESS_ITERATIONS = 20;
    public static final int STRESS_CONNECTIONS = 3;

    // times are in milliseconds
    public static final int STRESS_DELAY_BETWEEN_CONN_STARTS = 2000;
    public static final int STRESS_DELAY_BETWEEN_STREAM_STARTS = 50;
    public static final int STRESS_TEST_TIMEOUT_testSingleConnectionStress = 2 * 60000;
    public static final int STRESS_TEST_TIMEOUT_testMulitData = 4 * 60000;
    public static final int STRESS_TEST_TIMEOUT_testMultipleConnectionStress = 8 * 60000;

    public static final int STRESS_DELAY_MSECS = 2000;

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

}
