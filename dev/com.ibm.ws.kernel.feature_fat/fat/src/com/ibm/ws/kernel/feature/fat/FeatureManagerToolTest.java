/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.osgi.util.ManifestElement;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.LocalMachine;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LibertyServerUtils;

@Mode(TestMode.FULL)
public class FeatureManagerToolTest extends FeatureToolTestCommon {
    /**  */
    private static final String ZOS_EDITION = "zOS";
    /**  */
    private static final String INSTALLATION_MANAGER = "InstallationManager";
    /**  */
    private static final String PRODUCT_INSTALL_TYPE = "productInstallType";

    private static final String PRODUCT_EDITION = "productEdition";

    private static final String PRODUCT_VERSION = "productVersion";

    /**  */
    private static final String IBM_APPLIES_TO_HEADER = "IBM-AppliesTo";
    /**  */
    private static final String SUBSYSTEM_MANIFEST_FILE = "OSGI-INF/SUBSYSTEM.MF";
    private static final int ReturnCode_OK = 0;
    private static final int ReturnCode_BAD_ARGUMENT = 20;

    private static final Class<?> c = FeatureManagerToolTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.user");

    private static final String installFeatureMsgPrefix = "CWWKF0012I:";

    private final Collection<String> filesToTidy = new HashSet<String>();

    /**
     * This method removes all the testing artifacts from the server directories.
     *
     * @throws Exception
     */
    @After
    public void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";

        Log.entering(c, METHOD_NAME);

        if (server.isStarted())
            server.stopServer();

        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
        server.deleteDirectoryFromLibertyInstallRoot("producttest");
        server.deleteDirectoryFromLibertyInstallRoot("etc/extensions");
        server.deleteDirectoryFromLibertyInstallRoot("tool.output.dir");

