/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
import java.net.URISyntaxException;
import java.net.URL;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandConstants;
import com.ibm.ws.product.utility.ExecutionContext;

/**
 *
 */
public class IFixCompareCommandTaskTest {

    private Mockery mockery;
    private ExecutionContext context;
    private CommandConsole console;

    /**
     * This method is run before each test to make the {@link #mockery} object with a mocked {@link #console} and {@link #context} and you can retrieve the console via a call
     * {@link ExecutionContext#getCommandConsole()}
     */
    @Before
    public void createMockery() {
        this.mockery = new Mockery();
        this.context = mockery.mock(ExecutionContext.class);
        this.console = mockery.mock(CommandConsole.class);
        this.mockery.checking(new Expectations() {
            {
                allowing(context).getCommandConsole();
                will(returnValue(console));
                // Some messages are spaced out a bit so always allow ""
                allowing(console).printlnInfoMessage("");
            }
        });
    }

    /**
     * This test makes sure that when you have matching APARs to iFixes then the compare --target works correctly when comparing with directory install
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testCompareToMatchingApars() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL threeAparInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/twoIFixesThreeAparsTwoFP");
        final File threeAparInstallFile = new File(threeAparInstallFileLocation.toURI());
        URL threeAparFixPackFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oneFixPackThreeApars");
        final File threeAparFixPackFile = new File(threeAparFixPackFileLocation.toURI());
        this.setUpTargetMockery(threeAparInstallFile, threeAparFixPackFile);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnInfoMessage(
                                                  with("All of the iFixes in the image at " + threeAparInstallFile.getAbsolutePath() + " are present in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("The following iFixes are in the image at " + threeAparInstallFile.getAbsolutePath() + " and in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("1 in the iFix(es): [iFix1]"));
                oneOf(console).printlnInfoMessage(with("2 in the iFix(es): [iFix1]"));
                oneOf(console).printlnInfoMessage(with("3 in the iFix(es): [iFix2]"));
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test checks that when we compare two images with iFixes we use the iFixes in the target when working out if all
     * the fixes exist in the target install.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testCompareIncludesIFixes() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL threeAparInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/twoIFixesThreeAparsTwoFP");
        final File threeAparInstallFile = new File(threeAparInstallFileLocation.toURI());
        URL oneIFixTwoFP = this.getClass().getClassLoader().getResource("mockInstalls/oneIFixTwoFP");
        final File threeAparFixPackFile = new File(oneIFixTwoFP.toURI());
        this.setUpTargetMockery(threeAparInstallFile, threeAparFixPackFile);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnInfoMessage(
                                                  with("Some of the iFixes in the image at " + threeAparInstallFile.getAbsolutePath() + " are missing in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("The following iFixes are in the image at " + threeAparInstallFile.getAbsolutePath() + " but are missing in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("3 in the iFix(es): [iFix2]"));
                oneOf(console).printlnInfoMessage(with("The following iFixes are in the image at " + threeAparInstallFile.getAbsolutePath() + " and in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("1 in the iFix(es): [iFix1]"));
                oneOf(console).printlnInfoMessage(with("2 in the iFix(es): [iFix1]"));
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that when you have some APARs in iFixes then the compare --target works correctly when comparing with archive install
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testCompareToMissingApars() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL installLocation = this.getClass().getClassLoader().getResource("mockInstalls/twoIFixesThreeAparsTwoFP");
        final File installFile = new File(installLocation.toURI());
        URL fpLocation = this.getClass().getClassLoader().getResource("mockInstalls/TwoAparsArchiveFixPack.jar");
        final File fpFile = new File(fpLocation.toURI());
        this.setUpTargetMockery(installFile, fpFile);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnInfoMessage(
                                                  with("Some of the iFixes in the image at " + installFile.getAbsolutePath() + " are missing in the image at "
                                                       + fpFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("The following iFixes are in the image at " + installFile.getAbsolutePath() + " but are missing in the image at "
                                                       + fpFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("2 in the iFix(es): [iFix1]"));
                oneOf(console).printlnInfoMessage(with("The following iFixes are in the image at " + installFile.getAbsolutePath() + " and in the image at "
                                                       + fpFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("1 in the iFix(es): [iFix1]"));
                oneOf(console).printlnInfoMessage(with("3 in the iFix(es): [iFix2]"));
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    public static final class APARTestData {
        public final String description;
        public final String aparsOption;
        public final String[] presentApars;
        public final String[] missingApars;
        public final String resultText;

        public APARTestData(String description, String aparsOption, String[] presentApars, String[] missingApars) {
            this.description = description;
            this.aparsOption = aparsOption;
            this.presentApars = presentApars;
            this.missingApars = missingApars;
            this.resultText = getResultText(this.presentApars, this.missingApars);
        }

        public static final String ALL_APARS_PRESENT_TEXT = "All of the APARs are in the installation.";
        public static final String MISSING_APARS_PREFIX = "The following APARs are not in the installation: [";
        public static final String MISSING_APARS_DELIM = ", ";
        public static final String MISSING_APARS_SUFFIX = "].";

        public static String getResultText(String[] presentApars, String[] missingApars) {
            if ((missingApars == null) || (missingApars.length == 0)) {
                return ALL_APARS_PRESENT_TEXT;
            }

            StringBuilder builder = new StringBuilder(MISSING_APARS_PREFIX);
            for (int aparNo = 0; aparNo < missingApars.length; aparNo++) {
                if (aparNo > 0) {
                    builder.append(MISSING_APARS_DELIM);
                }
                builder.append(missingApars[aparNo]);
            }
            builder.append(MISSING_APARS_SUFFIX);

            return builder.toString();
        }
    }

    public static final String[] EMPTY_STRINGS = new String[] {};

    public static final APARTestData[] COMPREHENSIVE_APAR_TEST_DATA =
                    new APARTestData[] { new APARTestData("empty", "", EMPTY_STRINGS, EMPTY_STRINGS),
                                        new APARTestData("spaces", "   ", EMPTY_STRINGS, EMPTY_STRINGS),
                                        new APARTestData("tabs", "\t\t\t", EMPTY_STRINGS, EMPTY_STRINGS),
                                        new APARTestData("bare comma", ",", EMPTY_STRINGS, EMPTY_STRINGS),
                                        new APARTestData("comma and spaces", " , ", EMPTY_STRINGS, EMPTY_STRINGS),
                                        new APARTestData("comma and tabs", "\t,\t", EMPTY_STRINGS, EMPTY_STRINGS),
                                        new APARTestData("one absent", "PM99999", EMPTY_STRINGS, new String[] { "PM99999" }),
                                        new APARTestData("one absent, extra spaces", "  PM99999  ", EMPTY_STRINGS, new String[] { "PM99999" }),
                                        new APARTestData("one absent, extra tabs", "\t\tPM99999\t\t", EMPTY_STRINGS, new String[] { "PM99999" }),
                                        new APARTestData("multiple absent", "PM99999,PM99998", EMPTY_STRINGS, new String[] { "PM99999", "PM99998" }),
                                        new APARTestData("multiple absent, extra spaces", " PM99999 , PM99998 ", EMPTY_STRINGS, new String[] { "PM99999", "PM99998" }),
                                        new APARTestData("duplicate absent", "PM99999,PM99998,PM99999", EMPTY_STRINGS, new String[] { "PM99999", "PM99998" }),
                                        new APARTestData("one present", "1", new String[] { "1" }, EMPTY_STRINGS),
                                        new APARTestData("one present, one absent, comma delimited", "1,PM99999", new String[] { "1" }, new String[] { "PM99999" }),
                                        new APARTestData("one present, one absent, space delimited", "1 PM99999", new String[] { "1" }, new String[] { "PM99999" }),
                                        new APARTestData("one present, one absent, tab delimited", "1\tPM99999", new String[] { "1" }, new String[] { "PM99999" }),
                                        new APARTestData("two present, one absent, space delimited", "1 10 PM99999", new String[] { "1", "10" }, new String[] { "PM99999" }),
                                        new APARTestData("two present, one absent, mixed delimiters", "1,10 PM99999", new String[] { "1", "10" }, new String[] { "PM99999" })
                    };

    @Test
    public void testCompareAparsAllPresent() throws URISyntaxException {
        // Use the sample data with two ifixes and three APARs
        APARTestData testData = new APARTestData("three present", "1,4,10", new String[] { "1", "4", "10" }, EMPTY_STRINGS);
        runAparsTest(testData); // throws URISyntaxException
    }

    @Test
    public void testCompareAparsSomeMissing() throws URISyntaxException {
        APARTestData testData = new APARTestData("two present, one absent", "1,4,apar5", new String[] { "1", "4" }, new String[] { "apar5" });
        runAparsTest(testData); // throws URISyntaxException
    }

    @Test
    public void testAparsComprehensive() throws URISyntaxException {
        for (int testNo = 0; testNo < COMPREHENSIVE_APAR_TEST_DATA.length; testNo++) {
            if (testNo > 0) {
                createMockery(); // Get this for free on the first test.  Recreate for the remaining tests.
            }
            runAparsTest(COMPREHENSIVE_APAR_TEST_DATA[testNo]); // throws URISyntaxException
        }
    }

    public void runAparsTest(final APARTestData productInfoData) throws URISyntaxException {
        URL installLocation = this.getClass().getClassLoader().getResource("mockInstalls/twoIFixesThreeAparsTwoFP");
        final File installFile = new File(installLocation.toURI());
        mockery.checking(new Expectations() {
            {
                allowing(context).getAttribute(with(CommandConstants.WLP_INSTALLATION_LOCATION), with(File.class));
                will(returnValue(installFile));
                allowing(context).optionExists(with("--target"));
                will(returnValue(false));
                allowing(context).optionExists(with("--apars"));
                will(returnValue(true));
                allowing(context).optionExists(with("--verbose"));
                will(returnValue(false));
                allowing(context).getOptionValue(with("--apars"));
                will(returnValue(productInfoData.aparsOption));
            }
        });

        // Verify the specified expected result.
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnInfoMessage(with(productInfoData.resultText));
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * Test to make sure when no input is provided an error is output
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testNoOptionsSet() throws URISyntaxException {
        // Add expectations for no input
        mockery.checking(new Expectations() {
            {
                allowing(context).optionExists(with("--target"));
                will(returnValue(false));
                allowing(context).optionExists(with("--apars"));
                will(returnValue(false));
            }
        });

        // Add expectations for the error
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnErrorMessage(
                                                   with("Unable to compare because of invalid usage of command, one of --target or --apars\nmust be supplied."));
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * Test to make sure when no input is provided an error is output
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testToOptionDoesNotExist() throws URISyntaxException {
        // Add expectations for to an invalid location
        URL installLocation = this.getClass().getClassLoader().getResource("mockInstalls/twoIFixesThreeAparsTwoFP");
        final File installFile = new File(installLocation.toURI());
        final File missingFile = new File(installFile, "foo");
        this.setUpTargetMockery(installFile, missingFile);

        // Add expectations for the error
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnErrorMessage(
                                                   with("Installation file " + missingFile.getAbsolutePath() + " does not exist."));
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * Tests that when the to points to a file then there is a warning
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testInvalidToOption() throws URISyntaxException {
        // Add expectations for to an invalid location
        URL installLocation = this.getClass().getClassLoader().getResource("mockInstalls/twoIFixesThreeAparsTwoFP");
        final File installFile = new File(installLocation.toURI());
        final File illegalFile = new File(this.getClass().getClassLoader().getResource("mockInstalls/twoIFixesThreeAparsTwoFP/lib/fixes/iFix1.xml").toURI());
        this.setUpTargetMockery(installFile, illegalFile);

        // Add expectations for the error
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnErrorMessage(
                                                   with("The install location " + illegalFile.getAbsolutePath() + " is not a directory or archive file (.jar or .zip)"));
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * Tests that when the to points to an install directory that has an invalid APAR ID zip in it then there is an appropriate error
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testInvalidZip() throws URISyntaxException {
        // Add expectations for to an invalid location
        URL installLocation = this.getClass().getClassLoader().getResource("mockInstalls/twoIFixesThreeAparsTwoFP");
        final File installFile = new File(installLocation.toURI());
        final File illegalFile = new File(this.getClass().getClassLoader().getResource("mockInstalls/invalidZip").toURI());
        this.setUpTargetMockery(installFile, illegalFile);

        // Add expectations for the error
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnErrorMessage(
                                                   with("The installation location "
                                                        + illegalFile.getAbsolutePath()
                                                        + " is invalid. It contains an archive for listing APARs (wlp_fp8509_aparIds.zip) but no file listing the APARs inside it aparIds.csv."));
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that when you have a &lt;file&gt; element that has a badly formatted date then it produces the right error
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testInvalidTimeInFile() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL badDateInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/badDate");
        final File badDateInstallFile = new File(badDateInstallFileLocation.toURI());
        URL threeAparFixPackFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oneFixPackThreeApars");
        final File threeAparFixPackFile = new File(threeAparFixPackFileLocation.toURI());
        this.setUpTargetMockery(badDateInstallFile, threeAparFixPackFile, true);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnInfoMessage(
                                                  with("All of the iFixes in the image at " + badDateInstallFile.getAbsolutePath() + " are present in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("The following iFixes are in the image at " + badDateInstallFile.getAbsolutePath() + " and in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("1 in the iFix(es): [badDate]"));
                oneOf(console).printlnErrorMessage(
                                                   "Unable to read iFix <file/> information for badDate so cannot check if the iFix is still installed");
                oneOf(console).printlnErrorMessage(
                                                   "The following iFix(es) were not included in the comparison because their XML is invalid (use --verbose option for more information): [badDate]");
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that when you don't have any properties for WAS versions then that check is skipped
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testNoProperties() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL noPropertiesInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/noProperties");
        final File noPropertiesInstallFile = new File(noPropertiesInstallFileLocation.toURI());
        URL threeAparFixPackFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oneFixPackThreeApars");
        final File threeAparFixPackFile = new File(threeAparFixPackFileLocation.toURI());
        this.setUpTargetMockery(noPropertiesInstallFile, threeAparFixPackFile);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnErrorMessage(
                                                   "Unable to obtain version for the current install so cannot check if iFixes are applicable to this installation. The exception message is: No properties were found with productId 'com.ibm.websphere.appserver'");
                oneOf(console).printlnInfoMessage(
                                                  with("All of the iFixes in the image at " + noPropertiesInstallFile.getAbsolutePath() + " are present in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("The following iFixes are in the image at " + noPropertiesInstallFile.getAbsolutePath() + " and in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("1 in the iFix(es): [noProperties]"));
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that when there is an XML with no offering element that you get a warning
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testNoOffering() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL noOfferingInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/noOffering");
        final File noOfferingInstallFile = new File(noOfferingInstallFileLocation.toURI());
        URL threeAparFixPackFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oneFixPackThreeApars");
        final File threeAparFixPackFile = new File(threeAparFixPackFileLocation.toURI());
        this.setUpTargetMockery(noOfferingInstallFile, threeAparFixPackFile, true);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnErrorMessage(
                                                   "The iFix XML meta data for noOffering does not contain an offering element so cannot check that the iFix is valid for this installation");
                oneOf(console).printlnInfoMessage(
                                                  with("All of the iFixes in the image at " + noOfferingInstallFile.getAbsolutePath() + " are present in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("The following iFixes are in the image at " + noOfferingInstallFile.getAbsolutePath() + " and in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(with("1 in the iFix(es): [noOffering]"));
                oneOf(console).printlnErrorMessage(
                                                   "The following iFix(es) were not included in the comparison because their XML is invalid (use --verbose option for more information): [noOffering]");
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that when there is an XML with no problem element that you get a warning
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testNoApar() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL noAparInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/noApar");
        final File noAparInstallFile = new File(noAparInstallFileLocation.toURI());
        URL threeAparFixPackFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oneFixPackThreeApars");
        final File threeAparFixPackFile = new File(threeAparFixPackFileLocation.toURI());
        this.setUpTargetMockery(noAparInstallFile, threeAparFixPackFile, true);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnErrorMessage(
                                                   "The iFix noApar does not have any APARs listed in <problem/> elements in the meta data XML");
                oneOf(console).printlnInfoMessage(
                                                  with("All of the iFixes in the image at " + noAparInstallFile.getAbsolutePath() + " are present in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnErrorMessage(
                                                   "The following iFix(es) were not included in the comparison because their XML is invalid (use --verbose option for more information): [noApar]");
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test is the same as the previous test so it makes sure that when there is an XML with no problem element that you get a warning but there is no verbose option so it
     * should not give details of what is wrong in the XML.
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testNoApar_noVerbose() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL noAparInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/noApar");
        final File noAparInstallFile = new File(noAparInstallFileLocation.toURI());
        URL threeAparFixPackFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oneFixPackThreeApars");
        final File threeAparFixPackFile = new File(threeAparFixPackFileLocation.toURI());
        this.setUpTargetMockery(noAparInstallFile, threeAparFixPackFile);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnInfoMessage(
                                                  with("All of the iFixes in the image at " + noAparInstallFile.getAbsolutePath() + " are present in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnErrorMessage(
                                                   "The following iFix(es) were not included in the comparison because their XML is invalid (use --verbose option for more information): [noApar]");
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that when there is a feature JAR in the XML that isn't on the disk then that iFix is ignored
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testMissingFeatureJar() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL missingFeatureInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/missingFeatureJar");
        final File missingFeatureInstallFile = new File(missingFeatureInstallFileLocation.toURI());
        URL threeAparFixPackFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oneFixPackThreeApars");
        final File threeAparFixPackFile = new File(threeAparFixPackFileLocation.toURI());
        this.setUpTargetMockery(missingFeatureInstallFile, threeAparFixPackFile);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnInfoMessage(
                                                  with("All of the iFixes in the image at " + missingFeatureInstallFile.getAbsolutePath() + " are present in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(
                                                  "The following iFix(es) were not included in the comparison because the files listed in the <file/> are no longer present or are out of date: [missingFeatureJar]");
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that when there is a non-feature JAR file that is older than the time stamp in the XML then the test fails. This test will start failing in 2022 if the
     * files timestamps are recreated when check out of RTC!
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testOldStaticFile() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL oldStaticFileInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oldStaticFile");
        final File oldStaticFileInstallFile = new File(oldStaticFileInstallFileLocation.toURI());
        URL threeAparFixPackFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oneFixPackThreeApars");
        final File threeAparFixPackFile = new File(threeAparFixPackFileLocation.toURI());
        this.setUpTargetMockery(oldStaticFileInstallFile, threeAparFixPackFile);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnInfoMessage(
                                                  with("All of the iFixes in the image at " + oldStaticFileInstallFile.getAbsolutePath() + " are present in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(
                                                  "The following iFix(es) were not included in the comparison because the files listed in the <file/> are no longer present or are out of date: [oldStaticFile]");
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that when there is an iFix that is applicable to an old version of WAS then it isn't included in the test
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testOldVersion() throws URISyntaxException {
        // Add expectations to all the command to get the install location and compare to location
        URL oldVersionInstallFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oldVersion");
        final File oldVersionInstallFile = new File(oldVersionInstallFileLocation.toURI());
        URL threeAparFixPackFileLocation = this.getClass().getClassLoader().getResource("mockInstalls/oneFixPackThreeApars");
        final File threeAparFixPackFile = new File(threeAparFixPackFileLocation.toURI());
        this.setUpTargetMockery(oldVersionInstallFile, threeAparFixPackFile);

        // Add expectations for the result
        mockery.checking(new Expectations() {
            {
                oneOf(console).printlnInfoMessage(
                                                  with("All of the iFixes in the image at " + oldVersionInstallFile.getAbsolutePath() + " are present in the image at "
                                                       + threeAparFixPackFile.getAbsolutePath() + "."));
                oneOf(console).printlnInfoMessage(
                                                  "The following iFix(es) were not included in the comparison because it is not applicable to this version of WebSphere Application Server: [oldVersion]");
            }
        });

        // Run the test
        IFixCompareCommandTask testObject = new IFixCompareCommandTask();
        testObject.doExecute(context);
        mockery.assertIsSatisfied();
    }

    /**
     * Sets up the mockery to allow the --target to be run by itself defaulting verbose to false
     * 
     * @param installLocation The install location for the main product
     * @param targetFile The fix pack to put in the target
     */
    private void setUpTargetMockery(final File installLocation, final File targetFile) {
        setUpTargetMockery(installLocation, targetFile, false);
    }

    /**
     * Sets up the mockery to allow the --target to be run by itself
     * 
     * @param installLocation The install location for the main product
     * @param targetFile The fix pack to put in the target
     * @param verbose <code>true</code> if the command should be run in verbose mode
     */
    private void setUpTargetMockery(final File installLocation, final File targetFile, final boolean verbose) {
        mockery.checking(new Expectations() {
            {
                allowing(context).getAttribute(with(CommandConstants.WLP_INSTALLATION_LOCATION), with(File.class));
                will(returnValue(installLocation));
                allowing(context).optionExists(with("--target"));
                will(returnValue(true));
                allowing(context).optionExists(with("--apars"));
                will(returnValue(false));
                allowing(context).optionExists(with("--verbose"));
                will(returnValue(verbose));
                allowing(context).getOptionValue(with("--target"));
                will(returnValue(targetFile.getAbsolutePath()));
            }
        });
    }

}
