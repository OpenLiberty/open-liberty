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

package io.openliberty.jpa.defaultdatasource;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jpa.fat.defaultds.web.Spec21DDSServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jpa.RepeatWithJPA20;

@RunWith(FATRunner.class)
public class JPADefaultDataSourceTest extends FATServletClient {
    private static final String CLASS_NAME = JPADefaultDataSourceTest.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final String APP_NAME = "defaultdatasource";
    private static final String contextRoot = "defaultdatasource";
    private static final String servletName = "Spec21DDSServlet";
    private static final String appPath = contextRoot + "/" + servletName;
    private static final String EOLN = String.format("%n");

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

    @Server("com.ibm.ws.jpa.el.defaultds.fat.server")
    @TestServlets({
                    @TestServlet(servlet = Spec21DDSServlet.class, path = appPath)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")//
                        .addPackage("com.ibm.ws.jpa.fat.defaultds.entity")//
                        .addPackage("com.ibm.ws.jpa.fat.defaultds.web");//
        ShrinkHelper.addDirectory(app, "test-applications/" + APP_NAME + "/resources/");
        ShrinkHelper.exportToServer(server, "apps", app);

        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("org.eclipse.persistence.exceptions.DatabaseException");
    }

}