        for (String filePath : filesToTidy) {
            server.deleteFileFromLibertyInstallRoot(filePath);
        }
        filesToTidy.clear();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a user feature is installed when added to server.xml.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testFeatureInstall() throws Exception {
        final String METHOD_NAME = "testFeatureInstall";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest");

        Log.info(c, METHOD_NAME, po.getStdout());
        Log.info(c, METHOD_NAME, po.getStderr());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        // Start the server xml with a user feature available
        server.setServerConfigurationFile("server_no_features.xml");
        server.startServer(METHOD_NAME + ".log");

        TestUtils.makeConfigUpdateSetMark(server, "server_user_features.xml");

        // Get the install feature message for the added user feature.
        String output = server.waitForStringInLogUsingMark(installFeatureMsgPrefix);
        assertNotNull("We haven't found the " + installFeatureMsgPrefix + " in the logs.", output);

        assertTrue("usertest user feature was not installed and should have been: " + output, output.contains("usertest"));

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.mf"));
        assertTrue("The usertest bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a user feature is installed when added to server.xml.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallToProductExtension() throws Exception {
        final String METHOD_NAME = "testFeatureInstall";

        Log.entering(c, METHOD_NAME);

        server.copyFileToLibertyInstallRoot(PRODUCT_EXTENSIONS_PATH, PRODUCT_FEATURE_PROPERTIES_FILE);

        ProgramOutput po = server.installFeature("testproduct", "usertest");

        Log.info(c, METHOD_NAME, po.getStdout());
        Log.info(c, METHOD_NAME, po.getStderr());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("producttest/lib/features/usertest.mf"));
        assertTrue("The usertest bundle should exist.", server.fileExistsInLibertyInstallRoot("producttest/lib/usertest_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testFeatureInstallFromURL() throws Exception {
        final String METHOD_NAME = "testFeatureInstallFromURL";

        Log.entering(c, METHOD_NAME);

        LibertyServer installServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.install");

        try {

            installServer.addInstalledAppForValidation("feature");
            installServer.startServer(METHOD_NAME + ".log");

            String url = "http://localhost:" + installServer.getHttpDefaultPort() + "/feature/usertest.esa";

            ProgramOutput po = installServer.getMachine().execute(installServer.getInstallRoot() + "/bin/featureManager", new String[] { "install", "--to=usr", url },
                                                                  installServer.getInstallRoot());

            assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        } finally {
            if (installServer.isStarted())
                installServer.stopServer();

            installServer.deleteDirectoryFromLibertyInstallRoot("usr/extension/");

            Log.exiting(c, METHOD_NAME);
        }
    }

    /**
     * TestDescription:
     * This test ensures that you can't install a user feature if it already exists.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallTwice() throws Exception {
        final String METHOD_NAME = "testFeatureInstallTwice";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest");

        Log.info(c, METHOD_NAME, po.getStdout());
        Log.info(c, METHOD_NAME, po.getStderr());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        po = server.installFeature(null, "usertest");

        String stdout = po.getStdout();
        Log.info(c, METHOD_NAME, stdout);

        assertTrue("The feature should not have been installed. " + stdout, stdout.contains("CWWKF1000I"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can't install a feature with a missing bundle.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallMissingBundle() throws Exception {
        final String METHOD_NAME = "testFeatureInstallMissingBundle";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.missing.bundle");

        String stdout = po.getStdout();
        Log.info(c, METHOD_NAME, stdout);

        assertTrue("The feature should not have been installed. stdout:\r\n" + stdout, stdout.contains("CWWKF1013E"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can't install a user feature if it has a file missing.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallMissingFile() throws Exception {
        final String METHOD_NAME = "testFeatureInstallMissingFile";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.missing.file");

        String stdout = po.getStdout();
        Log.info(c, METHOD_NAME, stdout);

        assertTrue("The feature should not have been installed. stdout:\r\n" + stdout, stdout.contains("CWWKF1012E"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can't install a user feature if it does not have a subsystem manifest.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallMissingSubsystemManifest() throws Exception {
        final String METHOD_NAME = "testFeatureInstallMissingSubsystemManifest";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.no.subsystem.manifest");

        String stdout = po.getStdout();
        Log.info(c, METHOD_NAME, stdout);

        assertTrue("The feature should not have been installed. stdout:\r\n" + stdout, stdout.contains("CWWKF1022E"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can install a user feature with static files.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallWithFile() throws Exception {
        final String METHOD_NAME = "testFeatureInstallWithFile";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.file");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can install a user feature that relocates a jar from lib.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallWithAlternativeLocation() throws Exception {
        final String METHOD_NAME = "testFeatureInstallWithAlternativeLocation";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.api");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        assertTrue("The english translations were not installed", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/l10n/usertest.with.api.properties"));
        assertTrue("The french translations were not installed", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/l10n/usertest.with.api_fr.properties"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can install a user feature that has a dependency to another feature in the same install location
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallWithDependency() throws Exception {
        final String METHOD_NAME = "testFeatureInstallWithDependency";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.dependency", "usertest.dependency");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.mf"));
        assertTrue("The usertest bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest_1.0.0.jar"));

        assertTrue("The usertest dependency feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.dependency.mf"));
        assertTrue("The usertest dependency bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest.dependency_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can install a user feature that has a dependency to another feature in the same install location that has a duplicate bundle in both features.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallWithDependencyAndDuplicateBundle() throws Exception {
        final String METHOD_NAME = "testFeatureInstallWithDependencyAndDuplicateBundle";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.dependency.duplicate.bundle", "usertest.dependency.duplicate.bundle");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        assertTrue("The usertest with dependency feature manifest should exist.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.with.dependency.duplicate.bundle.mf"));
        assertTrue("The usertest bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest.duplicate_1.0.0.jar"));
        assertTrue("The usertest readme file should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/docs/readme.txt"));

        assertTrue("The usertest dependency feature manifest should exist.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.dependency.duplicate.bundle.mf"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can install a user feature that has a dependency to another feature in the same install location from a URL
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallWithDependencyFromURL() throws Exception {
        final String METHOD_NAME = "testFeatureInstallWithDependency";

        Log.entering(c, METHOD_NAME);

        // Our dependencies will come from a Liberty server so grab this
        LibertyServer esaProviderServer = null;
        try {
            esaProviderServer = LibertyServerFactory.getStartedLibertyServer("com.ibm.ws.kernel.feature.esa.provider");
            String url = "http://" + esaProviderServer.getHostname() + ":" + esaProviderServer.getHttpDefaultPort() + "/esas/usertest.with.dependency.esa";

            ProgramOutput po = server.getMachine().execute(server.getInstallRoot() + "/bin/featureManager", new String[] { "install", "--to=usr", url }, server.getInstallRoot());

            Log.info(c, METHOD_NAME, po.getStdout());

            assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

            assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.mf"));
            assertTrue("The usertest bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest_1.0.0.jar"));

            assertTrue("The usertest dependency feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.dependency.mf"));
            assertTrue("The usertest dependency bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest.dependency_1.0.0.jar"));

            Log.exiting(c, METHOD_NAME);
        } finally {
            if (esaProviderServer != null && esaProviderServer.isStarted()) {
                esaProviderServer.stopServer();
            }
        }
    }

    /**
     * TestDescription:
     * This test ensures that you can't install a user feature that has a dependency to another feature that does not exist
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallWithMissingDependency() throws Exception {
        final String METHOD_NAME = "testFeatureInstallWithDependency";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.missing.dependency");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertTrue("The feature should not have been installed and should have reported a missing file. stdout:\r\n" + po.getStdout(), po.getStdout().contains("CWWKF1009E"));
        assertTrue("The feature should not have been installed and should have reported a missing dependency. stdout:\r\n" + po.getStdout(), po.getStdout().contains("CWWKF1011E"));

        assertFalse("The usertest feature manifest should not exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.mf"));
        assertFalse("The usertest bundle should not exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can install a user feature that has a dependency to another feature that does exist and a second feature that does not. When the install fails
     * (which it should) everything should be rolled back. Sadly the dependencies are stored in a map so we can't guarantee that the missing dependency will be loaded second so
     * this test will only intermittently work!
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallRollbackWithMissingDependency() throws Exception {
        final String METHOD_NAME = "testFeatureInstallWithDependency";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.multiple.dependencies", "usertest.dependency");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertTrue("The feature should not have been installed and should have reported a missing file. stdout:\r\n" + po.getStdout(), po.getStdout().contains("CWWKF1009E"));
        assertTrue("The feature should not have been installed and should have reported a missing dependency. stdout:\r\n" + po.getStdout(), po.getStdout().contains("CWWKF1011E"));

        assertFalse("The usertest feature manifest should not exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.mf"));
        assertFalse("The usertest bundle should not exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest_1.0.0.jar"));

        assertFalse("The usertest dependency feature manifest should not exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.dependency.mf"));
        assertFalse("The usertest dependency bundle should not exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest.dependency_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a user feature that has the IBM-InstallTo and IBM-ShortName headers is installed and goes to the core.
     *
     * @throws Exception
     */
    @Test
    public void testIBMFeatureInstall() throws Exception {
        final String METHOD_NAME = "testFeatureInstall";

        Log.entering(c, METHOD_NAME);

        // This is adding files to core so can't just delete whole directories... record files added
        filesToTidy.add("lib/features/usertest.install.to.core.mf");
        filesToTidy.add("lib/usertest_1.0.0.jar");

        // Just in case the short name is used incorrectly
        filesToTidy.add("lib/features/ut.mf");

        ProgramOutput po = server.installFeature(null, "usertest.install.to.core");

        Log.info(c, METHOD_NAME, po.getStdout());
        Log.info(c, METHOD_NAME, po.getStderr());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        // The feature file now uses the full symbolic name rather than the short name
        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("lib/features/usertest.install.to.core.mf"));
        assertTrue("The usertest bundle should exist.", server.fileExistsInLibertyInstallRoot("lib/usertest_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test is the same as testIBMFeatureInstall but passes the --to=usr option to ensures that a user feature that has the IBM-InstallTo and IBM-ShortName headers override
     * the command line option.
     *
     * @throws Exception
     */
    @Test
    public void testIBMFeatureInstallWithToUser() throws Exception {
        final String METHOD_NAME = "testFeatureInstall";

        Log.entering(c, METHOD_NAME);

        // This is adding files to core so can't just delete whole directories... record files added
        filesToTidy.add("lib/features/usertest.install.to.core.mf");
        filesToTidy.add("lib/usertest_1.0.0.jar");

        // Just in case the short name is used incorrectly
        filesToTidy.add("lib/features/ut.mf");

        ProgramOutput po = server.installFeature("usr", "usertest.install.to.core");

        Log.info(c, METHOD_NAME, po.getStdout());
        Log.info(c, METHOD_NAME, po.getStderr());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        // The feature file now uses the full symbolic name rather than the short name
        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("lib/features/usertest.install.to.core.mf"));
        assertTrue("The usertest bundle should exist.", server.fileExistsInLibertyInstallRoot("lib/usertest_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that when there are two subsystem manifests with different case then an appropriate warning is issued.
     *
     * @throws Exception
     */
    @Test
    public void testTwoSubsystemManifests() throws Exception {
        final String METHOD_NAME = "testFeatureInstall";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.two.manifests");

        Log.info(c, METHOD_NAME, po.getStdout());
        Log.info(c, METHOD_NAME, po.getStderr());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("There should have been a warning message saying there were two manifests. stdout:\r\n" + po.getStdout(), po.getStdout().contains("CWWKF1023W"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can install a user feature that has a dependency to another feature that already exists
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallWithDependencyToExisting() throws Exception {
        final String METHOD_NAME = "testFeatureInstallWithDependency";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.depends.on.existing");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.depends.on.existing.mf"));
        assertTrue("The usertest bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can install features with cyclic dependencies
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallWithCyclicDependency() throws Exception {
        final String METHOD_NAME = "testFeatureInstallWithDependency";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.cyclic.1", "usertest.cyclic.2");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        assertTrue("The usertest.cyclic.1 feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.cyclic.1.mf"));
        assertTrue("The usertest.cyclic.2 feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.cyclic.2.mf"));
        assertTrue("The usertest.cyclic.1 bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest.cyclic.1_1.0.0.jar"));
        assertTrue("The usertest.cyclic.2 bundle should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest.cyclic.2_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test makes sure that an IBM license is shown to the user when present
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSimpleIBMLicenseStatement() throws Exception {
        final String METHOD_NAME = "testSimpleIBMLicenseStatement";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.ibm.license.same");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        // Just make sure the license text is printed, we don't do interactive install so this shows it was read
        assertTrue("The license should have been printed in the output:\r\n" + po.getStdout(), po.getStdout().contains("MainLicense"));

        // Make sure the license is copied in as well
        assertTrue("The english license file should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lafiles/usertest.with.ibm.license.same/lafiles/LA_en"));
        assertTrue("The Taiwanese license information file should exist.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lafiles/usertest.with.ibm.license.same/lafiles/LI_zh_TW"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test makes sure that where multiple ESAs are being installed all the IBM licenses are shown once and only once to the user when present
     *
     * @throws Exception
     */
    @Test
    public void testMultipleIBMLicenseStatement() throws Exception {
        final String METHOD_NAME = "testMultipleIBMLicenseStatement";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.ibm.license", "usertest.with.ibm.license.same", "usertest.with.ibm.license.different");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        // Just make sure the license text is printed, we don't do interactive install so this shows it was read
        assertTrue("The license should have been printed in the output once:\r\n" + po.getStdout(), po.getStdout().contains("MainLicense"));
        assertTrue("The license should have been printed in the output once:\r\n" + po.getStdout(),
                   po.getStdout().indexOf("MainLicense") == po.getStdout().lastIndexOf("MainLicense"));
        assertTrue("The second license should have been printed in the output:\r\n" + po.getStdout(), po.getStdout().contains("MainLicense"));

        /*
         * Make sure the licenses are copied in as well. Make sure the licenses for both where it is the same is copied so the user can see the license for each without having to
         * know the relationship between them
         */
        assertTrue("The english license file should exist for the root ESA.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lafiles/usertest.with.ibm.license/lafiles/LA_en"));
        assertTrue("The Taiwanese license information file should exist for the root ESA.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lafiles/usertest.with.ibm.license/lafiles/LI_zh_TW"));

        assertTrue("The english license file should exist for the dependent ESA with the same license.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lafiles/usertest.with.ibm.license.same/lafiles/LA_en"));
        assertTrue("The Taiwanese license information file should exist for the dependent ESA with the same license.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lafiles/usertest.with.ibm.license.same/lafiles/LI_zh_TW"));

        assertTrue("The english license file should exist for the dependent ESA with a different license.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lafiles/usertest.with.ibm.license.different/lafiles/LA_en"));
        assertTrue("The Taiwanese license information file should exist for the dependent ESA with a different license.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lafiles/usertest.with.ibm.license.different/lafiles/LI_zh_TW"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test makes sure that where a license is already on disk then we don't ask the user to accept it again
     *
     * @throws Exception
     */
    @Test
    public void testAcceptedLicenseStatement() throws Exception {
        final String METHOD_NAME = "testAcceptedLicenseStatement";

        Log.entering(c, METHOD_NAME);

        // First install the version with the same license
        ProgramOutput po = server.installFeature(null, "usertest.with.ibm.license.same");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        po = server.installFeature(null, "usertest.with.ibm.license", "usertest.with.ibm.license.different");
        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        // Make sure we were never asked for the main license
        assertFalse("The license was already on disk so shouldn't need accepting again:\r\n" + po.getStdout(), po.getStdout().contains("MainLicense"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test makes sure that third party licenses are shown to users
     *
     * @throws Exception
     */
    @Test
    public void testThirdPartyLicense() throws Exception {
        final String METHOD_NAME = "testThirdPartyLicense";

        Log.entering(c, METHOD_NAME);

        // First install the version with the same license
        ProgramOutput po = server.installFeature(null, "usertest.third.party.license");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        // Make sure we were never asked for the main license
        assertTrue("The license was not presented to the user:\r:\n", po.getStdout().contains("ThirdPartyLicense"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test makes sure that the viewLicenseInfo option works
     *
     * @throws Exception
     */
    @Test
    public void testViewLicenseInfo() throws Exception {
        final String METHOD_NAME = "testViewLicenseInfo";

        Log.entering(c, METHOD_NAME);

        // First install the version with the same license
        ProgramOutput po = server.installFeatureWithProgramArgs(null, "usertest.third.party.license", new String[] { "--viewLicenseInfo" });

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The viewLicense should have exited ok. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        // Make sure the license was printed
        assertTrue("The license should have been presented to the user:\r:\n" + po.getStdout(), po.getStdout().contains("ThirdPartyLicense"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test makes sure that the viewLicenseAgreement option works
     *
     * @throws Exception
     */
    @Test
    public void testViewLicenseAgreement() throws Exception {
        final String METHOD_NAME = "testViewLicenseAgreement";

        Log.entering(c, METHOD_NAME);

        // First install the version with the same license
        ProgramOutput po = server.installFeatureWithProgramArgs(null, "usertest.third.party.license", new String[] { "--viewLicenseAgreement" });

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The viewLicense should have exited ok. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        // Make sure the license was printed
        assertTrue("The license should have been presented to the user:\r:\n" + po.getStdout(), po.getStdout().contains("ThirdPartyLicense"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test makes sure that the viewLicenseAgreement option works for dependencies
     *
     * @throws Exception
     */
    @Test
    public void testViewLicenseAgreementWithDependencies() throws Exception {
        final String METHOD_NAME = "testViewLicenseAgreementWithDependencies";

        Log.entering(c, METHOD_NAME);

        // First install the version with the same license
        ProgramOutput po = server.installFeatureWithProgramArgs(null, "usertest.with.ibm.license", new String[] { "--viewLicenseAgreement" }, "usertest.with.ibm.license.same",
                                                                "usertest.with.ibm.license.different");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The viewLicense should have exited ok. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        // Make sure both licenses were printed
        assertTrue("The first license should have been presented to the user:\r:\n" + po.getStdout(), po.getStdout().contains("MainLicense"));
        assertTrue("The second license should have been presented to the user:\r:\n" + po.getStdout(), po.getStdout().contains("DifferentLicense"));

        // Make sure the main one was only shown once
        assertTrue("The license should have been printed in the output once:\r\n" + po.getStdout(),
                   po.getStdout().indexOf("MainLicense") == po.getStdout().lastIndexOf("MainLicense"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that the featureList action generates a feature list.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testFeatureList() throws Exception {
        final String METHOD_NAME = "testFeatureList";
        Log.entering(c, METHOD_NAME);

        setupEnv(server);

        ProgramOutput po = server.installFeature(null, "usertest.with.api");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        po = server.getMachine().execute(installRoot + "/bin/featureManager",
                                         new String[] { "featureList", "--productExtension=usr", installRoot + "/tool.output.dir/featurelist.xml" },
                                         installRoot);
        logInfo(po, "tool.output.dir/featurelist.xml");

        RemoteFile rf = server.getFileFromLibertyInstallRoot("tool.output.dir/featurelist.xml");
        InputStream in = rf.openForReading();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(in);
        NodeList nl = doc.getElementsByTagName("feature");

        boolean found = false;
        boolean foundApiJar = false;
        boolean foundDisplayName = false;
        boolean foundDescription = false;
        boolean foundCategory1 = false;
        boolean foundCategory2 = false;

        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            if ("usertest.with.api".equals(e.getAttribute("name"))) {
                found = true;
                NodeList nl2 = e.getChildNodes();
                for (int j = 0; j < nl2.getLength(); j++) {
                    if (nl2.item(j) instanceof Element) {
                        e = (Element) nl2.item(j);
                        if ("apiJar".equals(e.getNodeName())) {
                            foundApiJar = true;
                            assertEquals("The api jar name is incorrect", "usr/extension/dev/api/usertest_1.0.0.jar", e.getTextContent().trim());
                        } else if ("displayName".equals(e.getNodeName())) {
                            foundDisplayName = true;
                            assertEquals("The feature name is incorrect", "Test feature name", e.getTextContent().trim());
                        } else if ("description".equals(e.getNodeName())) {
                            foundDescription = true;
                            assertEquals("The feature description is incorrect", "Test feature description", e.getTextContent().trim());
                        } else if ("category".equals(e.getNodeName())) {
                            String catName = e.getTextContent();
                            if ("category1".equals(catName)) {
                                foundCategory1 = true;
                            } else if ("category2".equals(catName)) {
                                foundCategory2 = true;
                            }
                        }
                    }
                }
            }
        }

        assertTrue("The user feature should be in the feature list", found);
        assertTrue("The feature's api jar was not found", foundApiJar);
        assertTrue("The feature's name was not found", foundDisplayName);
        assertTrue("The feature's description was not found", foundDescription);
        assertTrue("The feature's category (category1) was not found", foundCategory1);
        assertTrue("The feature's category (category2) was not found", foundCategory2);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the featureManager featureList action help displays the --productExtension option.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureMgrToolProductExtensionParmHelpDisplay() throws Exception {
        setupEnv(server);
        testFeatureToolProductExtensionParmHelpDisplay(installRoot + "/bin/featureManager", new String[] { "help", "featureList" }, installRoot);
    }

    /**
     * Test featureManger featureList with --productExtension=testproduct where the features folder for the product extension is empty.
     * The request is expected to fail and issue message: CWWKF1019E.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureMgrToolUsingProductWithNoFeatureMfs() throws Exception {
        setupEnv(server);
        setupProductExtensions(SETUP_PROD_EXT);
        testFeatureToolUsingProductWithNoFeatureMfs(installRoot + "/bin/featureManager",
                                                    new String[] { "featureList", "--productExtension=testproduct",
                                                                   installRoot + "/tool.output.dir/prodExtFeaturelistNoFeatures.xml" },
                                                    installRoot,
                                                    "CWWKF1019E");
    }

    /**
     * Test featureManger featureList with a product extension name argument pointing to a product that does not exist: --productExtension=testproductbadName.
     * The request is expected to fail and issue message: CWWKF1021E.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureMgrToolUsingBadProductExtNameArgument() throws Exception {
        setupEnv(server);
        setupProductExtensions(SETUP_PROD_EXT);
        testFeatureToolUsingBadProductExtNameArgument(installRoot + "/bin/featureManager",
                                                      new String[] { "featureList", "--productExtension=testproductbadName",
                                                                     installRoot + "/tool.output.dir/prodExtFeaturelistBadNameArg.xml" },
                                                      installRoot,
                                                      "CWWKF1021E");
    }

    /**
     * Test featureManger featureList with --productExtension=testproduct where the com.ibm.websphere.productInstall in the
     * product extension's properties file points to "".
     * The request is expected to fail and issue message: CWWKF1020E.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureMgrToolUsingEmptyInstallLocationInProdExtPropsFile() throws Exception {
        setupEnv(server);
        setupProductExtensions(SETUP_PROD_EXT);
        testFeatureToolUsingEmptyInstallLocationInProdExtPropsFile(installRoot + "/bin/featureManager",
                                                                   new String[] { "featureList", "--productExtension=testproduct",
                                                                                  installRoot + "/tool.output.dir/prodExtFeaturelistWithInvalidInstLocInPropsFile.xml" },
                                                                   installRoot,
                                                                   "CWWKF1020E");
    }

    /**
     * Test featureManger featureList without the --productExtension argument.
     * Only Core features expected in the output list.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureMgrToolWithNoProdExtArgument() throws Exception {
        setupEnv(server);
        setupProductExtensions(SETUP_PROD_EXT);
        testFeatureToolWithNoProdExtArgument(installRoot + "/bin/featureManager",
                                             new String[] { "featureList", installRoot + "/tool.output.dir/coreFeaturelist.xml" },
                                             installRoot);
    }

    /**
     * Test featureManger featureList with the --productExtension=usr argument.
     * Only features in the default user product extension location are expected in the output list.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureMgrToolWithUsrProdExtArgument() throws Exception {
        setupEnv(server);
        setupProductExtensions(SETUP_USR_PROD_EXT);
        testFeatureToolWithUsrProdExtArgument(installRoot + "/bin/featureManager",
                                              new String[] { "featureList", "--productExtension=usr", installRoot + "/tool.output.dir/usrFeaturelist.xml" },
                                              installRoot);
    }

    /**
     * Test featureManger featureList with the --productExtension=testproduct argument.
     * Only features in the default user product extension testproduct are expected in the output list.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureMgrToolWithProdExtArgument() throws Exception {
        setupEnv(server);
        setupProductExtensions(SETUP_PROD_EXT);
        testFeatureToolWithProdExtArgument(installRoot + "/bin/featureManager",
                                           new String[] { "featureList", "--productExtension=testproduct", installRoot + "/tool.output.dir/prodExtFeaturelist.xml" },
                                           installRoot);
    }

    /**
     * This test makes sure that if the IBM-AppliesTo header is set to the current version then the install happens correctly
     *
     * @throws Exception
     */
    @Test
    public void testAppliesToCurrent() throws Exception {
        final String METHOD_NAME = "testAppliesToCurrent";

        // The "current" install will change over time (i.e. version number)
        // so read in what is current rather than have it hard coded in a test file
        RemoteFile propsFile = server.getFileFromLibertyInstallRoot("lib/versions/WebSphereApplicationServer.properties");
        File tempLocalPropsFile = File.createTempFile("WebSphereApplicationServer", ".properties");

        try {
            // Just in case the test goes wrong tidy on exit
            tempLocalPropsFile.deleteOnExit();
            RemoteFile tempLocalPropsRemoteFile = new RemoteFile(LocalMachine.getLocalMachine(), tempLocalPropsFile.getAbsolutePath());
            propsFile.copyToDest(tempLocalPropsRemoteFile);

            Properties properties = new Properties();
            properties.load(new FileInputStream(tempLocalPropsFile));
            String appliesTo = getAppliesToForCurrent(properties);
            Log.info(c, METHOD_NAME, "Adding this appliesTo to subsystem manifest: " + appliesTo);
            addAppliesToToZip(new File("publish/features/usertest.applies.to.current.source.esa"), new File("publish/features/usertest.applies.to.current.esa"), appliesTo);

            ProgramOutput po = server.installFeature(null, "usertest.applies.to.current");

            Log.info(c, METHOD_NAME, po.getStdout());

            assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

            Log.exiting(c, METHOD_NAME);
        } finally {
            tempLocalPropsFile.delete();
        }
    }

    /**
     * This will build up a valid applies to string for the current installation
     *
     * @param props The Websphere properties file for the current install
     * @return
     */
    private String getAppliesToForCurrent(Properties props) {

        String productId = props.getProperty("com.ibm.websphere.productId");
        StringBuilder appliesTo = new StringBuilder(productId);
        String version = props.getProperty("com.ibm.websphere.productVersion");
        if (version != null && !version.isEmpty()) {
            appliesTo.append("; productVersion=").append(version);
        }

        String installType = props.getProperty("com.ibm.websphere.productInstallType");
        if (installType != null && !installType.isEmpty()) {
            appliesTo.append("; productInstallType=").append(installType);
        }

        String productEdition = props.getProperty("com.ibm.websphere.productEdition");
        if (productEdition != null && !productEdition.isEmpty()) {
            appliesTo.append("; productEdition=").append(productEdition);
        }
        return appliesTo.toString();
    }

    /**
     * Adds the supplied applies to header to the subsystem manifest from an input zip copying it to the output zip
     *
     * @param in
     * @param out
     * @param appliesTo The value of the applies to to set
     * @throws IOException
     */
    private void addAppliesToToZip(File in, File out, String appliesTo) throws IOException {
        ZipFile inZip = new ZipFile(in);
        Enumeration<? extends ZipEntry> zipEntries = inZip.entries();
        FileOutputStream fos = new FileOutputStream(out);
        ZipOutputStream outZipStream = new ZipOutputStream(fos);
        byte[] bytes = new byte[1024];
        while (zipEntries.hasMoreElements()) {
            ZipEntry nextEntry = zipEntries.nextElement();
            outZipStream.putNextEntry(new ZipEntry(nextEntry.getName()));
            InputStream is = inZip.getInputStream(nextEntry);
            int len;
            while ((len = is.read(bytes)) != -1) {
                outZipStream.write(bytes, 0, len);
            }

            if (SUBSYSTEM_MANIFEST_FILE.equalsIgnoreCase(nextEntry.getName())) {
                String appliesToHeader = "\nIBM-AppliesTo: " + appliesTo;
                outZipStream.write(appliesToHeader.getBytes());
            }

            outZipStream.closeEntry();
        }
        outZipStream.close();
    }

    /**
     * This test makes sure that if the IBM-AppliesTo header is set to a different version then the install does not happen
     *
     * @throws Exception
     */
    @Test
    public void testAppliesToDifferent() throws Exception {
        final String METHOD_NAME = "testAppliesToDifferent";
        ProgramOutput po = server.installFeature(null, "usertest.applies.to.different");

        String stdout = po.getStdout();
        Log.info(c, METHOD_NAME, stdout);

        assertEquals("The feature should not have been installed. stdout:\r\n" + stdout, 29, po.getReturnCode());
        assertTrue("An error should have been printed. stdout:\r\n" + stdout,
                   stdout.contains("CWWKF1296E"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test makes sure that if the IBM-Feature-Version header is set to an invalid version then the install does not happen
     *
     * @throws Exception
     */
    @Test
    public void testInvalidFeatureVersion() throws Exception {
        final String METHOD_NAME = "testInvalidFeatureVersion";
        ProgramOutput po = server.installFeature(null, "usertest.invalid.feature.version");

        Log.info(c, METHOD_NAME, po.getStdout());

        assertNotSame("The feature should not have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());
        assertTrue("An error should have been printed. stdout:\r\n" + po.getStdout(),
                   po.getStdout().contains("CWWKF0022E"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This tests that when an ESA is installed that adds a JAR that had been previously ifixed then it should print a warning.
     */

    /*
     * @Test
     * public void testIFixedJar() throws Exception {
     * final String METHOD_NAME = "testIFixedJar";
     * Log.entering(c, METHOD_NAME);
     *
     * // First install the ifix
     * this.filesToTidy.add("lib/fixes/iFix1.xml");
     * this.filesToTidy.add("lib/fixes/iFix1.lpmf");
     * LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/lib/fixes", "publish/features/iFix1.xml");
     * LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/lib/fixes", "publish/features/iFix1.lpmf");
     *
     * // Now install the feature
     * filesToTidy.add("lib/features/usertest1.mf");
     * filesToTidy.add("lib/usertest_1.0.0.jar");
     * ProgramOutput po = server.installFeature(CORE_PRODUCT_NAME, "usertest_core");
     *
     * Log.info(c, METHOD_NAME, po.getStdout());
     *
     * assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());
     * assertTrue("A message saying the iFix would need to be applied should have been printed:\r\n" + po.getStdout(),
     * po.getStdout().contains("iFix1"));
     *
     * Log.exiting(c, METHOD_NAME);
     * }
     */

    @Test
    public void testIFixedJarFeatureManager() throws Exception {
        final String METHOD_NAME = "testIFixedJarusingFeatureManaaer";
        Log.entering(c, METHOD_NAME);
        // First install the ifix
        this.filesToTidy.add("lib/fixes/iFix1.xml");
        this.filesToTidy.add("lib/fixes/iFix1.lpmf");
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/lib/fixes", "publish/features/iFix1.xml");
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/lib/fixes", "publish/features/iFix1.lpmf");
        ProgramOutput po = server.getMachine().execute(server.getInstallRoot() + "/bin/featureManager",
                                                       new String[] { "install", "usertest_core.esa", "--to=core",
                                                                      "--from=" + server.getInstallRoot() + "lib/features" },
                                                       server.getInstallRoot());

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());
        assertTrue("A message saying the iFix would need to be applied should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("iFix1"));

        Log.exiting(c, METHOD_NAME);

    }

    /**
     * Test for defect 97183: Make sure the message is correct for default params when a bundle already exists
     */
    @Test
    public void testExistingBundles() throws Exception {
        final String METHOD_NAME = "testExistingBundles";
        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        // Now install the feature with a clashing JAR with no flags
        po = server.installFeature(null, "usertest.same.bundle");
        assertEquals("The feature should not have been installed. stdout:\r\n" + po.getStdout(), 25, po.getReturnCode());
        assertTrue("A message saying the file exists should have been outputted:\r\n" + po.getStdout(),
                   po.getStdout().contains("CWWKF1015E"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test for defect 97183: Make sure that if you are ignoring existing bundles then it works
     */
    @Test
    public void testExistingBundlesWithIgnore() throws Exception {
        final String METHOD_NAME = "testExistingBundlesWithIgnore";
        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        // Now install the feature with a clashing JAR with no flags
        po = server.installFeatureWithProgramArgs(null, "usertest.same.bundle", new String[] { "--when-file-exists=ignore" });
        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout(), 0, po.getReturnCode());

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that you can't install a user feature if it has a file with no location directive.
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallMissingFileLocation() throws Exception {
        final String METHOD_NAME = "testFeatureInstallMissingFileLocation";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.file.missing.location");

        String stdout = po.getStdout();
        Log.info(c, METHOD_NAME, stdout);

        assertTrue("The feature should not have been installed. stdout:\r\n" + stdout, stdout.contains("CWWKF1012E"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a bundle is copied to the location defined by a directory.
     *
     * @throws Exception
     */
    @Test
    public void testBundleWithDirLocation() throws Exception {
        final String METHOD_NAME = "testBundleWithDirLocation";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.dir.location");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.with.dir.location.mf"));
        assertTrue("The usertest bundle should exist in the directory specified by location. stdout:\r\n" + po.getStdout(),
                   server.fileExistsInLibertyInstallRoot("usr/extension/dev/usertest_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a bundle is copied to the location defined by a file.
     *
     * @throws Exception
     */
    @Test
    public void testBundleWithFileLocation() throws Exception {
        final String METHOD_NAME = "testBundleWithFileLocation";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.exact.location");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.with.exact.location.mf"));
        assertTrue("The usertest bundle should exist in the directory specified by location. stdout:\r\n" + po.getStdout(),
                   server.fileExistsInLibertyInstallRoot("usr/extension/dev/usertest_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that if there is a JAR entry that is missing the the build fails.
     *
     * @throws Exception
     */
    @Test
    public void testMissingJar() throws Exception {
        final String METHOD_NAME = "testMissingJar";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.missing.jar");
        assertEquals("The feature should not have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 24,
                     po.getReturnCode());
        assertTrue("An error should of been printed:\r\n" + po.getStdout(), po.getStdout().contains("CWWKF1013E"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a JAR is copied to the location defined by a directory.
     *
     * @throws Exception
     */
    @Test
    public void testJarWithDirLocation() throws Exception {
        final String METHOD_NAME = "testJarWithDirLocation";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.jar.with.dir.location");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.jar.with.dir.location.mf"));
        assertTrue("The usertest jar should exist in the directory specified by location. stdout:\r\n" + po.getStdout(),
                   server.fileExistsInLibertyInstallRoot("usr/extension/dev/usertest.jar.with.dir.location_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a JAR is copied to the location defined by a file.
     *
     * @throws Exception
     */
    @Test
    public void testJarWithFileLocation() throws Exception {
        final String METHOD_NAME = "testJarWithFileLocation";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.jar.with.exact.location");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.jar.with.exact.location.mf"));
        assertTrue("The usertest jar should exist in the file specified by location. stdout:\r\n" + po.getStdout(),
                   server.fileExistsInLibertyInstallRoot("usr/extension/dev/foo/usertest.jar.with.exact.location_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that a JAR is copied to the lib location when no location is defined.
     *
     * @throws Exception
     */
    @Test
    public void testJarWithNoLocation() throws Exception {
        final String METHOD_NAME = "testJarWithNoLocation";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.jar.with.no.location");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.jar.with.no.location.mf"));
        assertTrue("The usertest jar should exist in the default lib directory.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/usertest.jar.with.no.location_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that if you have a type="JAR" and a type="file" both pointing to a bundle with the same symbolic name
     * then both get installed correctly.
     *
     * @throws Exception
     */
    @Test
    public void testJarWithFileToSameJar() throws Exception {
        final String METHOD_NAME = "testJarWithFileToSameJar";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.jar.and.file");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist. stdout:\r\n" + po.getStdout(),
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.jar.and.file.mf"));
        assertTrue("The usertest jar should exist in the directory specified by location on the jar. stdout:\r\n" + po.getStdout(),
                   server.fileExistsInLibertyInstallRoot("usr/extension/dev/foo/usertest.jar.and.file_1.0.0.jar"));
        assertTrue("The usertest jar should exist in the directory specified by location on the file. stdout:\r\n" + po.getStdout(),
                   server.fileExistsInLibertyInstallRoot("usr/extension/dev/bar/usertest.jar.and.file_1.0.0.jar"));
        JarInputStream jarInputStream = new JarInputStream(server.getFileFromLibertyInstallRoot("usr/extension/dev/foo/usertest.jar.and.file_1.0.0.jar").openForReading());
        Manifest jarManifest = jarInputStream.getManifest();
        assertEquals("The wrong JAR was copied into the location for the type=\"jar\"", jarManifest.getMainAttributes().getValue("IBM-TEST-TYPE"), "jar");

        JarInputStream fileInputStream = new JarInputStream(server.getFileFromLibertyInstallRoot("usr/extension/dev/bar/usertest.jar.and.file_1.0.0.jar").openForReading());
        Manifest fileManifest = fileInputStream.getManifest();
        assertEquals("The wrong JAR was copied into the location for the type=\"file\"", fileManifest.getMainAttributes().getValue("IBM-TEST-TYPE"), "file");
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that if you have a type="file" entry pointing to a non OSGi JAR then it is handled correctly and doesn't
     * crash the processing.
     *
     * @throws Exception
     */
    @Test
    public void testFileWithNonOsgiJar() throws Exception {
        final String METHOD_NAME = "testFileWithNonOsgiJar";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.file.non.osgi.jar");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.file.non.osgi.jar.mf"));
        assertTrue("The usertest jar file should exist in the directory specified by location.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/dev/usertest.file.non.osgi.jar_1.0.0.jar"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that if you have a single type="jar" entry pointing but two JARs in your ESA then the right one is copied in
     *
     * @throws Exception
     */
    @Test
    public void testMultipleJars() throws Exception {
        final String METHOD_NAME = "testMultipleJars";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.multiple.jars");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.multiple.jars.mf"));
        assertTrue("The usertest jar should exist in the directory specified by location.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/dev/foo/usertest.multiple.jars_1.0.0.jar"));
        assertFalse("The usertest jar should not exist for the JAR that wasn't listed in the subsystem manifest.",
                    server.fileExistsInLibertyInstallRoot("usr/extension/dev/bar/usertest.multiple.jars_1.0.0.jar"));

        JarInputStream jarInputStream = new JarInputStream(server.getFileFromLibertyInstallRoot("usr/extension/dev/foo/usertest.multiple.jars_1.0.0.jar").openForReading());
        Manifest jarManifest = jarInputStream.getManifest();
        assertEquals("The wrong JAR was copied into the location for the type=\"jar\"", jarManifest.getMainAttributes().getValue("IBM-TEST-TYPE"), "jar");

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * TestDescription:
     * This test ensures that if you have a checksum file in the ESA then it is copied across
     *
     * @throws Exception
     */
    @Test
    public void testChecksumInstalled() throws Exception {
        final String METHOD_NAME = "testMultipleJars";

        Log.entering(c, METHOD_NAME);

        ProgramOutput po = server.installFeature(null, "usertest.with.checksum");

        assertEquals("The feature should have been installed. stdout:\r\n" + po.getStdout() + "\r\n" + po.getStderr(), 0, po.getReturnCode());

        assertTrue("The usertest feature manifest should exist.", server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/usertest.with.checksum.mf"));
        assertTrue("The checksum file should exists.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/checksums/usertest.with.checksum.cs"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test Description:
     * This test makes sure that the ESAs built for our features install using the feature manager. This will probably take a while... It works by minifying an empty server (so we
     * don't have feature clashes) extracting it and then installing a built ESA into it before deleting the extracted minified install... as we have 79 core ESAs and 95 base ESAs
     * at the time of writing that is a lot of I/O. The saving grace is that we only do it when we have built the image, otherwise it's a no-op.
     *
     * @throws Exception
     */
    @Test
    public void testBuiltEsasInstall() throws Exception {
        final String METHOD_NAME = "testBuiltEsasInstall";

        Log.entering(c, METHOD_NAME);

        // First test to see if we can run this test, only can do it if building image
        LocalFile dheRepoFeaturesDir = new LocalFile(LibertyServerUtils.makeJavaCompatible("publish/features/dhe_repo"));
        if (!dheRepoFeaturesDir.exists()) {
            Log.info(c, METHOD_NAME, "No ESAs to test so exiting");
            return;
        }

        // We can run, grab a minified server test utils to work with, always tear down so we'll do the rest in a try finally
        MinifiedServerTestUtils minifyUtils = new MinifiedServerTestUtils();
        try {
            LibertyServer unminifiedServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.miniest");
            minifyUtils.setup(c.getName(), "com.ibm.ws.kernel.feature.miniest", unminifiedServer);

            RemoteFile packagedServer = minifyUtils.minifyServer();
            if (packagedServer == null) {
                // Must be on z/OS can't do the test
                Log.info(c, METHOD_NAME, "Unable to create minified server so exiting");
                return;
            }

            // Always set the install to DEVELOPERS, all liberty features including ND should install into a developers server
            runTestRepositoryIndividually(dheRepoFeaturesDir, minifyUtils, unminifiedServer, packagedServer, "DEVELOPERS");
        } finally {
            minifyUtils.tearDown();
        }
    }

    @Test
    public void testInstallExtendedAfterBuiltEsasInstall() throws Exception {
        final String METHOD_NAME = "testInstallExtendedAfterBuiltEsasInstall";

        Log.entering(c, METHOD_NAME);

        // First test to see if we can run this test, only can do it if building image
        LocalFile dheRepoFeaturesDir = new LocalFile(LibertyServerUtils.makeJavaCompatible("publish/features/dhe_repo"));
        if (!dheRepoFeaturesDir.exists()) {
            Log.info(c, METHOD_NAME, "No ESAs to test so exiting");
            return;
        }

        // We can run, grab a minified server test utils to work with, always tear down so we'll do the rest in a try finally
        MinifiedServerTestUtils minifyUtils = new MinifiedServerTestUtils();
        try {
            LibertyServer unminifiedServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.miniest");
            minifyUtils.setup(c.getName(), "com.ibm.ws.kernel.feature.miniest", unminifiedServer);

            RemoteFile packagedServer = minifyUtils.minifyServer();
            if (packagedServer == null) {
                // Must be on z/OS can't do the test
                Log.info(c, METHOD_NAME, "Unable to create minified server so exiting");
                return;
            }

            // Always set the install to DEVELOPERS, all liberty features including ND should install into a developers server
            runTestRepositoryTogetherWithExtended(dheRepoFeaturesDir, minifyUtils, unminifiedServer, packagedServer, "DEVELOPERS");
        } finally {
            minifyUtils.tearDown();
        }
    }

    /**
     * This will take a local repository directory, copy it onto the remote machine and try to install each ESA into the miniest server zip.
     *
     * @param featuresRepoDir The local dir with the repository in
     * @param minifyUtils The minify utils that contains all the information about the servers being used
     * @param unminifiedServer The server pre-minify
     * @param packagedServer The minified server zip
     * @param repoEdition The edition to install into (will cause the properties file to be updated)
     * @throws Exception
     * @throws IOException
     */
    public void runTestRepositoryIndividually(LocalFile featuresRepoDir,
                                              MinifiedServerTestUtils minifyUtils,
                                              LibertyServer unminifiedServer,
                                              RemoteFile packagedServer,
                                              String repoEdition) throws Exception, IOException {
        final String METHOD_NAME = "runTestRepositoryIndividually";

        RemoteFile remoteRepository = createRemoteRepository(unminifiedServer, featuresRepoDir, repoEdition);

        for (RemoteFile esa : remoteRepository.list(false)) {
            // Create the miniest install
            LibertyServer minifiedServer = minifyUtils.useMinifiedServer(packagedServer);

            // TODO after resolution of 119915, there shouldn't be a need to create lib/features in the tests
            // When you create a miniest server as there aren't any feature xmls there is no features dir so make that now
            // As with above can't use utils on liberty server to get a file that doesn't exist
            RemoteFile featuresDir = LibertyFileManager.createRemoteFile(minifiedServer.getMachine(), minifiedServer.getInstallRoot() + "/lib/features");
            assertTrue("Should be able to create features dir " + featuresDir.getAbsolutePath() + " exists=" + featuresDir.exists(), featuresDir.mkdir());

            // By default the version property file has no value for edition, however, we've created image so it will have
            // iterated through all of the editions that need to be created so we don't know what state it'll be in.
            // Make it set to the right edition
            RemoteFile versionPropertiesFile = minifiedServer.getFileFromLibertyInstallRoot("lib/versions/WebSphereApplicationServer.properties");
            Properties versionProperties = new Properties();
            versionProperties.load(versionPropertiesFile.openForReading());
            versionProperties.put("com.ibm.websphere.productEdition", repoEdition);
            versionProperties.store(versionPropertiesFile.openForWriting(false), null);
            String libertyVersion = versionProperties.getProperty("com.ibm.websphere." + PRODUCT_VERSION);

            String esaName = esa.getName();

            if (shouldInstallESA(repoEdition, libertyVersion, esa, esaName)) {

                Log.info(c, METHOD_NAME, "Installing ESA " + esaName + " into edition: " + repoEdition);
                ProgramOutput po = minifiedServer.installFeature(esa);

                assertEquals("Installing ESA " + esa.getName() + " should of worked. Program output:\n" + po.getStdout() + "\nErr output:\n" + po.getStderr(), 0,
                             po.getReturnCode());
                assertFalse("There should not of been an error in the validation:\n " + po.getStdout(), po.getStdout().contains("ERROR"));
                assertTrue("The validation should of completed successfully:\n " + po.getStdout(), po.getStdout().contains("Product validation completed successfully."));

                // Also make sure that all of the checksum entries were copied
                verifyChecksums(unminifiedServer, esaName);
            }
            minifyUtils.deletedNestedServer();
        }
    }

    /**
     * @param repoEdition
     * @param METHOD_NAME
     * @param esa
     * @param esaName
     * @throws IOException
     * @throws BundleException
     */
    private boolean shouldInstallESA(String repoEdition, String libertyVersion, RemoteFile esa, String esaName) throws IOException, BundleException {
        String METHOD_NAME = "shouldInstallESA";

        // Check the product install type and edition to make sure we should be installing the ESA
        boolean installationManager = false;
        boolean isZosOnly = false;
        boolean versionMatch = false;
        JarFile jarFile = new JarFile(esa.getAbsolutePath());

        JarEntry entry = jarFile.getJarEntry(SUBSYSTEM_MANIFEST_FILE);
        if (entry != null) {
            Map<String, String> headers = new HashMap<String, String>();
            ManifestElement.parseBundleManifest(jarFile.getInputStream(entry), headers);

            String appliesTo = headers.get(IBM_APPLIES_TO_HEADER);
            if (appliesTo != null) {
                StringTokenizer tokenizer = new StringTokenizer(appliesTo, ";");
                while (tokenizer.hasMoreTokens()) {
                    String nextToken = tokenizer.nextToken();
                    nextToken = nextToken.trim();
                    if (nextToken.startsWith(PRODUCT_INSTALL_TYPE)) {
                        String installType = nextToken.substring(19);
                        Log.info(c, METHOD_NAME, "install type: " + installType);
                        if (installType != null && INSTALLATION_MANAGER.equalsIgnoreCase(installType))
                            installationManager = true;
                    } else if (nextToken.startsWith(PRODUCT_EDITION)) {
                        if (nextToken.contains(ZOS_EDITION) && !nextToken.contains(repoEdition))
                            isZosOnly = true;
                    } else if (nextToken.startsWith(PRODUCT_VERSION)) {
                        String installVersion = nextToken.substring(PRODUCT_VERSION.length() + 1);
                        Log.info(c, METHOD_NAME, "install version: " + installVersion);
                        if (installVersion != null && libertyVersion.equalsIgnoreCase(installVersion))
                            versionMatch = true;
                    }
                }
            }
        }

        jarFile.close();

        if (installationManager) {
            Log.info(c, METHOD_NAME, "ESA " + esaName + " applies only to InstallationManager installs. Not installing.");

        } else if (isZosOnly) {
            Log.info(c, METHOD_NAME, "ESA " + esaName + " applies only to the zOS edition. Not installing.");
        }

        return (!installationManager && !isZosOnly && versionMatch);
    }

    public void runTestRepositoryTogetherWithExtended(LocalFile featuresRepoDir,
                                                      MinifiedServerTestUtils minifyUtils,
                                                      LibertyServer unminifiedServer,
                                                      RemoteFile packagedServer,
                                                      String repoEdition) throws Exception, IOException {
        final String METHOD_NAME = "runTestRepositoryTogetherWithExtended";

        RemoteFile remoteRepository = createRemoteRepository(unminifiedServer, featuresRepoDir, repoEdition);

        // Create the miniest install
        LibertyServer minifiedServer = minifyUtils.useMinifiedServer(packagedServer);

        // TODO after resolution of 119915, there shouldn't be a need to create lib/features in the tests
        // When you create a miniest server as there aren't any feature xmls there is no features dir so make that now
        // As with above can't use utils on liberty server to get a file that doesn't exist
        RemoteFile featuresDir = LibertyFileManager.createRemoteFile(minifiedServer.getMachine(), minifiedServer.getInstallRoot() + "/lib/features");
        assertTrue("Should be able to create features dir " + featuresDir.getAbsolutePath() + " exists=" + featuresDir.exists(), featuresDir.mkdir());

        // By default the version property file has no value for edition, however, we've created image so it will have
        // iterated through all of the editions that need to be created so we don't know what state it'll be in.
        // Make it set to the right edition
        RemoteFile versionPropertiesFile = minifiedServer.getFileFromLibertyInstallRoot("lib/versions/WebSphereApplicationServer.properties");
        Properties versionProperties = new Properties();
        versionProperties.load(versionPropertiesFile.openForReading());
        versionProperties.put("com.ibm.websphere.productEdition", repoEdition);
        versionProperties.store(versionPropertiesFile.openForWriting(false), null);
        String libertyVersion = versionProperties.getProperty("com.ibm.websphere." + PRODUCT_VERSION);

        for (RemoteFile esa : remoteRepository.list(false)) {

            String esaName = esa.getName();
            if (shouldInstallESA(repoEdition, libertyVersion, esa, esaName)) {
                Log.info(c, METHOD_NAME, "Installing ESA " + esaName + " into edition: " + repoEdition);
                ProgramOutput po = minifiedServer.installFeature(esa, new String[] { "--when-file-exists=ignore" });

                if (po.getReturnCode() == 0) {
                    assertEquals("Installing ESA " + esa.getName() + " should of worked. Program output:\n" + po.getStdout() + "\nErr output:\n" + po.getStderr(),
                                 0, po.getReturnCode());
                    assertFalse("There should not of been an error in the validation:\n " + po.getStdout(),
                                po.getStdout().contains("ERROR"));
                    assertTrue("The validation should of completed successfully:\n " + po.getStdout(),
                               po.getStdout().contains("Product validation completed successfully."));
                } else {
                    Log.info(c, METHOD_NAME, "Could not install ESA " + esaName + " into edition: " +
                                             repoEdition + " Return code: " + po.getReturnCode() + " StdOut: " + po.getStdout());
                    // Assert that possibly the esa was already installed
                    assertTrue("Since we didn't successfully install the esa, it should already exist.",
                               po.getStdout().contains("CWWKF1000I"));
                }

                // Also make sure that all of the checksum entries were copied
                verifyChecksums(unminifiedServer, esaName);
            } else {
                Log.info(c, METHOD_NAME, "ESA " + esaName + " is not applied to Liberty edition: " + repoEdition + " and version:" + libertyVersion);
                minifyUtils.deletedNestedServer();
                return;
            }
        }

        // After we have installed every esa in the repository, try to install the extended
        // jar on top of the miniest server with every feature installed as an esa.
        boolean imageInstalled = minifiedServer.installExtendedImage();
        assertTrue("The extended image should have successfully installed after installing all esa's first.",
                   imageInstalled);

        // Finally, we are done with the test so we can delete the built up nested server
        minifyUtils.deletedNestedServer();
    }

    private RemoteFile createRemoteRepository(LibertyServer unminifiedServer,
                                              LocalFile featuresRepoDir,
                                              String repoEdition) throws Exception {
        final String METHOD_NAME = "createRemoteRepository";

        // Make a mock repository on the unminified server as this isn't deleted after each install
        // LibertyServer.getFileFromLibertyServerRoot goes through LibertyFileManager.getLibertyFile
        // which throws a not found exception when it doesn't exist so can't use it
        RemoteFile remoteRepository = LibertyFileManager.createRemoteFile(unminifiedServer.getMachine(),
                                                                          unminifiedServer.getServerRoot() + "/" + repoEdition + ".repository");
        if (!remoteRepository.exists()) {
            assertTrue("Should be able to create repository dir", remoteRepository.mkdir());
        } else {
            Log.info(c, METHOD_NAME, "The remote repository at " + remoteRepository.getAbsolutePath() + " already exists.");
        }

        for (RemoteFile esaSrc : featuresRepoDir.list(false)) {
            RemoteFile esaDest = new RemoteFile(remoteRepository, esaSrc.getName());
            esaDest.copyFromSource(esaSrc);
        }

        return remoteRepository;
    }

    private void verifyChecksums(LibertyServer unminifiedServer, String esaName) throws Exception {
        final String METHOD_NAME = "verifyChecksums";

        // Also make sure that all of the checksum entries were copied
        String checksumPath = "lib/features/checksums/" + esaName.substring(0, esaName.length() - 4) + ".cs";
        if (unminifiedServer.fileExistsInLibertyInstallRoot(checksumPath)) {
            RemoteFile originalChechsumFile = unminifiedServer.getFileFromLibertyInstallRoot(checksumPath);
            Properties originalChecksums = new Properties();
            originalChecksums.load(originalChechsumFile.openForReading());

            RemoteFile newChecksumFile = unminifiedServer.getFileFromLibertyInstallRoot(checksumPath);
            Properties newChecksums = new Properties();
            newChecksums.load(newChecksumFile.openForReading());

            assertEquals("All of the original checksum entries should of been installed into the new runtime:\nOriginal:\n" + originalChecksums.toString()
                         + "\nnewChecksums:\n"
                         + newChecksums.toString(), originalChecksums.size(), newChecksums.size());
        } else {
            // If there are no source CS file then there is nothing to test
            Log.info(c, METHOD_NAME, "No checksum for path " + checksumPath + " so not testing");
        }
    }

    private ProgramOutput runTool(String... args) throws Exception {
        Log.info(c, "runTool", "running bin/featureManager with arguments: " + Arrays.asList(args));
        ProgramOutput po = server.getMachine().execute(server.getInstallRoot() + "/bin/featureManager", args, server.getInstallRoot());
        Log.info(c, "runTool", "Return Code: " + po.getReturnCode() + ". STDOUT: " + po.getStdout());
        return po;
    }

    private File getClasspathOutputJar() {
        return new File(server.getServerRoot() + "/classpath.jar");
    }

    private Manifest dumpManifest(File file) throws Exception {
        JarFile jar = new JarFile(file);
        try {
            Manifest manifest = jar.getManifest();
            Assert.assertNotNull("expected manifest in " + file, manifest);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            manifest.write(out);

            String manifestContents = new String(out.toByteArray(), "UTF-8");
            Log.info(c, "dumpManifest", "META-INF/MANIFEST.MF contents of " + file, manifestContents);
            return manifest;
        } finally {
            jar.close();
        }
    }

    @SuppressWarnings("deprecation")
    private URLClassLoader newURLClassLoader(File file) throws Exception {
        // If there is no .close() method, then the output .jar will be locked,
        // which will cause problems.
        try {
            URLClassLoader.class.getMethod("close");
        } catch (NoSuchMethodException e) {
            Log.info(c, "newURLClassLoader", "Not running Java 7");
            Assume.assumeTrue(false);
        }

        return new URLClassLoader(new URL[] { file.toURL() }, null);
    }

    private void closeURLClassLoader(URLClassLoader cl) throws Exception {
        URLClassLoader.class.getMethod("close").invoke(cl);
    }

    private String getStderr(ProgramOutput po) {
        // LocalMachine.execute oddly merges stderr into stdout.
        return po.getStdout();
    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathNoArgs() throws Exception {
        ProgramOutput po = runTool(new String[] { "classpath" });
        assertEquals(ReturnCode_BAD_ARGUMENT, po.getReturnCode());
        assertTrue(getStderr(po).contains("CWWKF1001E:"));
    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathNoFeatures() throws Exception {
        ProgramOutput po = runTool(new String[] { "classpath", getClasspathOutputJar().getAbsolutePath() });
        assertEquals(ReturnCode_BAD_ARGUMENT, po.getReturnCode());
        assertTrue(getStderr(po).contains("CWWKF1025E:"));
    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathEmptyFeatures() throws Exception {
        ProgramOutput po = runTool(new String[] { "classpath", "--features=", getClasspathOutputJar().getAbsolutePath() });
        assertEquals(ReturnCode_BAD_ARGUMENT, po.getReturnCode());
        assertTrue(getStderr(po).contains("CWWKF1025E:"));
    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathInvalidFeature() throws Exception {
        ProgramOutput po = runTool(new String[] { "classpath", "--features=invalidFeatureName", getClasspathOutputJar().getAbsolutePath() });
        assertEquals(ReturnCode_BAD_ARGUMENT, po.getReturnCode());
        assertTrue(getStderr(po).contains("CWWKF1026E:"));
    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathProtectedFeature() throws Exception {
        ProgramOutput po = runTool(new String[] { "classpath", "--features=com.ibm.websphere.appserver.classloading-1.0", getClasspathOutputJar().getAbsolutePath() });
        assertEquals(ReturnCode_BAD_ARGUMENT, po.getReturnCode());
        assertTrue(getStderr(po).contains("CWWKF1027E:"));
    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathInvalidDrive() throws Exception {
        Assume.assumeTrue(File.separatorChar == '\\');

        String outputJarPath = getClasspathOutputJar().getAbsolutePath();
        boolean installOnCDrive = outputJarPath.substring(0, 1).equalsIgnoreCase("c");
        File badOutputJar = new File((installOnCDrive ? "D" : "C") + outputJarPath.substring(1));

        ProgramOutput po = runTool(new String[] { "classpath", "--features=servlet-3.0", badOutputJar.getAbsolutePath() });
        assertEquals(ReturnCode_BAD_ARGUMENT, po.getReturnCode());
        assertTrue(getStderr(po).contains("CWWKF1028E:"));
    }

    private static final String SERVLET_3_0_CLASS_NAME = "javax.servlet.ServletInputStream";
    private static final String EJBLITE_3_1_CLASS_NAME = "javax.ejb.Stateful";

    private void assertLoadClass(ClassLoader cl, String className) throws Exception {
        Class<?> klass = cl.loadClass(className);
        Log.info(c, "expectClass", "loaded class " + klass);
    }

    private void assertNotLoadClass(ClassLoader cl, String className) throws Exception {
        try {
            Assert.assertNull("unexpected successful loadClass " + className, cl.loadClass(className));
        } catch (ClassNotFoundException e) {
            Log.info(c, "checkClassAndMethod", "caught expected " + e);
        }
    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathServlet30() throws Exception {
        File output = getClasspathOutputJar();
        ProgramOutput po = runTool(new String[] { "classpath", "--features=servlet-3.0", output.getAbsolutePath() });
        dumpManifest(output);

        try {
            URLClassLoader cl = newURLClassLoader(output);
            try {
                Assert.assertEquals("expected successful exit code", ReturnCode_OK, po.getReturnCode());
                assertLoadClass(cl, SERVLET_3_0_CLASS_NAME);
                assertNotLoadClass(cl, EJBLITE_3_1_CLASS_NAME);
            } finally {
                closeURLClassLoader(cl);
            }
        } finally {
            Assert.assertTrue(output.delete());
        }
    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathServlet30AndServlet31() throws Exception {
        File output = getClasspathOutputJar();
        ProgramOutput po = runTool(new String[] {
                                                  "classpath",
                                                  "--features=appSecurity-2.0,appClientSupport-1.0,ssl-1.0,beanvalidation-1.1,cdi-1.2,concurrent-1.0,ejb-3.2,jacc-1.5,jaspic-1.1,javamail-1.5,jaxb-2.2,jaxrs-2.0,jaxws-2.2,batch-1.0,j2eeManagement-1.1,jca-1.7,jcaInboundSecurity-1.0,jdbc-4.1,jmsMdb-3.2,jpa-2.1,jsf-2.2,jsonp-1.0,el-3.0,jsp-2.3,localConnector-1.0,managedbeans-1.0,servlet-3.1,wasJmsClient-2.0,wasJmsServer-1.0,wasJmsSecurity-1.0,websocket-1.1",
                                                  output.getAbsolutePath() });
        Assert.assertEquals("expected successful exit code", ReturnCode_OK, po.getReturnCode());
        Manifest m = dumpManifest(output);

        String cp = m.getMainAttributes().getValue("Class-Path");

        assertTrue("Class-Path should contain servlet 3.1 API jar: " + cp, cp.contains("com.ibm.websphere.javaee.servlet.3.1"));
        assertFalse("Class-Path should not contain servlet 3.0 API jar: " + cp, cp.contains("com.ibm.websphere.javaee.servlet.3.0"));

    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathEJBLite31() throws Exception {
        File output = getClasspathOutputJar();
        ProgramOutput po = runTool(new String[] { "classpath", "--features=ejbLite-3.1", output.getAbsolutePath() });
        dumpManifest(output);

        try {
            URLClassLoader cl = newURLClassLoader(output);
            try {
                Assert.assertEquals("expected successful exit code", ReturnCode_OK, po.getReturnCode());
                assertNotLoadClass(cl, SERVLET_3_0_CLASS_NAME);
                assertLoadClass(cl, EJBLITE_3_1_CLASS_NAME);
            } finally {
                closeURLClassLoader(cl);
            }
        } finally {
            Assert.assertTrue(output.delete());
        }
    }

    @Test
    @Mode(TestMode.LITE)
    public void testClasspathServlet30AndEJBLite31() throws Exception {
        File output = getClasspathOutputJar();
        ProgramOutput po = runTool(new String[] { "classpath", "--features=servlet-3.0,ejbLite-3.1", output.getAbsolutePath() });
        dumpManifest(output);

        try {
            URLClassLoader cl = newURLClassLoader(output);
            try {
                Assert.assertEquals("expected successful exit code", ReturnCode_OK, po.getReturnCode());
                assertLoadClass(cl, SERVLET_3_0_CLASS_NAME);
                assertLoadClass(cl, EJBLITE_3_1_CLASS_NAME);
            } finally {
                closeURLClassLoader(cl);
            }
        } finally {
            Assert.assertTrue(output.delete());
        }
    }
}
