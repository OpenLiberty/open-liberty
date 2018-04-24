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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class MultipleApplicationsNotSupported extends AbstractSpringTests {

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-1.5", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_15_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.DROPINS_ROOT;
    }

    @Test
    public void testMultipleApplicationsNotSupported() throws Exception {
        // First stop the server so we can configure two spring boot applications
        server.stopServer(false);

        String applicationCopy = null;

        //Make a copy of configured application and place it in the same folder
        RemoteFile dropins = server.getFileFromLibertyServerRoot("dropins");

        RemoteFile[] dropinApps = dropins.list(true);
        for (RemoteFile dropinApp : dropinApps) {
            if (dropinApp.isFile()) {
                int dot = dropinApp.getName().lastIndexOf(".");
                applicationCopy = getApplicationName(dropinApp.getName()) + ".copy" + dropinApp.getName().substring(dot);
                RemoteFile copyFile = new RemoteFile(dropins, applicationCopy);
                dropinApp.copyToDest(copyFile);
                dropinFiles.add(copyFile);
            }
        }

        // start server
        server.startServer(false);

        // Make sure we get Error message indicating multiple spring boot applications cannot be configured in the same server configuration
        assertNotNull("Expected error message not found for not supporting multiple spring boot applications", server.waitForStringInLog("CWWKC0255E:.*"));

        //Get the message indicating web application is available on a particular virtual host
        String message1 = server.waitForStringInLog("CWWKT0016I:.*");
        assertNotNull("Web application not available", message1);
        String virtualHost1 = getVirtualHost(message1);

        //Get the name of the application installed so that we can remove it later
        Set<String> installedApps = server.getInstalledAppNames(getApplicationName(SPRING_BOOT_15_APP_BASE),
                                                                getApplicationName(applicationCopy));
        assertEquals("Expected number of applications not installed", 1, installedApps.size());
        String installedApp = installedApps.iterator().next();

        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        //Remove and restore the installed application from dropins
        String virtualHost2 = removeAndRestoreApplication(virtualHost1, installedApp);

        //Repeat the process to ensure it behaves consistently
        String virtualHost3 = removeAndRestoreApplication(virtualHost2, installedApp);

        stopServer(true, "CWWKC0255E", "CWWKZ0002E", "CWWKZ0014W");
    }

    private String getVirtualHost(String message) {
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(message);
        while (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String removeAndRestoreApplication(String virtualHost1, String installedApp) throws Exception, IOException {
        //Remove the installed application from dropins
        server.removeDropinsApplications(installedApp + "." + SPRING_APP_TYPE);
        assertNotNull("Web application not removed", server.waitForStringInLog("CWWKT0017I:.*"));

        //Set mark to end so we can test the messages after we add the dropins app back
        server.setMarkToEndOfLog();

        //Restore the deleted application back to dropins
        server.restoreDropinsApplications(installedApp + "." + SPRING_APP_TYPE);

        //Get the message indicating web application is available on a particular virtual host
        String message2 = server.waitForStringInLog("CWWKT0016I:.*");
        assertNotNull("Web application not available", message2);

        String virtualHost2 = getVirtualHost(message2);

        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        //The application should start on different virtual hosts
        assertFalse("Application should start on a different virtual host", virtualHost1.equals(virtualHost2));
        return virtualHost2;
    }

    private String getApplicationName(String application) {
        return application.substring(0, application.lastIndexOf("."));
    }
}
