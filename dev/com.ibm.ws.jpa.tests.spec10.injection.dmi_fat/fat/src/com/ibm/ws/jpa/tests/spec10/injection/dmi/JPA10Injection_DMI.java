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

package com.ibm.ws.jpa.tests.spec10.injection.dmi;

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
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIPkgYesInhAnoOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIPkgYesInhAnoOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIPkgYesInhAnoOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIPriYesInhAnoOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIPriYesInhAnoOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIPriYesInhAnoOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIProYesInhAnoOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIProYesInhAnoOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIProYesInhAnoOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIPubYesInhAnoOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIPubYesInhAnoOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd.DMIPubYesInhAnoOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIPkgYesInhDDOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIPkgYesInhDDOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIPkgYesInhDDOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIPriYesInhDDOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIPriYesInhDDOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIPriYesInhDDOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIProYesInhDDOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIProYesInhDDOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIProYesInhDDOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIPubYesInhDDOvrdEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIPubYesInhDDOvrdEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd.DMIPubYesInhDDOvrdEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIPkgNoInhEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIPkgNoInhEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIPkgNoInhEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIPriNoInhEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIPriNoInhEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIPriNoInhEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIProNoInhEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIProNoInhEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIProNoInhEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIPubNoInhEJBSFEXTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIPubNoInhEJBSFTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh.DMIPubNoInhEJBSLTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.anoovrd.DMIPkgYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.anoovrd.DMIPriYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.anoovrd.DMIProYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.anoovrd.DMIPubYesInhAnoOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.ddovrd.DMIPkgYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.ddovrd.DMIPriYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.ddovrd.DMIProYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.ddovrd.DMIPubYesInhDDOvrdTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.noinh.DMIPkgNoInhTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.noinh.DMIPriNoInhTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.noinh.DMIProNoInhTestServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.dmi.noinh.DMIPubNoInhTestServlet;
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
public class JPA10Injection_DMI extends JPAFATServletClient {

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

    private final static String dmiNoInherit_EJB_contextRoot = "JPA10Injection_DMINoInheritance_EJB";
    private final static String dmiNoInherit_WEB_contextRoot = "JPA10Injection_DMINoInheritance_Web";
    private final static String dmiNoInherit_WEBLIB_contextRoot = "JPA10Injection_DMINoInheritance_WebLib";

    private final static String dmiInherit_DD_EJB_contextRoot = "JPA10Injection_DMIYesInheritance_DDOvrd_EJB";
    private final static String dmiInherit_ANO_EJB_contextRoot = "JPA10Injection_DMIYesInheritance_EJB";

    private final static String dmiInherit_ANO_WEB_contextRoot = "JPA10Injection_DMIYesInheritance_Web";
    private final static String dmiInherit_DD_WEB_contextRoot = "JPA10Injection_DMIYesInheritance_DDOvrd_Web";

