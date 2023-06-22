/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
public class MultipleApplicationsNotSupported30 extends AbstractSpringTests {

    @Override
    public Set<String> getFeatures() {
        Set<String> features = new HashSet<>(2);
        features.add("springBoot-3.0");
        features.add("servlet-6.0");
        return features;
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.DROPINS_ROOT;
    }

    @Override
    public int getDropinCopyNum() {
        return 3;
    }

    public Set<String> getApplicationNames() {
        Set<String> appNames = new HashSet<>();

        appNames.add( getApplicationName( getApplication() ) );

        String baseName = "app.copy";
        for ( int appNo = 0; appNo < getDropinCopyNum(); appNo++ ) {
            appNames.add(baseName + appNo);
        }

        return appNames;
    }

    // [6/20/23, 23:51:30:440 EDT] 00000042 com.ibm.ws.app.manager.AppMessageHelper E
    // CWWKZ0002E: An exception occurred while starting the application com.ibm.ws.springboot.fat30.app-0.0.1-SNAPSHOT.
    // The exception message was: java.lang.IllegalStateException:
    // CWWKC0255E: Spring Boot application com.ibm.ws.springboot.fat30.app-0.0.1-SNAPSHOT cannot be started
    // because application app.copy1 is already active. You cannot configure multiple Spring Boot applications
    // in the same server configuration.

    @Test
    public void testMultipleApplicationsNotSupported() throws Exception {
        Set<String> appNames = getApplicationNames();

        try {
            checkOneInstalledApp(appNames);
            removeDropinApps(appNames);
            restoreDropinApps(appNames);
            checkOneInstalledApp(appNames);

        } finally {
            stopServer(true, "CWWKC0255E", "CWWKZ0002E", "CWWKZ0014W");
        }
    }

    private void checkOneInstalledApp(Set<String> appNames) throws Exception {
        Set<String> installedAppNames = server.getInstalledAppNames();
        assertEquals("Count of installed applications", 1, installedAppNames.size());
        String installedAppName = installedAppNames.iterator().next();
        System.out.println("Installed application [ " + installedAppName + " ] on [ " + server + " ]");

        List<String> appErrors = server.findStringsInLogs("CWWKC0255E");

        System.out.println("Application errors:");
        for ( String appError : appErrors ) {
            System.out.println("  [ " + appError + " ]");
        }

        for ( String appName : appNames ) {
            if ( appName.equals(installedAppName) ) {
                continue;
            }

            String appMessage = appName + " cannot be started";
            boolean locatedAppError = false;
            for ( String appError : appErrors ) {
                if ( appError.contains(appMessage) ) {
                    locatedAppError = true;
                    break;
                }
            }

            if ( !locatedAppError ) {
                assertTrue("Located app error [ " + appMessage + " ]", locatedAppError);
            }
        }

        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
        server.setMarkToEndOfLog();
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
        assertNotNull("The application was not installed", server.waitForStringInLog("CWWKZ0001I:.*"));
    }

    private String getApplicationName(String application) {
        return application.substring(0, application.lastIndexOf("."));
    }
}
