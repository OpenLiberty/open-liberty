/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.postgresql;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({
                PostgreSQLTest.class,
                PostgreSQLSSLTest.class
})
public class FATSuite {

    static {
        // TODO: temporary debug setting so we can further investigate intermittent
        // testcontainers ping issues on remote build machines
        System.setProperty("javax.net.debug", "all");
    }

    @BeforeClass
    public static void setupBukcet() throws Exception {
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
    }

}
