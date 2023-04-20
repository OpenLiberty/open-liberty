/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
import com.ibm.ws.cdi.jee.ShrinkWrapUtils;
import com.ibm.ws.cdi.jee.ejbWithJsp.servlet.JEEResourceExtension;
import com.ibm.ws.cdi.jee.ejbWithJsp.servlet.LoggerServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class JEEInjectionTargetTest extends FATServletClient {

    public static final String APP_NAME = "jeeInjectionTargetTest";
    public static final String SERVER_NAME = "cdi12JEEInjectionTargetTestServer";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE7);

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
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30PropertyNotFoundException.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30OperatorPrecedences.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "TagLibraryEventListenerCI.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/tlds/EventListeners.tld");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/tlds/Tag2Lib.tld");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/tlds/Tag1Lib.tld");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/tags/EL30CoercionRulesTest.tag");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "WEB-INF/web.xml");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "TagLibraryEventListenerMI.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/lt.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/gt.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/false.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/mod.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/le.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/or.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/NonReservedWords.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/ne.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/instanceof.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/not.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/ge.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/empty.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/eq.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/null.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/true.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/and.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30ReservedWords/div.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL22Operators.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30CoercionRules.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30Lambda.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30InvocationMethodExpressions.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "Tag2.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30MethodNotFoundException.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30PropertyNotWritableException.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "TagLibraryEventListenerFI.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30CollectionObjectOperations.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30AssignmentOperatorException.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30Operators.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "Tag1.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "EL30StaticFieldsAndMethodsTests.jsp");
        jeeInjectionTargetTestJSP = ShrinkWrapUtils.addAsRootResource(jeeInjectionTargetTestJSP, jspPkg, "Servlet31RequestResponseTest.jsp");

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
