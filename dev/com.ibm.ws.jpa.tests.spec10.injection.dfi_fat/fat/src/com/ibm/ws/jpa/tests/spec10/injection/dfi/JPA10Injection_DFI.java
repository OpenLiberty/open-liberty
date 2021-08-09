/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.spec10.injection.dfi;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIPkgYesInhAnoOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIPkgYesInhAnoOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIPkgYesInhAnoOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIPriYesInhAnoOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIPriYesInhAnoOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIPriYesInhAnoOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIProYesInhAnoOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIProYesInhAnoOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIProYesInhAnoOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIPubYesInhAnoOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIPubYesInhAnoOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd.DFIPubYesInhAnoOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIPkgYesInhDDOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIPkgYesInhDDOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIPkgYesInhDDOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIPriYesInhDDOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIPriYesInhDDOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIPriYesInhDDOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIProYesInhDDOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIProYesInhDDOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIProYesInhDDOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIPubYesInhDDOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIPubYesInhDDOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd.DFIPubYesInhDDOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIPkgNoInhEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIPkgNoInhEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIPkgNoInhEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIPriNoInhEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIPriNoInhEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIPriNoInhEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIProNoInhEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIProNoInhEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIProNoInhEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIPubNoInhEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIPubNoInhEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh.DFIPubNoInhEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd.DFIPkgYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd.DFIPriYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd.DFIProYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd.DFIPubYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd.DFIPkgYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd.DFIPriYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd.DFIProYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd.DFIPubYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.noinh.DFIPkgNoInhTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.noinh.DFIPriNoInhTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.noinh.DFIProNoInhTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dfi.noinh.DFIPubNoInhTestServlet;
import com.ibm.ws.jpa.tests.spec10.injection.common.JPAFATServletClient;
import com.ibm.ws.jpa.tests.spec10.injection.common.RepeaterInfo;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JPA10Injection_DFI extends JPAFATServletClient {

    @Rule
    public org.junit.rules.TestRule skipDBRule = new org.junit.rules.Verifier() {

        @Override
        public Statement apply(Statement arg0, Description arg1) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    String database = getDbVendor().name();
                    boolean shouldSkip = (database != null
                                          && !Pattern.compile("derby", Pattern.CASE_INSENSITIVE).matcher(database).find());
                    System.out.println("Checking if skip test");
                    if (shouldSkip) {
                        throw new AssumptionViolatedException("Database is not Derby. Skipping test!");
                    } else {
                        System.out.println("Not Skipping");
                        arg0.evaluate();
                    }
                }
            };
        }
    };

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_INJECTION_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_CREATE_${dbvendor}.ddl");
    }

    private final static String dfiNoInherit_EJB_contextRoot = "JPA10Injection_DFINoInheritance_EJB";
    private final static String dfiNoInherit_WEB_contextRoot = "JPA10Injection_DFINoInheritance_Web";
    private final static String dfiNoInherit_WEBLIB_contextRoot = "JPA10Injection_DFINoInheritance_WebLib";
    private final static String dfiInherit_DD_EJB_contextRoot = "JPA10Injection_DFIYesInheritance_DDOvrd_EJB";
    private final static String dfiInherit_ANO_EJB_contextRoot = "JPA10Injection_DFIYesInheritance_EJB";

    private final static String dfiInherit_ANO_WEB_contextRoot = "JPA10Injection_DFIYesInheritance_Web";
    private final static String dfiInherit_DD_WEB_contextRoot = "JPA10Injection_DFIYesInheritance_DDOvrd_Web";

    private final static String dfiInherit_ANO_WEBLIB_contextRoot = "JPA10Injection_DFIYesInheritance_WebLib";
    private final static String dfiInherit_DD_WEBLIB_contextRoot = "JPA10Injection_DFIYesInheritance_DDOvrd_WebLib";

    @Server("JPAServerDFI")
    @TestServlets({
                    // No Inheritance EJB
                    @TestServlet(servlet = DFIPkgNoInhEJBSLTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIPkgNoInhEJBSLTestServlet"),
                    @TestServlet(servlet = DFIPriNoInhEJBSLTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIPriNoInhEJBSLTestServlet"),
                    @TestServlet(servlet = DFIProNoInhEJBSLTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIProNoInhEJBSLTestServlet"),
                    @TestServlet(servlet = DFIPubNoInhEJBSLTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIPubNoInhEJBSLTestServlet"),

                    @TestServlet(servlet = DFIPkgNoInhEJBSFTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIPkgNoInhEJBSFTestServlet"),
                    @TestServlet(servlet = DFIPkgNoInhEJBSFEXTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIPkgNoInhEJBSFEXTestServlet"),
                    @TestServlet(servlet = DFIPriNoInhEJBSFTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIPriNoInhEJBSFTestServlet"),
                    @TestServlet(servlet = DFIPriNoInhEJBSFEXTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIPriNoInhEJBSFEXTestServlet"),
                    @TestServlet(servlet = DFIProNoInhEJBSFTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIProNoInhEJBSFTestServlet"),
                    @TestServlet(servlet = DFIProNoInhEJBSFEXTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIProNoInhEJBSFEXTestServlet"),
                    @TestServlet(servlet = DFIPubNoInhEJBSFTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIPubNoInhEJBSFTestServlet"),
                    @TestServlet(servlet = DFIPubNoInhEJBSFEXTestServlet.class, path = dfiNoInherit_EJB_contextRoot + "/" + "DFIPubNoInhEJBSFEXTestServlet"),

                    // No Inheritance WEB
                    @TestServlet(servlet = DFIPkgNoInhTestServlet.class, path = dfiNoInherit_WEB_contextRoot + "/" + "DFIPkgNoInhTestServlet"),
                    @TestServlet(servlet = DFIPriNoInhTestServlet.class, path = dfiNoInherit_WEB_contextRoot + "/" + "DFIPriNoInhTestServlet"),
                    @TestServlet(servlet = DFIProNoInhTestServlet.class, path = dfiNoInherit_WEB_contextRoot + "/" + "DFIProNoInhTestServlet"),
                    @TestServlet(servlet = DFIPubNoInhTestServlet.class, path = dfiNoInherit_WEB_contextRoot + "/" + "DFIPubNoInhTestServlet"),

                    // No Inheritance WEB-LIB
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.noinh.DFIPkgNoInhTestServlet.class,
                                 path = dfiNoInherit_WEBLIB_contextRoot + "/" + "DFIPkgNoInhTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.noinh.DFIPriNoInhTestServlet.class,
                                 path = dfiNoInherit_WEBLIB_contextRoot + "/" + "DFIPriNoInhTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.noinh.DFIProNoInhTestServlet.class,
                                 path = dfiNoInherit_WEBLIB_contextRoot + "/" + "DFIProNoInhTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.noinh.DFIPubNoInhTestServlet.class,
                                 path = dfiNoInherit_WEBLIB_contextRoot + "/" + "DFIPubNoInhTestServlet"),

                    // Inheritance DD Override EJB
                    @TestServlet(servlet = DFIPkgYesInhDDOvrdEJBSLTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIPkgYesInhDDOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DFIPriYesInhDDOvrdEJBSLTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIPriYesInhDDOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DFIProYesInhDDOvrdEJBSLTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIProYesInhDDOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DFIPubYesInhDDOvrdEJBSLTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIPubYesInhDDOvrdEJBSLTestServlet"),

                    @TestServlet(servlet = DFIPkgYesInhDDOvrdEJBSFTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIPkgYesInhDDOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DFIPriYesInhDDOvrdEJBSFTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIPriYesInhDDOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DFIProYesInhDDOvrdEJBSFTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIProYesInhDDOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DFIPubYesInhDDOvrdEJBSFTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIPubYesInhDDOvrdEJBSFTestServlet"),

                    @TestServlet(servlet = DFIPkgYesInhDDOvrdEJBSFEXTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIPkgYesInhDDOvrdEJBSFEXTestServlet"),
                    @TestServlet(servlet = DFIPriYesInhDDOvrdEJBSFEXTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIPriYesInhDDOvrdEJBSFEXTestServlet"),
                    @TestServlet(servlet = DFIProYesInhDDOvrdEJBSFEXTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIProYesInhDDOvrdEJBSFEXTestServlet"),
                    @TestServlet(servlet = DFIPubYesInhDDOvrdEJBSFEXTestServlet.class, path = dfiInherit_DD_EJB_contextRoot + "/" + "DFIPubYesInhDDOvrdEJBSFEXTestServlet"),

                    // Inheritance Ano Override EJB
                    @TestServlet(servlet = DFIPkgYesInhAnoOvrdEJBSLTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIPkgYesInhAnoOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DFIPriYesInhAnoOvrdEJBSLTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIPriYesInhAnoOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DFIProYesInhAnoOvrdEJBSLTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIProYesInhAnoOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DFIPubYesInhAnoOvrdEJBSLTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIPubYesInhAnoOvrdEJBSLTestServlet"),

                    @TestServlet(servlet = DFIPkgYesInhAnoOvrdEJBSFTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIPkgYesInhAnoOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DFIPkgYesInhAnoOvrdEJBSFEXTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIPkgYesInhAnoOvrdEJBSFEXTestServlet"),

                    @TestServlet(servlet = DFIPriYesInhAnoOvrdEJBSFTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIPriYesInhAnoOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DFIPriYesInhAnoOvrdEJBSFEXTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIPriYesInhAnoOvrdEJBSFEXTestServlet"),

                    @TestServlet(servlet = DFIProYesInhAnoOvrdEJBSFTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIProYesInhAnoOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DFIProYesInhAnoOvrdEJBSFEXTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIProYesInhAnoOvrdEJBSFEXTestServlet"),

                    @TestServlet(servlet = DFIPubYesInhAnoOvrdEJBSFTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIPubYesInhAnoOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DFIPubYesInhAnoOvrdEJBSFEXTestServlet.class, path = dfiInherit_ANO_EJB_contextRoot + "/" + "DFIPubYesInhAnoOvrdEJBSFEXTestServlet"),

                    // Inheritance Web
                    @TestServlet(servlet = DFIPkgYesInhAnoOvrdTestServlet.class, path = dfiInherit_ANO_WEB_contextRoot + "/" + "DFIPkgYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = DFIPriYesInhAnoOvrdTestServlet.class, path = dfiInherit_ANO_WEB_contextRoot + "/" + "DFIPriYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = DFIProYesInhAnoOvrdTestServlet.class, path = dfiInherit_ANO_WEB_contextRoot + "/" + "DFIProYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = DFIPubYesInhAnoOvrdTestServlet.class, path = dfiInherit_ANO_WEB_contextRoot + "/" + "DFIPubYesInhAnoOvrdTestServlet"),

                    @TestServlet(servlet = DFIPkgYesInhDDOvrdTestServlet.class, path = dfiInherit_DD_WEB_contextRoot + "/" + "DFIPkgYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = DFIPriYesInhDDOvrdTestServlet.class, path = dfiInherit_DD_WEB_contextRoot + "/" + "DFIPriYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = DFIProYesInhDDOvrdTestServlet.class, path = dfiInherit_DD_WEB_contextRoot + "/" + "DFIProYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = DFIPubYesInhDDOvrdTestServlet.class, path = dfiInherit_DD_WEB_contextRoot + "/" + "DFIPubYesInhDDOvrdTestServlet"),

                    // Inheritance Web Lib
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.anoovrd.DFIPkgYesInhAnoOvrdTestServlet.class,
                                 path = dfiInherit_ANO_WEBLIB_contextRoot + "/" + "DFIPkgYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.anoovrd.DFIPriYesInhAnoOvrdTestServlet.class,
                                 path = dfiInherit_ANO_WEBLIB_contextRoot + "/" + "DFIPriYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.anoovrd.DFIProYesInhAnoOvrdTestServlet.class,
                                 path = dfiInherit_ANO_WEBLIB_contextRoot + "/" + "DFIProYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.anoovrd.DFIPubYesInhAnoOvrdTestServlet.class,
                                 path = dfiInherit_ANO_WEBLIB_contextRoot + "/" + "DFIPubYesInhAnoOvrdTestServlet"),

                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.ddovrd.DFIPkgYesInhDDOvrdTestServlet.class,
                                 path = dfiInherit_DD_WEBLIB_contextRoot + "/" + "DFIPkgYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.ddovrd.DFIPriYesInhDDOvrdTestServlet.class,
                                 path = dfiInherit_DD_WEBLIB_contextRoot + "/" + "DFIPriYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.ddovrd.DFIProYesInhDDOvrdTestServlet.class,
                                 path = dfiInherit_DD_WEBLIB_contextRoot + "/" + "DFIProYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.ddovrd.DFIPubYesInhDDOvrdTestServlet.class,
                                 path = dfiInherit_DD_WEBLIB_contextRoot + "/" + "DFIPubYesInhDDOvrdTestServlet"),

    })
    public static LibertyServer server1;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        int appStartTimeout = server1.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server1.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server1.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server1.setConfigUpdateTimeout(120 * 1000);
        }

        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(JPA10Injection_DFI.class);
        timestart = System.currentTimeMillis();

        server1.addEnvVar("repeat_phase", RepeaterInfo.repeatPhase);

        //Get driver name
        server1.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server1, testContainer);

        server1.startServer();

        setupDatabaseApplication(server1, "test-applications/injection/" + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, false);

        setupTestApplication_NoInheritance_EJB();
        setupTestApplication_NoInheritance_WEB();
        setupTestApplication_NoInheritance_WEBLIB();
        setupTestApplication_Inheritance_DD_EJB();
        setupTestApplication_Inheritance_Ano_EJB();
        setupTestApplication_Inheritance_WEB();
        setupTestApplication_Inheritance_WEBLIB();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            try {
                ServerConfiguration sc = server1.getServerConfiguration();
                sc.getApplications().clear();
                server1.updateServerConfiguration(sc);
                server1.saveServerConfiguration();

                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionDFINoInheritanceEJB" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "JPA10Injection_DFINoInheritance_Web" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "JPA10Injection_DFINoInheritance_WebLib" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionDFIYesInheritanceDDOvrdEJB" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionDFIYesInheritanceEJB" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "JPA10Injection_DFIYesInheritance_Web" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "JPA10Injection_DFIYesInheritance_WebLib" + ".ear");

                server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(JPA10Injection_DFI.class, timestart);
        }
    }

    private static void setupTestApplication_NoInheritance_EJB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "InjectionDFINoInheritanceEJB"; // Name of EAR

        final String appModuleName = "injectionDFINoInheritance";
        final String appFileRootPath = RESOURCE_ROOT + "ejb/" + appModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");

        // jpaejbA.jar
        final JavaArchive jpaejbAJar = ShrinkWrap.create(JavaArchive.class, "jpaejbA.jar");
        jpaejbAJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityA.class);

        // jpaejbA.jar
        final JavaArchive jpaejbBJar = ShrinkWrap.create(JavaArchive.class, "jpaejbB.jar");
        jpaejbBJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityB.class);

        // jpawar.jar
        final JavaArchive jpawarJar = ShrinkWrap.create(JavaArchive.class, "jpawar.jar");
        jpawarJar.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        // EJB Jar Module
        JavaArchive ejbApp = ShrinkWrap.create(JavaArchive.class, appModuleName + ".jar");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.ejb.dfi.noinh");
        ShrinkHelper.addDirectory(ejbApp, appFileRootPath + appModuleName + ".jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appModuleName + "ejb.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.noinh");
        ShrinkHelper.addDirectory(webApp, appFileRootPath + appModuleName + "ejb.war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsModule(ejbApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbAJar);
        app.addAsLibrary(jpaejbBJar);
        app.addAsLibrary(jpawarJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, appFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    private static void setupTestApplication_NoInheritance_WEB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "JPA10Injection_DFINoInheritance_Web"; // Name of EAR

        final String webModuleName = "injectionDFINoInheritance";
        final String webFileRootPath = RESOURCE_ROOT + "web/" + webModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dfi.noinh");
        ShrinkHelper.addDirectory(webApp, webFileRootPath + webModuleName + ".war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, webFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    private static void setupTestApplication_NoInheritance_WEBLIB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "JPA10Injection_DFINoInheritance_WebLib"; // Name of EAR

        final String webModuleName = "injectionDFINoInheritance";
        final String webFileRootPath = RESOURCE_ROOT + "weblib/" + webModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Lib Jar
        final JavaArchive weblibJar = ShrinkWrap.create(JavaArchive.class, "weblib.jar");
        weblibJar.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        ShrinkHelper.addDirectory(weblibJar, webFileRootPath + "/weblib.jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.noinh");
        webApp.addAsLibrary(weblibJar);
        ShrinkHelper.addDirectory(webApp, webFileRootPath + webModuleName + ".war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, webFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    private static void setupTestApplication_Inheritance_DD_EJB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "InjectionDFIYesInheritanceDDOvrdEJB"; // Name of EAR

        final String appModuleName = "injectionDFIYesInheritanceDDOvrd";
        final String appFileRootPath = RESOURCE_ROOT + "ejb/" + appModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");

        // jpaejbA.jar
        final JavaArchive jpaejbAJar = ShrinkWrap.create(JavaArchive.class, "jpaejbA.jar");
        jpaejbAJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityA.class);

        // jpaejbA.jar
        final JavaArchive jpaejbBJar = ShrinkWrap.create(JavaArchive.class, "jpaejbB.jar");
        jpaejbBJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityB.class);

        // jpawar.jar
        final JavaArchive jpawarJar = ShrinkWrap.create(JavaArchive.class, "jpawar.jar");
        jpawarJar.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        // EJB Jar Module
        JavaArchive ejbApp = ShrinkWrap.create(JavaArchive.class, appModuleName + ".jar");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.ejb.dfi.inh.ddovrd");
        ShrinkHelper.addDirectory(ejbApp, appFileRootPath + appModuleName + ".jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appModuleName + "ejb.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.ddovrd");
        ShrinkHelper.addDirectory(webApp, appFileRootPath + appModuleName + "ejb.war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsModule(ejbApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbAJar);
        app.addAsLibrary(jpaejbBJar);
        app.addAsLibrary(jpawarJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, appFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    private static void setupTestApplication_Inheritance_Ano_EJB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "InjectionDFIYesInheritanceEJB"; // Name of EAR

        final String appModuleName = "injectionDFIYesInheritance";
        final String appFileRootPath = RESOURCE_ROOT + "ejb/" + appModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");
//        ShrinkHelper.addDirectory(jpacoreJar, libsPath + "jpacore.jar");

        // jpaejbA.jar
        final JavaArchive jpaejbAJar = ShrinkWrap.create(JavaArchive.class, "jpaejbA.jar");
        jpaejbAJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityA.class);
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        // jpaejbA.jar
        final JavaArchive jpaejbBJar = ShrinkWrap.create(JavaArchive.class, "jpaejbB.jar");
        jpaejbBJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityB.class);
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        // jpawar.jar
        final JavaArchive jpawarJar = ShrinkWrap.create(JavaArchive.class, "jpawar.jar");
        jpawarJar.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        // EJB Jar Module
        JavaArchive ejbApp = ShrinkWrap.create(JavaArchive.class, appModuleName + ".jar");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.ejb.dfi.inh.anoovrd");
        ShrinkHelper.addDirectory(ejbApp, appFileRootPath + appModuleName + ".jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appModuleName + "ejb.war");
//        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.ejb.dfi.inh.anoovrd");
        ShrinkHelper.addDirectory(webApp, appFileRootPath + appModuleName + "ejb.war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsModule(ejbApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbAJar);
        app.addAsLibrary(jpaejbBJar);
        app.addAsLibrary(jpawarJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, appFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    private static void setupTestApplication_Inheritance_WEB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "JPA10Injection_DFIYesInheritance_Web"; // Name of EAR

        final String webModuleName = "injectionDFIYesInheritance";
        final String webModule2Name = "injectionDFIYesInheritanceDDOvrd";
        final String webFileRootPath = RESOURCE_ROOT + "web/" + webModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.anoovrd");
        ShrinkHelper.addDirectory(webApp, webFileRootPath + webModuleName + ".war");

        WebArchive webApp2 = ShrinkWrap.create(WebArchive.class, webModule2Name + ".war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dfi.inh.ddovrd");
        ShrinkHelper.addDirectory(webApp2, webFileRootPath + webModule2Name + ".war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsModule(webApp2);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, webFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    private static void setupTestApplication_Inheritance_WEBLIB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "JPA10Injection_DFIYesInheritance_WebLib"; // Name of EAR

        final String webModuleName = "injectionDFIYesInheritance";
        final String webModule2Name = "injectionDFIYesInheritanceDDOvrd";
        final String webFileRootPath = RESOURCE_ROOT + "weblib/" + webModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Ano Web Lib Jar
        final JavaArchive weblibanoJar = ShrinkWrap.create(JavaArchive.class, "weblibano.jar");
        weblibanoJar.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        ShrinkHelper.addDirectory(weblibanoJar, webFileRootPath + "/weblibano.jar");

        // DD Web Lib Jar
        final JavaArchive weblibddJar = ShrinkWrap.create(JavaArchive.class, "weblibddovrd.jar");
        weblibddJar.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        ShrinkHelper.addDirectory(weblibddJar, webFileRootPath + "/weblibddovrd.jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.anoovrd");
        webApp.addAsLibrary(weblibanoJar);
        ShrinkHelper.addDirectory(webApp, webFileRootPath + webModuleName + ".war");

        WebArchive webApp2 = ShrinkWrap.create(WebArchive.class, webModule2Name + ".war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.weblib.dfi.inh.ddovrd");
        webApp2.addAsLibrary(weblibddJar);
        ShrinkHelper.addDirectory(webApp2, webFileRootPath + webModule2Name + ".war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsModule(webApp2);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, webFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

}
