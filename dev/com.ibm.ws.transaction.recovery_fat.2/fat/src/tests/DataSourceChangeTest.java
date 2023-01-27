/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package tests;

import java.util.Collections;
import java.util.ListIterator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.Fileset;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class DataSourceChangeTest extends FATServletClient {

    @Server("recovery.dblog")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, RecoveryTestBase.APP_NAME, "web.*");

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server);
    }

    @Test
    @ExpectedFFDC(value = { "javax.resource.spi.ResourceAllocationException" })
    public void datasourceChangeTest() throws Exception {
        final String method = "datasourceChangeTest";

        // Update the server configuration on the fly to force the invalidation of the DataSource
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Fileset> fsConfig = config.getFilesets();
        Log.info(this.getClass(), method, "retrieved fileset config " + fsConfig);
        String sfDirOrig = "";

        Fileset fs = null;
        ListIterator<Fileset> lItr = fsConfig.listIterator();
        while (lItr.hasNext()) {
            fs = lItr.next();
            Log.info(this.getClass(), method, "retrieved fileset " + fs);
            sfDirOrig = fs.getDir();

            Log.info(this.getClass(), method, "retrieved Dir " + sfDirOrig);
            fs.setDir(sfDirOrig + "2");
        }

        // We just did a slight change to the ds config which will cause the ds reset code to run.
        // Should end up with functionally equivalent ds though.

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(RecoveryTestBase.APP_NAME));

        // Do some transactional work to check we're still up and running
        FATServletClient.runTest(server, RecoveryTestBase.SERVLET_NAME, "doSome2PC");

        Log.info(this.getClass(), method, "Reset the config back the way it originally was");
        // Now reset the config back to the way it was
        if (fs != null)
            fs.setDir(sfDirOrig);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(RecoveryTestBase.APP_NAME));

        // Do some more transactional work to check we're still up and running
        FATServletClient.runTest(server, RecoveryTestBase.SERVLET_NAME, "doSome2PC");

        // Should have been some ResourceAllocationExceptions in the logs. ExpectedFFDC should take care of that.
    }
}
