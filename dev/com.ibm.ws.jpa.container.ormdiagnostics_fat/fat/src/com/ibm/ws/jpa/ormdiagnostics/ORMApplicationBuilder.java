/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.ormdiagnostics;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

/**
 * A Utility class used to construct applications in FAT tests
 */
public class ORMApplicationBuilder {

    private static final Logger LOG = Logger.getLogger(ORMApplicationBuilder.class.getName());

    public static void addArchivetoServer(LibertyServer server, String dir, Archive<?> arc) throws Exception {
        // If server is already started and app exists no need to add it.
        if (server.isStarted()) {
            String appName = arc.getName();
            Set<String> appInstalled = server.getInstalledAppNames(appName);
            LOG.info("addArchivetoServer : " + appName + " already installed : " + !appInstalled.isEmpty());

            if (!appInstalled.isEmpty())
                return;
        }
        ShrinkHelper.exportToServer(server, dir, arc);
    }

    public static JavaArchive createJAR(String jarName, String... packageNames) throws Exception {
        LOG.info("createJAR : create jar " + jarName);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, jarName);
        if (packageNames != null) {
            for (String packageName : packageNames) {
                jar.addPackage(packageName);
            }
        }

        ShrinkHelper.addDirectory(jar, "test-applications/" + jarName + "/resources");
        return jar;
    }

    public static WebArchive createWAR(String warName, String... packageNames) throws Exception {
        return createWAR(warName, null, packageNames);
    }

    public static WebArchive createWAR(String warName, List<JavaArchive> jars, String... packageNames) throws Exception {
        LOG.info("createWAR : create war " + warName);

        WebArchive war = ShrinkWrap.create(WebArchive.class, warName);
        if (packageNames != null) {
            for (String packageName : packageNames) {
                war.addPackage(packageName);
            }
        }

        if (jars != null) {
            for (JavaArchive jar : jars) {
                war.addAsLibrary(jar);
            }
        }

        ShrinkHelper.addDirectory(war, "test-applications/" + warName + "/resources");
        return war;
    }

    public static EnterpriseArchive createEAR(String earName, List<WebArchive> wars, List<JavaArchive> libs) {
        LOG.info("createEAR : create ear " + earName);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName);

        for (WebArchive war : wars) {
            ear.addAsModule(war);
        }

        for (JavaArchive lib : libs) {
            ear.addAsLibrary(lib);
        }

        ear.addAsManifestResource(new File("test-applications/" + earName + "/resources/META-INF/application.xml"));

        //Liberty does not use was.policy but permissions.xml
        File permissionsXML = new File("test-applications/" + earName + "/resources/META-INF/permissions.xml");
        if (permissionsXML.exists()) {
            ear.addAsManifestResource(permissionsXML);
        }

        return ear;
    }
}
