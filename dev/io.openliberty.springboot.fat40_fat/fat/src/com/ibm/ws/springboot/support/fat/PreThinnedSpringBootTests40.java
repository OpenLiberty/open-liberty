/*******************************************************************************
 * Copyright (c) 2018,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
/**
 * This tests what is considered the ideal path a customer would use.
 * Running from an already thinned app jar and a lib.index.cache
 * located in the shared/resources location.
 * Do not make this part of the FULL mode since we want to make sure
 * this tests always runs.
 */
@MinimumJavaLevel(javaLevel = 17)
public class PreThinnedSpringBootTests40 extends AbstractSpringTests {

    private String application = SPRING_BOOT_40_APP_BASE;

    @Override
    public Set<String> getFeatures() {
        return getWebFeatures();
    }

    @Override
    public String getApplication() {
        return application;
    }

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        if (application != SPRING_BOOT_40_APP_BASE) {
            appConfig.setName("testPreThinned");
        }
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Test
    public void testWithSharedCache() throws Exception {
        // First stop the server which has already thinned the test application
        stopServer(false);

        // locate and copy the lib.index.cache to the shared area
        RemoteFile libIndexCache = server.getFileFromLibertyServerRoot(SPRING_WORKAREA_DIR + SPRING_LIB_INDEX_CACHE);
        assertTrue("No cache found: " + libIndexCache.getAbsolutePath(), libIndexCache.exists());
        RemoteFile sharedDir = server.getMachine().getFile(server.getFileFromLibertyInstallRoot(""), "usr/shared");
        sharedDir.mkdirs();
        RemoteFile sharedLibIndexCache = server.getMachine().getFile(server.getFileFromLibertySharedDir(""), SHARED_SPRING_LIB_INDEX_CACHE);
        sharedLibIndexCache.mkdirs();
        libIndexCache.copyToDest(sharedLibIndexCache, true, true);

        // locate and copy the thinned app jar to the apps folder
        RemoteFile thinApps = server.getFileFromLibertyServerRoot(SPRING_WORKAREA_DIR + SPRING_THIN_APPS_DIR);
        RemoteFile[] apps = thinApps.list(false);
        assertEquals("Wrong number of apps.", 1, apps.length);
        RemoteFile thinnedApp = server.getMachine().getFile(server.getFileFromLibertyServerRoot("apps"), "thinnedApp.jar");
        apps[0].copyToDest(thinnedApp);

        // configure the pre-thinned app jar as a spring boot app
        application = thinnedApp.getName();
        configureServer();

        assertNotNull("The application was not installed", server
                        .waitForStringInLog("CWWKZ0001I:.*testPreThinned.*"));
        assertNotNull("The endpoint is not available", server
                        .waitForStringInLog("CWWKT0016I:.*"));

        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }
}
