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
package com.ibm.ws.artifact.fat_bvt.test.utils;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class FATWebArchiveDef {
    private static final Class<?> TEST_CLASS = FATWebArchiveDef.class;
    // private static final String CLASS_NAME = TEST_CLASS.getSimpleName();

    private static void info(String methodName, String text) {
        FATLogging.info(TEST_CLASS, methodName, text);
    }

    //

    public final String warName;
    public final String[] warPackageNames;

    public final String rootResourcesPath;
    public final String[] metaInfResourcePaths;
    public final String[] webInfResourcePaths;

    public FATWebArchiveDef(
        String warName,

        String[] warPackageNames,

        String rootResourcesPath,
        String[] metaInfResourcePaths,
        String[] webInfResourcePaths) {

        this.warName = warName;

        this.warPackageNames = warPackageNames;

        this.rootResourcesPath = rootResourcesPath;
        this.metaInfResourcePaths = metaInfResourcePaths;
        this.webInfResourcePaths = webInfResourcePaths;
    }

    public WebArchive asWebArchive() {
        String methodName = "asWebArchive";
        info(methodName, "WAR [ " + warName + " ]");
        info(methodName, "  Root Resources [ " + rootResourcesPath + " ]");

        WebArchive webArchive = ShrinkWrap.create(WebArchive.class, warName);

        if ( warPackageNames != null ) {
            for ( String warPackageName : warPackageNames ) {
                info(methodName, "  Package [ " + warPackageName + " ]");
                webArchive.addPackages(true, warPackageName);
            }
        } else {
            info(methodName, "  *** NO PACKAGES ***");
        }

        if ( metaInfResourcePaths != null ) {
            File metaInfResourcesFile = new File(rootResourcesPath, "META-INF");
            for ( String metaInfResourcePath : metaInfResourcePaths ) {
                info(methodName, "  META-INF Resource [ " + metaInfResourcePath + " ]");
                File metaInfResourceFile = new File(metaInfResourcesFile, metaInfResourcePath);
                webArchive.addAsManifestResource(metaInfResourceFile, metaInfResourcePath);
            }
        } else {
            info(methodName, "  *** NO META-INF RESOURCES ***");
        }

        if ( webInfResourcePaths != null ) {
            File webInfResourcesFile = new File(rootResourcesPath, "WEB-INF");
            for ( String webInfResourcePath : webInfResourcePaths ) {
                info(methodName, "  WEB-INF Resource [ " + webInfResourcePath + " ]");
                File webInfResourceFile = new File(webInfResourcesFile, webInfResourcePath);
                webArchive.addAsWebInfResource(webInfResourceFile, webInfResourcePath);
            }
        } else {
            info(methodName, "  *** NO WEB-INF RESOURCES ***");
        }

        return webArchive;
    }
}
