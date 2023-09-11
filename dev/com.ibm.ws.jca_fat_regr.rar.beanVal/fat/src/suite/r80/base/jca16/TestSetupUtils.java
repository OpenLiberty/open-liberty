/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

    private static EnterpriseArchive fvtapp_ear;
    private static JavaArchive resourceAdapter_jar;

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

        ShrinkHelper.exportAppToServer(server, fvtapp_ear);
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

}
