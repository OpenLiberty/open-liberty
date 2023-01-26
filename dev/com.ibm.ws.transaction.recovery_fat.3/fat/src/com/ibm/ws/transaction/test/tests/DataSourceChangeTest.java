/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.assertNotNull;

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

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that not all @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@Mode
@RunWith(FATRunner.class)
public class DataSourceChangeTest extends FATServletClient {

    public static final String APP_NAME = "transaction";

    @Server("recovery.dblog")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.*");

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server);

    }

    @Test
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException" })
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

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        Log.info(this.getClass(), method, "Reset the config back the way it originally was");
        // Now reset the config back to the way it was
        if (fs != null)
            fs.setDir(sfDirOrig);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        // Should see a message like
        // WTRN0108I: Have recovered from ResourceAllocationException in SQL RecoveryLog partnerlog for server recovery.dblog
        assertNotNull("No warning message signifying failover", server.waitForStringInLog("Have recovered from ResourceAllocationException"));
    }
}