    private final static String dmiInherit_ANO_WEBLIB_contextRoot = "JPA10Injection_DMIYesInheritance_WebLib";
    private final static String dmiInherit_DD_WEBLIB_contextRoot = "JPA10Injection_DMIYesInheritance_DDOvrd_WebLib";

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_INJECTION_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_CREATE_${dbvendor}.ddl");
    }

    @Server("JPAServerDMI")
    @TestServlets({
                    // No Inheritance EJB
                    @TestServlet(servlet = DMIPkgNoInhEJBSLTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIPkgNoInhEJBSLTestServlet"),
                    @TestServlet(servlet = DMIPriNoInhEJBSLTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIPriNoInhEJBSLTestServlet"),
                    @TestServlet(servlet = DMIProNoInhEJBSLTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIProNoInhEJBSLTestServlet"),
                    @TestServlet(servlet = DMIPubNoInhEJBSLTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIPubNoInhEJBSLTestServlet"),

                    @TestServlet(servlet = DMIPkgNoInhEJBSFTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIPkgNoInhEJBSFTestServlet"),
                    @TestServlet(servlet = DMIPkgNoInhEJBSFEXTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIPkgNoInhEJBSFEXTestServlet"),
                    @TestServlet(servlet = DMIPriNoInhEJBSFTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIPriNoInhEJBSFTestServlet"),
                    @TestServlet(servlet = DMIPriNoInhEJBSFEXTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIPriNoInhEJBSFEXTestServlet"),
                    @TestServlet(servlet = DMIProNoInhEJBSFTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIProNoInhEJBSFTestServlet"),
                    @TestServlet(servlet = DMIProNoInhEJBSFEXTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIProNoInhEJBSFEXTestServlet"),
                    @TestServlet(servlet = DMIPubNoInhEJBSFTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIPubNoInhEJBSFTestServlet"),
                    @TestServlet(servlet = DMIPubNoInhEJBSFEXTestServlet.class, path = dmiNoInherit_EJB_contextRoot + "/" + "DMIPubNoInhEJBSFEXTestServlet"),

                    // No Inheritance WEB
                    @TestServlet(servlet = DMIPkgNoInhTestServlet.class, path = dmiNoInherit_WEB_contextRoot + "/" + "DMIPkgNoInhTestServlet"),
                    @TestServlet(servlet = DMIPriNoInhTestServlet.class, path = dmiNoInherit_WEB_contextRoot + "/" + "DMIPriNoInhTestServlet"),
                    @TestServlet(servlet = DMIProNoInhTestServlet.class, path = dmiNoInherit_WEB_contextRoot + "/" + "DMIProNoInhTestServlet"),
                    @TestServlet(servlet = DMIPubNoInhTestServlet.class, path = dmiNoInherit_WEB_contextRoot + "/" + "DMIPubNoInhTestServlet"),

                    // No Inheritance WEB-LIB
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.noinh.DMIPkgNoInhTestServlet.class,
                                 path = dmiNoInherit_WEBLIB_contextRoot + "/" + "DMIPkgNoInhTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.noinh.DMIPriNoInhTestServlet.class,
                                 path = dmiNoInherit_WEBLIB_contextRoot + "/" + "DMIPriNoInhTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.noinh.DMIProNoInhTestServlet.class,
                                 path = dmiNoInherit_WEBLIB_contextRoot + "/" + "DMIProNoInhTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.noinh.DMIPubNoInhTestServlet.class,
                                 path = dmiNoInherit_WEBLIB_contextRoot + "/" + "DMIPubNoInhTestServlet"),

                    // Inheritance DD Override EJB
                    @TestServlet(servlet = DMIPkgYesInhDDOvrdEJBSLTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIPkgYesInhDDOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DMIPriYesInhDDOvrdEJBSLTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIPriYesInhDDOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DMIProYesInhDDOvrdEJBSLTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIProYesInhDDOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DMIPubYesInhDDOvrdEJBSLTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIPubYesInhDDOvrdEJBSLTestServlet"),

                    @TestServlet(servlet = DMIPkgYesInhDDOvrdEJBSFTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIPkgYesInhDDOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DMIPriYesInhDDOvrdEJBSFTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIPriYesInhDDOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DMIProYesInhDDOvrdEJBSFTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIProYesInhDDOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DMIPubYesInhDDOvrdEJBSFTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIPubYesInhDDOvrdEJBSFTestServlet"),

                    @TestServlet(servlet = DMIPkgYesInhDDOvrdEJBSFEXTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIPkgYesInhDDOvrdEJBSFEXTestServlet"),
                    @TestServlet(servlet = DMIPriYesInhDDOvrdEJBSFEXTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIPriYesInhDDOvrdEJBSFEXTestServlet"),
                    @TestServlet(servlet = DMIProYesInhDDOvrdEJBSFEXTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIProYesInhDDOvrdEJBSFEXTestServlet"),
                    @TestServlet(servlet = DMIPubYesInhDDOvrdEJBSFEXTestServlet.class, path = dmiInherit_DD_EJB_contextRoot + "/" + "DMIPubYesInhDDOvrdEJBSFEXTestServlet"),

                    // Inheritance Ano Override EJB
                    @TestServlet(servlet = DMIPkgYesInhAnoOvrdEJBSLTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIPkgYesInhAnoOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DMIPriYesInhAnoOvrdEJBSLTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIPriYesInhAnoOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DMIProYesInhAnoOvrdEJBSLTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIProYesInhAnoOvrdEJBSLTestServlet"),
                    @TestServlet(servlet = DMIPubYesInhAnoOvrdEJBSLTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIPubYesInhAnoOvrdEJBSLTestServlet"),

                    @TestServlet(servlet = DMIPkgYesInhAnoOvrdEJBSFTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIPkgYesInhAnoOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DMIPkgYesInhAnoOvrdEJBSFEXTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIPkgYesInhAnoOvrdEJBSFEXTestServlet"),

                    @TestServlet(servlet = DMIPriYesInhAnoOvrdEJBSFTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIPriYesInhAnoOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DMIPriYesInhAnoOvrdEJBSFEXTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIPriYesInhAnoOvrdEJBSFEXTestServlet"),

                    @TestServlet(servlet = DMIProYesInhAnoOvrdEJBSFTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIProYesInhAnoOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DMIProYesInhAnoOvrdEJBSFEXTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIProYesInhAnoOvrdEJBSFEXTestServlet"),

                    @TestServlet(servlet = DMIPubYesInhAnoOvrdEJBSFTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIPubYesInhAnoOvrdEJBSFTestServlet"),
                    @TestServlet(servlet = DMIPubYesInhAnoOvrdEJBSFEXTestServlet.class, path = dmiInherit_ANO_EJB_contextRoot + "/" + "DMIPubYesInhAnoOvrdEJBSFEXTestServlet"),

                    //
                    @TestServlet(servlet = DMIPkgYesInhAnoOvrdTestServlet.class, path = dmiInherit_ANO_WEB_contextRoot + "/" + "DMIPkgYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = DMIPriYesInhAnoOvrdTestServlet.class, path = dmiInherit_ANO_WEB_contextRoot + "/" + "DMIPriYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = DMIProYesInhAnoOvrdTestServlet.class, path = dmiInherit_ANO_WEB_contextRoot + "/" + "DMIProYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = DMIPubYesInhAnoOvrdTestServlet.class, path = dmiInherit_ANO_WEB_contextRoot + "/" + "DMIPubYesInhAnoOvrdTestServlet"),

                    @TestServlet(servlet = DMIPkgYesInhDDOvrdTestServlet.class, path = dmiInherit_DD_WEB_contextRoot + "/" + "DMIPkgYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = DMIPriYesInhDDOvrdTestServlet.class, path = dmiInherit_DD_WEB_contextRoot + "/" + "DMIPriYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = DMIProYesInhDDOvrdTestServlet.class, path = dmiInherit_DD_WEB_contextRoot + "/" + "DMIProYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = DMIPubYesInhDDOvrdTestServlet.class, path = dmiInherit_DD_WEB_contextRoot + "/" + "DMIPubYesInhDDOvrdTestServlet"),

                    //
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.anoovrd.DMIPkgYesInhAnoOvrdTestServlet.class,
                                 path = dmiInherit_ANO_WEBLIB_contextRoot + "/" + "DMIPkgYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.anoovrd.DMIPriYesInhAnoOvrdTestServlet.class,
                                 path = dmiInherit_ANO_WEBLIB_contextRoot + "/" + "DMIPriYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.anoovrd.DMIProYesInhAnoOvrdTestServlet.class,
                                 path = dmiInherit_ANO_WEBLIB_contextRoot + "/" + "DMIProYesInhAnoOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.anoovrd.DMIPubYesInhAnoOvrdTestServlet.class,
                                 path = dmiInherit_ANO_WEBLIB_contextRoot + "/" + "DMIPubYesInhAnoOvrdTestServlet"),

                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.ddovrd.DMIPkgYesInhDDOvrdTestServlet.class,
                                 path = dmiInherit_DD_WEBLIB_contextRoot + "/" + "DMIPkgYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.ddovrd.DMIPriYesInhDDOvrdTestServlet.class,
                                 path = dmiInherit_DD_WEBLIB_contextRoot + "/" + "DMIPriYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.ddovrd.DMIProYesInhDDOvrdTestServlet.class,
                                 path = dmiInherit_DD_WEBLIB_contextRoot + "/" + "DMIProYesInhDDOvrdTestServlet"),
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.ddovrd.DMIPubYesInhDDOvrdTestServlet.class,
                                 path = dmiInherit_DD_WEBLIB_contextRoot + "/" + "DMIPubYesInhDDOvrdTestServlet"),

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
        bannerStart(JPA10Injection_DMI.class);
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

                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionDMINoInheritanceEJB" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "JPA10Injection_DMINoInheritance_Web" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "JPA10Injection_DMINoInheritance_WebLib" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionDMIYesInheritanceDDOvrdEJB" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionDMIYesInheritanceEJB" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "JPA10Injection_DMIYesInheritance_Web" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "JPA10Injection_DMIYesInheritance_WebLib" + ".ear");

                server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(JPA10Injection_DMI.class, timestart);
        }
    }

    private static void setupTestApplication_NoInheritance_EJB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "InjectionDMINoInheritanceEJB"; // Name of EAR

        final String appModuleName = "injectionDMINoInheritance";
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
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.ejb.dmi.noinh");
        ShrinkHelper.addDirectory(ejbApp, appFileRootPath + appModuleName + ".jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appModuleName + "ejb.war");
//        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.noinh");
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
        final String applicationName = "JPA10Injection_DMINoInheritance_Web"; // Name of EAR

        final String webModuleName = "injectionDMINoInheritance";
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
//        ShrinkHelper.addDirectory(jpacoreJar, libsPath + "jpacore.jar");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dmi.noinh");
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
        final String applicationName = "JPA10Injection_DMINoInheritance_WebLib"; // Name of EAR

        final String webModuleName = "injectionDMINoInheritance";
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
//        ShrinkHelper.addDirectory(jpacoreJar, libsPath + "jpacore.jar");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Lib Jar
        final JavaArchive weblibJar = ShrinkWrap.create(JavaArchive.class, "weblib.jar");
        weblibJar.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        ShrinkHelper.addDirectory(weblibJar, webFileRootPath + "/weblib.jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.noinh");
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
        final String applicationName = "InjectionDMIYesInheritanceDDOvrdEJB"; // Name of EAR

        final String appModuleName = "injectionDMIYesInheritanceDDOvrd";
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
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.ejb.dmi");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.ejb.dmi.inh.ddovrd");
        ShrinkHelper.addDirectory(ejbApp, appFileRootPath + appModuleName + ".jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appModuleName + "ejb.war");
//        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.ddovrd");
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
        final String applicationName = "InjectionDMIYesInheritanceEJB"; // Name of EAR

        final String appModuleName = "injectionDMIYesInheritance";
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
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.ejb.dmi");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.ejb.dmi.inh.anoovrd");
        ShrinkHelper.addDirectory(ejbApp, appFileRootPath + appModuleName + ".jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appModuleName + "ejb.war");
//        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.ejb.dmi.inh.anoovrd");
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
        final String applicationName = "JPA10Injection_DMIYesInheritance_Web"; // Name of EAR

        final String webModuleName = "injectionDMIYesInheritance";
        final String webModule2Name = "injectionDMIYesInheritanceDDOvrd";
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
//        ShrinkHelper.addDirectory(jpacoreJar, libsPath + "jpacore.jar");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.anoovrd");
        ShrinkHelper.addDirectory(webApp, webFileRootPath + webModuleName + ".war");

        WebArchive webApp2 = ShrinkWrap.create(WebArchive.class, webModule2Name + ".war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh.ddovrd");
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
        final String applicationName = "JPA10Injection_DMIYesInheritance_WebLib"; // Name of EAR

        final String webModuleName = "injectionDMIYesInheritance";
        final String webModule2Name = "injectionDMIYesInheritanceDDOvrd";
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
//        ShrinkHelper.addDirectory(jpacoreJar, libsPath + "jpacore.jar");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

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
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.anoovrd");
        webApp.addAsLibrary(weblibanoJar);
        ShrinkHelper.addDirectory(webApp, webFileRootPath + webModuleName + ".war");

        WebArchive webApp2 = ShrinkWrap.create(WebArchive.class, webModule2Name + ".war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.dmi.inh");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.weblib.dmi.inh.ddovrd");
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
