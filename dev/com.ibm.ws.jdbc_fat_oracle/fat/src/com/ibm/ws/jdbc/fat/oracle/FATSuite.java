/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.database.DatabaseCluster;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                OracleTest.class,
                OracleUCPTest.class
})
public class FATSuite {

    public static DatabaseCluster dbCluster;

    @BeforeClass
    public static void setUp() throws Exception {
        dbCluster = new DatabaseCluster();
        dbCluster.createDatabase();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        dbCluster.dropDatabase();
    }

}
