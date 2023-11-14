/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package io.openliberty.jpa.dsoverride;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jpa.fat.dsoverride.web.DSOverrideTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jpa.RepeatWithJPA20;
import io.openliberty.jpa.defaultdatasource.JPADefaultDataSourceTest;

@RunWith(FATRunner.class)
public class DSOverrideTest extends FATServletClient {
    static final String APP_NAME = "dsoverride";
    static final String SERVLET_NAME = "DSOverrideTestServlet";

    @Rule
    public TestRule skipJPA20Rule = new TestRule() {
        @Override
        public Statement apply(Statement statement, Description arg1) {
            if (RepeatWithJPA20.ID.equals(RepeatTestFilter.getMostRecentRepeatAction().getID())) {
                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        Log.info(JPADefaultDataSourceTest.class, "skipJPA20", "Test method is skipped due to the current repeat action being JPA20.");
                    }
                };
            }

            return statement;
        }
    };
    @Server("com.ibm.ws.jpa.fat.dsoverride")
    @TestServlets({
                    @TestServlet(servlet = DSOverrideTestServlet.class, path = APP_NAME + "/" + SERVLET_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")//
                        .addPackage("com.ibm.ws.jpa.fat.dsoverride.entity")//
                        .addPackage("com.ibm.ws.jpa.fat.dsoverride.web");//
        ShrinkHelper.addDirectory(app, "test-applications/" + APP_NAME + "/resources/");
        ShrinkHelper.exportToServer(server, "apps", app);

        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWJP9991W");
    }

    @Test
    public void alwaysPass() throws Exception {
        // Always pass, to permit execution on !Derby platforms
    }

    public void runTest() throws Exception {
        FATServletClient.runTest(server, APP_NAME + '/' + SERVLET_NAME, testName.getMethodName());
    }

}
