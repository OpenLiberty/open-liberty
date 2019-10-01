/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tests.anno.util;

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
public class AppPackagingHelper {
    private static final Logger LOG = Logger.getLogger(AppPackagingHelper.class.getName());

    //

    public static void addEarToServerDropins(LibertyServer server, Ear ear, String... packageNames)
        throws Exception {
        addEarToServer(server, "dropins", ear);
    }

    public static void addEarToServerApps(LibertyServer server, Ear ear) throws Exception {
        addEarToServer(server, "apps", ear);
    }

    private static void addEarToServer(LibertyServer server, String dir, Ear ear) throws Exception {
        String methodName = "addEarToServer";

        if ( ear == null ) {
            LOG.info(methodName + ": ENTER / RETURN: No EAR specified"); 
            return;
        }

        String earName = ear.getName();
        LOG.info(methodName + ": ENTER: EAR: " + earName);

        if ( ear.getWars().isEmpty() ) {
            LOG.info(methodName + ": RETURN: EAR has no WARs");
            return;
        }

        LOG.info(methodName + ": Server: " + server.getServerName());

        if ( server.isStarted() ) {
            LOG.info(methodName + ": Server is started; scanning for absent WARs");

            boolean allWarsInstalled = true;
            for ( War war : ear.getWars() ) {
                String warName = war.getName();
                String warSimpleName = warName.substring(0, warName.indexOf(".war"));
                Set<String> warInstalled = server.getInstalledAppNames(warSimpleName);
                boolean isWarInstalled = !warInstalled.isEmpty();
                LOG.info(methodName + ": " + warSimpleName + " installed: " + isWarInstalled);
                if ( !isWarInstalled ) {
                    allWarsInstalled = false;
                }
            }

            if ( allWarsInstalled ) {
                LOG.info(methodName + ": RETURN: All wars are present");
                return;
            }

        } else {
            LOG.info(methodName + ": Server is not started");
        }

        LOG.info(methodName + ": Create ear: " + earName);
        EnterpriseArchive earArchive =
            ShrinkWrap.create(EnterpriseArchive.class, earName);

        for ( War war : ear.getWars() ) {
            String warName = war.getName();

            LOG.info(methodName + ": Create war: " + warName);
            WebArchive warArchive = ShrinkWrap.create(WebArchive.class, warName);

            for ( String packageName : war.getPackageNames() ) {
                if ( packageName.contains(".war.") ) {
                    LOG.info(methodName + ": Add package to war: " + packageName);
                    warArchive.addPackage(packageName);
                }
            }

            for ( Jar jar : war.getJars() ) {
                String jarName = jar.getName();

                LOG.info(methodName + ": Create jar: " + jarName);
                JavaArchive jarArchive = ShrinkWrap.create(JavaArchive.class, jarName);

                for ( String packageName : jar.getPackageNames() ) {
                    if ( packageName.contains(".jar.") ) {
                        LOG.info(methodName + ": Add package to jar: " + packageName);
                        jarArchive.addPackage(packageName);
                    }
                }

                LOG.info(methodName + ": Add jar resources: " + jarName);
                String jarResourcesDir = "test-applications/" + jarName + "/resources";
                ShrinkHelper.addDirectory(jarArchive, jarResourcesDir);

                LOG.info(methodName + ": Add jar: " + jarName);
                warArchive.addAsLibrary(jarArchive);
            }

            LOG.info(methodName + ": Add WAR resources: " + warName);
            String warResourcesDir = "test-applications/" + warName + "/resources";
            ShrinkHelper.addDirectory(warArchive, warResourcesDir);

            LOG.info(methodName + ": Add war: " + warName);
            earArchive.addAsModule(warArchive);
        }
    
        LOG.info(methodName + ": Add EAR descriptor: " + earName);
        String applicationResource = "test-applications/" + earName + "/resources/META-INF/application.xml";
        earArchive.addAsManifestResource(new File(applicationResource));

        File permissionsFile = new File("test-applications/" + earName + "/resources/META-INF/permissions.xml");
        if ( permissionsFile.exists() ) {
            LOG.info(methodName + ": Add EAR permissions: " + earName);
            earArchive.addAsManifestResource(permissionsFile);
        }

        LOG.info(methodName + ": Export EAR to server: " + earName);
        ShrinkHelper.exportToServer(server, dir, earArchive, ShrinkHelper.DeployOptions.OVERWRITE);

        LOG.info(methodName + ": RETURN");
    }

    //
    
    public static void addWarToServerDropins(
        LibertyServer server,
        String warName, boolean addWarResources,
        String... packageNames) throws Exception {

        addEarToServer(
            server, "dropins",
            null, false, // No EAR
            warName, addWarResources,
            null, false, // No JAR
            packageNames);
    }

    public static void addWarToServerApps(
        LibertyServer server,
        String warName, boolean addWarResources,
        String... packageNames) throws Exception {

        addEarToServer(
            server, "apps",
            null, false, // No EAR
            warName, addWarResources,
            null, false, // No JAR
            packageNames);
    }

    public static void addEarToServerDropins(
        LibertyServer server,
        String earName, boolean addEarResources,
        String warName, boolean addWarResources,
        String jarName, boolean addJarResources,
        String... packageNames) throws Exception {

        addEarToServer(
            server, "dropins",
            earName, addEarResources,
            warName, addWarResources,
            jarName, addJarResources,
            packageNames);
    }

