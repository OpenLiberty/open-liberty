/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.multiserver;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.Machine;

import componenttest.topology.database.DerbyNetworkUtilities;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({ FATValidateConcurrentMultiserver.class })
public class FATSuite {

    static LibertyServer server1 = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.multiserver.server1");
    static LibertyServer server2 = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.multiserver.server2");

    /**
     * Prepares shared file systems for DB creation.
     *
     * @param traceTag The tag String to be used to log info.
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Delete the Derby database that might be used by the persistent scheduled executor and the Derby-only test database
        System.out.println("Before Suite method");
        Machine machine = server1.getMachine();
        String installRoot = server1.getInstallRoot();
        System.out.println("Install Root for server 1 = " + installRoot);
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/scheddb");
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/testdb");

        // Delete the Derby database that might be used by the persistent scheduled executor and the Derby-only test database
        machine = server2.getMachine();
        installRoot = server2.getInstallRoot();
        System.out.println("Install Root for server 2 = " + installRoot);
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/testdb");
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/scheddb");

        server2.useSecondaryHTTPPort();
        
        // Start derby network
        DerbyNetworkUtilities.startDerbyNetwork();
    }
    
    @AfterClass
    public static void afterSuite() throws Exception{
    	// Stop derby network
        DerbyNetworkUtilities.stopDerbyNetwork();
    }
}