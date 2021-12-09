/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.utils;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.UserFeatureInstaller;
import componenttest.topology.impl.LibertyServer;

public class OAuth20TokenMapperFeatureInstaller implements UserFeatureInstaller {

    private static final Class<?> thisClass = OAuth20TokenMapperFeatureInstaller.class;

    TestServer aServer = null;

    @Override
    public void installUserFeature(TestServer aServer) throws Exception {
        Log.info(thisClass, "OAuth20TokenMapperFeatureInstaller", "Install Sample OAuth20 token mapping user feature");
        this.aServer = aServer;
        LibertyServer libertyServer = aServer.getServer();
        // install user feature in here
        libertyServer.installUserBundle("com.ibm.ws.security.oauth20.token.mapping_1.0");
        libertyServer.installUserFeature("oauth20TokenMapping-1.0");
    }

    @Override
    public void uninstallUserFeature() throws Exception {
        if (aServer != null) {
            Log.info(thisClass, "OAuth20TokenMapperFeatureInstaller", "Uninstall Sample OAuth20 token mapping user feature");
            LibertyServer libertyServer = aServer.getServer();
            libertyServer.uninstallUserBundle("com.ibm.ws.security.oauth20.token.mapping_1.0");
            libertyServer.uninstallUserFeature("oauth20TokenMapping-1.0");
        }
    }
}
