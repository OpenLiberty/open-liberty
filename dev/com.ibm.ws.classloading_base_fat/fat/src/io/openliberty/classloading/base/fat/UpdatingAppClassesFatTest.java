/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.classloading.base.fat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for updating app class binaries.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class UpdatingAppClassesFatTest extends AbstractUpdatingAppClassesFatTest {
    private final static Class<?> CLASS = UpdatingAppClassesFatTest.class;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Log.info(CLASS, "beforeClass", "entry");

        EnterpriseArchive ear = buildUpdateableApp();

        // looseConfigServer: deploy EAR exploded into loose/ dirs, use loose XML as app descriptor
        looseConfigServer = LibertyServerFactory.getLibertyServer("classloader_updateable", null, true);
        looseConfigServer.setServerConfigurationFile("Updateable/server.xml.fileMonitorTrigger");
        deployLoose(looseConfigServer, ear);
        looseConfigServer.addInstalledAppForValidation("updateableApp");
        looseConfigServer.startServer();

        // expandedAppServer: deploy EAR fully exploded on disk so updateFile() can rename into it
        expandedAppServer = LibertyServerFactory.getLibertyServer("classloader_updateable_expandedApp", null, true);
        expandedAppServer.setServerConfigurationFile("Updateable/server.xml.fileMonitorTrigger2");
        deployExpanded(expandedAppServer, ear);
        expandedAppServer.addInstalledAppForValidation("updateableApp");
        expandedAppServer.startServer();
        Log.info(CLASS, "beforeClass", "exit");
    }

    /**
     * Deploys the EAR as an exploded loose config application.
     * Exports each module exploded into the corresponding loose/ directory named without the archive
     * extension (e.g. loose/updateableAppEJB/) and copies the loose XML descriptor into apps/.
     *
     * exportArtifact with expand=true calls exportExploded(parentDir, archiveName), which produces
     * a directory named after the archive inside parentDir.  We export to loose/ and then rename the
     * resulting directory (e.g. loose/updateableAppEJB.jar -> loose/updateableAppEJB) so that the
     * paths match exactly what updateableApp.ear.xml expects.
     */
    static void deployLoose(LibertyServer server, EnterpriseArchive ear) throws Exception {
        String root = server.getServerRoot();
        java.io.File looseDir = new java.io.File(root + "/loose");
        looseDir.mkdirs();

        // Export EJB jar exploded — creates loose/updateableAppEJB.jar/, rename to loose/updateableAppEJB/
        JavaArchive ejbJar = ear.getAsType(JavaArchive.class, "/updateableAppEJB.jar");
        ShrinkHelper.exportArtifact(ejbJar, root + "/loose", true, true, true);
        Files.move(Paths.get(root + "/loose/updateableAppEJB.jar"),
                   Paths.get(root + "/loose/updateableAppEJB"),
                   StandardCopyOption.REPLACE_EXISTING);

        // Export WAR exploded — creates loose/updateableAppWeb.war/, rename to loose/updateableAppWeb/
        WebArchive webWar = ear.getAsType(WebArchive.class, "/updateableAppWeb.war");
        ShrinkHelper.exportArtifact(webWar, root + "/loose", true, true, true);
        Files.move(Paths.get(root + "/loose/updateableAppWeb.war"),
                   Paths.get(root + "/loose/updateableAppWeb"),
                   StandardCopyOption.REPLACE_EXISTING);

        // Export lib jar exploded — creates loose/updateableAppLib.jar/, rename to loose/updateableAppLib/
        JavaArchive libJar = ear.getAsType(JavaArchive.class, "/lib/updateableAppLib.jar");
        if (libJar != null) {
            ShrinkHelper.exportArtifact(libJar, root + "/loose", true, true, true);
            Files.move(Paths.get(root + "/loose/updateableAppLib.jar"),
                       Paths.get(root + "/loose/updateableAppLib"),
                       StandardCopyOption.REPLACE_EXISTING);
        }

        // Copy the loose XML descriptor to apps/ so Liberty uses it as the app definition
        server.copyFileToLibertyServerRoot("apps", "Updateable/updateableApp.ear.xml");
    }

    /**
     * Deploys the EAR fully exploded on disk so that individual class/resource files can be
     * swapped in place by updateFile().  The structure Liberty expects (and the tests write to) is:
     *   apps/updateableApp.ear/
     *     updateableAppEJB.jar/         <- EJB jar exploded
     *       com/ibm/test/updateable/ejb/
     *     updateableAppWeb.war/         <- WAR exploded
     *       WEB-INF/classes/com/ibm/test/updateable/web/
     *       WEB-INF/  (minor.xyz, major.txt)
     *       updateable.jsp
     *     lib/
     *       updateableAppLib.jar/       <- lib jar exploded
     */
    static void deployExpanded(LibertyServer server, EnterpriseArchive ear) throws Exception {
        String root = server.getServerRoot();
        String earDir = root + "/apps/updateableApp.ear";
        new java.io.File(earDir).mkdirs();

        // Explode EJB jar -> apps/updateableApp.ear/updateableAppEJB.jar/
        JavaArchive ejbJar = ear.getAsType(JavaArchive.class, "/updateableAppEJB.jar");
        ShrinkHelper.exportArtifact(ejbJar, earDir, true, true, true);
        // exportArtifact with expand=true creates earDir/updateableAppEJB.jar/ directly

        // Explode WAR -> apps/updateableApp.ear/updateableAppWeb.war/
        WebArchive webWar = ear.getAsType(WebArchive.class, "/updateableAppWeb.war");
        ShrinkHelper.exportArtifact(webWar, earDir, true, true, true);

        // Explode lib jar -> apps/updateableApp.ear/lib/updateableAppLib.jar/
        JavaArchive libJar = ear.getAsType(JavaArchive.class, "/lib/updateableAppLib.jar");
        if (libJar != null) {
            new java.io.File(earDir + "/lib").mkdirs();
            ShrinkHelper.exportArtifact(libJar, earDir + "/lib", true, true, true);
        }
    }

    static EnterpriseArchive buildUpdateableApp() throws Exception {
        JavaArchive ejbJar = ShrinkHelper.buildJavaArchive("updateableAppEJB.jar",
            "com.ibm.test.updateable.ejb");
        JavaArchive libJar = ShrinkHelper.buildJavaArchive("updateableAppLib.jar",
            "com.ibm.test.updateable.util");
        WebArchive webWar = ShrinkHelper.buildDefaultApp("updateableAppWeb.war",
            "com.ibm.test.updateable.web");
        ShrinkHelper.addDirectory(webWar, "test-applications/updateableAppWeb.war/resources");

        return ShrinkWrap.create(EnterpriseArchive.class, "updateableApp.ear")
            .addAsModule(webWar)
            .addAsModule(ejbJar)
            .addAsLibrary(libJar);
    }

    /*
     * (non-Javadoc)
     *
     * @see io.openliberty.classloading.base.fat.AbstractUpdatingAppClassesFatTest#updateFile(java.lang.String, java.lang.String)
     */
    @Override
    protected void updateFile(LibertyServer server, String dest, String src) throws Exception {
        server.copyFileToLibertyServerRoot(dest, src);
    }

}
