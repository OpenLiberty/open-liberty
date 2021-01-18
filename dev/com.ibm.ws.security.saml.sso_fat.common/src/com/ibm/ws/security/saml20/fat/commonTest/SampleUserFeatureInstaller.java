/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml20.fat.commonTest;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;

public class SampleUserFeatureInstaller implements SAMLInstallUninstallUserFeature {

    private static final Class<?> thisClass = SampleUserFeatureInstaller.class;

    SAMLTestServer aServer = null;

    //
    //  install user feature when needed
    // 
    public void installUserFeature(SAMLTestServer aServer) throws Exception {
        Log.info(thisClass, "SampleUserFeatureInstaller", "Install Sample User Feature");
        this.aServer = aServer;
        LibertyServer libertyServer = aServer.getServer();
        // install user feature in here
        libertyServer.installUserBundle("com.ibm.ws.security.saml.resolver.sample_1.0");
        libertyServer.installUserFeature("samlUserResolver-1.0");
    }

    @Override
    public void uninstallUserFeature() throws Exception {
        if (aServer != null) {
            Log.info(thisClass, "SampleUserFeatureInstaller", "Uninstall Sample User Feature");
            LibertyServer libertyServer = aServer.getServer();
            libertyServer.uninstallUserBundle("com.ibm.ws.security.saml.resolver.sample_1.0");
            libertyServer.uninstallUserFeature("samlUserResolver-1.0");
        }
    }

}
