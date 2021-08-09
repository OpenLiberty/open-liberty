/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.checkFeature;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

public final class CheckFeatureUtil {

    public static void checkFeature(final LibertyServer server, final String configFile) throws Exception {
        server.setServerConfigurationFile("server.without.cdi.xml");
        server.startServer();
        server.waitForStringInLog("CWWKT0016I: Web application available");

        HttpUtils.findStringInUrl(server, "/checkFeature/rest/front", "EJB not initialised");

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(configFile);

        Thread.sleep(3000);

        server.waitForConfigUpdateInLogUsingMark(null, true, new String[0]);
        server.waitForStringInLogUsingMark("CWWKT0016I: Web application available");

        HttpUtils.findStringInUrl(server, "/checkFeature/rest/front", "message from EJB");

        server.stopServer();
    }

    private CheckFeatureUtil() {}
}
