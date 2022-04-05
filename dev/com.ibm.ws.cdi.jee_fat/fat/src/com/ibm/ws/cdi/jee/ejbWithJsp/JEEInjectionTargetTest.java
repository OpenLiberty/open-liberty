/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.jee.ejbWithJsp;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi.jee.ejbWithJsp.servlet.JEEResourceExtension;
import com.ibm.ws.cdi.jee.ejbWithJsp.servlet.LoggerServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class JEEInjectionTargetTest extends FATServletClient {

    public static final String APP_NAME = "jeeInjectionTargetTest";
    public static final String SERVER_NAME = "cdi12JEEInjectionTargetTestServer";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE9, EERepeatActions.EE10, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    @TestServlet(servlet = LoggerServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive jeeInjectionTargetTestEJB = ShrinkWrap.create(JavaArchive.class, APP_NAME + "EJB.jar");
        Package pkg = com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MySessionBean1.class.getPackage();
        jeeInjectionTargetTestEJB.addPackages(true, pkg);
        jeeInjectionTargetTestEJB.addAsManifestResource(pkg, "ejb-jar.xml", "ejb-jar.xml");
        jeeInjectionTargetTestEJB.addAsManifestResource(pkg, "ibm-managed-bean-bnd.xml", "ibm-managed-bean-bnd.xml");

        WebArchive jeeInjectionTargetTest = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        jeeInjectionTargetTest.addPackage(com.ibm.ws.cdi.jee.ejbWithJsp.servlet.JEEResourceTestServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(jeeInjectionTargetTest, DiscoveryMode.ANNOTATED);
        CDIArchiveHelper.addCDIExtensionService(jeeInjectionTargetTest, JEEResourceExtension.class);

        WebArchive jeeInjectionTargetTestJSP = ShrinkWrap.create(WebArchive.class, APP_NAME + "JSP2.3.war");
        Package jspPkg = com.ibm.ws.cdi.jee.ejbWithJsp.jsp.SimpleTestServlet.class.getPackage();
        jeeInjectionTargetTestJSP.addPackages(true, jspPkg);
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30PropertyNotFoundException.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30OperatorPrecedences.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "TagLibraryEventListenerCI.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/tlds/EventListeners.tld");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/tlds/Tag2Lib.tld");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/tlds/Tag1Lib.tld");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/tags/EL30CoercionRulesTest.tag");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/web.xml");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "TagLibraryEventListenerMI.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/lt.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/gt.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/false.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/mod.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/le.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/or.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/NonReservedWords.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/ne.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/instanceof.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/not.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/ge.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/empty.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/eq.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/null.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/true.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/and.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/div.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL22Operators.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30CoercionRules.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30Lambda.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30InvocationMethodExpressions.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "Tag2.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30MethodNotFoundException.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30PropertyNotWritableException.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "TagLibraryEventListenerFI.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30CollectionObjectOperations.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30AssignmentOperatorException.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30Operators.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "Tag1.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "EL30StaticFieldsAndMethodsTests.jsp");
        jeeInjectionTargetTestJSP = ShrinkHelper.addResource(jeeInjectionTargetTestJSP, jspPkg, "Servlet31RequestResponseTest.jsp");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jeeInjectionTargetTest.ear");
        ear.addAsModule(jeeInjectionTargetTestEJB);
        ear.addAsModule(jeeInjectionTargetTest);
        ear.addAsModule(jeeInjectionTargetTestJSP);

        ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
