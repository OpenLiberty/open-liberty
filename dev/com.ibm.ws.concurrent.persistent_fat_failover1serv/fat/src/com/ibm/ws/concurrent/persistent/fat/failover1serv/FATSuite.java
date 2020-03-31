/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.failover1serv;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.Machine;

import componenttest.topology.database.DerbyNetworkUtilities;
import componenttest.topology.impl.LibertyFileManager;

@RunWith(Suite.class)
@SuiteClasses({
    Failover1ServerCoordinatedPollingTest.class,
    Failover1ServerTest.class,
    SwitchFromSingleInstanceToFailOverTest.class
    })
public class FATSuite {
    @BeforeClass
    public static void beforeSuite() throws Exception {
        DerbyNetworkUtilities.startDerbyNetwork();
    }

    @AfterClass
    public static void afterSuite() throws Exception{
        DerbyNetworkUtilities.stopDerbyNetwork();
    }
}