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

import com.gargoylesoftware.htmlunit.WebClient;
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
        // testmarker app goes into dropins so we don't have to specifically configure it in server.xml
        ShrinkHelper.exportDropinAppToServer(server, getTestMarkerApp());
        server.addInstalledAppForValidation(JwtFatConstants.APP_TESTMARKER);
    }

    protected static void deployFormLoginApp(LibertyServer server) throws Exception {
        deployApp(server, getFormLoginApp(), JwtFatConstants.APP_FORMLOGIN);
    }

    protected static void buildAndDeployApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive jwtBuilderApp = ShrinkHelper.buildDefaultApp(appName, packages);
        deployApp(server, jwtBuilderApp, appName);
    }

    protected static void deployApp(LibertyServer server, WebArchive app, String appName) throws Exception {
        ShrinkHelper.exportAppToServer(server, app);
        server.addInstalledAppForValidation(appName);
    }

    private static WebArchive getTestMarkerApp() throws Exception {
        return ShrinkHelper.buildDefaultApp(JwtFatConstants.APP_TESTMARKER, "com.ibm.ws.security.fat.common.apps.testmarker.*");
    }

    private static WebArchive getFormLoginApp() throws Exception {
        return ShrinkHelper.buildDefaultApp(JwtFatConstants.APP_FORMLOGIN, "com.ibm.ws.security.fat.common.apps.formlogin.*");
    }

    protected static WebClient getHtmlUnitWebClient() {
        WebClient webClient = new WebClient();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setUseInsecureSSL(true);
        return webClient;
    }

}
