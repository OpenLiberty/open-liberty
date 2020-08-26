/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.transaction.test.tests.SimpleFS2PCCloudTest;

@RunWith(Suite.class)
@SuiteClasses({
                SimpleFS2PCCloudTest.class,
})
public class FATSuite {

    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("com.ibm.ws.transaction_FSCLOUD001");
    private static LibertyServer server2 = LibertyServerFactory.getLibertyServer("com.ibm.ws.transaction_FSCLOUD002");
}
