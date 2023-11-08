/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package suite.r80.base.jca16.jbv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jca.fat.regr.util.HttpHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import junit.framework.AssertionFailedError;
import suite.r80.base.jca16.RarTests;
import suite.r80.base.jca16.TestSetupUtils;

/**
 *
 */
public abstract class RarBeanValidationTestCommon extends FATServletClient {

    private final static String CLASSNAME = "RarBeanValidationTest";
    private final static Logger logger = Logger.getLogger(CLASSNAME);
    private final static Class<?> c = RarBeanValidationTestCommon.class;
    protected static LibertyServer server;
    private String rarDisplayName = null;
    private String appName = null;
    protected static final String cfgFileExtn = "_server.xml";
    protected static ServerConfiguration originalServerConfig;

    private static final String JBV_WEB_WAR_NAME = "Jbvweb";
    private static final String JBV_WEB1_WAR_NAME = "jbv_web1";

    private static final String JBV_EJB1_NO_XMLS_JAR_NAME = "jbv_no_xmls_ejb1";
    private static final String JBV_EJB1_JAR_NAME = "jbv_ejb1";

    protected static void setupResources(LibertyServer server) throws Exception {
        TestSetupUtils.setUpFvtApp(server);

        //Package adapter_jca16_jbv_ResourceAdapterValidation_Success adapter_jca16_jbv_ResourceAdapterValidation_Success.rar
        JavaArchive resourceAdapterValidation_jar = ShrinkWrap.create(JavaArchive.class, "rarBeanResourceAdapterValidation.jar");
        resourceAdapterValidation_jar.addPackages(true, "com.ibm.ejs.ras");
        resourceAdapterValidation_jar.addClass("com.ibm.ws.csi.MessageEndpointTestResults");
        Filter<ArchivePath> packageFilterSuccess = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                boolean included = !currentPath.contains("com/ibm/adapter/jbv");

                //System.out.println("resourceAdapter_jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        resourceAdapterValidation_jar.addPackages(true, packageFilterSuccess, "com.ibm.adapter");

        JavaArchive resourceAdapterValidation_Success_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapterValidation_Success.jar");
        resourceAdapterValidation_Success_jar.addClass("com.ibm.adapter.jbv.JBVFATAdapterSuccessImpl");
        resourceAdapterValidation_Success_jar.addClass("com.ibm.adapter.jbv.JBVFATAdapterImpl");
        resourceAdapterValidation_Success_jar.addClass("com.ibm.adapter.jbv.JBVFATAdapter");

        ResourceAdapterArchive adapter_jca16_jbv_ResourceAdapterValidation_Success = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                       RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success
                                                                                                                                     + ".rar").addAsLibrary(resourceAdapterValidation_jar).addAsLibrary(resourceAdapterValidation_Success_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                              + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success
                                                                                                                                                                                                                                                                              + "-ra.xml"),
                                                                                                                                                                                                                                                                     "ra.xml").addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                              + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success
                                                                                                                                                                                                                                                                                                              + "-constraints.xml"),
                                                                                                                                                                                                                                                                                                     "constraints.xml").addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                                                                       + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success
                                                                                                                                                                                                                                                                                                                                                       + "-validation.xml"),
                                                                                                                                                                                                                                                                                                                                              "validation.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_jbv_ResourceAdapterValidation_Success);

        //Package adapter_jca16_jbv_ResourceAdapterValidation_Failure adapter_jca16_jbv_ResourceAdapterValidation_Failure.rar
        JavaArchive resourceAdapterValidation_Failure_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapterValidation_Failure.jar");
        resourceAdapterValidation_Failure_jar.addClass("com.ibm.adapter.jbv.JBVFATAdapterFailureImpl");
        resourceAdapterValidation_Failure_jar.addClass("com.ibm.adapter.jbv.JBVFATAdapterImpl");
        resourceAdapterValidation_Failure_jar.addClass("com.ibm.adapter.jbv.JBVFATAdapter");

        ResourceAdapterArchive adapter_jca16_jbv_ResourceAdapterValidation_Failure = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                       RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Failure
                                                                                                                                     + ".rar").addAsLibrary(resourceAdapterValidation_jar).addAsLibrary(resourceAdapterValidation_Failure_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                              + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Failure
                                                                                                                                                                                                                                                                              + "-ra.xml"),
                                                                                                                                                                                                                                                                     "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_jbv_ResourceAdapterValidation_Failure);

        //Package adapter_jca16_jbv_ResourceAdapterValidation_Embedded adapter_jca16_jbv_ResourceAdapterValidation_Embedded.rar
        JavaArchive resourceAdapterValidation_Embedded_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapterValidation_Embedded.jar");
        resourceAdapterValidation_Embedded_jar.addClass("com.ibm.adapter.jbv.JBVFATAdapterEmbeddedImpl");
        resourceAdapterValidation_Embedded_jar.addClass("com.ibm.adapter.jbv.JBVFATAdapterImpl");
        resourceAdapterValidation_Embedded_jar.addClass("com.ibm.adapter.jbv.JBVFATAdapter");

        ResourceAdapterArchive adapter_jca16_jbv_ResourceAdapterValidation_Embedded = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                        RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Embedded
                                                                                                                                      + ".rar").addAsLibrary(resourceAdapterValidation_jar).addAsLibrary(resourceAdapterValidation_Embedded_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Embedded
                                                                                                                                                                                                                                                                                + "-ra.xml"),
                                                                                                                                                                                                                                                                       "ra.xml");

        WebArchive jbvweb1_war = ShrinkWrap.create(WebArchive.class, JBV_WEB1_WAR_NAME + ".war");
        jbvweb1_war.addPackage("web");
        jbvweb1_war.addAsWebInfResource(new File("test-applications/" + JBV_WEB1_WAR_NAME + "/resources/WEB-INF/web.xml"));

        JavaArchive jbv_no_xmls_ejb1_jar = ShrinkWrap.create(JavaArchive.class, JBV_EJB1_NO_XMLS_JAR_NAME + ".jar");
        jbv_no_xmls_ejb1_jar.addPackage("ejb");

        EnterpriseArchive sampleapp_jca16_jbv_embeddedra_ear = ShrinkWrap.create(EnterpriseArchive.class, RarTests.sampleapp_jca16_jbv_embeddedraApp + ".ear");
        sampleapp_jca16_jbv_embeddedra_ear.addAsModules(jbvweb1_war, adapter_jca16_jbv_ResourceAdapterValidation_Embedded, jbv_no_xmls_ejb1_jar);

        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_embeddedra_ear);

        //Package adapter_jca16_jbv_AdministeredObjectValidation_Success adapter_jca16_jbv_AdministeredObjectValidation_Success.rar
        JavaArchive resourceAdapterAdminObject_jar = ShrinkWrap.create(JavaArchive.class, "rarBeanResourceAdapterAdminObject.jar");
        resourceAdapterAdminObject_jar.addPackages(true, "com.ibm.ejs.ras");
        resourceAdapterAdminObject_jar.addClass("com.ibm.ws.csi.MessageEndpointTestResults");
        Filter<ArchivePath> packageFilterAdminObject = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                boolean included = !currentPath.contains("com/ibm/adapter/adminobject/jbv");

                //System.out.println("resourceAdapter_jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        resourceAdapterAdminObject_jar.addPackages(true, packageFilterAdminObject, "com.ibm.adapter");

        JavaArchive administeredObjectValidation_Successs_jar = ShrinkWrap.create(JavaArchive.class, "AdministeredObjectValidation_Success.jar");
        administeredObjectValidation_Successs_jar.addClass("com.ibm.adapter.adminobject.jbv.JBVFATAOSuccessImpl");
        administeredObjectValidation_Successs_jar.addClass("com.ibm.adapter.adminobject.jbv.JBVFATAOImpl");
        administeredObjectValidation_Successs_jar.addClass("com.ibm.adapter.adminobject.jbv.JBVFATAO");

        ResourceAdapterArchive adapter_jca16_jbv_AdministeredObjectValidation_Success = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                          RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Success
                                                                                                                                        + ".rar").addAsLibrary(resourceAdapterAdminObject_jar).addAsLibrary(administeredObjectValidation_Successs_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                      + RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Success
                                                                                                                                                                                                                                                                                      + "-ra.xml"),
                                                                                                                                                                                                                                                                             "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_jbv_AdministeredObjectValidation_Success);

        //Package adapter_jca16_jbv_AdministeredObjectValidation_Failure adapter_jca16_jbv_AdministeredObjectValidation_Failure.rar
        JavaArchive resourceAdapterAdminObject_Failure_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapterAdminObject_Failure.jar");
        resourceAdapterAdminObject_Failure_jar.addClass("com.ibm.adapter.adminobject.jbv.JBVFATAOFailureImpl");
        resourceAdapterAdminObject_Failure_jar.addClass("com.ibm.adapter.adminobject.jbv.JBVFATAOImpl");
        resourceAdapterAdminObject_Failure_jar.addClass("com.ibm.adapter.adminobject.jbv.JBVFATAO");

        ResourceAdapterArchive adapter_jca16_jbv_AdministeredObjectValidation_Failure = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                          RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Failure
                                                                                                                                        + ".rar").addAsLibrary(resourceAdapterAdminObject_jar).addAsLibrary(resourceAdapterAdminObject_Failure_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                   + RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Failure
                                                                                                                                                                                                                                                                                   + "-ra.xml"),
                                                                                                                                                                                                                                                                          "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_jbv_AdministeredObjectValidation_Failure);

        //Package adapter_jca16_jbv_AdministeredObjectValidation_Embedded adapter_jca16_jbv_AdministeredObjectValidation_Embedded.rar
        JavaArchive resourceAdapterAdminObject_Embedded_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapterAdminObject_Embedded.jar");
        resourceAdapterAdminObject_Embedded_jar.addClass("com.ibm.adapter.adminobject.jbv.JBVFATAOEmbeddedImpl");
        resourceAdapterAdminObject_Embedded_jar.addClass("com.ibm.adapter.adminobject.jbv.JBVFATAOImpl");
        resourceAdapterAdminObject_Embedded_jar.addClass("com.ibm.adapter.adminobject.jbv.JBVFATAO");

        ResourceAdapterArchive adapter_jca16_jbv_AdministeredObjectValidation_Embedded = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                           RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Embedded
                                                                                                                                         + ".rar").addAsLibrary(resourceAdapterAdminObject_jar).addAsLibrary(resourceAdapterAdminObject_Embedded_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                     + RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Embedded
                                                                                                                                                                                                                                                                                     + "-ra.xml"),
                                                                                                                                                                                                                                                                            "ra.xml").addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                                     + RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Embedded
                                                                                                                                                                                                                                                                                                                     + "-constraints.xml"),
                                                                                                                                                                                                                                                                                                            "constraints.xml").addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                                                                              + RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Embedded
                                                                                                                                                                                                                                                                                                                                                              + "-validation.xml"),
                                                                                                                                                                                                                                                                                                                                                     "validation.xml");
        WebArchive jvbweb_war = ShrinkWrap.create(WebArchive.class, JBV_WEB_WAR_NAME + ".war");
        jvbweb_war.addPackage("web");
        jvbweb_war.addAsWebInfResource(new File("test-applications/" + JBV_WEB_WAR_NAME + "/resources/WEB-INF/web.xml"));

        EnterpriseArchive jvbapp_ear = ShrinkWrap.create(EnterpriseArchive.class, RarTests.Jbvapp + ".ear");
        jvbapp_ear.addAsModules(jvbweb_war);
        ShrinkHelper.addDirectory(jvbapp_ear, "lib/LibertyFATTestFiles/" + RarTests.Jbvapp);
        ShrinkHelper.exportToServer(server, "apps", jvbapp_ear);

        EnterpriseArchive sampleapp_jca16_jbv_embeddedao_ear = ShrinkWrap.create(EnterpriseArchive.class, RarTests.sampleapp_jca16_jbv_embeddedaoApp + ".ear");
        sampleapp_jca16_jbv_embeddedao_ear.addAsModules(jbvweb1_war, adapter_jca16_jbv_AdministeredObjectValidation_Embedded, jbv_no_xmls_ejb1_jar, jvbweb_war);

        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_embeddedao_ear);

        //Package adapter_jca16_jbv_ActivationSpecValidation_Success adapter_jca16_jbv_ActivationSpecValidation_Success.rar
        JavaArchive resourceAdapterActivSpecResourceAdapter_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapterActivSpecResourceAdapter.jar");
        resourceAdapterActivSpecResourceAdapter_jar.addPackages(true, "com.ibm.ejs.ras");
        resourceAdapterActivSpecResourceAdapter_jar.addClass("com.ibm.ws.csi.MessageEndpointTestResults");
        Filter<ArchivePath> packageFilterActiveSpect = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                boolean included = !currentPath.contains("com/ibm/adapter/activationspec/jbv");

                //System.out.println("resourceAdapter_jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        resourceAdapterActivSpecResourceAdapter_jar.addPackages(true, packageFilterActiveSpect, "com.ibm.adapter");

        JavaArchive activationSpecValidation_Success_jar = ShrinkWrap.create(JavaArchive.class, "ActivationSpecValidation_Success.jar");
        activationSpecValidation_Success_jar.addClass("com.ibm.adapter.activationspec.jbv.JBVFATActSpecSuccessImpl");
        activationSpecValidation_Success_jar.addClass("com.ibm.adapter.activationspec.jbv.JBVFATActSpecImpl");
        activationSpecValidation_Success_jar.addClass("com.ibm.adapter.activationspec.jbv.JBVFATActSpec");

        ResourceAdapterArchive adapter_jca16_jbv_ActivationSpecValidation_Success = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                      RarTests.adapter_jca16_jbv_ActivationSpecValidation_Success
                                                                                                                                    + ".rar").addAsLibrary(resourceAdapterActivSpecResourceAdapter_jar).addAsLibrary(activationSpecValidation_Success_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                          + RarTests.adapter_jca16_jbv_ActivationSpecValidation_Success
                                                                                                                                                                                                                                                                                          + "-ra.xml"),
                                                                                                                                                                                                                                                                                 "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_jbv_ActivationSpecValidation_Success);

        //Package adapter_jca16_jbv_ActivationSpecValidation_Failure adapter_jca16_jbv_ActivationSpecValidation_Failure.rar
        JavaArchive activationSpecValidation_Failure_jar = ShrinkWrap.create(JavaArchive.class, "ActivationSpecValidation_Failure.jar");
        activationSpecValidation_Failure_jar.addClass("com.ibm.adapter.activationspec.jbv.JBVFATActSpecFailureImpl");
        activationSpecValidation_Failure_jar.addClass("com.ibm.adapter.activationspec.jbv.JBVFATActSpecImpl");
        activationSpecValidation_Failure_jar.addClass("com.ibm.adapter.activationspec.jbv.JBVFATActSpec");

        ResourceAdapterArchive adapter_jca16_jbv_ActivationSpecValidation_Failure = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                      RarTests.adapter_jca16_jbv_ActivationSpecValidation_Failure
                                                                                                                                    + ".rar").addAsLibrary(resourceAdapterActivSpecResourceAdapter_jar).addAsLibrary(activationSpecValidation_Failure_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                          + RarTests.adapter_jca16_jbv_ActivationSpecValidation_Failure
                                                                                                                                                                                                                                                                                          + "-ra.xml"),
                                                                                                                                                                                                                                                                                 "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_jbv_ActivationSpecValidation_Failure);

        //Package adapter_jca16_jbv_ActivationSpecValidation_Embedded adapter_jca16_jbv_ActivationSpecValidation_Embedded.rar
        JavaArchive activationSpecValidation_Embedded_jar = ShrinkWrap.create(JavaArchive.class, "ActivationSpecValidation_Embedded.jar");
        activationSpecValidation_Embedded_jar.addClass("com.ibm.adapter.activationspec.jbv.JBVFATActSpecEmbeddedImpl");
        activationSpecValidation_Embedded_jar.addClass("com.ibm.adapter.activationspec.jbv.JBVFATActSpecImpl");
        activationSpecValidation_Embedded_jar.addClass("com.ibm.adapter.activationspec.jbv.JBVFATActSpec");

        ResourceAdapterArchive rar10 = ShrinkWrap.create(ResourceAdapterArchive.class, RarTests.adapter_jca16_jbv_ActivationSpecValidation_Embedded
                                                                                       + ".rar").addAsLibrary(activationSpecValidation_Embedded_jar).addAsLibrary(resourceAdapterActivSpecResourceAdapter_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                              + RarTests.adapter_jca16_jbv_ActivationSpecValidation_Embedded
                                                                                                                                                                                                                                              + "-ra.xml"),
                                                                                                                                                                                                                                     "ra.xml").addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                              + RarTests.adapter_jca16_jbv_ActivationSpecValidation_Embedded
                                                                                                                                                                                                                                                                              + "-constraints.xml"),
                                                                                                                                                                                                                                                                     "constraints.xml").addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                                       + RarTests.adapter_jca16_jbv_ActivationSpecValidation_Embedded
                                                                                                                                                                                                                                                                                                                       + "-validation.xml"),
                                                                                                                                                                                                                                                                                                              "validation.xml");
        JavaArchive jbv_no_override_ejb2_jar = ShrinkWrap.create(JavaArchive.class, "jbv_ejb2.jar");
        Filter<ArchivePath> packageFilterJbvNoOverrideEjb2 = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                boolean included = !currentPath.contains("OverrideMdb");

                //System.out.println("jbv_no_override_ejb2_jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        jbv_no_override_ejb2_jar.addPackages(true, packageFilterJbvNoOverrideEjb2, "ejb1");

        EnterpriseArchive sampleapp_jca16_jbv_embeddedas_ear = ShrinkWrap.create(EnterpriseArchive.class, RarTests.sampleapp_jca16_jbv_embeddedasApp + ".ear");
        sampleapp_jca16_jbv_embeddedas_ear.addAsModules(rar10, jbv_no_override_ejb2_jar, jvbweb_war);

        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_embeddedas_ear);

        EnterpriseArchive sampleapp_jca16_jbv_standaloneassuccess_ear = ShrinkWrap.create(EnterpriseArchive.class,
                                                                                          RarTests.sampleapp_jca16_jbv_standaloneassuccessApp + ".ear");
        sampleapp_jca16_jbv_standaloneassuccess_ear.addAsModules(jbv_no_override_ejb2_jar);

        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_standaloneassuccess_ear);

        //Package adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success.rar
        JavaArchive managedConnectionFactoryValidation_Success_jar = ShrinkWrap.create(JavaArchive.class, "ManagedConnectionFactoryValidation_Success.jar");
        managedConnectionFactoryValidation_Success_jar.addClass("com.ibm.adapter.spi.jbv.JBVFATMCFSuccessImpl");
        managedConnectionFactoryValidation_Success_jar.addClass("com.ibm.adapter.spi.jbv.JBVFATMCFImpl");
        managedConnectionFactoryValidation_Success_jar.addClass("com.ibm.adapter.spi.jbv.JBVFATMCF");

        JavaArchive resourceAdapter_jar = TestSetupUtils.getResourceAdapter_jar();

        ResourceAdapterArchive adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                RarTests.adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success
                                                                                                                                              + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(managedConnectionFactoryValidation_Success_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                      + RarTests.adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success
                                                                                                                                                                                                                                                                                      + "-ra.xml"),
                                                                                                                                                                                                                                                                             "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success);

        //Package adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure.rar
        JavaArchive managedConnectionFactoryValidation_Failure = ShrinkWrap.create(JavaArchive.class, "ManagedConnectionFactoryValidation_Failure.jar");
        managedConnectionFactoryValidation_Failure.addClass("com.ibm.adapter.spi.jbv.JBVFATMCFFailureImpl");
        managedConnectionFactoryValidation_Failure.addClass("com.ibm.adapter.spi.jbv.JBVFATMCFImpl");
        managedConnectionFactoryValidation_Failure.addClass("com.ibm.adapter.spi.jbv.JBVFATMCF");

        ResourceAdapterArchive adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                RarTests.adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure
                                                                                                                                              + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(managedConnectionFactoryValidation_Failure).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                  + RarTests.adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure
                                                                                                                                                                                                                                                                                  + "-ra.xml"),
                                                                                                                                                                                                                                                                         "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure);

        //Package adapter_jca16_jbv_ResourceAdapterValidation_Embedded adapter_jca16_jbv_ResourceAdapterValidation_Embedded.rar                                                                                                                                                                                                                      "ra.xml");
        EnterpriseArchive adapter_jca16_jbv_ResourceAdapterValidation_Embedded_ear = ShrinkWrap.create(EnterpriseArchive.class,
                                                                                                       RarTests.sampleapp_jca16_jbv_embeddedraApp + ".ear");
        adapter_jca16_jbv_ResourceAdapterValidation_Embedded_ear.addAsModules(jbvweb1_war, adapter_jca16_jbv_ResourceAdapterValidation_Embedded, jbv_no_xmls_ejb1_jar);

        ShrinkHelper.exportToServer(server, "apps", adapter_jca16_jbv_ResourceAdapterValidation_Embedded_ear);

        JavaArchive jbv_ejb1_jar = ShrinkWrap.create(JavaArchive.class, JBV_EJB1_JAR_NAME + ".jar");
        jbv_ejb1_jar.addPackage("ejb");
        jbv_ejb1_jar.addAsManifestResource(new File("test-applications/" + JBV_EJB1_JAR_NAME
                                                    + "/resources/META-INF/constraints.xml")).addAsManifestResource(new File("test-applications/" + JBV_EJB1_JAR_NAME
                                                                                                                             + "/resources/META-INF/validation.xml"));

        EnterpriseArchive sampleapp_jca16_jbv_embeddedra_ejbvalconfig_ear = ShrinkWrap.create(EnterpriseArchive.class,
                                                                                              RarTests.sampleapp_jca16_jbv_embeddedra_ejbvalconfigApp + ".ear");
        sampleapp_jca16_jbv_embeddedra_ejbvalconfig_ear.addAsModules(jbvweb1_war, adapter_jca16_jbv_ResourceAdapterValidation_Embedded, jbv_ejb1_jar);
        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_embeddedra_ejbvalconfig_ear);

        EnterpriseArchive sampleapp_jca16_jbv_embeddedravalconfig_ejb = ShrinkWrap.create(EnterpriseArchive.class,
                                                                                          RarTests.sampleapp_jca16_jbv_embeddedravalconfig_ejbApp + ".ear");
        sampleapp_jca16_jbv_embeddedravalconfig_ejb.addAsModules(jbvweb1_war, adapter_jca16_jbv_ResourceAdapterValidation_Success, jbv_no_xmls_ejb1_jar);
        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_embeddedravalconfig_ejb);

        EnterpriseArchive sampleapp_jca16_jbv_embeddedravalconfig_ejbvalconfig_ear = ShrinkWrap.create(EnterpriseArchive.class,
                                                                                                       RarTests.sampleapp_jca16_jbv_embeddedravalconfig_ejbvalconfigApp
                                                                                                                                + ".ear");
        sampleapp_jca16_jbv_embeddedravalconfig_ejbvalconfig_ear.addAsModules(jbvweb1_war, adapter_jca16_jbv_ResourceAdapterValidation_Success, jbv_ejb1_jar);
        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_embeddedravalconfig_ejbvalconfig_ear);

        JavaArchive jbv_ejb2_jar = ShrinkWrap.create(JavaArchive.class, "jbv_ejb2.jar");
        jbv_ejb2_jar.addPackages(true, "ejb1");

        EnterpriseArchive sampleapp_jca16_jbv_standaloneasfailure_ear = ShrinkWrap.create(EnterpriseArchive.class,
                                                                                          RarTests.sampleapp_jca16_jbv_standaloneasfailureApp + ".ear");
        sampleapp_jca16_jbv_standaloneasfailure_ear.addAsModules(jbv_ejb2_jar);
        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_standaloneasfailure_ear);

        EnterpriseArchive sampleapp_jca16_jbv_ejb_ear = ShrinkWrap.create(EnterpriseArchive.class,
                                                                          RarTests.sampleapp_jca16_jbv_ejbApp + ".ear");
        sampleapp_jca16_jbv_ejb_ear.addAsModules(jbvweb1_war, jbv_no_xmls_ejb1_jar);
        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_ejb_ear);

        EnterpriseArchive sampleapp_jca16_jbv_ejbvalconfig_ear = ShrinkWrap.create(EnterpriseArchive.class,
                                                                                   RarTests.sampleapp_jca16_jbv_ejbvalconfigApp + ".ear");
        sampleapp_jca16_jbv_ejbvalconfig_ear.addAsModules(jbvweb1_war, jbv_ejb1_jar);
        ShrinkHelper.exportToServer(server, "apps", sampleapp_jca16_jbv_ejbvalconfig_ear);

        // Application start may take longer than local run default
        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (30 * 1000)) {
            server.setAppStartTimeout(30 * 1000);
        }

        // Configuration update may take longer than local run default
        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (30 * 1000)) {
            server.setConfigUpdateTimeout(30 * 1000);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, "tearDown");

        if (server.isStarted()) {
            // Do not include message text because tests can run in a different locale
            server.stopServer("CWWKE0700W", // EXPECTED
                              "CWWKE0701E", // EXPECTED
                              "J2CA8802E: .*(adapter_jca16_jbv_ActivationSpecValidation_Failure|sampleapp_jca16_jbv_embeddedas.adapter_jca16_jbv_ActivationSpecValidation_Embedded).*javax.validation.ConstraintViolationException:", // EXPECTED
                              "CNTR4015W: .*(sampleapp_jca16_jbv_standaloneasfailure/jbv_ejb2/OverrideMdb|sampleapp_jca16_jbv_embeddedas/jbv_ejb2/SampleMdb|sampleapp_jca16_jbv_standaloneassuccess/jbv_ejb2/SampleMdb)", // EXPECTED in some cases where activateEndpoint Entry get delayed.
                              "J2CA0238E: .*(JBVFAT(Adapter|AO|ActSpec|MCF)(Failure|Embedded)Impl)" // EXPECTED
            );
        }
    }

    private StringBuilder runInServlet(String test, String servlet, String webmodule) throws IOException {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                          + "/" + webmodule + "/" + servlet + "?test=" + test);
        Log.info(getClass(), "runInServlet", "URL is " + url);
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(c, "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(c, "runInServlet", "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }
            return lines;
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    /**
     * cleanUpPerTest After running each test, restore to the original configuration.
     * 
     * @throws Exception
     */
    @After
    public void cleanUpPerTest() throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalServerConfig);

        if (appName != null) {
            //Make sure the application stops, otherwise we may try to start it again in the next test and get:
            //E CWWKZ0013E: It is not possible to start two applications called <appName>
            //We are assuming each test only uses one application.
            server.waitForConfigUpdateInLogUsingMark(null, "CWWKZ0009I.*" + appName);
            appName = null;
        } else {
            server.waitForConfigUpdateInLogUsingMark(null);
        }

        Log.info(getClass(), "cleanUpPerTest", "server configuration restored");
    }

    /**
     * Case 1: This test case will test for the successful startup of a stand-alone Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ResourceAdapterValidation_Success}. This resource adapter has config properties
     * that are annotated with the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations having values
     * that do not violate the constraints that these annotations declare.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJavaBeanValidationSuccessStandaloneRA() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(method + cfgFileExtn);

        rarDisplayName = RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success;
        assertNotNull("Resource adapter " + rarDisplayName + " was not installed.",
                      server.waitForStringInLogUsingMark("J2CA7001I"));
        assertNotNull("Resource adapter " + rarDisplayName + " was not started.",
                      server.waitForStringInLogUsingMark(rarDisplayName + " starts successfully"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    /**
     * Case 2: This test case will test for the failure of a Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ResourceAdapterValidation_Failure} to start. This RA has config properties that are
     * annotated with the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations with values that violate
     * the constraints that these annotations denote. The RAR startup should fail due to
     * a {@code ConstraintValidationException} and each validation constraint should appear in the exception stack
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    //@ExpectedFFDC("javax.validation.ConstraintViolationException")
    public void testJavaBeanValidationFailureStandaloneRA() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(method + cfgFileExtn);

        rarDisplayName = RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Failure;
        assertNotNull("The log does not contain constraint violation " + method,
                      server.waitForStringInLogUsingMark("J2CA0238E"));
        assertNotNull("The log does not contain constraint violation message for @Size(max=10) for the databaseName property " + method,
                      server.waitForStringInLogUsingMark("The maximum size is 10"));
        assertNull("Resource adapter " + rarDisplayName + " has started",
                   server.waitForStringInLogUsingMark(RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Embedded + " starts successfully", 5000));
        // since the above message has come 5 seconds is a good time to wait
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    /**
     * Case 3: This test case will test for the successful startup of an embedded Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ResourceAdapterValidation_Embedded}. This RA has config properties that are
     * annotated with the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations with values that do not
     * violate the constraints that these annotations denote. The startup of this RA is tested by checking for the
     * startup of the Enterprise App that contains it, namely {@code sampleapp_jca16_jbv_embeddedra}
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJavaBeanValidationSuccessEmbeddedRA() throws Throwable {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        appName = RarTests.sampleapp_jca16_jbv_embeddedraApp;
        server.setServerConfigurationFile(method + cfgFileExtn);
        Set<String> apps = new HashSet<String>();
        apps.add(appName);
        server.waitForConfigUpdateInLogUsingMark(apps);

        rarDisplayName = appName + "." + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Embedded;
        assertNotNull("Resource adapter " + rarDisplayName + " has not started",
                      server.waitForStringInLogUsingMark(RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Embedded + " starts successfully"));
        assertNotNull("Application " + appName + " has not started " + method,
                      server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName + ".*"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    /**
     * Case 4: This test case will test for the failure of an embedded Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ResourceAdapterValidation_Embedded} to start. This RA has config properties
     * that are annotated with the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations with values
     * that violate the constraints that these annotations denote. The RAR startup should fail due to
     * a {@code ConstraintValidationException} and each validation constraint should appear in the exception stack.
     * This should cause the EAR embedding the RAR also to fail startup.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    //@ExpectedFFDC("javax.validation.ConstraintViolationException")
    public void testJavaBeanValidationFailureEmbeddedRA() throws Throwable {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        appName = RarTests.sampleapp_jca16_jbv_embeddedraApp;
        rarDisplayName = appName + "." + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Embedded;
        server.setServerConfigurationFile(method + cfgFileExtn);
        Set<String> apps = new HashSet<String>();
        apps.add(appName);
        server.waitForConfigUpdateInLogUsingMark(apps);

        assertNotNull("The log does not contain constraint violation " + method,
                      server.waitForStringInLogUsingMark("J2CA0238E"));
        assertNotNull("The log does not contain constraint violation message for @NotNull for the mode property",
                      server.waitForStringInLogUsingMark(".*This property cannot be null.*"));
        assertNotNull("The log does not contain constraint violation message for @Max(value=100) for the idleTimeout property ",
                      server.waitForStringInLogUsingMark(".*The maximum value allowed is 100.*"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    /**
     * Case 5: This test case will test for the successful startup of a Stand-alone Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_AdministeredObjectValidation_Success}. This RA has an Administered Object (AO) with
     * config properties that are annotated with the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations
     * with values that do not violate the constraints that these annotations denote.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testJavaBeanValidationSuccessStandaloneAO() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        final String servletName = "RBVTestServlet";
        rarDisplayName = RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Success;
        appName = RarTests.Jbvapp;
        server.setServerConfigurationFile(method + cfgFileExtn);
        Set<String> apps = new HashSet<String>();
        apps.add(appName);
        server.waitForConfigUpdateInLogUsingMark(apps);
        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(rarDisplayName + " starts successfully"));
        runInServlet(method, servletName, "Jbvweb");
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);
    }

    /**
     * Case 6: This test case will test for the Constraint Validation on an Administered Object (AO). It uses a
     * Resource Adapter (RA) that has an AO defined with config properties that are annotated with
     * the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations with values that violate the
     * constraints that these annotations denote. The RAR will startup but the AO will not be bound into JNDI
     * and there will be {@code ConstraintViolationException} thrown. The constraints should appear in the
     * exception stack.
     */
    @Test
    @ExpectedFFDC("javax.validation.ConstraintViolationException")
    public void testJavaBeanValidationFailureStandaloneAO() throws Exception {
        final String testName = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, testName);

        final String servletName = "RBVTestServlet";
        server.setMarkToEndOfLog();
        Log.info(c, testName, "Executing " + testName);
        rarDisplayName = RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Failure;
        appName = RarTests.Jbvapp;
        server.setServerConfigurationFile(testName + cfgFileExtn);
        Set<String> apps = new HashSet<String>();
        apps.add(appName);
        server.waitForConfigUpdateInLogUsingMark(apps);
        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark("adapter_jca16_jbv_AdministeredObjectValidation_Failure starts successfully"));
        server.waitForStringInLog("CWWKZ0001I.*Jbvapp.*");
        runInServlet(testName, servletName, "Jbvweb");
        assertNotNull("The log does not contain constraint violation " + testName,
                      server.waitForStringInLogUsingMark("J2CA0238E"));
        assertNotNull("The log contains constraint violation message for @Min(value=10) for the aoProperty4 property " + testName,
                      server.waitForStringInLogUsingMark("The value should be greater than 10"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, testName);
    }

    /**
     * Case 7: This test case will test for the successful startup of an Embedded Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_AdministeredObjectValidation_Embedded}. This RA has an Administered
     * Object (AO) with config properties that are annotated with
     * the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations with values that do not
     * violate the constraints that these annotations denote. The startup of this RA is tested by checking
     * for the startup of the Enterprise App that contains it, namely {@code JavaBeanAOValidationEARApp}
     */
    @Test
    public void testJavaBeanValidationSuccessEmbeddedAO() throws Exception {
        final String testName = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, testName);

        server.setMarkToEndOfLog();
        appName = RarTests.sampleapp_jca16_jbv_embeddedaoApp;
        rarDisplayName = appName + "." + RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Embedded;
        final String servletName = "RBVTestServlet";
        server.setServerConfigurationFile(testName + cfgFileExtn);
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);
        appName = RarTests.sampleapp_jca16_jbv_embeddedaoApp;
        rarDisplayName = RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Embedded;

        assertNotNull("The embedded adapter " + rarDisplayName + " did not start.",
                      server.waitForStringInLogUsingMark("adapter_jca16_jbv_AdministeredObjectValidation_Embedded starts successfully"));
        assertNotNull("The sampleapp_jca16_jbv_embeddedaoApp did not start successfully " + testName,
                      server.waitForStringInLogUsingMark("CWWKZ000[13]I:.*" + appName + ".*"));
        runInServlet(testName, servletName, "Jbvweb");
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, testName);
    }

    /**
     * Case 8: This test case will test for the failure of an embedded Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_AdministeredObjectValidation_Embedded} to start. This RA has config
     * properties that are annotated with the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations
     * with values that violate the constraints that these annotations denote. The RAR startup should fail due to
     * a {@code ConstraintValidationException} and each validation constraint should appear in the exception stack.
     * This should cause the EAR embedding the RAR also to fail startup i.e. {@code JavaBeanAOValidationEARApp}.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    @ExpectedFFDC("javax.validation.ConstraintViolationException")
    public void testJavaBeanValidationFailureEmbeddedAO() throws Exception {
        final String testName = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, testName);

        server.setMarkToEndOfLog();
        appName = RarTests.sampleapp_jca16_jbv_embeddedaoApp;
        rarDisplayName = appName + "." + RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Embedded;
        final String servletName = "RBVTestServlet";
        server.setServerConfigurationFile(testName + cfgFileExtn);
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);
        assertNotNull("The embedded adapter " + rarDisplayName + " did not start.",
                      server.waitForStringInLogUsingMark(RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Embedded + " starts successfully"));
        assertNotNull("The sampleapp_jca16_jbv_embeddedao did not start successfully " + testName,
                      server.waitForStringInLogUsingMark("CWWKZ000[13]I"));
        runInServlet(testName, servletName, "Jbvweb");
        assertNotNull("The log does not contain constraint violation",
                      server.waitForStringInLogUsingMark("J2CA0238E"));
        assertNotNull("The log does not contain constraint violation message for @Size(min=5) for the aoProperty1 property",
                      server.waitForStringInLogUsingMark(".*The minimum value allowed is 5.*"));
        assertNotNull("The log does not contain constraint violation message for @Min(20) for the aoProperty5 property",
                      server.waitForStringInLogUsingMark(".*Minimum possible value is 20.*"));
        assertNotNull("The log does not contain constraint violation message for @NotNull for the aoProperty3 property",
                      server.waitForStringInLogUsingMark(".*This property cannot be null.*"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, testName);
    }

    /**
     * Case 9: This test case will test for the successful startup of a Stand-alone Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ActivationSpecValidation_Success} and the
     * application {@code sampleapp_jca16_jbv_standaloneassuccess}. This RA has an Activation Spec (AS)
     * with config properties that are annotated with the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations
     * with values that do not violate the constraints that these annotations denote. The
     * application {@code sampleapp_jca16_jbv_standaloneassuccess} contains an Mdb that is bound to this AS. During the
     * startup of this application the endpoint container initializes the endpoint and at this time the AS javabean is
     * validated by the Java Bean Validator and found valid resulting in successful application startup.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testJavaBeanValidationSuccessStandaloneAS() throws Exception {
        final String testName = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, testName);

        server.setMarkToEndOfLog();
        rarDisplayName = RarTests.adapter_jca16_jbv_ActivationSpecValidation_Success;
        appName = RarTests.sampleapp_jca16_jbv_standaloneassuccessApp;
        server.setServerConfigurationFile(testName + cfgFileExtn);
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(rarDisplayName + " starts successfully"));
        assertNotNull("The log does not contain message J2CA8801I",
                      server.waitForStringInLogUsingMark("J2CA8801I"));
        assertNotNull("The log does not contain message J2CA8801I",
                      server.waitForStringInLogUsingMark("J2CA8801I:.*sampleapp_jca16_jbv_standaloneassuccess/jbv_ejb2/SampleMdb.*"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, testName);
    }

    /**
     * Case 10: This test case will test for the successful startup of a Standalone Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ActivationSpecValidation_Failure} and the failure of the application
     * named {@code sampleapp_jca16_jbv_standaloneasfailure} to start. This RA has an Activation Spec (AS)
     * with config properties that are annotated with
     * the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations with values that violate
     * the constraints that these annotations denote. The application
     * named {@code sampleapp_jca16_jbv_standaloneassuccess} contains an Mdb that is bound to this AS.
     * During the startup of this application the endpoint container initializes the endpoint and at this
     * time the AS javabean is validated by the Java Bean Validator throwing
     * a {@code ConstraintViolationException} for each violation resulting in application startup failure
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    @ExpectedFFDC({ "javax.validation.ConstraintViolationException",
                    "javax.resource.ResourceException" })
    public void testJavaBeanValidationFailureStandaloneAS() throws Exception {
        final String testName = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, testName);

        server.setMarkToEndOfLog();
        rarDisplayName = RarTests.adapter_jca16_jbv_ActivationSpecValidation_Failure;
        appName = RarTests.sampleapp_jca16_jbv_standaloneasfailureApp;
        server.setServerConfigurationFile(testName + cfgFileExtn);
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(rarDisplayName + " starts successfully"));
        assertNotNull("The log does not contain message J2CA0238E",
                      server.waitForStringInLogUsingMark(RarTests.CONSTRAINT_VIOLATION_J2CA0238E));
        assertNotNull("The log does not contain constraint violation message for @Min(value=20) for the asProperty2 property",
                      server.waitForStringInLogUsingMark("should be > 20"));
        assertNotNull("The log does not contain constraint violation message for @Size(min=2,max=4) for the asProperty1 property",
                      server.waitForStringInLogUsingMark("Size should be between 2 and 4"));
        assertNotNull("The log does not contain constraint violation message for @NotNull for the asProperty3 property",
                      server.waitForStringInLogUsingMark("This field should not be null"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, testName);
    }

    /**
     * Case 11: This test case will test for the successful startup of an embedded Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ActivationSpecValidation_Embedded} and the successful startup of the application
     * named {@code sampleapp_jca16_jbv_embeddedas} that embeds this RA. This RA has an Activation Spec (AS) with
     * config properties that are annotated with
     * the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations with values that do not violate
     * the constraints that these annotations denote. The application
     * named {@code sampleapp_jca16_jbv_embeddedas} contains an Mdb that is bound to this AS. During the startup of
     * this application the endpoint container initializes the endpoint and at this time the AS javabean is validated
     * by the Java Bean Validator and found valid resulting in successful application startup.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testJavaBeanValidationSuccessEmbeddedAS() throws Exception {
        final String testName = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, testName);

        server.setMarkToEndOfLog();
        appName = RarTests.sampleapp_jca16_jbv_embeddedasApp;
        rarDisplayName = appName + "." + RarTests.adapter_jca16_jbv_ActivationSpecValidation_Embedded;
        server.setServerConfigurationFile(testName + cfgFileExtn);
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(RarTests.adapter_jca16_jbv_ActivationSpecValidation_Embedded + " starts successfully"));
        assertNotNull("The log does not contain message J2CA8801I", server.waitForStringInLogUsingMark("J2CA8801I"));
        assertNotNull("The log does not contain message J2CA8801I for the right activation spec",
                      server.waitForStringInLogUsingMark("J2CA8801I:.*sampleapp_jca16_jbv_embeddedas/jbv_ejb2/SampleMdb.*"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, testName);
    }

    /**
     * Case 12: This test case will test for the successful startup of an embedded Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ActivationSpecValidation_Embedded} and the failure of the application
     * named {@code sampleapp_jca16_jbv_embeddedas} to start. This RA has an Activation Spec (AS) with
     * config properties that are annotated with the {@code NotNull}, {@code Min}, {@code Max}, {@code Size} annotations
     * with values that violate the constraints that these annotations denote. The application
     * named {@code sampleapp_jca16_jbv_embeddedas} contains an Mdb that is bound to this AS. During the startup of this
     * application the endpoint container initializes the endpoint and at this time the AS javabean is validated by the
     * Java Bean Validator throwing a {@code ConstraintViolationException} for each violation resulting in application
     * startup failure
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    @ExpectedFFDC({ "javax.validation.ConstraintViolationException",
                    "javax.resource.ResourceException" })
    public void testJavaBeanValidationFailureEmbeddedAS() throws Exception {
        final String testName = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, testName);

        server.setMarkToEndOfLog();
        appName = RarTests.sampleapp_jca16_jbv_embeddedasApp;
        rarDisplayName = appName + "." + RarTests.adapter_jca16_jbv_ActivationSpecValidation_Embedded;
        server.setServerConfigurationFile(testName + cfgFileExtn);
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        assertNotNull("The log does not contain message J2CA0238E",
                      server.waitForStringInLogUsingMark(RarTests.CONSTRAINT_VIOLATION_J2CA0238E));
        assertNotNull("The log does not contain constraint violation message for @Min(value=20) for the asProperty2 property",
                      server.waitForStringInLogUsingMark("must be greater than or equal to 20"));
        assertNotNull("The log does not contain constraint violation message for @Size(min=2,max=4) for the asProperty1 property",
                      server.waitForStringInLogUsingMark("Size should be between 2 and 4"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, testName);
    }

    /**
     * Case 13: This test case will test for the successful startup of a stand-alone Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success} and the successful lookup of the
     * Managed Connection Factory (MCF) {@code TestMCFSuccess} from JNDI. The MCF JavaBean is initialized during
     * JNDI lookup successfully.
     * 
     * @throws Throwable
     */
    @Test
    public void testJavaBeanValidationSuccessStandaloneMCF() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        final String servletName = "RBVTestServlet";
        rarDisplayName = RarTests.adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success;
        server.setServerConfigurationFile(method + cfgFileExtn);
        appName = RarTests.Jbvapp;
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);
        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(rarDisplayName + " starts successfully"));
        runInServlet(method, servletName, "Jbvweb");
        // looking for the message printed by testJavaBeanValidationSuccessStandaloneMCF(HttpServletRequest request,
        // HttpServletResponse response) in RBVTestServlet class
        assertNotNull("The Managed Connection Factory with name TestMCFSuccess is not bound in JNDI",
                      server.waitForStringInLogUsingMark("09292014TE01 As expected Object is bound in JNDI for " + method));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    /**
     * Case 14: This test case will test for the successful startup of an Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure}. This RA will have a Managed
     * Connection Factory (MCF) configured with properties that have values violating the Java Bean constraints
     * specified on them. This test case also tests for the failure of the lookup of the MCF with
     * a {@code ConstraintViolationException} for the properties violating the constraints.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    @ExpectedFFDC("javax.validation.ConstraintViolationException")
    public void testJavaBeanValidationFailureStandaloneMCF() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        final String servletName = "RBVTestServlet";
        rarDisplayName = RarTests.adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure;
        server.setServerConfigurationFile(method + cfgFileExtn);
        appName = RarTests.Jbvapp;
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(rarDisplayName + " starts successfully"));
        runInServlet(method, servletName, "Jbvweb");
        // looking for the message printed by testJavaBeanValidationFailureStandaloneMCF(HttpServletRequest request,
        // HttpServletResponse response) in RBVTestServlet class
        assertNotNull("The Managed Connection Factory with name TestMCFFailure is bound in JNDI",
                      server.waitForStringInLogUsingMark("09292014TE02 As expected Object is not bound in JNDI for " + method));
        assertNotNull("The log contains message J2CA0238E",
                      server.waitForStringInLogUsingMark(RarTests.CONSTRAINT_VIOLATION_J2CA0238E));
        assertNotNull("The log contains constraint violation message for @Min(value=10) for the mcfProperty4 property",
                      server.waitForStringInLogUsingMark(".*The value should be greater than 10"));
        assertNotNull("The log contains constraint violation message for @Size(min=4) for the mcfProperty1 property",
                      server.waitForStringInLogUsingMark(".*The minimum value allowed is 4"));
        assertNotNull("The log contains constraint violation message for @Max(value=30) for the mcfProperty2 property",
                      server.waitForStringInLogUsingMark(".*must be less than or equal to 30"));
        // The below constraint is defined in xml.
        assertNotNull("The log contains constraint violation message for @NotNull for the mcfProperty5 property",
                      server.waitForStringInLogUsingMark(".*This config property cannot be null"));
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    //*********************** begin F743-28728 ***********************// 
    //****************************************************************//
    //************* Multiple validation descriptor tests *************//
    // 1) An EJB application containing an embedded JCA resource, where both
    //    an enterprise bean and a resource adapter bean requires bean validation.
    // 1.a) Test bean uses default (annotations) BV and RA uses validation.xml

    /**
     * Case 15: This test case will test for the successful startup of an Embedded Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ResourceAdapterValidation_Embedded}. This RA has ResourceAdapter JavaBean with
     * config properties decorated with built-in {@code NotNull}, {@code Min}, {@code Max}, {@code Size} constraint
     * annotations, where the property values do not violate constraints. The RA does not provide a validation
     * configuration. The startup of this RA is tested by checking for the startup of the Enterprise App that contains
     * it, named {@code sampleapp_jca16_jbv_embeddedra_ejbvalconfigApp}. The app contains an EJB module that provides a
     * validation configuration. The EJB injects a validator and exercises it when invoked.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testEmbeddedRar_EjbValConfig() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        appName = RarTests.sampleapp_jca16_jbv_embeddedra_ejbvalconfigApp;
        rarDisplayName = appName + "." + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Embedded;
        server.setServerConfigurationFile(method + cfgFileExtn);
        String contextRoot = "jbv_web1";
        int testNum = 1;
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);
        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Embedded + " starts successfully"));
        assertNotNull("Application " + appName + " has not started " + method,
                      server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName + ".*"));
        // Exercise an EJB that performs bean validation via a Servlet request 
        HttpURLConnection huConn = HttpHelper.getHttpURLConnection("SampleServlet", testNum, contextRoot, server);
        assertNotNull("Sample Servlet connected successfully", huConn);
        String[] results = HttpHelper.readFromHttpConnection(huConn);
        assertServletResult(results, "pass");
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    /**
     * A helper method to assert a single result returned from a servlet that is executed on behalf of this test
     * case in order to exercise a bean or servlet behavior in another application ear.
     * 
     * @param results    An array where in each element has the form: {@code results[i]=<"pass"|"fail">:<assert-condition-string>}
     * @param passOrFail The expected result, "pass" or "fail"
     */
    private void assertServletResult(String[] results, String passOrFail) {

        for (int i = 0; i < results.length; i++) {
            logger.log(Level.INFO, "results[i]=" + results[i]);
            if (results[i] != null && results[i] != "") {
                String[] result = results[i].split(":");
                if (result.length >= 2) // assert(condition, expected-result, actual-result);
                    assertEquals(result[1], passOrFail, result[0]);
            }
        }
    }

    // 1) An EJB application containing an embedded JCA resource, where both an enterprise bean 
    //    and a resource adapter bean requires bean validation.
    // 1.b) Test bean uses validation.xml file and RA uses default(annotations) BV
    /**
     * Case 16: This test case will verify the successful startup of an Embedded Resource Adapter(RA)
     * named {@code adapter_jca16_jbv_ResourceAdapterValidation_Success}. This RA has a ResourceAdapter JavaBean with
     * config properties decorated with built-in {@code NotNull}, {@code Min}, {@code Max}, {@code Size} constraint
     * annotations, where the property values do not violate constraints. The RA provides a validation configuration.
     * The startup of the RA is verified by checking for the startup of the application that contains it,
     * name {@code sampleapp_jca16_jbv_embeddedravalconfig_ejbApp}. The application contains an EJB module that
     * does not provide a validation configuration. The EJB injects a validator and exercises it when invoked.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testEmbeddedRarValConfig_Ejb() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        appName = RarTests.sampleapp_jca16_jbv_embeddedravalconfig_ejbApp;
        rarDisplayName = appName + "." + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success;
        server.setServerConfigurationFile(method + cfgFileExtn);
        String contextRoot = "jbv_web1";
        int testNum = 1;
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);
        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success + " starts successfully"));
        assertNotNull("Application " + appName + " has not started " + method,
                      server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName + ".*"));
        // Exercise an EJB that performs bean validation via a Servlet request 
        HttpURLConnection huConn = HttpHelper.getHttpURLConnection("SampleServlet", testNum, contextRoot, server);
        assertNotNull("Sample Servlet connected successfully", huConn);
        String[] results = HttpHelper.readFromHttpConnection(huConn);
        assertServletResult(results, "pass");
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    // 1) An EJB application containing an embedded JCA resource, where both an enterprise 
    //    bean and a resource adapter bean requires bean validation.
    // 1.c) Test bean and RA both use different validation.xml files 
    /**
     * Case 17: This test case will verify the successful startup of an Embedded Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ResourceAdapterValidation_Success}. This RA has ResourceAdapter JavaBean with
     * config properties decorated with built-in {@code NotNull}, {@code Min}, {@code Max}, {@code Size} constraint
     * annotations, where the property values do not violate constraints. The RA provides a validation configuration.
     * The startup of the RA is verified by checking for the startup of the app that contains it,
     * named {@code sampleapp_jca16_jbv_embeddedravalconfig_ejbvalconfigApp}. The app contains an EJB module
     * that also provides a validation configuration. The EJB injects a validator and exercises it when invoked.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testEmbeddedRarValConfig_EjbValConfig() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(method + cfgFileExtn);
        appName = RarTests.sampleapp_jca16_jbv_embeddedravalconfig_ejbvalconfigApp;
        rarDisplayName = appName + "." + RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success;
        String contextRoot = "jbv_web1";
        int testNum = 1;
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success + " starts successfully"));
        assertNotNull("Application " + appName + " has not started " + method,
                      server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName + ".*"));
        // Exercise an EJB that performs bean validation via a servlet request 
        HttpURLConnection huConn = HttpHelper.getHttpURLConnection("SampleServlet", testNum, contextRoot, server);
        assertNotNull("Sample Servlet connected successfully", huConn);
        String[] results = HttpHelper.readFromHttpConnection(huConn);
        assertServletResult(results, "pass");
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    // 2) An EJB application and a stand-alone JCA resource, where both an enterprise 
    //    bean and a resource adapter bean requires bean validation.
    // 2.a) Test bean uses default(annotations) BV and RA uses validation.xml
    /**
     * Case 18: This test case will verify the successful startup of an stand-alone Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ResourceAdapterValidation_Success}. This RA has a ResourceAdapter JavaBean with
     * config properties decorated with built-in {@code NotNull}, {@code Min}, {@code Max}, {@code Size} constraint
     * annotations, where the property values do not violate constraints when the RA starts. The RA provides a validation
     * configuration. The test also verifies that a test application starts {@code sampleapp_jca16_jbv_ejbApp},
     * and that an EJB successfully injects a validator and exercises it. The EJB module does not provide a
     * validation configuration.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testStandaloneRarValConfig_Ejb() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(method + cfgFileExtn);
        appName = RarTests.sampleapp_jca16_jbv_ejbApp;
        rarDisplayName = RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success;
        String contextRoot = "jbv_web1";
        int testNum = 1;
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success + " starts successfully"));
        assertNotNull("Application " + appName + " has not started " + method,
                      server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName + ".*"));
        // Exercise an EJB that performs bean validation via a servlet request 
        HttpURLConnection huConn = HttpHelper.getHttpURLConnection("SampleServlet", testNum, contextRoot, server);
        assertNotNull("Sample Servlet connected successfully", huConn);
        String[] results = HttpHelper.readFromHttpConnection(huConn);
        assertServletResult(results, "pass");
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    // 2) An EJB application and a stand-alone JCA resource, where both an enterprise 
    //    bean and a resource adapter bean requires bean validation.
    // 2.b) Test bean uses validation.xml file and RA uses default(annotations) BV
    /**
     * Case 19: This test case will verify the successful startup of an stand-alone Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_AdministeredObjectValidation_Success}. This RA has an AdministeredObject JavaBean
     * with config properties decorated with built-in {@code NotNull}, {@code Min}, {@code Max}, {@code Size} constraint
     * annotations, where the property values do not violate constraints during RA startup. The RA does not provide a
     * validation configuration. The test also verifies that a test application starts {@code sampleapp_jca16_jbv_ejbvalconfigApp},
     * and that an EJB successfully injects a validator and exercises it. The EJB module provides a validation configuration.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testStandaloneRar_EjbValConfig() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(method + cfgFileExtn);
        appName = RarTests.sampleapp_jca16_jbv_ejbvalconfigApp;
        rarDisplayName = RarTests.adapter_jca16_jbv_AdministeredObjectValidation_Success;
        String contextRoot = "jbv_web1";
        int testNum = 1;
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started", server.waitForStringInLogUsingMark(rarDisplayName + " starts successfully"));
        assertNotNull("Application " + appName + " has not started " + method,
                      server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName + ".*"));
        // Exercise an EJB that performs bean validation via a servlet request 
        HttpURLConnection huConn = HttpHelper.getHttpURLConnection("SampleServlet", testNum, contextRoot, server);
        assertNotNull("Sample Servlet connected successfully", huConn);
        String[] results = HttpHelper.readFromHttpConnection(huConn);
        assertServletResult(results, "pass");
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    // 2) An EJB application and a stand-alone JCA resource, where both an enterprise 
    //    bean and a resource adapter bean requires bean validation.
    // 2.c) Test bean and RA both use different validation.xml files
    /**
     * Case 20: This test case will verify the successful startup of an stand-alone Resource Adapter (RA)
     * named {@code adapter_jca16_jbv_ResourceAdapterValidation_Success}. This RA has a ResourceAdapter JavaBean with
     * config properties decorated with built-in {@code NotNull}, {@code Min}, {@code Max}, {@code Size} constraint
     * annotations, where the property values do not violate constraints when the RA starts. The RA provides a validation
     * configuration. The test also verifies that a test application starts {@code sampleapp_jca16_jbv_ejbvalconfigApp},
     * and that an EJB successfully injects a validator and exercises it. The EJB module provides a validation configuration.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testStandaloneRarValConfig_EjbValConfig() throws Exception {
        final String method = getTestMethodSimpleName();
        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, method);

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(method + cfgFileExtn);
        appName = RarTests.sampleapp_jca16_jbv_ejbvalconfigApp;
        rarDisplayName = RarTests.adapter_jca16_jbv_ResourceAdapterValidation_Success;
        Set<String> appNames = new HashSet<String>();
        appNames.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNames);
        String contextRoot = "jbv_web1";
        int testNum = 1;
        assertNotNull("The Resource adapter with name " + rarDisplayName + " is not started",
                      server.waitForStringInLogUsingMark(rarDisplayName + " starts successfully"));
        assertNotNull("Application " + appName + " has not started " + method,
                      server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + appName + ".*"));
        // Exercise an EJB that performs bean validation via a servlet request 
        HttpURLConnection huConn = HttpHelper.getHttpURLConnection("SampleServlet", testNum, contextRoot, server);
        assertNotNull("Sample Servlet connected successfully", huConn);
        String[] results = HttpHelper.readFromHttpConnection(huConn);
        assertServletResult(results, "pass");
        if (logger.isLoggable(Level.FINER))
            logger.exiting(CLASSNAME, method);
    }

    // Test cases 21 22 and 23 are not required as there is no isolated rar concept in liberty. So these are redundant.
}
