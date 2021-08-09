/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cloudant.fat;

import static com.ibm.ws.cloudant.fat.FATSuite.cloudant;

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.AuthData;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.Cloudant;
import com.ibm.websphere.simplicity.config.CloudantDatabase;
import com.ibm.websphere.simplicity.config.Fileset;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CloudantModifyConfigTest extends FATServletClient {
    private static final String APP = "cloudantconfigmodfat";
    private static final Set<String> APP_NAMES = Collections.singleton(APP);
    private static final String[] APP_RECYCLE_LIST = new String[] { "CWWKZ0009I.*" + APP, "CWWKZ000[13]I.*" + APP };
    private static final String[] EMPTY_RECYCLE_LIST = new String[0];
    private static final String CLOUDANT_JNDI = "java:module/env/cloudant/builderRef";
    private static final String CLOUDANT_JNDI_MOD = "java:module/env/cloudant/builderModRef";
    private static final String DATABASE_JNDI = "java:comp/env/cloudant/cfgmoddbRef";
    private static final String DATABASE_JNDI_MOD = "java:comp/env/cloudant/cfgmoddbModRef";
    private static final String SERVLET_NAME = "CloudantConfigModTestServlet";

    private static String[] cleanupList = EMPTY_RECYCLE_LIST;

    private static String databaseName = "cloudantmoddb";

    private static ServerConfiguration originalConfig;

    @Server("com.ibm.ws.cloudant.fat.modifyconfig")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.addEnvVar("cloudant_url", cloudant.getURL(false));
        server.addEnvVar("cloudant_username", cloudant.getUser());
        server.addEnvVar("cloudant_password", cloudant.getPassword());
        server.addEnvVar("cloudant_databaseName", databaseName);

        originalConfig = server.getServerConfiguration();

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, APP, "configmod.web");

        // Create another application, to be used only as needed (not configured in server.xml by default)
        ShrinkHelper.defaultApp(server, "cloudantlookupapp", "configmod.web.lookup");
        server.removeInstalledAppForValidation("cloudantlookupapp");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0046W.*cloudantlookupapp"); // expected because this app sometimes doesn't have access to Cloudant classes
    }

    /**
     * After running each test, restore to the original configuration.
     */
    @After
    public void cleanUpPerTest() throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalConfig);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, cleanupList);
        cleanupList = EMPTY_RECYCLE_LIST;
        Log.info(getClass(), "cleanUpPerTest", "server configuration restored");
    }

    private void runTest(int position, String servletMethod, String jndiName, String... params) throws Exception {
        StringBuilder sb = new StringBuilder(servletMethod).append("&test=")
                        .append(testName.getMethodName())
                        .append('-')
                        .append(position)
                        .append("&jndiName=")
                        .append(jndiName)
                        .append("&databaseName=")
                        .append(databaseName);
        for (String s : params)
            sb.append('&').append(s);
        runTest(server, APP + '/' + SERVLET_NAME, sb.toString());
    }

    /**
     * Starting with a cloudant instance that doesn't configure a libraryRef, have an application provide
     * the Cloudant libraries. Verify that Cloudant database can be looked up.
     * Remove the library from the application. Verify that the Cloudant database can no longer be looked up
     * (verifies that the Cloudant client cache was cleared upon application stop).
     * Add the library back to the application and verify that it works again.
     */
    @ExpectedFFDC("java.lang.ClassNotFoundException") // when lookup fails due to missing libraries in app
    @Test
    public void testAddAndRemoveApplicationLibrary() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();

        Cloudant cloudant_builder = config.getCloudants().getById("builder");
        Cloudant cloudant_builderLoadFromApp = new Cloudant();
        cloudant_builderLoadFromApp.setId("builderLoadFromApp");
        cloudant_builderLoadFromApp.setUrl(cloudant_builder.getUrl());
        cloudant_builderLoadFromApp.setContainerAuthDataRef(cloudant_builder.getContainerAuthDataRef());
        cloudant_builderLoadFromApp.setExtraAttribute("ibm.internal.nonship.function", "true"); // TODO remove temporary
        Library noLibrary = new Library(); // TODO remove temporary
        noLibrary.setId("ibm.internal.simulate.no.library.do.not.ship"); // TODO remove temporary
        cloudant_builderLoadFromApp.getLibraries().add(noLibrary); // TODO remove temporary
        config.getCloudants().add(cloudant_builderLoadFromApp);

        CloudantDatabase cloudantDatabase_myCloudantDB = config.getCloudantDatabases().getById("myCloudantDB");
        CloudantDatabase cloudantDatabase_dbLoadFromApp = new CloudantDatabase();
        cloudantDatabase_dbLoadFromApp.setId("dbLoadFromApp");
        cloudantDatabase_dbLoadFromApp.setJndiName("cloudant/dbLoadFromApp");
        cloudantDatabase_dbLoadFromApp.setDatabaseName(cloudantDatabase_myCloudantDB.getDatabaseName());
        cloudantDatabase_dbLoadFromApp.setCreate("true");
        cloudantDatabase_dbLoadFromApp.setCloudantRef("builderLoadFromApp");
        config.getCloudantDatabases().add(cloudantDatabase_dbLoadFromApp);

        Application application_cloudantlookupapp = new Application();
        application_cloudantlookupapp.setId("cloudantlookupapp");
        application_cloudantlookupapp.setLocation("cloudantlookupapp.war");
        ClassloaderElement classloader_cloudantlookupapp = new ClassloaderElement();
        classloader_cloudantlookupapp.getCommonLibraryRefs().add("CloudantLib");
        application_cloudantlookupapp.getClassloaders().add(classloader_cloudantlookupapp);
        config.getApplications().add(application_cloudantlookupapp);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("cloudantlookupapp"), "CWWKZ000[13]I.*cloudantlookupapp");

        // use for first time
        runTest(server, "cloudantlookupapp/CloudantLookupTestServlet",
                "testDatabaseInsert&kingdom=Animalia&phylum=Chordata&class=Aves&order=Galliformes&family=Phasianidae&genus=Meleagris&species=gallopavo" +
                                                                       "&test=testAddAndRemoveApplicationLibrary1&&jndiName=java:app/env/cloudant/dbLoadFromApp");

        // reuse value from Cloudant Client cache
        runTest(server, "cloudantlookupapp/CloudantLookupTestServlet",
                "testDatabaseInsert&kingdom=Animalia&phylum=Chordata&class=Mammalia&order=Carnivora&family=Mustelidae&genus=Lontra&species=canadensis" +
                                                                       "&test=testAddAndRemoveApplicationLibrary2&&jndiName=java:app/env/cloudant/dbLoadFromApp");

        // remove classloader from application
        // classloader_cloudantlookupapp.getCommonLibraryRefs().clear(); // TODO this should have worked but simplicity is buggy in how it clears the list. Need an open-liberty update to fix, which requires an independent delivery.
        application_cloudantlookupapp.getClassloaders().clear();
        application_cloudantlookupapp.getClassloaders().add(classloader_cloudantlookupapp = new ClassloaderElement());

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("cloudantlookupapp"), "CWWKZ0009I.*cloudantlookupapp", "CWWKZ000[13]I.*cloudantlookupapp");

        runTest(server, "cloudantlookupapp/CloudantLookupTestServlet",
                "testLookupFailsClassNotFoundException&test=testAddAndRemoveApplicationLibrary3&jndiName=java:app/env/cloudant/dbLoadFromApp");

        // restore library to application
        classloader_cloudantlookupapp.getCommonLibraryRefs().add("CloudantLib");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("cloudantlookupapp"), "CWWKZ0009I.*cloudantlookupapp", "CWWKZ000[13]I.*cloudantlookupapp");

        runTest(server, "cloudantlookupapp/CloudantLookupTestServlet",
                "testDatabaseInsert&kingdom=Animalia&phylum=Chordata&class=Mammalia&order=Cetartiodactyla&family=Cervidae&genus=Cervus&species=canadensis" +
                                                                       "&test=testAddAndRemoveApplicationLibrary4&&jndiName=java:app/env/cloudant/dbLoadFromApp");

        // Verify database entries with the other Cloudant instance (which defines the library in the server)
        runTest(5, "testFindById", CLOUDANT_JNDI, "id=Meleagris+gallopavo", "kingdom=Animalia", "phylum=Chordata", "class=Aves", "order=Galliformes", "family=Phasianidae",
                "genus=Meleagris", "species=gallopavo");
        runTest(6, "testFindById", CLOUDANT_JNDI, "id=Lontra+canadensis", "kingdom=Animalia", "phylum=Chordata", "class=Mammalia", "order=Carnivora", "family=Mustelidae",
                "genus=Lontra", "species=canadensis");
        runTest(7, "testFindById", CLOUDANT_JNDI, "id=Cervus+canadensis", "kingdom=Animalia", "phylum=Chordata", "class=Mammalia", "order=Cetartiodactyla", "family=Cervidae",
                "genus=Cervus", "species=canadensis");

        // restoring original configuration will remove the new application
        cleanupList = new String[] { "CWWKZ0009I.*cloudantlookupapp" };
    }

    /**
     * Update authData configuration while the server is running, and expect cloudant configuration that
     * references it to honor the update.
     */
    @Test
    public void testModifyAuthData() throws Exception {
        runTest(1, "testInsert", CLOUDANT_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Aves", "order=Gaviiformes", "family=Gaviidae", "genus=Gavia", "species=immer");

        // put an invalid password in the authData
        ServerConfiguration config = server.getServerConfiguration();
        AuthData cloudantAuth = config.getAuthDataElements().getById("cloudantAuth");
        String password = cloudantAuth.getPassword();
        cloudantAuth.setPassword("not-a-valid-password");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(2, "testIncorrectPassword", CLOUDANT_JNDI);

        // correct the authData password
        cloudantAuth.setPassword(password);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(3, "testContainsId", CLOUDANT_JNDI, "id=Gavia+immer");

        cleanupList = EMPTY_RECYCLE_LIST;
    }

    /**
     * Update databaseName of cloudantDatabase configuration while the server is running,
     * and expect new lookups to attempt to use the new database.
     */
    @Test
    public void testModifyDatabaseName() throws Exception {
        runTest(1, "testInsert", DATABASE_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Archosauria", "order=Ornithischia", "family=Ankylosauridae", "genus=Ankylosaurus",
                "species=magniventris");

        // change the database name
        ServerConfiguration config = server.getServerConfiguration();
        CloudantDatabase myCloudantDB = config.getCloudantDatabases().getById("myCloudantDB");
        String dbName = myCloudantDB.getDatabaseName();
        myCloudantDB.setDatabaseName("does-not-exist");
        myCloudantDB.setCreate("false");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(2, "testDatabaseNotFound", DATABASE_JNDI);

        // change it back
        myCloudantDB.setDatabaseName(dbName);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(3, "testContainsId", DATABASE_JNDI, "id=Ankylosaurus+magniventris");

        cleanupList = APP_RECYCLE_LIST;
    }

    /**
     * Modify the jndiName attribute of a cloudantDatabase instance while the server is running.
     * Verify that it can be looked up as the new name and no longer as the old name.
     * Then create a new cloudantDatabase config with the original name, and verify both can be looked up.
     */
    @Test
    public void testModifyDatabaseJNDIName() throws Exception {
        runTest(1, "testInsert", DATABASE_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Archosauria", "order=Ornithischia", "family=Ceratopsidae", "genus=Triceratops",
                "species=horridus");

        // change the JNDI name
        ServerConfiguration config = server.getServerConfiguration();
        CloudantDatabase myCloudantDB = config.getCloudantDatabases().getById("myCloudantDB");
        String jndiName = myCloudantDB.getJndiName();
        myCloudantDB.setJndiName("cloudant/dbmodified");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(2, "testContainsId", DATABASE_JNDI_MOD, "id=Triceratops+horridus");
        runTest(3, "testNameNotFound", DATABASE_JNDI);

        // new instance with the original jndi name, after which both should work
        CloudantDatabase newCloudantDB = (CloudantDatabase) myCloudantDB.clone();
        newCloudantDB.setId("newCloudantDB");
        newCloudantDB.setJndiName(jndiName);
        config.getCloudantDatabases().add(newCloudantDB);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        runTest(4, "testInsert", DATABASE_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Archosauria", "order=Saurischia", "family=Diplodocidae", "genus=Apatosaurus",
                "species=ajax");
        runTest(5, "testContainsId", DATABASE_JNDI_MOD, "id=Apatosaurus+ajax");

        // delete the instance with id=myCloudantDB, after which only the instance with the original jndi name should work
        config.getCloudantDatabases().removeById("myCloudantDB");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(6, "testNameNotFound", DATABASE_JNDI_MOD);
        runTest(7, "testContainsId", DATABASE_JNDI, "id=Apatosaurus+ajax");

        cleanupList = APP_RECYCLE_LIST;
    }

    /**
     * Modify the jndiName attribute of a cloudant instance while the server is running.
     * Verify that it can be looked up as the new name and no longer as the old name.
     * Then create a new cloudant config with the original name, and verify both can be looked up.
     */
    @Test
    public void testModifyJNDIName() throws Exception {
        runTest(1, "testInsert", CLOUDANT_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Mammalia", "order=Primates", "family=Hominidae", "genus=Gorilla", "species=beringei");

        // change the JNDI name
        ServerConfiguration config = server.getServerConfiguration();
        Cloudant builder = config.getCloudants().getById("builder");
        String jndiName = builder.getJndiName();
        builder.setJndiName("cloudant/modified");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(2, "testContainsId", CLOUDANT_JNDI_MOD, "id=Gorilla+beringei");
        runTest(3, "testNameNotFound", CLOUDANT_JNDI);

        // new instance with the original jndi name, after which both should work
        Cloudant newBuilder = (Cloudant) builder.clone();
        builder.setId("newBuilder");
        builder.setJndiName(jndiName);
        config.getCloudants().add(newBuilder);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, EMPTY_RECYCLE_LIST);

        runTest(4, "testInsert", CLOUDANT_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Mammalia", "order=Rodentia", "family=Erethizontidae", "genus=Erethizon",
                "species=dorsatum");
        runTest(5, "testContainsId", CLOUDANT_JNDI_MOD, "id=Erethizon+dorsatum");

        // delete the instance with id=builder, after which only the instance with the original jndi name should work
        config.getCloudants().removeById("builder");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(6, "testNameNotFound", CLOUDANT_JNDI_MOD);
        runTest(7, "testInsert", CLOUDANT_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Mammalia", "order=Artiodactyla", "family=Bovidae",
                "genus=Ovis", "species=aries");

        cleanupList = APP_RECYCLE_LIST;
    }

    /**
     * Update library used by cloudant, once as a config change and another time as a filesystem change.
     */
    @Test
    @ExpectedFFDC("java.lang.NoClassDefFoundError")
    public void testModifyLibrary() throws Exception {
        runTest(1, "testInsert", CLOUDANT_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Mammalia", "order=Artiodactyla", "family=Cervidae", "genus=Rangifer",
                "species=tarandus");

        // Copy 3 of the cloudant jars to a new location and point a new library at that location
        RemoteFile sourceFolder = server.getFileFromLibertySharedDir("resources/cloudant-2.16");
        RemoteFile[] sourceFiles = sourceFolder.list(false);
        RemoteFile gsonSource = null;
        RemoteFile gsonDestination = null;
        for (int i = 0; i < sourceFiles.length; i++) {
            String fileName = sourceFiles[i].getName();
            RemoteFile destination = new RemoteFile(sourceFiles[i].getMachine(), server.getServerRoot() + "/copylib/" + fileName);

            if (fileName.startsWith("gson")) {
                gsonSource = sourceFiles[i];
                gsonDestination = destination;
            } else {
                boolean copied = sourceFiles[i].copyToDest(destination);
                System.out.println("Copied cloudant binary file to: " + destination.toString() + "? " + copied);
            }
        }

        // Make a new library config element for the partial set of cloudant jars
        ServerConfiguration config = server.getServerConfiguration();
        Fileset copyLib_fileset = new Fileset();
        copyLib_fileset.setDir("${server.config.dir}/copylib");
        copyLib_fileset.setScanInterval("5s");
        Library copyLib = new Library();
        copyLib.setId("copyLib");
        copyLib.setNestedFileset(copyLib_fileset);
        config.getLibraries().add(copyLib);
        // Only the cloudant instance will point at the new library, so as to avoid having library change listener for app interfere with the test
        config.getCloudants().getById("builder").setLibraryRef("copyLib");

        // Save and verify the application gets an error when looking up the ClientBuilder instance
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(2, "testMissingLibraryJAR", CLOUDANT_JNDI);

        // Copy over the remaining library - should be detected by the library change listener,
        // after which the cloudant instance should be come usable
        server.setMarkToEndOfLog();
        boolean copied = gsonSource.copyToDest(gsonDestination);
        System.out.println("Copied cloudant gson jar to: " + gsonDestination.toString() + "? " + copied);
        server.waitForStringInLogUsingMark("CWWKZ000[13]I.*" + APP);

        runTest(3, "testReflectiveContainsId", DATABASE_JNDI, "id=Rangifer+tarandus");

        cleanupList = APP_RECYCLE_LIST;
    }

    /**
     * Update cloudant server configuration while the server is running to switch back and forth between
     * containerAuthData, no authentication, and user/password.
     */
    @Test
    public void testSwitchBetweenAuthDataAndUserPassword() throws Exception {
        // configuration starts out with containerAuthData but without user/password
        runTest(1, "testInsert", CLOUDANT_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Mammalia", "order=Proboscidea", "family=Elephantidae", "genus=Mammuthus",
                "species=primigenius");

        // remove containerAuthData, expect an error
        ServerConfiguration config = server.getServerConfiguration();
        Cloudant builder = config.getCloudants().getById("builder");
        builder.setContainerAuthDataRef(null);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(2, "testNoAuthentication", CLOUDANT_JNDI);

        // add user and password
        AuthData cloudantAuth = config.getAuthDataElements().getById("cloudantAuth");
        String user = cloudantAuth.getUser();
        String password = cloudantAuth.getPassword();
        builder.setUsername(user);
        builder.setPassword(password);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(3, "testContainsId", CLOUDANT_JNDI, "id=Mammuthus+primigenius");

        // add nested containerAuthData with bad user/password - should override the username/password attributes
        AuthData builder_containerAuthData = new AuthData();
        builder_containerAuthData.setUser("nobody");
        builder_containerAuthData.setPassword("unknown");
        builder.getContainerAuthDatas().add(builder_containerAuthData);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(4, "testInvalidUser", CLOUDANT_JNDI);

        // correct the user/password in the nested containerAuthData
        builder_containerAuthData.setUser(user);
        builder_containerAuthData.setPassword(password);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(5, "testInsert", CLOUDANT_JNDI, "kingdom=Animalia", "phylum=Chordata", "class=Mammalia", "order=Carnivora", "family=Ursidae", "genus=Ursus", "species=arctos");

        // remove the user/password from cloudant. They aren't needed for container auth when there is a nested containerAuthData
        builder.setUsername(null);
        builder.setPassword(null);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(APP_NAMES, APP_RECYCLE_LIST);

        runTest(6, "testFindById", CLOUDANT_JNDI, "id=Ursus+arctos", "kingdom=Animalia", "phylum=Chordata", "class=Mammalia", "order=Carnivora", "family=Ursidae", "genus=Ursus",
                "species=arctos");

        cleanupList = APP_RECYCLE_LIST;
    }
}
