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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
 * This tests that multiple spring boot applications
 * are not supported in the same server configuration
 */
@Mode(FULL)
public class MultipleApplicationsNotSupported20 extends AbstractSpringTests {

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.DROPINS_ROOT;
    }

    @Override
    public int getDropinCopyNum() {
        return 3;
    }

    @Test
    public void testMultipleApplicationsNotSupported() throws Exception {

        //Get the name of the application installed so that we can remove it later
        Collection<String> appNames = new ArrayList<>();
        String baseName = getApplicationName(SPRING_BOOT_20_APP_BASE);
        appNames.add(baseName);
        appNames.add("app.copy0");
        appNames.add("app.copy1");
        appNames.add("app.copy2");

        checkOneInsetalledApp(appNames);

        removeDropinApps(appNames);

        //Set mark to end so we can test the messages after we add the dropins app back
        server.setMarkToEndOfLog();

        restoreDropinApps(appNames);

        checkOneInsetalledApp(appNames);

        stopServer(true, "CWWKC0255E", "CWWKZ0002E", "CWWKZ0014W");
    }

    private void checkOneInsetalledApp(Collection<String> appNames) throws Exception {
        Set<String> installedApps = server.getInstalledAppNames(appNames.toArray(new String[0]));
        assertEquals("Expected number of applications not installed", 1, installedApps.size());
        String installedApp = installedApps.iterator().next();

        for (String dropinApp : appNames) {
            if (!dropinApp.equals(installedApp)) {
                // Make sure we get Error messages indicating multiple spring boot applications cannot be configured in the same server configuration
                assertNotNull("Expected error message not found for not supporting multiple spring boot applications",
                              server.waitForStringInLog("CWWKC0255E:.*" + dropinApp + ".*"));
            }
        }

        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

    }

    private void removeDropinApps(Collection<String> appNames) throws Exception {
        RemoteFile dropins = server.getFileFromLibertyServerRoot("dropins");
        for (String appName : appNames) {
            String appFileName = appName + '.' + SPRING_APP_TYPE;
            RemoteFile appFile = new RemoteFile(dropins, appFileName);
            appFile.rename(new RemoteFile(server.getFileFromLibertyServerRoot(""), appFileName));
        }
        assertNotNull("Web application not removed", server.waitForStringInLog("CWWKT0017I:.*"));
    }

    private void restoreDropinApps(Collection<String> appNames) throws Exception {
        RemoteFile dropins = server.getFileFromLibertyServerRoot("dropins");
        for (String appName : appNames) {
            String appFileName = appName + '.' + SPRING_APP_TYPE;
            RemoteFile appFile = new RemoteFile(server.getFileFromLibertyServerRoot(""), appFileName);
            RemoteFile restoreDest = new RemoteFile(dropins, "restore." + appFileName);
            appFile.rename(restoreDest);
            dropinFiles.add(restoreDest);
        }
        assertNotNull("The application was not installed", server
                        .waitForStringInLog("CWWKZ0001I:.*"));
    }

    private String getApplicationName(String application) {
        return application.substring(0, application.lastIndexOf("."));
    }
}
