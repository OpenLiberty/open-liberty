/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.filemonitor.FileNotificationMBean;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class FileNotificationMBeanTest extends AbstractNotificationTest {

    /** Used for sanity checks of changes in the timed scan case */
    private static final MonitorReader recursiveMonitor = new MonitorReader("-RECURSIVETESTMONITOROUTPUT-", "recursive folder monitor");
    private static final MonitorReader manualMonitor = new MonitorReader("-MANUALMONITOROUTPUT-", "externally triggered folder monitor");

    private static JMXConnector jmxConnector;
    private static FileNotificationMBean mbean;

    /**
     * JUnit guarantees that this gets run after the static set up in the superclass (as long as the names are different).
     */
    @BeforeClass
    public static void jmxSetUp() throws Exception {

        // Wait for the JMX server to be started 
        assertNotNull("The application 'IBMJMXConnectorREST' did not report it was started",
                      server.waitForStringInLog("CWWKT0016I.*IBMJMXConnectorREST"));
        // Wait for secure port to be ready
        assertNotNull("SSL port is not ready",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        assertNotNull("The server is not ready to run a smarter planet",
                      server.waitForStringInLog("CWWKF0011I"));
        assertNotNull("The security service is not ready",
                      server.waitForStringInLog("CWWKS0008I"));

        trustEverything();

        Map<String, Object> fileTransferConfig = new HashMap<String, Object>();
        fileTransferConfig.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
        fileTransferConfig.put(JMXConnector.CREDENTIALS, new String[] { "theUser", "thePassword" });
        fileTransferConfig.put(ClientProvider.DISABLE_HOSTNAME_VERIFICATION, true);
        fileTransferConfig.put(ClientProvider.READ_TIMEOUT, 2 * 60 * 1000);

        JMXServiceURL jmxServiceUrl = new JMXServiceURL("REST", server.getHostname(), server
                        .getHttpDefaultSecurePort(), "/IBMJMXConnectorREST");
        long connectTime = System.currentTimeMillis();
        try {
            jmxConnector = JMXConnectorFactory.connect(jmxServiceUrl, fileTransferConfig);

            MBeanServerConnection mbeanConn = jmxConnector.getMBeanServerConnection();
            final ObjectName name = new ObjectName(com.ibm.ws.kernel.filemonitor.FileNotificationMBean.INSTANCE_NAME);
            mbean = JMX.newMBeanProxy(mbeanConn, name,
                                      FileNotificationMBean.class);

            assertNotNull("We should have got access to the FileNotificationMBean", mbean);
        } catch (ConnectException e) {
            DateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss:SSS z");
            String timestamp = formatter.format(new Date(connectTime));
            fail("Could not get a connection to the JMX server using URL " + jmxServiceUrl + ".\n Tried at " + timestamp + ".\n The exception was " + e);
        }

    }

    /**
     * Adjusts our SSL code so that it doesn't complain when we connect to servers using untrusted
     * certificates.
     * 
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private static void trustEverything() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    }

    @AfterClass
    public static void classTearDown() throws MalformedObjectNameException, NullPointerException, IOException {
        if (jmxConnector != null) {
            jmxConnector.close();
        }
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
        // Clear up after any tests which may have deleted the monitored folder
        if (!monitoredFolder.exists()) {
            monitoredFolder.mkdirs();
            // Nothing we're using is monitoring the folder itself
        }

        // Get rid of any dangling notifications from tests
        flushNotifications(recursiveMonitor);
        flushNotifications();

    }

    @Test
    public void testFileInFolderCreationIsNotNotifiedWithoutMBeanRequest() throws Throwable {

        File f = new File(monitoredFolder, "testFileInFolderModificationIsNotNotifiedWithoutMBeanRequest");
        createFile(f);

        manualMonitor.scrapeLogsForChanges();
        // We shouldn't be told about anything, since we haven't triggered the scan
        assertNothingCreated(manualMonitor);
        assertNothingDeleted(manualMonitor);
        assertNothingModified(manualMonitor);

        // Sanity check - the recursive monitor should have noticed
        HashSet<File> created = new HashSet<File>();
        created.add(f);
        recursiveMonitor.scrapeLogsForExpectedChanges(created, null, null);
        assertCreated(recursiveMonitor, f);
    }

    @Test
    public void testFileInFolderCreationIsNotNotifiedForMBeanRequestOnUnrelatedFile() throws Throwable {

        File f = new File(monitoredFolder, "testFileInFolderCreationIsNotNotifiedForMBeanRequestOnUnrelatedFile");
        createFile(f);

        Collection<String> fileSet = new HashSet<String>();
        fileSet.add(f.getAbsolutePath());

        Collection<String> wrongFileSet = new HashSet<String>();
        wrongFileSet.add(monitoredFile.getAbsolutePath());

        try {
            // Notify changes, but for the wrong file 
            mbean.notifyFileChanges(wrongFileSet, null, null);
            manualMonitor.scrapeLogsForChanges();
            // We shouldn't be told about anything since we passed through the wrong file
            assertNothingCreated(manualMonitor);
            assertNothingDeleted(manualMonitor);
            assertNothingModified(manualMonitor);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInFolderCreationIsNotNotifiedForMBeanRequestOfWrongChangeType() throws Throwable {

        File f = new File(monitoredFolder, "testFileInFolderCreationIsNotNotifiedForMBeanRequestOfWrongChangeType");
        createFile(f);

        Collection<String> fileSet = new HashSet<String>();
        fileSet.add(f.getAbsolutePath());

        Collection<String> wrongFileSet = new HashSet<String>();
        wrongFileSet.add(monitoredFile.getAbsolutePath());

        try {
            // Now request a notification, but of modification, so the wrong type 
            mbean.notifyFileChanges(null, fileSet, null);
            manualMonitor.scrapeLogsForChanges();
            // We shouldn't be told about anything, since we have the right file but the wrong type
            assertNothingCreated(manualMonitor);
            assertNothingDeleted(manualMonitor);
            assertNothingModified(manualMonitor);

        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    /**
     * In the externally triggered case, we need to be specific about what we think might have changed.
     * If we have a recursive monitor, and we pass the parent folder as the parameter on the mbean call,
     * we won't get told about things inside that folder which have changed.
     */
    @Mode(TestMode.FULL)
    @Test
    public void testFileInFolderModificationIsNotNotifiedWhenWePassBackTheParentFolder() throws Throwable {

        File f = new File(monitoredFolder, "testFileInFolderModificationIsNotNotifiedWhenWePassBackTheParentFolder");
        createFile(f);

        Collection<String> folderSet = new HashSet<String>();
        folderSet.add(monitoredFolder.getAbsolutePath());
        Collection<File> created = new HashSet<File>();
        created.add(monitoredFolder);

        try {
            mbean.notifyFileChanges(folderSet, null, null);
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            assertNothingCreated(manualMonitor);
            assertNothingDeleted(manualMonitor);
            assertNothingModified(manualMonitor);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInFolderCreationIsNotifiedAfterRequest() throws Throwable {

        File f = new File(monitoredFolder, "testFileInFolderCreationIsNotifiedAfterRequest");
        createFile(f);

        // Sanity check - the recursive monitor should have noticed
        recursiveMonitor.scrapeLogsForChanges();
        assertCreated(recursiveMonitor, f);

        Collection<String> fileSet = new HashSet<String>();
        fileSet.add(f.getAbsolutePath());
        Collection<File> created = new HashSet<File>();
        created.add(f);

        try {
            // Ok, let's actually get our notification now
            mbean.notifyFileChanges(fileSet, null, null);
            //manualMonitor.scrapeLogsForChanges();
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            assertCreated(manualMonitor, f);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInFolderModificationIsNotifiedAfterRequest() throws Throwable {

        File f = new File(monitoredFolder, "testFileInFolderModificationIsNotifiedAfterRequest");
        createFile(f);

        Collection<String> fileSet = new HashSet<String>();
        fileSet.add(f.getAbsolutePath());
        Collection<File> changed = new HashSet<File>();
        changed.add(f);

        try {
            // Setup for test 
            mbean.notifyFileChanges(fileSet, null, null); // Need to let them know we created a file that we are going to modify later
            manualMonitor.scrapeLogsForExpectedChanges(changed, null, null);
            assertCreated(manualMonitor, f);
            flushNotifications(recursiveMonitor); // Need to clear recursive monitor

            //////// Actual test
            // Now let's make a change and get that notification
            appendSomething(f);
            mbean.notifyFileChanges(null, fileSet, null);
            manualMonitor.scrapeLogsForExpectedChanges(null, changed, null);
            assertModified(manualMonitor, f);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInFolderDeletionIsNotifiedAfterRequest() throws Throwable {

        File f = new File(monitoredFolder, "testFileInFolderDeletionIsNotifiedAfterRequest");
        createFile(f);

        Collection<String> fileSet = new HashSet<String>();
        fileSet.add(f.getAbsolutePath());
        Collection<File> deleted = new HashSet<File>();
        deleted.add(f);

        try {
            // Setup for test 
            mbean.notifyFileChanges(fileSet, null, null); // Need to let them know we created a file that we are going to modify later
            manualMonitor.scrapeLogsForExpectedChanges(deleted, null, null);
            assertCreated(manualMonitor, f);
            flushNotifications(recursiveMonitor); // Need to clear recursive monitor

            // Now let's make a change and get that notification
            deleteFile(f);
            mbean.notifyFileChanges(null, null, fileSet);
            manualMonitor.scrapeLogsForExpectedChanges(null, null, deleted);
            assertDeleted(manualMonitor, f);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInNestedFolderCreationIsNotifiedAfterRequestAboutFolder() throws Throwable {

        File folder = new File(monitoredFolder, "testFileInNestedFolderCreationIsNotifiedAfterRequestAboutFolder");
        createFolder(folder);

        File f = new File(folder, "childFile");
        createFile(f);

        // Sanity check - the recursive monitor should have noticed
        recursiveMonitor.scrapeLogsForChanges();
        assertCreated(recursiveMonitor, f, folder);

        Collection<String> fileSet = new HashSet<String>();
        fileSet.add(folder.getAbsolutePath());
        Collection<File> created = new HashSet<File>();
        created.add(folder);

        try {
            // Ok, let's actually get our notification now
            mbean.notifyFileChanges(fileSet, null, null);
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            assertCreated(manualMonitor, folder);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInNestedFolderModificationIsNotNotifiedAfterRequestAboutFolder() throws Throwable {

        File folder = new File(monitoredFolder, "testFileInNestedFolderModificationIsNotifiedAfterRequestAboutFolder");
        createFolder(folder);

        File f = new File(folder, "childFile");
        createFile(f);

        // Sanity check - the recursive monitor should have noticed
        recursiveMonitor.scrapeLogsForChanges();
        assertCreated(recursiveMonitor, f, folder);

        Collection<String> fileSet = new HashSet<String>();
        fileSet.add(folder.getAbsolutePath());

        try {
            flushNotifications();

            // Now let's make a change and get that notification
            appendSomething(f);
            updateTimestamp(folder);
            mbean.notifyFileChanges(null, fileSet, null);
            manualMonitor.scrapeLogsForChanges();
            // We could only get told about the folder modification, and we don't tend to generate those 
            // events except for delete-create cycles
            assertNothingModified(manualMonitor);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInNestedFolderDeletionIsNotifiedAfterRequestAboutFolder() throws Throwable {

        File folder = new File(monitoredFolder, "testFileInNestedFolderDeletionIsNotifiedAfterRequestAboutFolder");

        createFolder(folder);
        Collection<String> fileSet = new HashSet<String>();
        Collection<File> files = new HashSet<File>();
        fileSet.add(folder.getAbsolutePath());
        files.add(folder);
        mbean.notifyFileChanges(fileSet, null, null); // Need to let them know we created a file that we are going to modify later
        manualMonitor.scrapeLogsForExpectedChanges(files, null, null);
        assertCreated(manualMonitor, folder);

        try {

            // Setup for test 
            // Create folder
            mbean.notifyFileChanges(fileSet, null, null); // Need to let them know we created a file that we are going to modify later
            manualMonitor.scrapeLogsForExpectedChanges(files, null, null);
            assertCreated(manualMonitor, folder);
            flushNotifications(recursiveMonitor); // Need to clear recursive monitor
            // Create nested file
            File f = new File(folder, "childFile");
            createFile(f);
            Collection<String> fileSet2 = new HashSet<String>();
            fileSet2.add(f.getAbsolutePath());
            Collection<File> created = new HashSet<File>();
            created.add(f);
            mbean.notifyFileChanges(fileSet2, null, null); // Need to let them know we created a file that we are going to modify later
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            assertCreated(manualMonitor, f);
            flushNotifications(recursiveMonitor); // Need to clear recursive monitor

            // Now let's make a change and get that notification
            deleteFile(folder);
            mbean.notifyFileChanges(null, null, fileSet);
            manualMonitor.scrapeLogsForExpectedChanges(null, null, created);
            assertDeleted(manualMonitor, folder);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInNestedFolderCreationIsNotifiedAfterRequestAboutFile() throws Throwable {

        File folder = new File(monitoredFolder, "testFileInNestedFolderCreationIsNotifiedAfterRequestAboutFile");
        createFolder(folder);

        File f = new File(folder, "childFile");
        createFile(f);

        // Sanity check - the recursive monitor should have noticed
        recursiveMonitor.scrapeLogsForChanges();
        assertCreated(recursiveMonitor, f, folder);

        Collection<String> fileSet = new HashSet<String>();
        fileSet.add(f.getAbsolutePath());
        Collection<File> created = new HashSet<File>();
        created.add(f);
        created.add(folder);

        try {
            // Ok, let's actually get our notification now
            mbean.notifyFileChanges(fileSet, null, null);
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            // defect 87702 requires that we generate events for parent directories of paths that we found files for
            // even if the external scan has not requested them.
            assertCreated(manualMonitor, f, folder);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInNestedFolderModificationIsNotifiedAfterRequestAboutFile() throws Throwable {

        File folder = new File(monitoredFolder, "testFileInNestedFolderModificationIsNotifiedAfterRequestAboutFile");
        createFolder(folder);
        Collection<String> fileSet = new HashSet<String>();
        Collection<File> files = new HashSet<File>();
        fileSet.add(folder.getAbsolutePath());
        files.add(folder);
        mbean.notifyFileChanges(fileSet, null, null); // Need to let them know we created a file that we are going to modify later
        manualMonitor.scrapeLogsForExpectedChanges(files, null, null);
        assertCreated(manualMonitor, folder);

        try {

            // Setup for test 
            // Create folder
            mbean.notifyFileChanges(fileSet, null, null); // Need to let them know we created a file that we are going to modify later
            manualMonitor.scrapeLogsForExpectedChanges(files, null, null);
            assertCreated(manualMonitor, folder);
            flushNotifications(recursiveMonitor); // Need to clear recursive monitor
            // Create nested file
            File f = new File(folder, "childFile");
            createFile(f);
            Collection<String> fileSet2 = new HashSet<String>();
            fileSet2.add(f.getAbsolutePath());
            Collection<File> created = new HashSet<File>();
            created.add(f);
            mbean.notifyFileChanges(fileSet2, null, null); // Need to let them know we created a file that we are going to modify later
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            assertCreated(manualMonitor, f);
            flushNotifications(recursiveMonitor); // Need to clear recursive monitor

            // Actual test
            // Now let's make a change and get that notification
            appendSomething(f);
            updateTimestamp(folder);
            mbean.notifyFileChanges(null, fileSet2, null);
            manualMonitor.scrapeLogsForChanges();
            // defect 87702 requires that we generate events for parent directories of paths that we found files for
            // even if the external scan has not requested them, but only for deletion and creation, not modification 
            assertModified(manualMonitor, f);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testFileInNestedFolderDeletionIsNotifiedAfterRequestAboutFile() throws Throwable {

        File folder = new File(monitoredFolder, "testFileInNestedFolderDeletionIsNotifiedAfterRequestAboutFile");
        createFolder(folder);
        Collection<String> fileSet = new HashSet<String>();
        Collection<File> files = new HashSet<File>();
        fileSet.add(folder.getAbsolutePath());
        files.add(folder);
        mbean.notifyFileChanges(fileSet, null, null); // Need to let them know we created a file that we are going to modify later
        manualMonitor.scrapeLogsForExpectedChanges(files, null, null);
        assertCreated(manualMonitor, folder);

        try {

            // Setup for test 
            // Create folder
            mbean.notifyFileChanges(fileSet, null, null); // Need to let them know we created a file that we are going to modify later
            manualMonitor.scrapeLogsForExpectedChanges(files, null, null);
            assertCreated(manualMonitor, folder);
            flushNotifications();
            // Create nested file
            File f = new File(folder, "childFile");
            createFile(f);

            // We're only interested in changes to the file, not the folder
            // This file:
            Collection<String> fileSet2 = new HashSet<String>();
            fileSet2.add(f.getAbsolutePath());
            Collection<File> created = new HashSet<File>();
            created.add(f);
            mbean.notifyFileChanges(fileSet2, null, null); // Need to let them know we created a file that we are going to modify later
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            assertCreated(manualMonitor, f);
            flushNotifications();

            // Now let's make a change and get that notification
            deleteFile(folder);
            mbean.notifyFileChanges(null, null, fileSet2);
            manualMonitor.scrapeLogsForChanges();
            // defect 87702 requires that we generate events for parent directories of paths that we found files for
            // even if the external scan has not requested them.
            assertDeleted(manualMonitor, f, folder);
        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    private void flushNotifications() throws Exception {
        flushNotifications(manualMonitor);
    }

    /*
     * The FileNotificationMbean method for notifyFileChanges will now
     * cache unrequested/unnotified file changes, so that they can be requested on
     * later calls. However, this means that certain unrequested options
     * can become invalid based on later filesystem changes.
     */

    // State change loop to track the status of changed files.
    // D = Delete, C = Create, M = modified, - = not in any list, X = not possible
    // Example, "CM = C" would mean create followed by a modify would
    // result in the file being in the create list
    // C = C
    // D = D
    // M = M
    // CC = X
    // CD = -   // testPersistedNotificationCaseCD()
    // CM = C   // testPersistedNotificationCaseCM()
    // DC = M   // testPersistedNotificationCaseCM()
    // DD = X
    // DM = X
    // MC = X
    // MD = D   // testPersistedNotificationCaseMD()
    // MM = M

    @Test
    public void testPersistedNotificationCaseCD() throws Throwable {

        File folder = new File(monitoredFolder, "testPersistedNotificationCaseCD");

        try {

            createFolder(folder);
            File f = new File(folder, "childFile1");
            createFile(f);

            Collection<String> fileSet = new HashSet<String>();
            fileSet.add(f.getAbsolutePath());
            Collection<File> created = new HashSet<File>();
            created.add(f);
            created.add(folder);

            mbean.notifyFileChanges(null, null, null); // Does not notify of a new file create
            assertNothingChanged(manualMonitor);
            deleteFile(folder); // Now remove this new file from the filesystem
            mbean.notifyFileChanges(fileSet, null, null); // Now ask to create the file, but its gone
            manualMonitor.scrapeLogsForChanges();
            assertNothingChanged(manualMonitor);

        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Test
    public void testPersistedNotificationCaseCM() throws Throwable {

        File folder = new File(monitoredFolder, "testPersistedNotificationCaseCM");

        try {

            createFolder(folder);
            File f = new File(folder, "childFile1");
            createFile(f);
            Collection<String> fileSet = new HashSet<String>();
            fileSet.add(f.getAbsolutePath());
            Collection<File> created = new HashSet<File>();
            created.add(f);
            created.add(folder);
            mbean.notifyFileChanges(null, null, null); // Does not notify of a new file create
            manualMonitor.scrapeLogsForChanges();
            assertNothingChanged(manualMonitor);
            appendSomething(f); // Modify this new file
            mbean.notifyFileChanges(null, fileSet, null); // Notify of a modify, but we didn't request create yet
            manualMonitor.scrapeLogsForChanges();
            assertNothingChanged(manualMonitor);
            mbean.notifyFileChanges(fileSet, null, null); // Notify of a create now, this is valid
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            assertCreated(manualMonitor, folder, f); // Should be shown as created now. 

        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPersistedNotificationCaseMD() throws Throwable {

        File folder = new File(monitoredFolder, "testPersistedNotificationCaseMD");

        try {

            createFolder(folder);
            File f = new File(folder, "childFile1");
            createFile(f);
            Collection<String> fileSet = new HashSet<String>();
            fileSet.add(f.getAbsolutePath());
            Collection<File> created = new HashSet<File>();
            created.add(f);
            created.add(folder);
            mbean.notifyFileChanges(fileSet, null, null); // Create a new file
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            assertCreated(manualMonitor, folder, f); // Should be shown as created now. 
            flushNotifications();
            appendSomething(f); // Modify this new file
            mbean.notifyFileChanges(null, null, null); // Don't notify of the modify
            manualMonitor.scrapeLogsForChanges();
            assertNothingChanged(manualMonitor);
            flushNotifications();
            deleteFile(f); // Remove the file that has a pending create being stored in monitorholder
            mbean.notifyFileChanges(fileSet, null, null); // Now try to notify of a create
            manualMonitor.scrapeLogsForChanges();
            assertNothingChanged(manualMonitor); // The create is no longer valid, so nothing should change
            mbean.notifyFileChanges(null, null, fileSet); // Now ask for the delete
            manualMonitor.scrapeLogsForExpectedChanges(null, null, created);
            assertDeleted(manualMonitor, f);

        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPersistedNotificationCaseDC() throws Throwable {

        File folder = new File(monitoredFolder, "testPersistedNotificationCaseDC");

        try {

            createFolder(folder);
            File f = new File(folder, "childFile1");
            createFile(f);
            // Sanity check - the recursive monitor should have noticed
            recursiveMonitor.scrapeLogsForChanges();
            assertCreated(recursiveMonitor, f, folder);
            Collection<String> fileSet = new HashSet<String>();
            fileSet.add(f.getAbsolutePath());
            Collection<File> created = new HashSet<File>();
            created.add(f);
            created.add(folder);
            mbean.notifyFileChanges(fileSet, null, null); // Create a new file
            manualMonitor.scrapeLogsForExpectedChanges(created, null, null);
            assertCreated(manualMonitor, folder, f); // Should be shown as created now. 
            flushNotifications();
            deleteFile(f); // Remove the file that has a pending create being stored in monitorholder
            mbean.notifyFileChanges(null, null, null); // Don't notify of the delete
            manualMonitor.scrapeLogsForChanges();
            assertNothingChanged(manualMonitor);
            flushNotifications();
            createFile(f); // Now recreate this file on the filesystem again
            // Now try to notify of a create, but the only valid option
            // is a modify, because we never notified of a delete
            mbean.notifyFileChanges(fileSet, null, null);
            manualMonitor.scrapeLogsForChanges();
            assertNothingChanged(manualMonitor); // Nothing should have changed
            // Let's ask for a delete too, this was once a valid option
            mbean.notifyFileChanges(null, null, fileSet);
            manualMonitor.scrapeLogsForChanges();
            assertNothingChanged(manualMonitor);
            flushNotifications();
            // Now notify for a modify, this is the only valid option
            mbean.notifyFileChanges(null, fileSet, null);
            manualMonitor.scrapeLogsForExpectedChanges(null, created, null);
            assertModified(manualMonitor, f);

        } catch (UndeclaredThrowableException e) {
            // Unwrap the exception (but rethrow, so we get a nice stack trace in the failure report)
            throw e.getCause();
        }
    }

    /**
     * @param manualMonitor
     */
    private void assertNothingChanged(MonitorReader manualMonitor) {
        assertNothingModified(manualMonitor);
        assertNothingCreated(manualMonitor);
        assertNothingDeleted(manualMonitor);
    }

}