/*******************************************************************************
 * Copyright (c) 2018,2024 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

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
public class MultipleApplicationsNotSupported20 extends AbstractSpringTests {

    @Override
    public Set<String> getFeatures() {
        Set<String> features = new HashSet<>(2);
        features.add("springBoot-2.0");
        features.add("servlet-3.1");
        return features;
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

    private String[] applicationNames;
    private String[] shortApplicationNames;

    public String[] getApplicationNames() {
        if (applicationNames == null) {
            computeAppNames();
        }
        return applicationNames;
    }

    public String[] getShortApplicationNames() {
        // The test on 'applicationNames' is correct.
        if (applicationNames == null) {
            computeAppNames();
        }
        return shortApplicationNames;
    }

    protected void computeAppNames() {
        int numCopies = getDropinCopyNum();
        String[] shortAppNames = new String[1 + numCopies];
        String[] appNames = new String[1 + numCopies];

        String appName = getApplication();
        String appHead = appName.substring(0, appName.length() - 4);

        shortAppNames[0] = appHead;
        appNames[0] = appHead + "." + SPRING_APP_TYPE;

        for (int appNo = 0; appNo < numCopies; appNo++) {
            String shortAppHead = "app.copy" + appNo;
            shortAppNames[1 + appNo] = shortAppHead;
            appNames[1 + appNo] = shortAppHead + "." + SPRING_APP_TYPE;
        }

        shortApplicationNames = shortAppNames;
        applicationNames = appNames;
    }

    // [AUDIT   ] CWWKE0001I: The server SpringBootTests has been launched.
    // [AUDIT   ] CWWKZ0058I: Monitoring dropins for applications.
    //
    // [AUDIT   ] CWWKZ0001I: Application app.copy1 started in 7.692 seconds.
    // [AUDIT   ] CWWKF0012I: The server installed the following features: [servlet-3.1, springBoot-2.0, timedexit-1.0].
    // [AUDIT   ] CWWKF0011I: The SpringBootTests server is ready to run a smarter planet. The SpringBootTests server started in 14.828 seconds.
    // [AUDIT   ] CWWKT0016I: Web application available (springBootVirtualHost-8081): http://laptop-evshm5o4.attlocal.net:8081/
    //
    // [AUDIT   ] CWWKT0017I: Web application removed (springBootVirtualHost-8081): http://laptop-evshm5o4.attlocal.net:8081/
    // [AUDIT   ] CWWKZ0009I: The application app.copy1 has stopped successfully.
    //
    // [AUDIT   ] CWWKZ0001I: Application app.copy1 started in 5.155 seconds.
    //
    // [AUDIT   ] CWWKE0055I: Server shutdown requested on Friday, July 14, 2023 at 11:36 PM. The server SpringBootTests is shutting down.
    // [AUDIT   ] CWWKE1100I: Waiting for up to 30 seconds for the server to quiesce.
    // [AUDIT   ] CWWKZ0009I: The application app.copy1 has stopped successfully.

    // [6/20/23, 23:51:30:440 EDT] 00000042 com.ibm.ws.app.manager.AppMessageHelper E
    // CWWKZ0002E: An exception occurred while starting the application com.ibm.ws.springboot.fat30.app-0.0.1-SNAPSHOT.
    // The exception message was: java.lang.IllegalStateException:
    // CWWKC0255E: Spring Boot application com.ibm.ws.springboot.fat30.app-0.0.1-SNAPSHOT cannot be started
    // because application app.copy1 is already active. You cannot configure multiple Spring Boot applications
    // in the same server configuration.

    @Test
    public void testMultipleApplicationsNotSupported() throws Exception {
        try {
            String shortAppName = checkOneInstalledApp();
            String appName = shortAppName + "." + SPRING_APP_TYPE;

            removeDropinApps(appName);
            restoreDropinApps(appName);

            checkOneInstalledApp();

        } finally {
            stopServer(true, "CWWKC0255E", "CWWKZ0002E", "CWWKZ0014W");
        }
    }

    private String checkOneInstalledApp() throws Exception {
        String[] shortAppNames = getShortApplicationNames();
        getDropinsFile();

        Set<String> installedAppNames = server.getInstalledAppNames(shortAppNames);
        assertEquals("Count of installed applications", 1, installedAppNames.size());
        String installedAppName = installedAppNames.iterator().next();
        System.out.println("Installed application [ " + installedAppName + " ] on [ " + server + " ]");

        List<String> appErrors = server.findStringsInLogs("CWWKC0255E");
        System.out.println("Application errors:");
        for (String appError : appErrors) {
            System.out.println("  [ " + appError + " ]");
        }

        for (String shortAppName : shortAppNames) {
            if (shortAppName.equals(installedAppName)) {
                continue;
            }

            String appMessage = shortAppName + " cannot be started";
            boolean locatedAppError = false;
            for (String appError : appErrors) {
                if (appError.contains(appMessage)) {
                    locatedAppError = true;
                    break;
                }
            }

            if (!locatedAppError) {
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
        System.out.println("Removing applications");

        for (String appName : getApplicationNames()) {
            if (appName.equals(installedAppName)) {
                continue;
            }
            System.out.println("Removing application [ " + appName + " ]");
            xferApp(appName, DO_BACKUP);

            // Note: The dropin remains recorded by the server.  There
            // will be a failed attempt to delete it during server cleanup,
            // which will fail, but the failure will be ignored.
        }

        System.out.println("Removing installed application [ " + installedAppName + " ]");
        xferApp(installedAppName, DO_BACKUP);

        // [AUDIT   ] CWWKT0017I: Web application removed (springBootVirtualHost-8081): http://laptop-evshm5o4.attlocal.net:8081/
        // [AUDIT   ] CWWKZ0009I: The application app.copy1 has stopped successfully.

        requireServerMessage("Installed application not removed", "CWWKT0017I:.*");
    }

    private static final boolean DO_BACKUP = true;
    private static final boolean DO_RESTORE = !DO_BACKUP;

    private void xferApp(String appName, boolean doBackup) throws Exception {
        RemoteFile dropins = getDropinsFile();
        RemoteFile serverRoot = getServerRootFile();

        RemoteFile originalFile = server.getMachine().getFile(dropins, appName);
        RemoteFile backupFile = server.getMachine().getFile(serverRoot, appName);

        RemoteFile srcFile;
        RemoteFile dstFile;
        if (doBackup) {
            srcFile = originalFile;
            dstFile = backupFile;
        } else {
            srcFile = backupFile;
            dstFile = originalFile;
        }

        System.out.println("Renaming [ " + srcFile.getAbsolutePath() + " ]" +
                           " to [ " + dstFile.getAbsolutePath() + " ]");
        srcFile.rename(dstFile);
    }

    private void restoreDropinApps(String installedAppName) throws Exception {
        System.out.println("Restoring applications");

        for (String appName : getApplicationNames()) {
            if (appName.equals(installedAppName)) {
                continue;
            }
            System.out.println("Restoring application [ " + appName + " ]");
            xferApp(appName, DO_RESTORE);
        }

        System.out.println("Restoring installed application [ " + installedAppName + " ]");
        xferApp(installedAppName, DO_RESTORE);

        // [AUDIT   ] CWWKZ0001I: Application app.copy1 started in 5.155 seconds.
        // [AUDIT   ] CWWKT0016I: Web application available (springBootVirtualHost-8081): http://laptop-evshm5o4.attlocal.net:8081/

        requireServerMessage("The application was not installed", "CWWKT0016I:.*");
    }
}
