/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc;

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

    /*
     * Helper method to create a war and add it to the dropins directory
     */
    public static void addWarToServerDropins(LibertyServer server, String warName, boolean addWarResources,
                                             String... packageNames) throws Exception {
        addEarToServer(server, "dropins", null, false, warName, addWarResources, null, false, packageNames);
    }

    /*
     * Helper method to create a war and add it to the apps directory
     */
    public static void addWarToServerApps(LibertyServer server, String warName, boolean addWarResources,
                                          String... packageNames) throws Exception {
        addEarToServer(server, "apps", null, false, warName, addWarResources, null, false, packageNames);
    }

    /*
     * Helper method to create an ear and add it to the dropins directory
     */
    public static void addEarToServerDropins(LibertyServer server, String earName, boolean addEarResources,
                                             String warName, boolean addWarResources, String jarName, boolean addJarResources, String... packageNames) throws Exception {
        addEarToServer(server, "dropins", earName, addEarResources, warName, addWarResources, jarName, addJarResources,
                       packageNames);
    }

    /*
     * Helper method to create an ear and add it to the apps directory
     */
    public static void addEarToServerApps(LibertyServer server, String earName, boolean addEarResources, String warName,
                                          boolean addWarResources, String jarName, boolean addJarResources, String... packageNames) throws Exception {
        addEarToServer(server, "apps", earName, addEarResources, warName, addWarResources, jarName, addJarResources,
                       packageNames);
    }

    /*
     * Method to create jars, wars and ears for testing. Resources are created
     * as needed and added to dropins or apps directories as specified.
     */
    private static void addEarToServer(LibertyServer server, String dir, String earName, boolean addEarResources,
                                       String warName, boolean addWarResources, String jarName, boolean addJarResources, String... packageNames) throws Exception {

        if (warName == null)
            return;

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
            ShrinkHelper.addDirectory(war, "test-applications/" + warName + "/resources");

        if (earName != null) {
            LOG.info("addEarToServer : create ear " + earName + ", ear include application/.xml : " + addEarResources);
            EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName);
            ear.addAsModule(war);
            if (addEarResources)
                ear.addAsManifestResource(
                                          new File("test-applications/" + earName + "/resources/META-INF/application.xml"));

            //Liberty does not use was.policy but permissions.xml
            File permissionsXML = new File("test-applications/" + earName + "/resources/META-INF/permissions.xml");
            if (permissionsXML.exists()) {
                ear.addAsManifestResource(permissionsXML);
            }

            ShrinkHelper.exportToServer(server, dir, ear);
        } else {
            ShrinkHelper.exportToServer(server, dir, war);
        }

    }
}
