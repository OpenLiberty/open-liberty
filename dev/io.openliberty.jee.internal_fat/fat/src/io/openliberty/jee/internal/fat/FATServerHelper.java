/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jee.internal.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

public class FATServerHelper {
    public static final String DROPINS_DIR = "dropins";
    public static final String APPS_DIR = "apps";

    public static final boolean DO_ADD_RESOURCES = true;
    public static final boolean DO_NOT_ADD_RESOURCES = false;

    /**
     * Package a WAR and add it to a server.
     *
     * The rules of {@link #addToServer} are followed, using a null
     * EAR name and a null JAR name.
     *
     * @param targetServer    The server to which to add the EAR or WAR.
     * @param targetDir       The directory of the server in which to place
     *                            the EAR or WAR.
     * @param warName         The name of the WAR which is to be created and added.
     * @param warPackageNames The names of packages to be placed in the WAR.
     * @param addWarResources Control parameter: Tells if resources are to
     *                            be added for the WAR.
     *
     * @throws Exception Thrown if any of the steps fails.
     */
    public static boolean addWarToServer(
                                         LibertyServer targetServer, String targetDir,
                                         String warName, String[] warPackageNames, boolean addWarResources) throws Exception {

        String earName = null;
        boolean addEarResources = DO_NOT_ADD_RESOURCES;

        String jarName = null;
        boolean addJarResources = DO_NOT_ADD_RESOURCES;
        String[] jarPackageNames = null;

        return addToServer(
                           targetServer, targetDir,
                           earName, addEarResources,
                           warName, warPackageNames, addWarResources,
                           jarName, jarPackageNames, addJarResources);
    }

    /**
     * Package a WAR with a JAR and add the WAR to a server.
     *
     * The rules of {@link #addToServer} are followed, using a null
     * EAR name.
     *
     * @param targetServer    The server to which to add the EAR or WAR.
     * @param targetDir       The directory of the server in which to place
     *                            the EAR or WAR.
     * @param warName         The name of the WAR which is to be created and added.
     * @param warPackageNames The names of packages to be placed in the WAR.
     * @param addWarResources Control parameter: Tells if resources are to
     *                            be added for the WAR.
     *
     * @param jarName         The name of the JAR which is to be created and added.
     * @param jarPackageNames The names of packages to be placed in the JAR.
     *                            EAR, WAR, and JAR.
     * @param addJarResources Control parameter: Tells if resources are to
     *                            be added for the JAR.
     *
     * @throws Exception Thrown if any of the steps fails.
     */
    public static boolean addWarToServer(
                                         LibertyServer targetServer, String targetDir,
                                         String warName, String[] warPackageNames, boolean addWarResources,
                                         String jarName, String[] jarPackageNames, boolean addJarResources) throws Exception {

        String earName = null;
        boolean addEarResources = DO_NOT_ADD_RESOURCES;

        return addToServer(
                           targetServer, targetDir,
                           earName, addEarResources,
                           warName, warPackageNames, addWarResources,
                           jarName, jarPackageNames, addJarResources);
    }

