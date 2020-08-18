/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat_helper;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class WCApplicationHelper {

    private static final Logger LOG = Logger.getLogger(WCApplicationHelper.class.getName());
    private static final String DIR_APPS = "apps";
    private static final String DIR_DROPINS = "dropins";
    private static final String DIR_PUBLISH = "publish/servers/";

    /*
     * Helper method to create a war and add it to the dropins directory
     */
    public static void addWarToServerDropins(LibertyServer server, String warName, boolean addWarResources,
                                             String... packageNames) throws Exception {
        addEarToServer(server, DIR_DROPINS, null, false, warName, addWarResources, null, false, packageNames);
    }

    /*
     * Helper method to create a war and add it to the apps directory
     */
    public static void addWarToServerApps(LibertyServer server, String warName, boolean addWarResources, String jarName, boolean addJarResources,
                                          String... packageNames) throws Exception {
        addEarToServer(server, DIR_APPS, null, false, warName, addWarResources, jarName, addJarResources, packageNames);
    }

    /*
     * Helper method to create a war and add it to the apps directory
     */
    public static void addWarToServerApps(LibertyServer server, String warName, boolean addWarResources, String warResourceLocation, String jarName, boolean addJarResources,
                                          String... packageNames) throws Exception {
        addEarToServer(server, DIR_APPS, null, false, warName, addWarResources, warResourceLocation, jarName, addJarResources, packageNames);
    }

    /*
     * Helper method to create an ear and add it to the dropins directory
     */
    public static void addEarToServerDropins(LibertyServer server, String earName, boolean addEarResources,
                                             String warName, boolean addWarResources, String jarName, boolean addJarResources, String... packageNames) throws Exception {
        addEarToServer(server, DIR_DROPINS, earName, addEarResources, warName, addWarResources, jarName, addJarResources,
                       packageNames);
    }

    /*
     * Helper method to create an ear and add it to the apps directory
     */
    public static void addEarToServerApps(LibertyServer server, String earName, boolean addEarResources, String warName,
                                          boolean addWarResources, String jarName, boolean addJarResources, String... packageNames) throws Exception {
        addEarToServer(server, DIR_APPS, earName, addEarResources, warName, addWarResources, jarName, addJarResources,
                       packageNames);
    }

    /*
     * Helper method to create a war and placed it to the specified directory which is relative from /publish/servers/ directory.
     */
    public static void createWar(LibertyServer server, String dir, String warName, boolean addWarResources, String jarName, boolean addJarResources,
                                 String... packageNames) throws Exception {
        addEarToServer(server, dir, null, false, warName, addWarResources, jarName, addJarResources, packageNames);
    }

    /*
     * Helper method to create a ear and placed it to the specified directory which is relative from /publish/servers/ directory.
     * this method supports adding jar file.
     */
    public static void createJar(LibertyServer server, String dir, String jarName, boolean addJarResources, String... packageNames) throws Exception {
        String baseDir = DIR_PUBLISH + server.getServerName() + "/" + dir + "/";
        JavaArchive jar = null;
        if (jarName != null) {
            LOG.info("createJar : create jar " + jarName + ", jar includes resources : " + addJarResources);
            jar = ShrinkWrap.create(JavaArchive.class, jarName);
            if (packageNames != null) {
                for (String packageName : packageNames) {
                    if (packageName.contains(".jar.")) {
                        jar.addPackage(packageName);
                    }
                }
            }
            if (addJarResources)
                ShrinkHelper.addDirectory(jar, "test-applications/" + jarName + "/resources");
        }
        ShrinkHelper.exportArtifact(jar, DIR_PUBLISH + server.getServerName() + "/" + dir, true, true);
    }

    /*
     * Helper method to create a jar and placed it to the specified directory which is relative from /publish/servers/ directory.
     * this method supports adding jar file.
     */
    public static void createJarAllPackages(LibertyServer server, String dir, String jarName, boolean addJarResources, String... packageNames) throws Exception {
        String baseDir = DIR_PUBLISH + server.getServerName() + "/" + dir + "/";
        JavaArchive jar = null;
        if (jarName != null) {
            LOG.info("createJar : create jar " + jarName + ", jar includes resources : " + addJarResources);
            jar = ShrinkWrap.create(JavaArchive.class, jarName);
            if (packageNames != null) {
                for (String packageName : packageNames) {
                    jar.addPackage(packageName);
                }
            }
            if (addJarResources)
                ShrinkHelper.addDirectory(jar, "test-applications/" + jarName + "/resources");
        }
        ShrinkHelper.exportArtifact(jar, DIR_PUBLISH + server.getServerName() + "/" + dir, true, true);
    }

    /*
     * Helper method to create a ear and placed it to the specified directory which is relative from /publish/servers/ directory.
     */
    public static void packageWarsToEar(LibertyServer server, String dir, String earName, boolean addEarResources, String... warFiles) throws Exception {
        String baseDir = DIR_PUBLISH + server.getServerName() + "/" + dir + "/";
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName);
        if (addEarResources) {

            ShrinkHelper.addDirectory(ear, "test-applications/" + earName + "/resources");

        }
        for (String warFile : warFiles) {
            WebArchive war = ShrinkWrap.createFromZipFile(WebArchive.class, new File(baseDir + warFile));
            ear.addAsModule(war);
        }
        ShrinkHelper.exportArtifact(ear, DIR_PUBLISH + server.getServerName() + "/" + dir, true, true);
    }

    public static EnterpriseArchive createEar(LibertyServer server, String dir, String earName, boolean addEarResources) throws Exception {
        String baseDir = DIR_PUBLISH + server.getServerName() + "/" + dir + "/";
        LOG.info("createEar: dir : " + dir + ", earName : " + earName + ", includes resources : " + addEarResources);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName);
        if (addEarResources) {
            ShrinkHelper.addDirectory(ear, "test-applications/" + earName + "/resources");
        }
        return ear;
    }

    public static void exportEar(LibertyServer server, String dir, EnterpriseArchive ear) throws Exception {
        ShrinkHelper.exportArtifact(ear, DIR_PUBLISH + server.getServerName() + "/" + dir, true, true);
    }

    /*
     */
    public static EnterpriseArchive packageWars(LibertyServer server, String dir, EnterpriseArchive ear, String... warFiles) throws Exception {
        String baseDir = DIR_PUBLISH + server.getServerName() + "/" + dir + "/";
        for (String warFile : warFiles) {
            WebArchive war = ShrinkWrap.createFromZipFile(WebArchive.class, new File(baseDir + warFile));
            ear.addAsModule(war);
        }
        return ear;
    }

    /*
     */
    public static EnterpriseArchive packageJars(LibertyServer server, String dir, EnterpriseArchive ear, String... jarFiles) throws Exception {
        String baseDir = DIR_PUBLISH + server.getServerName() + "/" + dir + "/";
        for (String jarFile : jarFiles) {
            JavaArchive jar = ShrinkWrap.createFromZipFile(JavaArchive.class, new File(baseDir + jarFile));
            ear.addAsLibrary(jar);
        }
        return ear;
    }

    /*
     * Helper method to create a ear and placed it to the specified directory which is relative from /publish/servers/ directory.
     */
    public static void addEarToServerApps(LibertyServer server, String dir, String earName) throws Exception {
        server.copyFileToLibertyServerRoot(DIR_PUBLISH + server.getServerName() + "/" + dir, DIR_APPS, earName);
    }

    /*
     * Method to create jars, wars and ears for testing. Resources are created
     * as needed and added to dropins or apps directories as specified.
     * if dir is other than dropins or apps, do not deploy the app.
     */
    private static void addEarToServer(LibertyServer server, String dir, String earName, boolean addEarResources,
                                       String warName, boolean addWarResources, String jarName, boolean addJarResources, String... packageNames) throws Exception {

        addEarToServer(server, dir, earName, addEarResources, warName, addWarResources, null, jarName, addJarResources, packageNames);
    }

    private static void addEarToServer(LibertyServer server, String dir, String earName, boolean addEarResources,
                                       String warName, boolean addWarResources, String warResourceLocation, String jarName, boolean addJarResources,
                                       String... packageNames) throws Exception {

        if (warName == null)
            return;
        if (addWarResources && warResourceLocation == null) {
            warResourceLocation = warName;
        }
        // If server is already started and app exists no need to add it.
        if (server.isStarted()) {
            String appName = warName.substring(0, warName.indexOf(".war"));
            Set<String> appInstalled = server.getInstalledAppNames(appName);
            LOG.info("addEarToServer : " + appName + " already installed : " + !appInstalled.isEmpty());

            if (!appInstalled.isEmpty())
                return;
        }

        JavaArchive jar = null;
        WebArchive war = null;

        if (jarName != null) {

            LOG.info("addEarToServer : create jar " + jarName + ", jar includes resources : " + addJarResources);

            jar = ShrinkWrap.create(JavaArchive.class, jarName);
            if (packageNames != null) {
                for (String packageName : packageNames) {
                    if (packageName.contains(".jar.")) {
                        jar.addPackage(packageName);
                    }
                }
            }
            if (addJarResources)
                ShrinkHelper.addDirectory(jar, "test-applications/" + jarName + "/resources");
        }

        war = ShrinkWrap.create(WebArchive.class, warName);
        LOG.info("addEarToServer : create war " + warName + ", war includes resources : " + addWarResources);
        if (packageNames != null) {
            for (String packageName : packageNames) {
                if (packageName.contains(".war.")) {
                    war.addPackage(packageName);
                }
            }
        }
        if (jar != null)
            war.addAsLibrary(jar);
        if (addWarResources)
            ShrinkHelper.addDirectory(war, "test-applications/" + warResourceLocation + "/resources");

        boolean deploy = false;
        if (dir.equals(DIR_APPS) || dir.equals(DIR_DROPINS)) {
            deploy = true;
        }

        if (earName != null) {
            LOG.info("addEarToServer : create ear " + earName + ", ear include application/.xml : " + addEarResources);
            EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName);
            ear.addAsModule(war);
            if (addEarResources) {
                ShrinkHelper.addDirectory(ear, "test-applications/" + earName + "/resources");
            }
            if (deploy) {
                // delete
                deleteFileIfExist("publish/servers/" + server.getServerName() + "/" + dir + "/" + ear.getName());
                ShrinkHelper.exportToServer(server, dir, ear);
            } else {
                ShrinkHelper.exportArtifact(ear, DIR_PUBLISH + server.getServerName() + "/" + dir, true, true);
            }
        } else {
            if (deploy) {
                deleteFileIfExist("publish/servers/" + server.getServerName() + "/" + dir + "/" + war.getName());
                ShrinkHelper.exportToServer(server, dir, war);
            } else {
                ShrinkHelper.exportArtifact(war, DIR_PUBLISH + server.getServerName() + "/" + dir, true, true);
            }
        }
    }

    private static void deleteFileIfExist(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            LOG.info("deleteFileIfExist: " + filename + " already exists. It's deleted before re-creating it.");
            file.delete();
        }
    }

}
