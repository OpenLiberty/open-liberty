/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.restfulWS30.client.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.restfulWS30.client.fat.namebinding.NameBindingClientFilterTestServlet;

/**
 * Test case to reproduce the customer issue reported in TS020587370:
 * Client filters with @NameBinding annotations don't work in RESTful WS 3.0.
 *
 * This test demonstrates the difference in behavior between:
 * - JAX-RS 2.1 (Apache CXF): @NameBinding annotations worked with client filters
 * - RESTful WS 3.0 (RESTEasy): @NameBinding annotations are ignored on client filters
 *
 * The issue is that @NameBinding annotations are only supported for server-side components
 * in the Jakarta RESTful Web Services specification, but Apache CXF (used in JAX-RS 2.1)
 * was more lenient and allowed them to work with client filters as well.
 *
 * RESTEasy (used in RESTful WS 3.0) strictly follows the specification and ignores
 * @NameBinding annotations on client filters, which breaks existing code that relied
 * on the more lenient behavior of JAX-RS 2.1.
 */
@RunWith(FATRunner.class)
public class NameBindingClientFilterTest extends FATServletClient {

    private static final String APP_NAME = "namebindingclientfilter";

    @Server("namebinding")
    @TestServlet(servlet = NameBindingClientFilterTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, NameBindingClientFilterTestServlet.class.getPackage());
        
        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);
        
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0921W", "CWWKE0912W", "CWWKE1102W", "CWWKE1106W", "CWWKE1107W");
    }
}