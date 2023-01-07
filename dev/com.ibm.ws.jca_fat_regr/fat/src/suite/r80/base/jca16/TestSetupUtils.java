/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package suite.r80.base.jca16;

import java.io.File;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

/**
 * Class <CODE>TestSetupUtils</CODE> provides utility methods used
 * to help set up resources needed for tests
 */
public class TestSetupUtils {

    public static final String raDir = "test-resourceadapters/adapter.regr/resources/META-INF/";

    private static EnterpriseArchive gwcapp_ear;
    private static EnterpriseArchive fvtapp_ear;
    private static EnterpriseArchive annApp_ear;
    private static JavaArchive resourceAdapter_jar;
    private static JavaArchive traAnnresourceAdapter_jar;
    private static JavaArchive resourceAdapterEjs_jar;

    public static void setUpGwcApp(LibertyServer server) throws Exception {
        String war_name = "gwcweb";
        String app_name = "gwcapp";

        if (gwcapp_ear == null) {
            JavaArchive resourceAdapter_jar = ShrinkWrap.create(JavaArchive.class, "gwctResourceAdapter.jar");
            resourceAdapter_jar.addPackages(true, "com.ibm.adapter");
            resourceAdapter_jar.addPackages(true, "com.ibm.ejs.ras");
            resourceAdapter_jar.addPackages(true, "com.ibm.ws.test");
            resourceAdapter_jar.addClass("com.ibm.ws.csi.MessageEndpointTestResults");

            // Build the web module and application
            WebArchive fvtweb_war = ShrinkWrap.create(WebArchive.class, war_name + ".war");
            fvtweb_war.addPackage("web");
            fvtweb_war.addAsWebInfResource(new File("test-applications/" + war_name + "/resources/WEB-INF/web.xml"));

            ResourceAdapterArchive rar1 = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                            "adapter_jca16_gwc_GenericWorkContextTestRAR.rar").addAsLibrary(resourceAdapter_jar).addAsManifestResource(new File("test-resourceadapters/adapter.regr/resources/META-INF/adapter_jca16_gwc_GenericWorkContextTestRAR-ra.xml"),
                                                                                                                                                                       "ra.xml");

            gwcapp_ear = ShrinkWrap.create(EnterpriseArchive.class, app_name + ".ear");
            gwcapp_ear.addAsModules(fvtweb_war, rar1);
            ShrinkHelper.addDirectory(gwcapp_ear, "lib/LibertyFATTestFiles/" + app_name);
        }
        ShrinkHelper.exportToServer(server, "apps", gwcapp_ear);
        server.addInstalledAppForValidation(app_name);
    }

    public static void setUpFvtApp(LibertyServer server) throws Exception {
        String war_name = "fvtweb";
        String app_name = "fvtapp";

        if (fvtapp_ear == null) {
            JavaArchive resourceAdapter_jar = TestSetupUtils.getResourceAdapter_jar();

            JavaArchive annotatedInboundSecurity_jar = ShrinkWrap.create(JavaArchive.class, "AnnotatedInboundSecurity.jar");
            annotatedInboundSecurity_jar.addPackages(true, "com.ibm.inout.adapter");

            ResourceAdapterArchive adapter_jca16_insec_AnnotatedInboundSecurity = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                    RarTests.adapter_jca16_insec_AnnotatedInboundSecurity
                                                                                                                                  + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(annotatedInboundSecurity_jar).addAsManifestResource(new File(raDir
                                                                                                                                                                                                                                                        + RarTests.adapter_jca16_insec_AnnotatedInboundSecurity
                                                                                                                                                                                                                                                        + "-ra.xml"),
                                                                                                                                                                                                                                               "ra.xml");

            // Build the web module and application
            WebArchive fvtweb_war = ShrinkWrap.create(WebArchive.class, war_name + ".war");
            fvtweb_war.addPackage("web");
            fvtweb_war.addPackage("ejb.inboundsec");
            fvtweb_war.addPackage("web.inboundsec");
            fvtweb_war.addAsWebInfResource(new File("test-applications/" + war_name + "/resources/WEB-INF/web.xml"));

            fvtapp_ear = ShrinkWrap.create(EnterpriseArchive.class, app_name + ".ear");
            fvtapp_ear.addAsModules(fvtweb_war, adapter_jca16_insec_AnnotatedInboundSecurity);
            ShrinkHelper.addDirectory(fvtapp_ear, "lib/LibertyFATTestFiles/" + app_name);
        }

        ShrinkHelper.exportToServer(server, "apps", fvtapp_ear);
    }

    public static JavaArchive getResourceAdapter_jar() {
        if (resourceAdapter_jar == null) {
            resourceAdapter_jar = ShrinkWrap.create(JavaArchive.class, "rarBeanResourceAdapter.jar");
            resourceAdapter_jar.addPackages(true, "com.ibm.ejs.ras");
            resourceAdapter_jar.addPackages(true, "com.ibm.ws.csi");
            Filter<ArchivePath> packageFilter = new Filter<ArchivePath>() {
                @Override
                public boolean include(final ArchivePath object) {
                    final String currentPath = object.get();

                    boolean included = !currentPath.contains("com/ibm/adapter/spi/jbv");

                    //System.out.println("resourceAdapter_jar included: " + included + " packageFilter object name: " + currentPath);
                    return included;

                }
            };
            resourceAdapter_jar.addPackages(true, packageFilter, "com.ibm.adapter");
        }

        return resourceAdapter_jar;
    }

    public static JavaArchive getTraAnnResourceAdapter_jar() {
        if (traAnnresourceAdapter_jar == null) {
            traAnnresourceAdapter_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapter.jar");
            Filter<ArchivePath> packageFilter = new Filter<ArchivePath>() {
                @Override
                public boolean include(final ArchivePath object) {
                    final String currentPath = object.get();

                    boolean included = !currentPath.contains("com/ibm/tra/ann");

                    //System.out.println("ResourceAdapter included: " + included + " packageFilter object name: " + currentPath);
                    return included;

                }
            };
            traAnnresourceAdapter_jar.addPackages(true, packageFilter, "com.ibm.tra");
        }

        return traAnnresourceAdapter_jar;
    }

    public static JavaArchive getTraAnnEjsResourceAdapter_jar() {
        if (resourceAdapterEjs_jar == null) {
            resourceAdapterEjs_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapter.jar");
            resourceAdapterEjs_jar.addPackages(true, "com.ibm.ejs.ras");
            Filter<ArchivePath> packageFilter = new Filter<ArchivePath>() {
                @Override
                public boolean include(final ArchivePath object) {
                    final String currentPath = object.get();

                    boolean included = !currentPath.contains("com/ibm/tra/ann");

                    //System.out.println("ResourceAdapterEjs included: " + included + " packageFilter object name: " + currentPath);
                    return included;

                }
            };
            resourceAdapterEjs_jar.addPackages(true, packageFilter, "com.ibm.tra");
        }

        return resourceAdapterEjs_jar;
    }

    public static void setupAnnapp(LibertyServer server) throws Exception {
        String war_name = "annweb";
        String app_name = "annapp";
        if (annApp_ear == null) {
            // Build the web module and application
            WebArchive fvtweb_war = ShrinkWrap.create(WebArchive.class, war_name + ".war");
            fvtweb_war.addPackage("web");
            fvtweb_war.addAsWebInfResource(new File("test-applications/" + war_name + "/resources/WEB-INF/web.xml"));

            annApp_ear = ShrinkWrap.create(EnterpriseArchive.class, app_name + ".ear");
            annApp_ear.addAsModules(fvtweb_war);
            ShrinkHelper.addDirectory(annApp_ear, "lib/LibertyFATTestFiles/" + app_name);
        }
        ShrinkHelper.exportToServer(server, "apps", annApp_ear);
        server.addInstalledAppForValidation(app_name);
    }

}
