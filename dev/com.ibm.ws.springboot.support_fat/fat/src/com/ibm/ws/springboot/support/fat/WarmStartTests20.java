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
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
/**
 * This tests what is considered the ideal path a customer would use.
 * Running from an already thinned app jar and a lib.index.cache
 * located in the shared/resources location.
 * Do not make this part of the FULL mode since we want to make sure
 * this tests always runs.
 */
@Mode(FULL)
public class WarmStartTests20 extends AbstractSpringTests {

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

    @Test
    public void testWarmStartConfig() throws Exception {
        // First stop the server so we can start from a warm start
        server.stopServer(false);

        // remove any dropins
        RemoteFile dropinsSpr = server.getFileFromLibertyServerRoot("dropins/" + SPRING_APP_TYPE);
        RemoteFile[] dropinApps = dropinsSpr.list(true);
        for (RemoteFile dropinApp : dropinApps) {
            if (dropinApp.isFile()) {
                dropinApp.delete();
            }
        }

        // start server without clean; no applications should start now
        server.startServer(false);

        // set mark to end so we can test the messages after we add the dropin app back
        server.setMarkToEndOfLog();
        getApplicationFile().copyToDest(dropinsSpr);

        // make sure we get the TCP channel message between the starting and installed message for the app
        assertNotNull("The application is not starting", server.waitForStringInLog("CWWKZ0018I:.*"));
        assertNotNull("The TCP Channel did not start after application starting", server.waitForStringInLog("CWWKO0219I:.*"));
        assertNotNull("The application was not installed", server.waitForStringInLog("CWWKZ0001I:.*"));

        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }
}