    public static void addEarToServerApps(
        LibertyServer server,
        String earName, boolean addEarResources,
        String warName, boolean addWarResources,
        String jarName, boolean addJarResources,
        String... packageNames) throws Exception {

        addEarToServer(
            server, "apps",
            earName, addEarResources,
            warName, addWarResources,
            jarName, addJarResources,
            packageNames);
    }

    //

    /**
     * Main application packaging helper.  Add a web module application to
     * a specified server.
     * 
     * Add a web module to a server, either directly as a WAR file,
     * or packaged into an EAR file.
     *
     * Conditionally, add a JAR as a web module library JAR to the WAR file.
     *
     * Conditionally, add classes from the current project into the application.
     * Packages which target the JAR must have "jar" as a subpackage name.
     * Packages which target the WAR must have "war" as a subpackage name.
     * In both cases, "jar" or "war" must be interior to the package name. 
     *
     * Conditionally, add resources to the EAR, WAR, or JAR.
     *
     * @param server The target server to receive the application archive.
     * @param toServerDir The directory of the server into which to place the
     *     application archive.  Most commonly, the target server directory is
     *     "apps" or "dropins".
     * @param earName Optional: The name of the EAR which is to be created.
     * @param addEarResources Control parameter: Whether to add resources to the EAR.
     * @param warName The name of the WAR which is to be created.
     * @param addWarResources Control parameter: Whether to add resources to the WAR.
     * @param jarName Optional: The name of the JAR which is to be created.
     * @param addJarResources Control parameter: Whether to add resources to the JAR.
     * @param packageNames Packages to add to the JAR or to the WAR.
     *
     * @throws Exception Thrown if packaging or placing the application fails.
     */
    public static void addEarToServer(
        LibertyServer server, String toServerDir,
        String earName, boolean addEarResources,
        String warName, boolean addWarResources,
        String jarName, boolean addJarResources,
        String... packageNames) throws Exception {

        String methodName = "addEarToServer";

        if ( warName == null ) {
            LOG.info(methodName + ": ENTER / RETURN: No WAR");
            return;
        }

        LOG.info(methodName + ": ENTER: WAR: " + warName);

        LOG.info(methodName + ": Server: " + server.getServerName());

        if ( server.isStarted() ) {
            LOG.info(methodName + ": Server is started; scanning for absent WARs");
            String warSimpleName = warName.substring(0, warName.indexOf(".war"));

            Set<String> warInstalled = server.getInstalledAppNames(warSimpleName);
            LOG.info(methodName + ": WAR " + warSimpleName + " is installed: " + !warInstalled.isEmpty());
            if ( !warInstalled.isEmpty() ) {
                return; // Already installed.
            }

        } else {
            LOG.info(methodName + ": Server is not started");
        }

        LOG.info(methodName + ": Create war " + warName);
        WebArchive warArchive = ShrinkWrap.create(WebArchive.class, warName);

        if ( packageNames != null ) {
            for ( String packageName : packageNames ) {
                if ( packageName.contains(".war.") ) {
                    LOG.info(methodName + ": Add package to war: " + packageName);
                    warArchive.addPackage(packageName);
                }
            }
        }

        if ( jarName != null ) {
            LOG.info(methodName + ": Create jar: " + jarName);
            JavaArchive jarArchive = ShrinkWrap.create(JavaArchive.class, jarName);

            if ( packageNames != null ) {
                for ( String packageName : packageNames ) {
                    if ( packageName.contains(".jar.") ) {
                        LOG.info(methodName + ": Add package to jar: " + packageName);
                        jarArchive.addPackage(packageName);
                    }
                }
            }

            if ( addJarResources ) {
                LOG.info(methodName + ": Add resources to jar: " + jarName);
                ShrinkHelper.addDirectory(jarArchive, "test-applications/" + jarName + "/resources");
            }

            LOG.info(methodName + ": Add jar: " + jarName);
            warArchive.addAsLibrary(jarArchive);
        }

        if ( addWarResources ) {
            LOG.info(methodName + ": Add resources to war: " + warName);
            ShrinkHelper.addDirectory(warArchive, "test-applications/" + warName + "/resources");
        }

        if ( earName != null ) {
            LOG.info(methodName + ": Create ear: " + earName);
            EnterpriseArchive earArchive = ShrinkWrap.create(EnterpriseArchive.class, earName);

            LOG.info(methodName + ": Add war: " + warName);
            earArchive.addAsModule(warArchive);

            if ( addEarResources ) {
                LOG.info(methodName + ": Add EAR descriptor: " + earName);
                String appDescriptorPath =
                       "test-applications/" + earName + "/resources/META-INF/application.xml";
                earArchive.addAsManifestResource( new File(appDescriptorPath) );
            }

            String permissionsPath = "test-applications/" + earName + "/resources/META-INF/permissions.xml";
            File permissionsFile = new File(permissionsPath);
            if ( permissionsFile.exists() ) {
                LOG.info(methodName + ": Add EAR permissions: " + earName);                
                earArchive.addAsManifestResource(permissionsFile);
            }

            LOG.info(methodName + ": Export EAR to server: " + earName);
            ShrinkHelper.exportToServer(server, toServerDir, earArchive);

        } else {
            LOG.info(methodName + ": Export WAR to server: " + warName);
            ShrinkHelper.exportToServer(server, toServerDir, warArchive);
        }

        LOG.info(methodName + ": RETURN");
    }
}