    /**
     * Conditionally, package a JAR, WAR, and EAR and add them to a
     * server.
     *
     * Most often, the target directory is one of the standard server
     * folders for applications, {@link #DROPINS_DIR} or {@link #APPS_DIR}.
     *
     * A WAR is always created. The WAR name parameter cannot be null.
     *
     * A JAR and an EAR are not always created.
     *
     * When the EAR name is null, the EAR is not created, and the the
     * WAR is added directly to the specified directory. When the EAR
     * name is not-null, an EAR is created and added to the specified
     * directory. The WAR is added to the server indirectly by adding
     * the WAR to the EAR.
     *
     * When the JAR name is null no JAR is created. When the JAR name
     * is not null, the JAR is created and is added to the server
     * indirectly as a fragment JAR of the WAR. The JAR is not added
     * directly to the server.
     *
     * Resources are added for each of the EAR, WAR, and JAR which are
     * created according to the corresponding 'addResources' parameter.
     * The source resources must be located in the "test-applications"
     * folder under the folder for the named archive as "resources".
     * For example,
     *
     * <code>"test-applications/" + warName + "/resources"</code>
     *
     * The package names are the fully qualified names of the packages
     * which are to be placed in the WAR and JAR which are created.
     *
     * @param targetServer    The server to which to add the EAR or WAR.
     * @param targetDir       The directory of the server in which to place
     *                            the EAR or WAR.
     *
     * @param earName         The name of the EAR which is to be created and added.
     * @param addEarResources Control parameter: Tells if resources are to
     *                            be added for the EAR.
     *
     * @param warName         The name of the WAR which is to be created and added.
     * @param warPackageNames The names of packages to be placed in the WAR.
     * @param addWarResources Control parameter: Tells if resources are to
     *                            be added for the WAR.
     *
     * @param jarName         The name of the JAR which is to be created and added.
     * @param jarPackageNames The names of packages to be placed in the JAR.
     *                            EAR, WAR, and JAR.
     * @param addJarResources Control parameter: Tells if resources are to
     *                            be added for the JAR.
     *
     * @throws Exception Thrown if any of the steps fails.
     */
    public static boolean addToServer(
                                      LibertyServer targetServer, String targetDir,
                                      String earName, boolean addEarResources,
                                      String warName, String[] warPackageNames, boolean addWarResources,
                                      String jarName, String[] jarPackageNames, boolean addJarResources) throws Exception {

        if (warName == null) {
            throw new IllegalArgumentException("A war name must be specified.");
        }

        if (targetServer.isStarted()) {
            String appName = warName.substring(0, warName.indexOf(".war"));
            if (!targetServer.getInstalledAppNames(appName).isEmpty()) {
                return false;
            }
        }

        JavaArchive jar;

        if (jarName == null) {
            jar = null;
        } else {
            jar = createJar(jarName, jarPackageNames, addJarResources); // throws Exception
        }

        WebArchive war = createWar(warName, warPackageNames, addWarResources, jar); // throws Exception

        EnterpriseArchive ear;

        if (earName == null) {
            ear = null;
        } else {
            ear = createEar(earName, addEarResources, war); // throws Exception
        }

        if (ear != null) {
            ShrinkHelper.exportToServer(targetServer, targetDir, ear); // throws Exception
        } else {
            ShrinkHelper.exportToServer(targetServer, targetDir, war); // throws Exception
        }

        return true;
    }

    public static JavaArchive createJar(
                                        String jarName,
                                        String[] jarPackageNames,
                                        boolean addJarResources) throws Exception {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, jarName);

        if (jarPackageNames != null) {
            for (String packageName : jarPackageNames) {
                jar.addPackage(packageName);
            }
        }

        if (addJarResources) {
            ShrinkHelper.addDirectory(jar, "test-applications/" + jarName + "/resources");
        }

        return jar;
    }

    public static WebArchive createWar(
                                       String warName, String[] warPackageNames, boolean addWarResources,
                                       JavaArchive jar) throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, warName);

        if (warPackageNames != null) {
            for (String packageName : warPackageNames) {
                war.addPackage(packageName);
            }
        }

        if (addWarResources) {
            ShrinkHelper.addDirectory(war, "test-applications/" + warName + "/resources");
        }

        if (jar != null) {
            war.addAsLibrary(jar);
        }

        return war;
    }

    public static EnterpriseArchive createEar(
                                              String earName, boolean addEarResources,
                                              WebArchive war) throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName);

        if (addEarResources) {
            ShrinkHelper.addDirectory(ear, "test-applications/" + earName + "/resources");
        }

        ear.addAsModule(war);

        return ear;
    }

    public static void installUserFeature(LibertyServer server) throws Exception {
        server.installUserFeature("io.extension.jeeTestFeature-1.0");
        server.installUserFeature("io.extension.jeeTestFeature.internal-10.0");
        server.installUserFeature("io.extension.jeeTestFeature.internal-11.0");
        server.installUserFeature("io.extension.jeeTestFeature.internal-6.0");
        server.installUserFeature("io.extension.jeeTestFeature.internal-7.0");
        server.installUserFeature("io.extension.jeeTestFeature.internal-8.0");
        server.installUserFeature("io.extension.jeeTestFeature.internal-9.1");
    }

    public static void uninstallUserFeature(LibertyServer server) throws Exception {
        server.uninstallUserFeature("io.extension.jeeTestFeature-1.0");
        server.uninstallUserFeature("io.extension.jeeTestFeature.internal-10.0");
        server.uninstallUserFeature("io.extension.jeeTestFeature.internal-11.0");
        server.uninstallUserFeature("io.extension.jeeTestFeature.internal-6.0");
        server.uninstallUserFeature("io.extension.jeeTestFeature.internal-7.0");
        server.uninstallUserFeature("io.extension.jeeTestFeature.internal-8.0");
        server.uninstallUserFeature("io.extension.jeeTestFeature.internal-9.1");
    }
}
