/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.bnd.lookupoverride.web.LookupOverrideServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

// From AppInstallChgDefBndTest
@RunWith(FATRunner.class)
public class LookupOverrideTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.injection.fat.mdbdatasourceserver")
    @TestServlet(servlet = LookupOverrideServlet.class, contextRoot = "LookupOverrideWeb")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.injection.fat.mdbdatasourceserver")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.injection.fat.mdbdatasourceserver"));

    public static JavaArchive LookupOverrideEJBShared;
    public static JavaArchive LookupOverrideEJB;

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the ears

        //Shared INTF
        LookupOverrideEJBShared = ShrinkHelper.buildJavaArchive("LookupOverrideIntf.jar", "com.ibm.bnd.lookupoverride.shared.");

        JavaArchive LookupOverrideEJB = ShrinkHelper.buildJavaArchive("LookupOverrideEJB.jar", "com.ibm.bnd.lookupoverride.driver.ejb.");
        ShrinkHelper.addDirectory(LookupOverrideEJB, "test-applications/LookupOverrideEJB.jar/resources/driver");

        WebArchive LookupOverrideWeb = ShrinkHelper.buildDefaultApp("LookupOverrideWeb.war", "com.ibm.bnd.lookupoverride.web.");

        EnterpriseArchive LookupOverrideTestApp = ShrinkWrap.create(EnterpriseArchive.class, "LookupOverrideApp.ear");
        LookupOverrideTestApp.addAsModules(LookupOverrideEJB, LookupOverrideWeb);
        LookupOverrideTestApp.addAsLibrary(LookupOverrideEJBShared);
        ShrinkHelper.addDirectory(LookupOverrideTestApp, "test-applications/LookupOverrideTestApp.ear/resources");

        for (int appNum = 1; appNum <= 7; appNum++) {
            JavaArchive DoaAppEJB = ShrinkHelper.buildJavaArchive("DoaApp" + appNum + ".jar");
            if (appNum == 1) {
                DoaAppEJB.addPackage("com.ibm.bnd.lookupoverride.doaApp1.ejb.");
            } else if (appNum == 7) {
                DoaAppEJB.addPackage("com.ibm.bnd.lookupoverride.doaApp7.ejb.");
            }
            ShrinkHelper.addDirectory(DoaAppEJB, "test-applications/LookupOverrideEJB.jar/resources/doaApp" + appNum);
            LookupOverrideTestApp.addAsModules(DoaAppEJB);
        }

        ShrinkHelper.exportDropinAppToServer(server, LookupOverrideTestApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {

// W CWNEN0047W: Resource annotations on the fields of the com.ibm.bnd.lookupoverride.shared.Bad6Bean class will be ignored. The annotations could not be obtained because of the exception : java.lang.NoClassDefFoundError: com.ibm.bnd.lookupoverride.hidden.MissingClass
// E CWNEN0054E: The Bad4Bean bean in the DoaApp4.jar module of the LookupOverrideApp application has conflicting configuration data in source code annotations. Conflicting beanName/lookup attribute values exist for multiple @EJB annotations with the same name attribute value : bad4combo. The conflicting beanName/lookup attribute values are fooBean and ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean.
// E CNTR4006E: The Bad4Bean enterprise bean in the DoaApp4.jar module of the LookupOverrideApp application failed to start. Exception: com.ibm.ejs.container.EJBConfigurationException: com.ibm.wsspi.injectionengine.InjectionConfigurationException: The Bad4Bean bean in the DoaApp4.jar module of the LookupOverrideApp application has conflicting configuration data in source code annotations. Conflicting beanName/lookup attribute values exist for multiple @EJB annotations with the same name attribute value : bad4combo. The conflicting beanName/lookup attribute values are fooBean and ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean.
// E CWNEN0054E: The Bad1Bean bean in the DoaApp1.jar module of the LookupOverrideApp application has conflicting configuration data in source code annotations. Conflicting beanName/lookup attribute values exist for multiple @EJB annotations with the same name attribute value : com.ibm.bnd.lookupoverride.doaApp1.ejb.Bad1Bean/ivTarget. The conflicting beanName/lookup attribute values are fooBean and ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean.
// E CNTR4006E: The Bad1Bean enterprise bean in the DoaApp1.jar module of the LookupOverrideApp application failed to start. Exception: com.ibm.ejs.container.EJBConfigurationException: com.ibm.wsspi.injectionengine.InjectionConfigurationException: The Bad1Bean bean in the DoaApp1.jar module of the LookupOverrideApp application has conflicting configuration data in source code annotations. Conflicting beanName/lookup attribute values exist for multiple @EJB annotations with the same name attribute value : com.ibm.bnd.lookupoverride.doaApp1.ejb.Bad1Bean/ivTarget. The conflicting beanName/lookup attribute values are fooBean and ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean.
// E CWNEN0052E: The Bad3Bean bean in the DoaApp3.jar module of the LookupOverrideApp application has conflicting configuration data in the XML deployment descriptor. Conflicting ejb-link/lookup-name element values exist for multiple ejb-local-ref elements with the same ejb-ref-name element value : T6. The conflicting ejb-link/lookup-name element values are FooBarBean and ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean.
// E CWNEN0009E: The injection engine failed to process the XML code from the deployment descriptor due to the following error: The Bad3Bean bean in the DoaApp3.jar module of the LookupOverrideApp application has conflicting configuration data in the XML deployment descriptor. Conflicting ejb-link/lookup-name element values exist for multiple ejb-local-ref elements with the same ejb-ref-name element value : T6. The conflicting ejb-link/lookup-name element values are "FooBarBean" and "ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean".
// E CNTR4006E: The Bad3Bean enterprise bean in the DoaApp3.jar module of the LookupOverrideApp application failed to start. Exception: com.ibm.ejs.container.EJBConfigurationException: com.ibm.wsspi.injectionengine.InjectionConfigurationException: The Bad3Bean bean in the DoaApp3.jar module of the LookupOverrideApp application has conflicting configuration data in the XML deployment descriptor. Conflicting ejb-link/lookup-name element values exist for multiple ejb-local-ref elements with the same ejb-ref-name element value : T6. The conflicting ejb-link/lookup-name element values are "FooBarBean" and "ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean".
// E CWNEN0052E: The Bad5Bean bean in the DoaApp5.jar module of the LookupOverrideApp application has conflicting configuration data in the XML deployment descriptor. Conflicting ejb-link/lookup-name element values exist for multiple ejb-local-ref elements with the same ejb-ref-name element value : bad5combo. The conflicting ejb-link/lookup-name element values are SLT and ejblocal:SLT2.
// E CWNEN0009E: The injection engine failed to process the XML code from the deployment descriptor due to the following error: The Bad5Bean bean in the DoaApp5.jar module of the LookupOverrideApp application has conflicting configuration data in the XML deployment descriptor. Conflicting ejb-link/lookup-name element values exist for multiple ejb-local-ref elements with the same ejb-ref-name element value : bad5combo. The conflicting ejb-link/lookup-name element values are "SLT" and "ejblocal:SLT2".
// E CNTR4006E: The Bad5Bean enterprise bean in the DoaApp5.jar module of the LookupOverrideApp application failed to start. Exception: com.ibm.ejs.container.EJBConfigurationException: com.ibm.wsspi.injectionengine.InjectionConfigurationException: The Bad5Bean bean in the DoaApp5.jar module of the LookupOverrideApp application has conflicting configuration data in the XML deployment descriptor. Conflicting ejb-link/lookup-name element values exist for multiple ejb-local-ref elements with the same ejb-ref-name element value : bad5combo. The conflicting ejb-link/lookup-name element values are "SLT" and "ejblocal:SLT2".
// E CNTR0154E: Another component is attempting to reference the SimpleLookupTargetBean2 enterprise bean in the LookupOverrideEJB.jar module.  This bean does not support an implementation of the com.ibm.bnd.lookupoverride.shared.TargetBean interface, which the other component is attempting to reference.
// E CNTR0019E: EJB threw an unexpected (non-declared) exception during invocation of method "boing". Exception data: javax.ejb.EJBException: The EJB reference in the Bad7Bean component in the DoaApp7.jar module of the LookupOverrideApp application could not be resolved; nested exception is: com.ibm.ejs.container.EJBConfigurationException: Another component is attempting to reference local interface: com.ibm.bnd.lookupoverride.shared.TargetBean which is not implemented by bean: LookupOverrideApp#LookupOverrideEJB.jar#SimpleLookupTargetBean2
// E CWNEN0054E: The Bad2Bean bean in the DoaApp2.jar module of the LookupOverrideApp application has conflicting configuration data in source code annotations. Conflicting beanName/lookup attribute values exist for multiple @EJB annotations with the same name attribute value : bad2combo. The conflicting beanName/lookup attribute values are fooBean and ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean.
// E CNTR4006E: The Bad2Bean enterprise bean in the DoaApp2.jar module of the LookupOverrideApp application failed to start. Exception: com.ibm.ejs.container.EJBConfigurationException: com.ibm.wsspi.injectionengine.InjectionConfigurationException: The Bad2Bean bean in the DoaApp2.jar module of the LookupOverrideApp application has conflicting configuration data in source code annotations. Conflicting beanName/lookup attribute values exist for multiple @EJB annotations with the same name attribute value : bad2combo. The conflicting beanName/lookup attribute values are fooBean and ejblocal:com.ibm.bnd.lookupoverride.shared.TargetBean.

            server.stopServer("CWNEN0047W:", "CWNEN0054E:", "CNTR4006E:", "CWNEN0054E:", "CNTR4006E:", "CWNEN0052E:", "CWNEN0009E:", "CNTR4006E:", "CWNEN0052E:", "CWNEN0009E:",
                              "CNTR4006E:", "CNTR0154E:", "CNTR0019E:", "CWNEN0054E:", "CNTR4006E:");
        }
    }

}
