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
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs20.fat.AbstractTest;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class JAXRS20ClientServerBookTest extends AbstractTest {

    @Server("com.ibm.ws.jaxrs.fat.bookstore")
    public static LibertyServer server;

    private static final String CONTEXT_ROOT = "bookstore";
    private final static String target = CONTEXT_ROOT + "/TestServlet";
    private static final String cxf = "publish/shared/resources/cxf/";
    private static final String jackson = "publish/shared/resources/jackson2x/";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(CONTEXT_ROOT, "com.ibm.ws.jaxrs20.fat.bookstore");
        app.addAsLibraries(new File(cxf).listFiles());
        app.addAsLibraries(new File(jackson).listFiles());
        ShrinkHelper.exportDropinAppToServer(server, app);
        server.addInstalledAppForValidation(CONTEXT_ROOT);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testGetGenericBook() throws Exception {
        this.runTestOnServer(target, "testGetGenericBook", null, "OK");
    }

    @Test
    public void testGetGenericBook2() throws Exception {
        this.runTestOnServer(target, "testGetGenericBook2", null, "OK");
    }

    @Test
    public void testGetBook() throws Exception {
        this.runTestOnServer(target, "testGetBook", null, "OK");
    }

    @Test
    public void testGetBookSyncLink() throws Exception {
        this.runTestOnServer(target, "testGetBookSyncLink", null, "OK");
    }

    @Test
    public void testGetBookSpec() throws Exception {
        this.runTestOnServer(target, "testGetBookSpec", null, "OK");
    }

    @Test
    public void testGetBookSyncWithAsync() throws Exception {
        this.runTestOnServer(target, "testGetBookSyncWithAsync", null, "OK");
    }

    @Test
    public void testGetBookAsync() throws Exception {
        this.runTestOnServer(target, "testGetBookAsync", null, "OK");
    }

    @Test
    public void testGetBookAsyncNoCallback() throws Exception {
        this.runTestOnServer(target, "testGetBookAsyncNoCallback", null, "OK");
    }

    @Test
    public void testGetBookAsyncResponse() throws Exception {
        this.runTestOnServer(target, "testGetBookAsyncResponse", null, "OK");
    }

    @Test
    public void testGetBookAsyncInvoker() throws Exception {
        this.runTestOnServer(target, "testGetBookAsyncInvoker", null, "OK");
    }

    @Test
    public void testPreMatchContainerFilterThrowsException() throws Exception {
        this.runTestOnServer(target, "testPreMatchContainerFilterThrowsException", null, "OK");
    }

    @Test
    public void testPostMatchContainerFilterThrowsException() throws Exception {
        this.runTestOnServer(target, "testPostMatchContainerFilterThrowsException", null, "OK");
    }

    @Test
    public void testGetBookWrongPath() throws Exception {
        this.runTestOnServer(target, "testGetBookWrongPath", null, "OK");
    }

    @Test
    public void testGetBookWrongPathAsync() throws Exception {
        this.runTestOnServer(target, "testGetBookWrongPathAsync", null, "OK");
    }

    @Test
    public void testPostCollectionGenericEntity() throws Exception {
        this.runTestOnServer(target, "testPostCollectionGenericEntity", null, "OK");
    }

    @Test
    public void testPostCollectionGenericEntityAsEntity() throws Exception {
        this.runTestOnServer(target, "testPostCollectionGenericEntityAsEntity", null, "OK");
    }

    @Test
    public void testPostReplaceBook() throws Exception {
        this.runTestOnServer(target, "testPostReplaceBook", null, "OK");
    }

    @Test
    public void testPostReplaceBookMistypedCT() throws Exception {
        this.runTestOnServer(target, "testPostReplaceBookMistypedCT", null, "OK");
    }

    @Test
    public void testPostGetCollectionGenericEntityAndType() throws Exception {
        this.runTestOnServer(target, "testPostGetCollectionGenericEntityAndType", null, "OK");
    }

    @Test
    public void testPostGetCollectionGenericEntityAndType2() throws Exception {
        this.runTestOnServer(target, "testPostGetCollectionGenericEntityAndType2", null, "OK");
    }

    @Test
    public void testClientFiltersLocalResponse() throws Exception {
        this.runTestOnServer(target, "testClientFiltersLocalResponse", null, "OK");
    }

    @Test
    public void testPostBook() throws Exception {
        this.runTestOnServer(target, "testPostBook", null, "OK");
    }

    @Test
    public void testPostBookNewMediaType() throws Exception {
        this.runTestOnServer(target, "testPostBookNewMediaType", null, "OK");
    }

    @Test
    public void testBookExistsServerStreamReplace() throws Exception {
        this.runTestOnServer(target, "testBookExistsServerStreamReplace", null, "OK");
    }

    @Test
    public void testBookExistsServerAddressOverwrite() throws Exception {
        this.runTestOnServer(target, "testBookExistsServerAddressOverwrite", null, "OK");
    }

    @Test
    public void testPostBookAsync() throws Exception {
        this.runTestOnServer(target, "testPostBookAsync", null, "OK");
    }

    @Test
    public void testPostBookAsyncHandler() throws Exception {
        this.runTestOnServer(target, "testPostBookAsyncHandler", null, "OK");
    }

    /**
     * Tests whether the PostConstruct method is invoked - even if it has private visibility.
     */
    @Test
    public void testPrivatePostConstructMethodInvoked() throws Exception {
        this.runTestOnServer(target, "testPrivatePostConstructMethodInvoked", null, "OK");
    }
}
