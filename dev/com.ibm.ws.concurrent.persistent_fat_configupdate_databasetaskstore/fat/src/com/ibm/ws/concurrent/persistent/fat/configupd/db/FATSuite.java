/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.configupd.db;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.Machine;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test cases for configuration updates to databaseTaskStore while the server is running
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({ ExecEnabledDBStoreConfigUpdateTest.class,
                PersistentExecutionConfigUpdateDBStoreTest.class})
public class FATSuite {
    static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.configupdate.dbtaskstore");
    static {
        // Delete the Derby database that might be used by the persistent scheduled executor and the Derby-only test database
        System.out.println("Executing before suite setup");
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        try {
            LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/persistcfg1db");
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }
}
