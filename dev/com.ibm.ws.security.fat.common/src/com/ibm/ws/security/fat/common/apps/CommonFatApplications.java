/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.apps;

import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.security.fat.common.Constants;

import componenttest.topology.impl.LibertyServer;

public class CommonFatApplications {

    public static WebArchive getTestMarkerApp() throws Exception {
        return ShrinkHelper.buildDefaultAppFromPath("testmarker", getPathToTestApps(), "com.ibm.ws.security.fat.common.apps.testmarker.*");
    }

    public static WebArchive getFormLoginApp() throws Exception {
        return ShrinkHelper.buildDefaultAppFromPath("formlogin", getPathToTestApps(), "com.ibm.ws.security.fat.common.apps.formlogin.*");
    }

    private static String getPathToTestApps() {
        // When executing FATs, the "user.dir" property is <OL root>/dev/<FAT project>/build/libs/autoFVT/
        // Hence, to get back to this project, we have to navigate a few levels up.
        return System.getProperty("user.dir") + "/../../../../com.ibm.ws.security.fat.common/";
    }

    public static void deployTestMarkerApp(LibertyServer server) throws Exception {
        // testmarker app goes into dropins so we don't have to specifically configure it in server.xml
        ShrinkHelper.exportDropinAppToServer(server, CommonFatApplications.getTestMarkerApp());
        server.addInstalledAppForValidation(Constants.APP_TESTMARKER);
    }

    public static void deployFormLoginApp(LibertyServer server) throws Exception {
        deployApp(server, CommonFatApplications.getFormLoginApp(), Constants.APP_FORMLOGIN);
    }

    public static void buildAndDeployApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive jwtBuilderApp = ShrinkHelper.buildDefaultApp(appName, packages);
        deployApp(server, jwtBuilderApp, appName);
    }

    public static void deployApp(LibertyServer server, WebArchive app, String appName) throws Exception {
        ShrinkHelper.exportAppToServer(server, app);
        server.addInstalledAppForValidation(appName);
    }

}