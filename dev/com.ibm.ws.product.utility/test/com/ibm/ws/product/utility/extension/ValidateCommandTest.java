/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandConstants;
import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.product.utility.extension.utils.TestUtils;

/**
 *
 */
public class ValidateCommandTest {

    // This is used to create a unique directory for each test. We sometimes get failures deleting files/dirs which can
    // cause the next test to fail.
    private static int testInstallDirCount = 0;
    // Some default File names that we'll use in the tests.
    private static final File tmpDir = new File("build/unittest/tmp");
    private static File uniqueTestDir;
    private static File wlpDir;
    private static File libDir;
    private static File binDir;
    private static File fixesDir;
    private static File featuresDir;
    private static File platformDir;
    private static File versionsDir;
    private static File checksumsDir;
    private static File toolsDir;
    // This dir is for the ifix files which are effectively from the ifix jar file.
    // We need them to generate hashes and locations in the ifix.xml
    private static File ifixLibDir;
    private static File ifixBinDir;
    private static File ifixFeaturesDir;

    // The List that will hold the console messages from the validate command.
    private static final List<String> consoleInfoMessages = new ArrayList<String>();
    private static final List<String> consoleErrorMessages = new ArrayList<String>();

    // The JMock objects
    private static Mockery mockery = null;
    private static ExecutionContext context = null;
    private static CommandConsole console = null;

    // A static instance of the ValidateCommandTask.
    public static final ValidateCommandTask task = new ValidateCommandTask();

    @Rule
    public final TestName name = new TestName();

    /**
     * This method is run before each test to setup the mocked Liberty runtime.
     */
    @Before
    public void testSetup() {

        // Clean up from last test.
        if (tmpDir.exists())
            TestUtils.delete(tmpDir);

        // Define unique test dir.
        uniqueTestDir = new File(tmpDir, "Test" + testInstallDirCount++);
        wlpDir = new File(uniqueTestDir, "wlp");
        setupMocking();

        // Reflectively amend the Utils class to set the current WLP dir as the installDir field.
        try {
            Field getInstallField = Utils.class.getDeclaredField("installDir");
            getInstallField.setAccessible(true);
            getInstallField.set(null, wlpDir);
        } catch (Exception e) {
            e.printStackTrace();
        }

        libDir = new File(wlpDir, "lib");
        binDir = new File(wlpDir, "bin");
        fixesDir = new File(libDir, "fixes");
        featuresDir = new File(libDir, "features");
        platformDir = new File(libDir, "platform");
        versionsDir = new File(libDir, "versions");
        checksumsDir = new File(versionsDir, "checksums");
        toolsDir = new File(libDir, "internal-tools");
        // This dir is for the ifix files which are effectively from the ifix jar file.
        // We need them to generate hashes and locations in the ifix.xml
        ifixLibDir = new File(uniqueTestDir, "lib");
        ifixBinDir = new File(uniqueTestDir, "bin");
        ifixFeaturesDir = new File(ifixLibDir, "features");
        // These dirs should make all parent dirs.
        fixesDir.mkdirs();
        binDir.mkdirs();
        featuresDir.mkdirs();
        checksumsDir.mkdirs();
        ifixLibDir.mkdirs();
        ifixBinDir.mkdirs();
        ifixFeaturesDir.mkdirs();
        toolsDir.mkdirs();
        platformDir.mkdirs();

        // Setup up the ContentBasedLocalBundleRepository
        TestUtils.refreshBundleRepository();
    }

    private static void setupMocking() {
        // Configure the JMock Objects. This is mainly setting up the console to store any messages that are written to
        // it so we can read them back and assert that we're getting the expected messages.
        mockery = new Mockery();
        context = mockery.mock(ExecutionContext.class);
        console = mockery.mock(CommandConsole.class);
        mockery.checking(new Expectations() {
            {
                //------Context expectations ------
                // Configure the Context to return the wlp location when the attribute is called.
                allowing(context).getAttribute(with(CommandConstants.WLP_INSTALLATION_LOCATION), with(File.class));
                will(returnValue(wlpDir));

                // Configure the Context to return the null when the against option value is requested.
                allowing(context).getOptionValue(with("against"));
                will(returnValue(null));

                //Return the mocked CommandConsole object when the method is called on the context.
                allowing(context).getCommandConsole();
                will(returnValue(console));

                // When an info message is written to the console, write this to our list so we can parse the output in the test.
                allowing(console).printlnInfoMessage(with(any(String.class)));
                will(new CustomAction("Add string to collection") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        consoleInfoMessages.add(((String) invocation.getParameter(0)).trim());
                        return null;
                    }
                });

                //------Context expectations ------

                // When an info message is written to the console, write this to our list so we can parse the output in the test.
                allowing(console).printInfoMessage(with(any(String.class)));
                will(new CustomAction("Add string to collection") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        consoleInfoMessages.add(((String) invocation.getParameter(0)).trim());
                        return null;
                    }
                });

