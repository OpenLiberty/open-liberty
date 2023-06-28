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

    private String[] applicationNames;
    private String[] shortApplicationNames;

    public String[] getApplicationNames() {
        if ( applicationNames == null ) {
            computeAppNames();
        }
        return applicationNames;
    }

    public String[] getShortApplicationNames() {
        // The test on 'applicationNames' is correct.
        if ( applicationNames == null ) {
            computeAppNames();
        }
        return shortApplicationNames;
    }

    protected void computeAppNames() {
        int numCopies = getDropinCopyNum();
        String[] shortAppNames = new String[ 1 + numCopies ];
        String[] appNames = new String[ 1 + numCopies ];

        String appName = getApplication();
        String appHead = appName.substring(0, appName.length() - 4 );

        shortAppNames[0] = appHead;
        appNames[0] = appHead + "." + SPRING_APP_TYPE;

        for ( int appNo = 0; appNo < numCopies; appNo++ ) {
            String shortAppHead = "app.copy" + appNo;
            shortAppNames[1 + appNo] = shortAppHead;
            appNames[1 + appNo] = shortAppHead + "." + SPRING_APP_TYPE;
        }

        shortApplicationNames = shortAppNames;
        applicationNames = appNames;
    }

    // [6/20/23, 23:51:30:440 EDT] 00000042 com.ibm.ws.app.manager.AppMessageHelper E
    // CWWKZ0002E: An exception occurred while starting the application com.ibm.ws.springboot.fat30.app-0.0.1-SNAPSHOT.
    // The exception message was: java.lang.IllegalStateException:
    // CWWKC0255E: Spring Boot application com.ibm.ws.springboot.fat30.app-0.0.1-SNAPSHOT cannot be started
    // because application app.copy1 is already active. You cannot configure multiple Spring Boot applications
    // in the same server configuration.

    @Test
    public void testMultipleApplicationsNotSupported() throws Exception {
        try {
            String appName = checkOneInstalledApp();

            removeDropinApps(appName);
            restoreDropinApps(appName);

            String newAppName = checkOneInstalledApp();

        } finally {
            stopServer(true, "CWWKC0255E", "CWWKZ0002E", "CWWKZ0014W");
        }
    }

    private String checkOneInstalledApp() throws Exception {
        String[] shortAppNames = getShortApplicationNames();
        RemoteFile dropins = getDropinsFile();

        Set<String> installedAppNames = server.getInstalledAppNames(shortAppNames);
        assertEquals("Count of installed applications", 1, installedAppNames.size());
        String installedAppName = installedAppNames.iterator().next();
        System.out.println("Installed application [ " + installedAppName + " ] on [ " + server + " ]");

        List<String> appErrors = server.findStringsInLogs("CWWKC0255E");
        System.out.println("Application errors:");
        for ( String appError : appErrors ) {
            System.out.println("  [ " + appError + " ]");
        }

        for ( String shortAppName : shortAppNames ) {
            if ( shortAppName.equals(installedAppName) ) {
                continue;
            }

            String appMessage = shortAppName + " cannot be started";
            boolean locatedAppError = false;
            for ( String appError : appErrors ) {
                if ( appError.contains(appMessage) ) {
                    locatedAppError = true;
                    break;
                }
            }

            if ( !locatedAppError ) {
                assertTrue("Failed to locate app error [ " + appMessage + " ]", locatedAppError);
            } else {
                System.out.println("Located app error [ " + appMessage + " ]");
            }
        }

        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
        server.setMarkToEndOfLog();

        return installedAppName;
    }

    private void removeDropinApps(String installedAppName) throws Exception {
        RemoteFile serverRoot = getServerRootFile();
        RemoteFile dropins = getDropinsFile();

        for ( String appName : getApplicationNames() ) {
            if ( appName.equals(installedAppName) ) {
                System.out.println("Skipping installed application [ " + appName + " ]");
                continue;
            }
            System.out.println("Removing application file [ " + appName + " ]");

            RemoteFile originalFile = new RemoteFile(dropins, appName);
            RemoteFile backupFile = new RemoteFile(serverRoot, appName );
            originalFile.rename(backupFile);

            // Note: The dropin remains recorded.
            // There will be a failed attempt to delete it, which
            // is a little extra work, and which will fail, but
            // the failure will be ignored.
        }

        requireServerMessage("Application not removed", "CWWKT0017I:.*");
    }

    private void restoreDropinApps(String installedAppName) throws Exception {
        RemoteFile serverRoot = getServerRootFile();
        RemoteFile dropins = getDropinsFile();

        for ( String appName : getApplicationNames() ) {
            if ( appName.equals(installedAppName) ) {
                continue;
            }

            RemoteFile backupFile = new RemoteFile(serverRoot, appName);
            RemoteFile restoredFile = new RemoteFile(dropins, appName);
            backupFile.rename(restoredFile);
        }

        requireServerMessage("The application was not installed", "CWWKZ0001I:.*");
    }
}
