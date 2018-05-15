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
package com.ibm.ws.security.jwtsso.fat;

import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

import componenttest.topology.impl.LibertyServer;

public class CommonJwtFat extends CommonSecurityFat {

    static void setUpAndStartServer(LibertyServer server, String startingConfigFile) throws Exception {
        deployApps(server);
        serverTracker.addServer(server);
        server.setServerConfigurationFile(startingConfigFile);
        server.startServer();
    }

    protected static void deployApps(LibertyServer server) throws Exception {
        deployTestMarkerApp(server);
        deployFormLoginApp(server);
    }

    protected static void deployTestMarkerApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, getTestMarkerApp());
        server.addInstalledAppForValidation(JwtFatConstants.APP_TESTMARKER);
    }

    protected static void deployFormLoginApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, getFormLoginApp());
        server.addInstalledAppForValidation(JwtFatConstants.APP_FORMLOGIN);
    }

    private static WebArchive getTestMarkerApp() throws Exception {
        return ShrinkHelper.buildDefaultApp(JwtFatConstants.APP_TESTMARKER, "com.ibm.ws.security.fat.common.apps.testmarker.*");
    }

    private static WebArchive getFormLoginApp() throws Exception {
        return ShrinkHelper.buildDefaultApp(JwtFatConstants.APP_FORMLOGIN, "com.ibm.ws.security.fat.common.apps.formlogin.*");
    }

}
