/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.suite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.repository.resources.internal.test.HashUtilsMultiThreadedTest;
import com.ibm.ws.repository.strategies.test.AddNewStrategyTest;
import com.ibm.ws.repository.strategies.test.AddThenDeleteStrategyTest;
import com.ibm.ws.repository.strategies.test.AddThenHideOldStrategyTest;
import com.ibm.ws.repository.strategies.test.UpdateInPlaceStrategyTest;
import com.ibm.ws.repository.test.AdminScriptsResourceTest;
import com.ibm.ws.repository.test.ConfigSnippetResourceTest;
import com.ibm.ws.repository.test.EsaResourceTest;
import com.ibm.ws.repository.test.IfixResourceTest;
import com.ibm.ws.repository.test.ProductResourceTest;
import com.ibm.ws.repository.test.RepositoryUtilsTest;
import com.ibm.ws.repository.test.ResourceFilteringTest;
import com.ibm.ws.repository.test.ResourceTest;
import com.ibm.ws.repository.test.SampleResourceTest;
import com.ibm.ws.repository.test.ToolResourceTest;
import com.ibm.ws.repository.transport.client.test.FileClientLicenseTest.DirectoryClientLicenseTest;
import com.ibm.ws.repository.transport.client.test.FileClientLicenseTest.ZipClientLicenseTest;
import com.ibm.ws.repository.transport.client.test.InvalidDirectoryClientTest;
import com.ibm.ws.repository.transport.client.test.InvalidZipClientTest;
import com.ibm.ws.repository.transport.client.test.RepositoryClientTest.DirectoryRepositoryClientTest;
import com.ibm.ws.repository.transport.client.test.RepositoryClientTest.LooseFileRepositoryClientTest;
import com.ibm.ws.repository.transport.client.test.RepositoryClientTest.RestRepositoryClientTest;
import com.ibm.ws.repository.transport.client.test.RepositoryClientTest.SingleFileRepositoryClientTest;
import com.ibm.ws.repository.transport.client.test.RepositoryClientTest.ZipRepositoryClientTest;
import com.ibm.ws.repository.transport.client.test.RestClientTest;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                AddNewStrategyTest.class,
                AddThenDeleteStrategyTest.class,
                AddThenHideOldStrategyTest.class,
                UpdateInPlaceStrategyTest.class,
                AdminScriptsResourceTest.class,
                ConfigSnippetResourceTest.class,
                EsaResourceTest.class,
                IfixResourceTest.class,
                ProductResourceTest.class,
                RepositoryUtilsTest.class,
                ResourceFilteringTest.class,
                ResourceTest.class,
                SampleResourceTest.class,
                ToolResourceTest.class,
                DirectoryClientLicenseTest.class,
                ZipClientLicenseTest.class,
                InvalidDirectoryClientTest.class,
                InvalidZipClientTest.class,
                DirectoryRepositoryClientTest.class,
                ZipRepositoryClientTest.class,
                LooseFileRepositoryClientTest.class,
                SingleFileRepositoryClientTest.class,
                RestRepositoryClientTest.class,
                RestClientTest.class,
                HashUtilsMultiThreadedTest.class,
})
public class FATSuite {

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("FATServer");

    @BeforeClass
    public static void setUp() throws Exception {
        FatUtils.setRestFixture(server);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE0190E", // File not found
                          "CWNEN0049W", // Cannot load resource annotations on PersistenceBean
                          "CWNEN0047W");// Resource annotations will be ignored
    }

}