                // When an error message is written to the console, write this to our list so we can parse the output in the test.
                allowing(console).printlnErrorMessage(with(any(String.class)));
                will(new CustomAction("Add string to collection") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        consoleErrorMessages.add((String) invocation.getParameter(0));
                        return null;
                    }
                });

                // When an error message is written to the console, write this to our list so we can parse the output in the test.
                allowing(console).printErrorMessage(with(any(String.class)));
                will(new CustomAction("Add string to collection") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        consoleErrorMessages.add((String) invocation.getParameter(0));
                        return null;
                    }
                });
            }
        });
    }

    @After
    public void teardown() {
        TestUtils.clearBundleRepositoryHolders();
        TestUtils.delete(tmpDir);
    }

    /**
     * This test proves that the ValidateCommandTask displays any IFixes that need to be reapplied when checking jar files. The ifix xml file
     * also contains a Test3 ifix jar. Because we don't have a test3 jar in the runtime, it shouldn't affect whether we need to reapply the ifix or
     * not. This test does a 2 step process. It checks that we do need a reapply when we are genuinely missing an ifix jar, and then we "apply"
     * the ifix, and ensure that the ifix reapply msg isn't issued. This will prove that the test3 missing ifix jar isn't relevant.
     *
     * @throws Exception
     */
    @Test
    public void testValidateDisplaysIfixReApplicationForJars() throws Exception {

        // These are the files that are the ifix files that have been installed into the runtime.
        File test1IFixJar = TestUtils.createJarFile(libDir, "test1", "1.0.0.20130101", true, true);
        // Store the ifix jar 2 and 3 in a temp dir outside of the wlp runtime, as we want to generate hashes but don't want them in the runtime.
        // We will create the runtime version of jar 2 later in the test.
        File test2IFixJar = TestUtils.createJarFile(ifixLibDir, "test2", "1.0.0.20130101", true, false);
        File test3IFixJar = TestUtils.createJarFile(ifixLibDir, "test3", "1.0.0.20130101", true, true);

        // These are the jars files that are put in the runtime lib dir.
        File test1Jar = TestUtils.createJarFile(libDir, "test1", "1.0.0", false, false);
        File test2Jar = TestUtils.createJarFile(libDir, "test2", "1.0.0", false, false);

        // Generate the IFix xml file for just ifix jars
        Set<String> updateFiles = new HashSet<String>();
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixJar) + "\" size=\"" + test1IFixJar.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test1IFixJar, wlpDir) + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixJar) + "\" size=\"" + test2IFixJar.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test2IFixJar, uniqueTestDir) + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test3IFixJar) + "\" size=\"" + test3IFixJar.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test3IFixJar, uniqueTestDir) + "\"/>");
        String ifixName1 = "Test-Ifix1";
        TestUtils.createIfixXML(new File(fixesDir, "ifix1.xml"), ifixName1, updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Create test feature
        Map<String, String> featureFiles = new HashMap<String, String>();
        featureFiles.put(TestUtils.createRelativePath(test1Jar, wlpDir), test1Jar.getName());
        featureFiles.put(TestUtils.createRelativePath(test2Jar, wlpDir), test2Jar.getName());
        TestUtils.createFeature(new File(featuresDir, "test-1.0.mf"), "test-1.0", featureFiles);

        runValidateCommand();
        // Check that the command has run successfully, and ensure it includes the reapply Ifix msg.
        assertChecks(true, "The following fixes must be reapplied: [" + ifixName1 + "].");

        // Now "reapply" the ifix and check that the command doesn't report any issues.
        File copiedTest2IfixJar = new File(libDir, test2IFixJar.getName());
        TestUtils.copyFile(test2IFixJar, copiedTest2IfixJar);
        // Rerun the command after clearing the console
        runValidateCommand();

        // Check that the command has run successfully, but ensure it doesn't include the reapply Ifix msg.
        assertChecks(false, "The following fixes must be reapplied: [" + ifixName1 + "].");
    }

    /**
     * This test proves that the ValidateCommandTask displays any IFixes that need to be reapplied when checking static content files.
     *
     * @throws Exception
     */
    @Test
    public void testValidateDisplaysIfixReApplicationForStaticContent() throws Exception {

        // Create the static files
        StringBuffer fileContents = new StringBuffer();
        fileContents.append("This is the 1st line of file \n");
        fileContents.append("This is the 2nd line of file \n");

        String staticFile1Name = "staticFile1.bat";
        String staticFile2Name = "staticFile2";
        String staticFile3Name = "staticFile3.bat";
        // Just create the static file 1 and 2.
        TestUtils.createFile(new File(binDir, staticFile1Name), fileContents);

        // Add an extra line to the static files so that we get a different hash.
        fileContents.append("This is an ifix static file \n");

        File test1IFixStaticFile = TestUtils.createFile(new File(ifixBinDir, staticFile1Name), fileContents);
        File test2IFixStaticFile = TestUtils.createFile(new File(ifixBinDir, staticFile2Name), fileContents);
        File test3IFixStaticFile = TestUtils.createFile(new File(ifixBinDir, staticFile3Name), fileContents);

        // Generate the IFix xml files for just ifix static content
        Set<String> updateFiles = new HashSet<String>();
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixStaticFile) + "\" size=\"" + test1IFixStaticFile.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test1IFixStaticFile, uniqueTestDir) + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixStaticFile) + "\" size=\"" + test2IFixStaticFile.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test2IFixStaticFile, uniqueTestDir) + "\"/>");
        String ifixName1 = "Test-Ifix1";
        TestUtils.createIfixXML(new File(fixesDir, "ifix1.xml"), ifixName1, updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Generate a second ifix xml for the static 3 file
        String ifixName2 = "Test-Ifix2";
        updateFiles.clear();
        updateFiles.add("<file date=\"2013-02-01 12:00\" hash=\"" + TestUtils.generateHash(test3IFixStaticFile) + "\" size=\"" + test3IFixStaticFile.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test3IFixStaticFile, uniqueTestDir) + "\"/>");
        TestUtils.createIfixXML(new File(fixesDir, "ifix2.xml"), ifixName2, updateFiles, TestUtils.generateSet("Apar4"));

        // Create test feature
        Map<String, String> featureContent = new HashMap<String, String>();
        featureContent.put(TestUtils.createRelativePath(test1IFixStaticFile, uniqueTestDir), staticFile1Name);
        featureContent.put(TestUtils.createRelativePath(test2IFixStaticFile, uniqueTestDir), staticFile2Name);
        TestUtils.createFeature(new File(featuresDir, "test-1.0.mf"), "test-1.0", featureContent);

        // Create test feature 2
        Map<String, String> feature2Content = new HashMap<String, String>();
        feature2Content.put(TestUtils.createRelativePath(test3IFixStaticFile, uniqueTestDir), staticFile3Name);
        TestUtils.createFeature(new File(featuresDir, "test-2.0.mf"), "test-2.0", feature2Content);

        // Run the command
        runValidateCommand();
        // Check that the command has run successfully, and ensure it includes the reapply Ifix msg.
        assertChecks2Ifixes(ifixName1, ifixName2);

        // Now "reapply" the the static1 file and check that the we still report an error for static2 file
        File copiedTest1StaticFile = new File(binDir, test1IFixStaticFile.getName());
        TestUtils.copyFile(test1IFixStaticFile, copiedTest1StaticFile);

        // Rerun the command
        runValidateCommand();

        // Check that the command has run successfully, and ensure it includes the reapply Ifix msg.
        assertChecks2Ifixes(ifixName1, ifixName2);

        // Now "reapply" the static2 ifix and check that the command doesn't report any issues with ifix 1.
        File copiedTest2StaticFile = new File(binDir, test2IFixStaticFile.getName());
        TestUtils.copyFile(test2IFixStaticFile, copiedTest2StaticFile);

        // Rerun the command
        runValidateCommand();

        // Check that the command has run successfully, and ensure it includes the reapply Ifix msg.
        assertChecks(true, "The following fixes must be reapplied: [" + ifixName2 + "].");

        // Now "reapply" the static3 file which fixes the ifix2 xml and check that the command doesn't report any issues.
        File copiedTest3StaticFile = new File(binDir, test3IFixStaticFile.getName());
        TestUtils.copyFile(test3IFixStaticFile, copiedTest3StaticFile);
        // Rerun the command
        runValidateCommand();
        // Check that the command has run successfully, but ensure it doesn't include the reapply Ifix msg.
        assertChecks(false, "The following fixes must be reapplied: [" + ifixName2 + "].");
    }

    /**
     * This test proves that the ValidateCommandTask doesn't display the reapplication message if the ifix files are already installed.
     *
     * @throws Exception
     */
    @Test
    public void testValidateDoesNotDisplayIfixReApplicationIfIfixInstalled() throws Exception {

        // These are the files that are the ifix files that have been installed into the runtime.
        TestUtils.createJarFile(libDir, "test1", "1.0.0", false, false);
        File test1IFixJar = TestUtils.createJarFile(libDir, "test1", "1.0.0.20130101", true, true);
        TestUtils.createJarFile(libDir, "test2", "1.0.0", false, false);
        File test2IFixJar = TestUtils.createJarFile(libDir, "test2", "1.0.0.20130101", true, false);

        // Create the static files
        StringBuffer fileContents = new StringBuffer();
        fileContents.append("This is the 1st line of file \n");
        fileContents.append("This is the 2nd line of file \n");

        File test1IFixStaticFile = TestUtils.createFile(new File(binDir, "staticFile1.bat"), fileContents);
        File test2IFixStaticFile = TestUtils.createFile(new File(binDir, "staticFile2"), fileContents);

        // Generate the IFix xml files for just ifix static content
        Set<String> updateFiles = new HashSet<String>();
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixStaticFile) + "\" size=\"" + test1IFixStaticFile.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test1IFixStaticFile, wlpDir) + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixJar) + "\" size=\"" + test1IFixJar.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test1IFixJar, wlpDir) + "\"/>");
        String ifixName1 = "Test-Ifix1";
        TestUtils.createIfixXML(new File(fixesDir, "ifix1.xml"), ifixName1, updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        Set<String> update2Files = new HashSet<String>();
        update2Files.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixStaticFile) + "\" size=\"" + test2IFixStaticFile.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test2IFixStaticFile, wlpDir) + "\"/>");
        update2Files.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixJar) + "\" size=\"" + test2IFixJar.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test2IFixJar, wlpDir) + "\"/>");
        String ifixName2 = "Test-Ifix2";
        TestUtils.createIfixXML(new File(fixesDir, "ifix2.xml"), ifixName2, update2Files, TestUtils.generateSet("Apar4"));

        // Create test feature
        Map<String, String> featureContent = new HashMap<String, String>();
        featureContent.put(TestUtils.createRelativePath(test1IFixStaticFile, uniqueTestDir), test1IFixStaticFile.getName());
        featureContent.put(TestUtils.createRelativePath(test1IFixJar, wlpDir), test1IFixJar.getName());
        TestUtils.createFeature(new File(featuresDir, "test-1.0.mf"), "test-1.0", featureContent);

        Map<String, String> feature2Content = new HashMap<String, String>();
        feature2Content.put(TestUtils.createRelativePath(test2IFixJar, wlpDir), test2IFixJar.getName());
        feature2Content.put(TestUtils.createRelativePath(test2IFixStaticFile, uniqueTestDir), test2IFixStaticFile.getName());
        TestUtils.createFeature(new File(featuresDir, "test2-1.0.mf"), "test2-1.0", feature2Content);

        runValidateCommand();
        assertChecks(false, null);
    }

    /**
     * This test proves that the ValidateCommandTask displays multiple IFixes that need to be reapplied when checking both
     * static content and jars together.
     *
     * @throws Exception
     */
    @Test
    public void testValidateDisplaysIfixReApplicationForMultipleIFix() throws Exception {

        // Create the static files
        StringBuffer fileContents = new StringBuffer();
        fileContents.append("This is the 1st line of file \n");
        fileContents.append("This is the 2nd line of file \n");
        // These are the static files that we'll use for the tests. They are in the runtime structure.
        File test1StaticFile = TestUtils.createFile(new File(binDir, "server.bat"), fileContents);
        File test2StaticFile = TestUtils.createFile(new File(binDir, "server"), fileContents);
        File test3StaticFile = TestUtils.createFile(new File(binDir, "version.bat"), fileContents);
        File test4StaticFile = TestUtils.createFile(new File(toolsDir, "version.bat"), fileContents);

        // Store the ifix Static files in the tmp dir, as we want to generate hashes but don't want them in the runtime.
        fileContents.append("This is the 2nd line of file \n");
        File test4IFixStaticFile = TestUtils.createFile(new File(ifixBinDir, "version.bat"), fileContents);
        File test5IFixStaticFile = TestUtils.createFile(new File(ifixBinDir, "nonexistantFile"), fileContents);

        // Base Bundle for test 1
        File test1Jar = TestUtils.createJarFile(libDir, "test1", "1.0.0", false, false);
        // Ifix Bundle for test 1
        File test1IFixJar = TestUtils.createJarFile(libDir, "test1", "1.0.0.20130101", true, true);
        // Base Bundle for test 2
        File test2Jar = TestUtils.createJarFile(libDir, "test2", "1.0.0", false, false);
        // Ifix Bundle for test 2
        File test2IFixJar = TestUtils.createJarFile(ifixLibDir, "test2", "1.0.0.20130101", true, false);
        // Test3IfixJar isn't meant to be installed into the runtime. It is just in the ifix xml. No base bundle exists
        File test3IFixJar = TestUtils.createJarFile(ifixLibDir, "test3", "1.0.0.20130101", true, true);

        // Maps that we'll store the file content info for the ifix xml files. The key is the file name, and the
        // value is the actual file that we use for hashes/file size etc.
        Set<String> ifix1Content = new HashSet<String>();
        Set<String> ifix2Content = new HashSet<String>();
        Set<String> ifix3Content = new HashSet<String>();
        Set<String> ifix4Content = new HashSet<String>();

        // Load the ifix content for ifix 1.
        ifix1Content.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixJar) + "\" size=\"" + test1IFixJar.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test1IFixJar, wlpDir) + "\"/>");
        ifix1Content.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test5IFixStaticFile) + "\" size=\"" + test5IFixStaticFile.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test5IFixStaticFile, uniqueTestDir) + "\"/>");

        // Load the ifix content for ifix 2. This contains the ifix1 content as this ifix supersedes ifix1.
        ifix2Content.addAll(ifix1Content);
        ifix2Content.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixJar) + "\" size=\"" + test2IFixJar.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test2IFixJar, uniqueTestDir) + "\"/>");
        ifix2Content.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1StaticFile) + "\" size=\"" + test1StaticFile.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test1StaticFile, wlpDir) + "\"/>");
        ifix2Content.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test2StaticFile) + "\" size=\"" + test2StaticFile.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test2StaticFile, wlpDir) + "\"/>");

        // Load the ifix content for ifix 3. This contains files that aren't relevant for this runtime, and should be ignored, and not
        // reported as being required to be reapplied.
        ifix3Content.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test5IFixStaticFile) + "\" size=\"" + test5IFixStaticFile.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test5IFixStaticFile, uniqueTestDir) + "\"/>");

        // Load the ifix content for ifix 4. This also supersedes ifix 1.
        ifix4Content.addAll(ifix1Content);
        ifix4Content.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test3IFixJar) + "\" size=\"" + test3IFixJar.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test3IFixJar, uniqueTestDir) + "\"/>");
        ifix4Content.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test4IFixStaticFile) + "\" size=\"" + test4IFixStaticFile.length() + "\" id=\"" +
                         "lib/internal-tools/version.bat\"/>");

        // Generate the 1st IFix xml file.
        File ifixFile = new File(fixesDir, "ifix1.xml");
        TestUtils.createIfixXML(ifixFile, "ifix1", ifix1Content, TestUtils.generateSet("Apar1"));

        // Generate the 2nd IFix xml file.
        File ifixFile2 = new File(fixesDir, "ifix2.xml");
        TestUtils.createIfixXML(ifixFile2, "ifix2", ifix2Content, TestUtils.generateSet("Apar1", "Apar2"));

        // Generate the 3rd IFix xml file.
        File ifixFile3 = new File(fixesDir, "ifix3.xml");
        TestUtils.createIfixXML(ifixFile3, "ifix3", ifix3Content, TestUtils.generateSet("Apar4"));

        // Generate the 3rd IFix xml file.
        File ifixFile4 = new File(fixesDir, "ifix4.xml");
        TestUtils.createIfixXML(ifixFile4, "ifix4", ifix4Content, TestUtils.generateSet("Apar1", "Apar3"));

        // Create test feature1
        Map<String, String> feature1Files = new HashMap<String, String>();
        feature1Files.put(TestUtils.createRelativePath(test1StaticFile, wlpDir), test1StaticFile.getName());
        feature1Files.put(TestUtils.createRelativePath(test1Jar, wlpDir), test1Jar.getName());
        TestUtils.createFeature(new File(featuresDir, "testFeature1-1.0.mf"), "testFeature1-1.0", feature1Files);

        // Create test feature2
        Map<String, String> feature2Files = new HashMap<String, String>();
        feature2Files.put(TestUtils.createRelativePath(test2StaticFile, wlpDir), test2StaticFile.getName());
        feature2Files.put(TestUtils.createRelativePath(test2Jar, wlpDir), test2Jar.getName());
        TestUtils.createFeature(new File(featuresDir, "testFeature2-1.0.mf"), "testFeature2-1.0", feature2Files);

        // Create test feature4
        Map<String, String> feature3Files = new HashMap<String, String>();
        feature3Files.put(TestUtils.createRelativePath(test3StaticFile, wlpDir), test3StaticFile.getName());
        feature3Files.put(TestUtils.createRelativePath(test4StaticFile, wlpDir), test4StaticFile.getName() + ".internal");
        TestUtils.createFeature(new File(featuresDir, "testFeature3-1.0.mf"), "testFeature3-1.0", feature3Files);

        runValidateCommand();
        // Check that the command has run successfully, and ensure it includes the reapply Ifix msg.
        assertChecks2Ifixes("ifix2", "ifix4");
    }

    /**
     * This test proves that changes to a Manifest file will also flag an ifix reapply.
     *
     * @throws Exception
     */
    @Test
    public void testValidateDisplaysIfixReApplicationForManifestChanges() throws Exception {

        // Base Bundle for test 1
        File test1Jar = TestUtils.createJarFile(libDir, "test1", "1.0.0", false, false);
        // Ifix Bundle for test 2
        File test2Jar = TestUtils.createJarFile(libDir, "test2", "1.0.0", false, false);
        // Base Bundle for test 3
        File test3Jar = TestUtils.createJarFile(libDir, "test3", "1.0.0", false, false);
        // Ifix Bundle for test 4
        File test4Jar = TestUtils.createJarFile(libDir, "test4", "1.0.0", false, false);

        // Create test feature 1
        Map<String, String> feature1Content = new HashMap<String, String>();
        feature1Content.put(TestUtils.createRelativePath(test1Jar, wlpDir), test1Jar.getName());
        File feature1 = TestUtils.createFeature(new File(featuresDir, "test-1.0.mf"), "test-1.0", feature1Content);

        // Create test ifix feature 1
        Map<String, String> feature1IfixContent = new HashMap<String, String>();
        feature1IfixContent.put(TestUtils.createRelativePath(test2Jar, wlpDir), test2Jar.getName());
        File featureIfix1 = TestUtils.createFeature(new File(ifixFeaturesDir, "test-1.0.mf"), "test-1.0", feature1IfixContent);

        // Create test feature 2
        Map<String, String> feature2Content = new HashMap<String, String>();
        feature2Content.put(TestUtils.createRelativePath(test3Jar, wlpDir), test3Jar.getName());
        File feature2 = TestUtils.createFeature(new File(featuresDir, "test-2.0.mf"), "test-2.0", feature2Content);

        // Create test ifix feature 2
        Map<String, String> feature2IfixContent = new HashMap<String, String>();
        feature2IfixContent.put(TestUtils.createRelativePath(test4Jar, wlpDir), test4Jar.getName());
        File featureIfix2 = TestUtils.createFeature(new File(ifixFeaturesDir, "test-2.0.mf"), "test-2.0", feature2IfixContent);

        // Generate the IFix xml files for just ifix static content
        Set<String> updateFiles = new HashSet<String>();
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(featureIfix1) + "\" size=\"" + featureIfix1.length() + "\" id=\"" +
                        TestUtils.createRelativePath(featureIfix1, uniqueTestDir) + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(featureIfix2) + "\" size=\"" + featureIfix2.length() + "\" id=\"" +
                        TestUtils.createRelativePath(featureIfix2, uniqueTestDir) + "\"/>");
        String ifixName1 = "Test-Ifix1";
        TestUtils.createIfixXML(new File(fixesDir, "ifix1.xml"), ifixName1, updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Run the command
        runValidateCommand();
        // Check that the command has run successfully, and ensure it includes the reapply Ifix msg.
        assertChecks(true, "The following fixes must be reapplied: [" + ifixName1 + "].");

        // Now "reapply" the the static1 file and check that the we still report an error for static2 file
        TestUtils.copyFile(featureIfix1, feature1);
        TestUtils.copyFile(featureIfix2, feature2);

        // Rerun the command
        runValidateCommand();

        // Check that the command has run successfully, and ensure there is no reapply Ifix msg.
        assertChecks(false, null);
    }

    /**
     * This test proves that multiple product extensions that have missing ifixes are reported.
     *
     * @throws Exception
     */
    @Test
    public void testValidateDisplaysIfixReApplicationForMultipleProductExtensions() throws Exception {

        // Setup the Alternate product extension dir.
        File alternateWLPDir = new File(uniqueTestDir, "alternate/wlp");
        File alternateLibDir = new File(alternateWLPDir, "lib");
        File alternateBinDir = new File(alternateWLPDir, "bin");
        File alternateFixesDir = new File(alternateLibDir, "fixes");
        File alternateFeaturesDir = new File(alternateLibDir, "features");
        File extensionsDir = new File(wlpDir, "etc/extensions");

        alternateFeaturesDir.mkdirs();
        alternateFixesDir.mkdirs();
        alternateBinDir.mkdirs();
        extensionsDir.mkdirs();

        // Plug in the new alternate product extension location to the runtime.
        Properties extensionProps = new Properties();
        extensionProps.put("com.ibm.websphere.productInstall", alternateWLPDir.getAbsolutePath());
        extensionProps.put("com.ibm.websphere.productId", "ALTERNATE");

        FileOutputStream fos = new FileOutputStream(new File(extensionsDir, "alternate.properties"));
        try {
            extensionProps.store(fos, "testing extension props");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (fos != null)
                fos.close();
        }

        // These are the files that are the ifix files that have been installed into the runtime using a properties file.
        File test1Jar = TestUtils.createJarFile(alternateLibDir, "test1", "1.0.0", false, false);
        File test1IFixJar = TestUtils.createJarFile(ifixLibDir, "test1", "1.0.0.20130101", true, true);

        // Create the static files
        StringBuffer fileContents = new StringBuffer();
        fileContents.append("This is the 1st line of file \n");

        File test1IFixStaticFile = TestUtils.createFile(new File(ifixBinDir, "staticFile1.bat"), fileContents);

        // Generate the IFix xml files for just ifix static content
        Set<String> updateFiles = new HashSet<String>();
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixStaticFile) + "\" size=\"" + test1IFixStaticFile.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test1IFixStaticFile, uniqueTestDir) + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixJar) + "\" size=\"" + test1IFixJar.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test1IFixJar, uniqueTestDir) + "\"/>");
        String ifixName1 = "Test-Ifix1";
        TestUtils.createIfixXML(new File(alternateFixesDir, "ifix1.xml"), ifixName1, updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Create test feature
        Map<String, String> featureContent = new HashMap<String, String>();
        featureContent.put(TestUtils.createRelativePath(test1IFixStaticFile, uniqueTestDir), test1IFixStaticFile.getName());
        featureContent.put(TestUtils.createRelativePath(test1Jar, alternateWLPDir), test1Jar.getName());
        TestUtils.createFeature(new File(alternateFeaturesDir, "test-1.0.mf"), "test-1.0", featureContent);

        // Add the new repository for the alternate location.
        BundleRepositoryRegistry.addBundleRepository(alternateWLPDir.getAbsolutePath(), "alternate");

        // Run the validate command.
        runValidateCommand();

        // Check we have the expected error message
        assertChecks(true, "The following fixes must be reapplied: [" + ifixName1 + "].");

        // "Re-apply the ifix"
        TestUtils.copyFile(test1IFixJar, new File(alternateLibDir, test1IFixJar.getName()));
        TestUtils.copyFile(test1IFixStaticFile, new File(alternateBinDir, test1IFixStaticFile.getName()));
        runValidateCommand();

        // Check that the command is now happy and not reporting any reapplication msgs
        assertChecks(false, null);
    }

    /**
     * This test proves that the correct file is checked when it is included in multiple ifix xmls. We work out the entry with the newest date
     * and log the hash/location to use for checking.
     *
     * @throws Exception
     */
    @Test
    public void testValidateDisplaysIfixReApplicationForSameFileInMultipleIfixes() throws Exception {

        // Store the ifix jars in a temp dir outside of the wlp runtime, as we want to generate hashes but don't want them in the runtime.
        // We will create the runtime version of jar 2 later in the test.
        File test1IFixJar = TestUtils.createJarFile(ifixLibDir, "test1", "1.0.0.20130101", true, true);
        File test2IFixJar = TestUtils.createJarFile(ifixLibDir, "test1", "1.0.0.20130201", true, false);
        File test3IFixJar = TestUtils.createJarFile(ifixLibDir, "test1", "1.0.0.20130301", true, true);

        // Create the static files
        StringBuffer fileContents = new StringBuffer();
        fileContents.append("This is the 1st line of file \n");
        fileContents.append("This is the 2nd line of file \n");

        File test1IFixStaticFile = TestUtils.createFile(new File(ifixBinDir, "server.bat"), fileContents);

        // Create the 1st ifix.xml. It has the latest version of the test1 ifix jar.
        Set<String> updateFiles = new HashSet<String>();
        updateFiles.add("<file date=\"2013-03-01 12:00\" hash=\"" + TestUtils.generateHash(test3IFixJar) + "\" size=\"" + test3IFixJar.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test3IFixJar, uniqueTestDir) + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixStaticFile) + "\" size=\"" + test1IFixStaticFile.length() + "\" id=\"" +
                        TestUtils.createRelativePath(test1IFixStaticFile, uniqueTestDir) + "\"/>");
        String ifixName1 = "Test-Ifix1";
        TestUtils.createIfixXML(new File(fixesDir, "ifix1.xml"), ifixName1, updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Amend the static files so we get different hashes for each ifix xml.
        fileContents.append("This is the 3rd line of file \n");

        File test2IFixStaticFile = TestUtils.createFile(new File(ifixBinDir, "server.bat"), fileContents);
        // Create the 2nd ifix.xml. This has been superseded and doen't have latest version of any of the files.
        Set<String> updateFiles2 = new HashSet<String>();
        updateFiles2.add("<file date=\"2013-02-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixJar) + "\" size=\"" + test2IFixJar.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test2IFixJar, uniqueTestDir) + "\"/>");
        updateFiles2.add("<file date=\"2013-02-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixStaticFile) + "\" size=\"" + test2IFixStaticFile.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test2IFixStaticFile, uniqueTestDir) + "\"/>");
        String ifixName2 = "Test-Ifix2";
        TestUtils.createIfixXML(new File(fixesDir, "ifix2.xml"), ifixName2, updateFiles2, TestUtils.generateSet("Apar4"));

        // Amend the static files so we get different hashes for each ifix xml.
        fileContents.append("This is the 4th line of file \n");
        File test3IFixStaticFile = TestUtils.createFile(new File(ifixBinDir, "server.bat"), fileContents);

        // Create the 3rd ifix.xml. This has the latest version of server.bat.
        Set<String> updateFiles3 = new HashSet<String>();
        updateFiles3.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixJar) + "\" size=\"" + test1IFixJar.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test1IFixJar, uniqueTestDir) + "\"/>");
        updateFiles3.add("<file date=\"2013-03-01 12:00\" hash=\"" + TestUtils.generateHash(test3IFixStaticFile) + "\" size=\"" + test3IFixStaticFile.length() + "\" id=\"" +
                         TestUtils.createRelativePath(test3IFixStaticFile, uniqueTestDir) + "\"/>");
        String ifixName3 = "Test-Ifix3";
        TestUtils.createIfixXML(new File(fixesDir, "ifix3.xml"), ifixName3, updateFiles3, TestUtils.generateSet("Apar5"));

        // These are the jars files that are put in the runtime lib dir.
        File test1Jar = TestUtils.createJarFile(libDir, "test1", "1.0.0", false, false);

        // Create test feature
        Map<String, String> featureFiles = new HashMap<String, String>();
        featureFiles.put(TestUtils.createRelativePath(test1IFixStaticFile, uniqueTestDir), test1IFixStaticFile.getName());
        featureFiles.put(null, test1Jar.getName());
        TestUtils.createFeature(new File(featuresDir, "test-1.0.mf"), "test-1.0", featureFiles);

        runValidateCommand();
        // Check that the command has run successfully, and ensure it includes the reapply Ifix msg.
        assertChecks2Ifixes(ifixName1, ifixName3);

        TestUtils.copyFile(test3IFixStaticFile, new File(binDir, test3IFixStaticFile.getName()));
        TestUtils.copyFile(test3IFixJar, new File(libDir, test3IFixJar.getName()));

        runValidateCommand();
        // Check that the command has run successfully, and ensure no reapply Ifix msg is issued.
        assertChecks(false, null);
    }

    /**
     * When a fix has been applied we need to use the latest version of the hashcode for validation.
     * This test checks that the latest hash is used (and hence validation passes) for a static file
     * after an ifix has been applied.
     */
    @Test
    public void testStaticContentIfixHashValidation() {
        //mock up a feature
        String featureName = "testIfixHash-1.0";
        File testFeatureFile = generateIfixFeatureWithChecksum(featureName);

        //create a "fixed" version of the feature manifest file
        File fixedTestFeatureFile = generateAugmentedFeatureManifest(featureName, testFeatureFile, "SomeNewMfHeader: someFakeValue");

        // Create an ifix.xml. It has the latest version of the feature manifest we are testing.
        generateSingleFixXml(fixedTestFeatureFile, "TF0001", "2013-08-21 11:15", "Apar1");

        //copy the fix file, equivalent to an archive install of the ifix
        TestUtils.copyFile(fixedTestFeatureFile, testFeatureFile);

        //run the validation
        runValidateCommand();

        //check that validation succeeds and there are no reapply messages
        assertChecks(false, null);
    }

    /**
     * This test ensures that date ordering works when multiple fixes are made to the same file
     */
    @Test
    public void testStaticContentMultipleIfixHashValidation() {
        performMultipleFixTest(false);
    }

    /**
     * This test ensures that two fixes to the same static file with the same date still results
     * in the correct hash being used for validation.
     * This is testing for an issue caused because some fixes were published with incorrect (and constant)
     * date information for some static files.
     */
    @Test
    public void testStaticContentMultipleIfixHashValidationSameDate() {
        performMultipleFixTest(true);
    }

    private void generateSingleFixXml(File fixFile, String ifixName, String date, String... apars) {
        // Create an ifix.xml. It has the latest version of the feature manifest we are testing.
        Set<String> updateFiles = new HashSet<String>();
        updateFiles.add("<file date=\"" + date + "\" hash=\"" + TestUtils.generateHash(fixFile) + "\" size=\"" + fixFile.length() + "\" id=\"" +
                        TestUtils.createRelativePath(fixFile, uniqueTestDir) + "\"/>");
        TestUtils.createIfixXML(new File(fixesDir, ifixName + ".xml"), ifixName, updateFiles, TestUtils.generateSet(apars));
    }

    /**
     * We need to run two very similar tests so we just common it up in this method
     *
     * @param useSameDateForSecondFix
     */
    private void performMultipleFixTest(boolean useSameDateForSecondFix) {
        //mock up a feature
        String featureName = "testIfixHash-1.0";
        File testFeatureFile = generateIfixFeatureWithChecksum(featureName);

        //create a "fixed" version of the feature manifest file
        File fixedTestFeatureFile = generateAugmentedFeatureManifest(featureName, testFeatureFile, "SomeNewMfHeader: someFakeValue");

        // Create an ifix.xml
        generateSingleFixXml(fixedTestFeatureFile, "TF0001", "2013-08-21 11:15", "Apar1");

        // Now perform a second patch of the same file
        File secondFixTestFeatureFile = generateAugmentedFeatureManifest(featureName, testFeatureFile, "AnotherNewMfHeader: differentValue");
        // Create another ifix.xml
        generateSingleFixXml(secondFixTestFeatureFile, "TF0002", "2013-08-21 " + (useSameDateForSecondFix ? "11:15" : "12:15"), "Apar1", "Apar2");

        //copy the fix file, equivalent to an archive install of the ifix
        TestUtils.copyFile(secondFixTestFeatureFile, testFeatureFile);

        //run the validation
        runValidateCommand();

        //check that validation succeeds and there are no reapply messages
        assertChecks(false, null);
    }

    /**
     * This method creates a mock feature manifest and the appropriate checksum file
     *
     * @param featureName
     * @return
     */
    private File generateIfixFeatureWithChecksum(String featureName) {
        String featureMfName = featureName + ".mf";

        //create a test feature
        Map<String, String> featureFiles = new HashMap<String, String>();
        File testFeatureFile = TestUtils.createFeature(new File(featuresDir, featureName + ".mf"), featureName, featureFiles);

        //create a checksum for the original feature
        String originalMfHash = TestUtils.generateHash(testFeatureFile);
        StringBuffer csFileContent = new StringBuffer();
        csFileContent.append("lib/features/" + featureMfName);
        csFileContent.append("=");
        csFileContent.append(originalMfHash);
        TestUtils.createFile(new File(featuresDir, "checksums/com.ibm.websphere.appserver." + featureName + ".cs"), csFileContent);

        return testFeatureFile;
    }

    /**
     * This method augments a feature manifest with some new content, to immitate the patching of a static file
     *
     * @param featureName
     * @param featureFile
     * @param newContent
     * @return
     */
    private File generateAugmentedFeatureManifest(String featureName, File featureFile, String newContent) {
        StringBuffer fixContent = new StringBuffer();
        fixContent.append(newContent + "\n");
        //create the new file
        File fixedTestFeatureFile = TestUtils.createFile(new File(ifixFeaturesDir, featureName + ".mf"), fixContent);
        //append the rest of the feature content to our "fix"
        TestUtils.copyFile(featureFile, fixedTestFeatureFile, true);
        return fixedTestFeatureFile;
    }

    /**
     * This method asserts that the right messages are in the console logs.
     * WARNING THIS ONLY WORKS FOR 1 IFIX the ifixes are a set not a list so could be in any order.
     *
     * @param ifixAppliedMessage - This boolean indicates whether the ifix reapply message is expected or not.
     *            If not, we assert that it isn't in the list.
     * @parem ifixReApplicationMsg - The message to ensure is or isn't in the console logs. If the ifixReApplicationMsg is null, we just check that the
     *        "The following fixes must be reapplied:" prefix is either present or not present depending on the ifixApplicationMessage boolean.
     */
    private void assertChecks(boolean ifixAppliedMessage, String ifixReApplicationMsg) {

        String ifixReApplicationMessagePrefix = "The following fixes must be reapplied:";
        // Assert that we always get a successful run of the command.
        Assert.assertTrue("The console info messages don't contain message \"Product validation completed successfully.\": " + Arrays.toString(consoleInfoMessages.toArray()),
                          consoleInfoMessages.contains("Product validation completed successfully."));

        if (ifixAppliedMessage) {
            if (ifixReApplicationMsg == null) {
                Assert.assertTrue("The console info messages don't contain message \"" + ifixReApplicationMessagePrefix + "\": " +
                                  Arrays.toString(consoleInfoMessages.toArray()),
                                  messagePrefixPresentInLogs(consoleInfoMessages, ifixReApplicationMessagePrefix));
            } else {
                Assert.assertTrue("The console info messages don't contain message \"" + ifixReApplicationMsg + "\": " +
                                  Arrays.toString(consoleInfoMessages.toArray()), consoleInfoMessages.contains(ifixReApplicationMsg));
            }
        } else {
            if (ifixReApplicationMsg == null) {
                Assert.assertFalse("The console info messages unexpectedly contains message \"" + ifixReApplicationMessagePrefix + "\": " +
                                   Arrays.toString(consoleInfoMessages.toArray()),
                                   messagePrefixPresentInLogs(consoleInfoMessages, ifixReApplicationMessagePrefix));
            } else {
                Assert.assertFalse("The console info messages unexpectedly contains message \"" + ifixReApplicationMsg + "\": " +
                                   Arrays.toString(consoleInfoMessages.toArray()), consoleInfoMessages.contains(ifixReApplicationMsg));
            }
        }
    }

    /**
     * This method asserts that 2 Ifixes are in the console logs in either order.
     * It looks for strings of the form "The following fixes must be reapplied: [ifixName1,ifixName2]."
     * or in the reverse order. If we ever needed to check for more than 2 ifies we should re-write this to take a list.
     *
     * @param ifixName1 -
     * @parem ifixName2 -
     */
    private void assertChecks2Ifixes(String ifixName1, String ifixName2) {

        String ifixReApplicationMessagePrefix = "The following fixes must be reapplied:";
        // Assert that we always get a successful run of the command.
        Assert.assertTrue("The console info messages don't contain message \"Product validation completed successfully.\": " + Arrays.toString(consoleInfoMessages.toArray()),
                          consoleInfoMessages.contains("Product validation completed successfully."));

        String message1 = "The following fixes must be reapplied: [" + ifixName1 + ", " + ifixName2 + "].";
        String message2 = "The following fixes must be reapplied: [" + ifixName2 + ", " + ifixName1 + "].";

        Assert.assertTrue("The console info messages don't contain message \"" + message1 + "\" or \"" + message2 + "\": " +
                          Arrays.toString(consoleInfoMessages.toArray()),
                          consoleInfoMessages.contains(message1) || consoleInfoMessages.contains(message2));
    }

    /**
     * This method checks all of the strings in the list of msgs, and checks to see if any entry starts with the specified prefix.
     *
     * @param msgs - The list of Strings to check
     * @param msgPrefix - The prefix to check.
     * @return - A boolean indicating whether any of the strings starts with the message prefix.
     */
    private boolean messagePrefixPresentInLogs(List<String> msgs, String msgPrefix) {
        boolean msgFound = false;
        for (String msg : msgs) {
            if (msg.startsWith(msgPrefix))
                msgFound = true;
        }
        return msgFound;
    }

    /**
     * Run the validate Command after clearing the logs.
     */
    private void runValidateCommand() {
        consoleInfoMessages.clear();
        consoleErrorMessages.clear();
        TestUtils.refreshBundleRepository();
        task.doExecute(context);
        //log the output in case we need to debug
        List<String> output = new ArrayList<String>();
        output.addAll(consoleInfoMessages);
        output.addAll(consoleErrorMessages);
        System.out.println("====== Validate results for test " + name.getMethodName() + "======");
        for (String line : output) {
            System.out.println(line);
        }
    }
}
