/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package componenttest.topology.utils.tck;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Assume;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.Props;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.TCKJarInfo;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import junit.framework.AssertionFailedError;

/**
 * MvnUtils allows an arquillian based MicroProfile TCK suite to be launched via Maven. The results will then be converted to junit format and presented
 * as if they were the output of a normal FAT project.
 */
public class TCKRunner {

    private static final Class<TCKRunner> c = TCKRunner.class;

    private static final String DEFAULT_FAILSAFE_UNDEPLOYMENT = "true";
    private static final String DEFAULT_APP_DEPLOY_TIMEOUT = "180";
    private static final String DEFAULT_APP_UNDEPLOY_TIMEOUT = "60";
    private static final int DEFAULT_MBEAN_TIMEOUT = 60000;

    private static final String DEFAULT_SUITE_FILENAME = "tck-suite.xml";
    private static final String MVN_FILENAME_PREFIX = "mvnOutput_";

    private static final String RELATIVE_POM_FILE = "tck/pom.xml";
    private static final String RELATIVE_POM_FILE2 = "pom.xml";
    private static final String RELATIVE_TCK_RUNNER = "publish/tckRunner";
    private static final String MVN_CLEAN = "clean";
    private static final String MVN_TEST = "test";
    private static final String MVN_DEPENDENCY = "dependency:list";

    private static final String SUREFIRE_REPORTS = "surefire-reports";
    private static final String TESTNG_REPORTS = SUREFIRE_REPORTS + "/junitreports";

    private static final String MVN_TEST_OUTPUT_FILENAME_PREFIX = MVN_FILENAME_PREFIX + MVN_TEST + "_";
    private static final String MVN_TARGET_FOLDER_PREFIX = "tck_";

    private static final String FAT_TEST_PREFIX = "fat.test.";
    private static final String ARTIFACTORY_SERVER_KEY = "artifactory.download.server";
    private static final String ARTIFACTORY_USER_KEY = "artifactory.download.user";
    private static final String ARTIFACTORY_TOKEN_KEY = "artifactory.download.token";
    private static final String MVN_DISTRIBUTION_URL_KEY = "distributionUrl";
    private static final String MVN_WRAPPER_URL_KEY = "wrapperUrl";

    private final String bucketName;
    private final String testName;
    private final LibertyServer server;
    private final String suiteFileName;
    private final Map<String, String> additionalMvnProps;
    private final boolean isTestNG;
    private final Type type;
    private final String specName;
    private final File mavenUserHome;
    private final File settingsFile;
    private final File tckRunnerDir;

    private final File mvnWrapperDir;

    /**
     * runs "mvn clean test" in the tck folder
     *
     * @param server     the liberty server which should be used to run the TCK
     * @param bucketName the name of the test project
     * @param testName   the name of the method that's being used to launch the TCK
     * @param type       the type of TCK (either MICROPROFILE or JAKARTA)
     * @param specName   the formal name for the specification being tested
     */
    public static void runTCK(LibertyServer server, String bucketName, String testName, Type type, String specName) throws Exception {
        runTCK(server, bucketName, testName, type, specName, DEFAULT_SUITE_FILENAME, RELATIVE_TCK_RUNNER, Collections.<String, String> emptyMap());
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     *
     * @param server          the liberty server which should be used to run the TCK
     * @param bucketName      the name of the test project
     * @param testName        the name of the method that's being used to launch the TCK
     * @param type            the type of TCK (either MICROPROFILE or JAKARTA)
     * @param specName        the formal name for the specification being tested
     * @param additionalProps java properties to set when running the mvn command
     */
    public static void runTCK(LibertyServer server, String bucketName, String testName, Type type, String specName,
                              Map<String, String> additionalProps) throws Exception {
        runTCK(server, bucketName, testName, type, specName, DEFAULT_SUITE_FILENAME, RELATIVE_TCK_RUNNER, additionalProps);
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     *
     * @param  server        the liberty server which should be used to run the TCK
     * @param  bucketName    the name of the test project
     * @param  testName      the name of the method that's being used to launch the TCK
     * @param  type          the type of TCK (either MICROPROFILE or JAKARTA)
     * @param  specName      the formal name for the specification being tested
     * @param  suiteFileName the name of the suite xml file
     * @return               the integer return code from the mvn command. Anything other than 0 should be regarded as a failure.
     * @throws Exception     occurs if anything goes wrong in setting up and running the mvn command.
     */
    public static void runTCK(LibertyServer server, String bucketName, String testName, Type type, String specName,
                              String suiteFileName) throws Exception {
        runTCK(server, bucketName, testName, type, specName, suiteFileName, RELATIVE_TCK_RUNNER, Collections.<String, String> emptyMap());
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     *
     * @param  server          the liberty server which should be used to run the TCK
     * @param  bucketName      the name of the test project
     * @param  testName        the name of the method that's being used to launch the TCK
     * @param  type            the type of TCK (either MICROPROFILE or JAKARTA)
     * @param  specName        the formal name for the specification being tested
     * @param  suiteFileName   the name of the suite xml file
     * @param  additionalProps java properties to set when running the mvn command
     * @return                 the integer return code from the mvn command. Anything other than 0 should be regarded as a failure.
     * @throws Exception       occurs if anything goes wrong in setting up and running the mvn command.
     */
    public static void runTCK(LibertyServer server, String bucketName, String testName, Type type, String specName, String suiteFileName,
                              Map<String, String> additionalProps) throws Exception {
        TCKRunner mvn = new TCKRunner(server, bucketName, testName, type, specName, suiteFileName, RELATIVE_TCK_RUNNER, additionalProps);
        mvn.runTCK();
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     *
     * @param  server            the liberty server which should be used to run the TCK
     * @param  bucketName        the name of the test project
     * @param  testName          the name of the method that's being used to launch the TCK
     * @param  type              the type of TCK (either MICROPROFILE or JAKARTA)
     * @param  specName          the formal name for the specification being tested
     * @param  suiteFileName     the name of the suite xml file
     * @param  relativeTckRunner the relative path to the TCK runner when multiple exist
     * @param  additionalProps   java properties to set when running the mvn command
     * @return                   the integer return code from the mvn command. Anything other than 0 should be regarded as a failure.
     * @throws Exception         occurs if anything goes wrong in setting up and running the mvn command.
     */
    public static void runTCK(LibertyServer server, String bucketName, String testName, Type type, String specName, String suiteFileName,
                              String relativeTckRunner, Map<String, String> additionalProps) throws Exception {
        TCKRunner mvn = new TCKRunner(server, bucketName, testName, type, specName, suiteFileName, relativeTckRunner, additionalProps);
        mvn.runTCK();
    }

    /**
     * Full constructor for MvnUtils. In most cases one of the static convenience methods should be used instead of calling this directly.
     *
     * @param  server          the liberty server which should be used to run the TCK
     * @param  bucketName      the name of the test project
     * @param  testName        the name of the method that's being used to launch the TCK
     * @param  type            the type of TCK (either MICROPROFILE or JAKARTA)
     * @param  specName        the formal name for the specification being tested
     * @param  suiteFileName   the name of the suite xml file
     * @param  additionalProps java properties to set when running the mvn command
     * @throws IOException
     */
    private TCKRunner(LibertyServer server, String bucketName, String testName, Type type, String specName, String suiteFileName,
                      String relativeTckRunner, Map<String, String> additionalMvnProps) throws IOException {
        this.server = server;
        this.suiteFileName = suiteFileName;
        this.bucketName = bucketName;
        this.testName = testName;
        this.type = type;
        this.specName = specName;
        this.additionalMvnProps = additionalMvnProps;
        this.isTestNG = suiteFileName != null;
        File relativeTckRunnerDir = new File(relativeTckRunner);
        if (relativeTckRunnerDir.exists() && relativeTckRunnerDir.isDirectory()) {
            this.tckRunnerDir = relativeTckRunnerDir.getAbsoluteFile();
        } else {
            this.tckRunnerDir = new File(RELATIVE_TCK_RUNNER).getAbsoluteFile();
        }

        this.mavenUserHome = getM2Dir();
        //create and populate the dev/.m2 folder if it does not already exist
        if (!this.mavenUserHome.exists()) {
            createDirectory(this.mavenUserHome);
            exportMvnSettings(this.mavenUserHome);
        }

        this.mvnWrapperDir = new File(this.tckRunnerDir, ".mvn");
        if (this.mvnWrapperDir.exists() && !this.mvnWrapperDir.isDirectory()) {
            throw new IOException(this.mvnWrapperDir + " exists but is not a directory.");
        }

        //create and populate the tckRunner/.mvn folder if it does not already exist
        if (!this.mvnWrapperDir.exists()) {
            createDirectory(this.mvnWrapperDir);
            exportMvnWrapper(this.mvnWrapperDir);
        }

        this.settingsFile = new File(this.mavenUserHome, "settings.xml");
        Log.info(c, "TCKRunner", "Using maven settings file: " + this.settingsFile);
    }

    /**
     * run the TCK and process the results
     */
    private void runTCK() throws Exception {
        Assume.assumeThat(System.getProperty("os.name"), CoreMatchers.not("OS/400")); // skip tests on IBM i due to mvn issue.

        String[] testOutput = runCleanTestCmd();
        List<String> failingTestsList = postProcessTestResults(testOutput);
        assertTestsPassed(this.bucketName, this.testName, failingTestsList);

        String[] dependencyOutput = runDependencyCmd();
        TCKJarInfo tckJarInfo = TCKUtilities.getTCKJarInfo(this.type, dependencyOutput);
        TCKResultsInfo resultsInfo = new TCKResultsInfo(this.type, this.specName, this.server, tckJarInfo);
        TCKResultsWriter.preparePublicationFile(resultsInfo);
    }

    /**
     * Export the maven wrapper scripts, jars and config to the TCK working directory
     * Updates the maven wrapper config to use the correct artifactory host
     *
     * @throws IOException
     */
    private static void exportMvnWrapper(File exportDir) throws IOException {
        File mvnwFile = exportResource("mvnw", exportDir);
        mvnwFile.setExecutable(true, false);

        if (TCKUtilities.isZos()) {
            TCKUtilities.zosTagASCII(mvnwFile);
        }

        File mvnwCmdFile = exportResource("mvnw.cmd", exportDir);
        mvnwCmdFile.setExecutable(true, false);

        File targetFolder = createDirectory(exportDir, ".mvn");
        targetFolder = createDirectory(targetFolder, "wrapper");

        exportResource("maven-wrapper.jar", targetFolder);

        File wrapperPropertiesFile = exportResource("maven-wrapper.properties", targetFolder);
        updatePropertiesFile(wrapperPropertiesFile);
    }

    /**
     * Export the maven settings.xml file to the Maven User Home dir (.m2 folder).
     *
     * @throws IOException
     */
    private static File exportMvnSettings(File exportDir) throws IOException {
        File settingsFile = exportResource("settings.xml", exportDir);
        return settingsFile;
    }

    /**
     * Update the mvnw wrapper properties file to use the artifactory server defined in the
     * environment. Specifically, this looks for the "distributionUrl" and "wrapperUrl" properties
     * and replaces "${artifactory.download.server}" with the value of the "artifactory.download.server"
     * environment variable.
     *
     * @param  wrapperPropertiesFile
     * @return
     * @throws IOException
     */
    private static File updatePropertiesFile(File wrapperPropertiesFile) throws IOException {
        //get the artifactory server
        String artifactoryServer = System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_SERVER_KEY);
        if (artifactoryServer != null) {
            //load the properties file
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream(wrapperPropertiesFile);
            try {
                props.load(fis);
            } finally {
                fis.close();
            }
            //get the existing value of the distributionUrl property
            String distributionURL = props.getProperty(MVN_DISTRIBUTION_URL_KEY);
            //substitute the server string
            distributionURL = distributionURL.replace("${" + ARTIFACTORY_SERVER_KEY + "}", artifactoryServer);
            //set it back into the properties
            props.setProperty(MVN_DISTRIBUTION_URL_KEY, distributionURL);
            Log.info(c, "updatePropertiesFile", MVN_DISTRIBUTION_URL_KEY + "=" + distributionURL);

            //get the existing value of the wrapperUrl property
            String wrapperURL = props.getProperty(MVN_WRAPPER_URL_KEY);
            //substitute the server string
            wrapperURL = wrapperURL.replace("${" + ARTIFACTORY_SERVER_KEY + "}", artifactoryServer);
            //set it back into the properties
            props.setProperty(MVN_WRAPPER_URL_KEY, wrapperURL);
            Log.info(c, "updatePropertiesFile", MVN_WRAPPER_URL_KEY + "=" + wrapperURL);

            //write the properties back out into the original file
            FileOutputStream fos = new FileOutputStream(wrapperPropertiesFile);
            try {
                props.store(fos, "MVN Wrapper Properties");
            } finally {
                fos.close();
            }
        }
        return wrapperPropertiesFile;
    }

    private static File createDirectory(File parent, String dirName) throws IOException {
        File targetFolder = new File(parent, dirName);
        targetFolder = createDirectory(targetFolder);
        return targetFolder;
    }

    private static File createDirectory(File targetFolder) throws IOException {
        if (targetFolder.exists()) {
            if (!targetFolder.isDirectory()) {
                throw new IOException("Target path exists but is not a directory: " + targetFolder);
            }
        } else {
            targetFolder = Files.createDirectory(targetFolder.toPath()).toFile();
        }
        return targetFolder;
    }

    /**
     * Export a file from the fattest.simplicity jar to the local filesystem. If the local file
     * already exists then this method does nothing, it does not overwrite.
     *
     * @param  sourceName   The name of the jar resource, relative to TCKRunner.class
     * @param  targetFolder The target local folder to export to
     * @return              the File of the exported file (or the existing one)
     * @throws IOException
     */
    private static File exportResource(String sourceName, File targetFolder) throws IOException {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        File targetFile = null;
        if (targetFolder.exists() && targetFolder.isDirectory()) {
            targetFile = new File(targetFolder, sourceName);
            if (targetFile.exists()) {
                Log.info(c, "exportResource", "Target File already exists: " + targetFile);
            } else {
                try {
                    stream = TCKRunner.class.getResourceAsStream(sourceName);
                    if (stream == null) {
                        throw new IOException("Cannot get resource \"" + sourceName + "\" from Jar file.");
                    }

                    int readBytes;
                    byte[] buffer = new byte[4096];
                    resStreamOut = new FileOutputStream(targetFile);
                    while ((readBytes = stream.read(buffer)) > 0) {
                        resStreamOut.write(buffer, 0, readBytes);
                    }
                    resStreamOut.flush();
                } catch (IOException e) {
                    Log.info(c, "exportResource", "Failed to export: " + sourceName + ". Exception: " + e.getMessage());
                    throw e;
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                    if (resStreamOut != null) {
                        resStreamOut.close();
                    }
                }

                if (targetFile.exists()) {
                    Log.info(c, "exportResource", sourceName + " exported to: " + targetFile);
                } else {
                    Log.info(c, "exportResource", "Failed to export " + sourceName + " to: " + targetFile);
                }
            }
        } else {
            Log.info(c, "exportResource", "Target folder does not exist or is not a directory: " + targetFolder);
        }

        return targetFile;
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     */
    private String[] runCleanTestCmd() throws Exception {

        String[] testcmd = getMvnCommandArray(MVN_CLEAN, MVN_TEST);
        String[] mvnOutput = runMvnCmd(testcmd, getTCKRunnerDir(), getMavenUserHome());
        TCKUtilities.writeStringsToFile(mvnOutput, getMvnTestOutputFile());
        return mvnOutput;
    }

    /**
     * runs "mvn dependency:list" in the tck folder, passing through all the required properties
     */
    private String[] runDependencyCmd() throws Exception {
        String[] dependencyCmd = getMvnCommandArray(MVN_DEPENDENCY);
        String[] mvnOutput = runMvnCmd(dependencyCmd, getTCKRunnerDir(), getMavenUserHome());
        TCKUtilities.writeStringsToFile(mvnOutput, getMvnDependencyOutputFile());
        return mvnOutput;
    }

    /**
     * Get a list of Files which represent JUnit xml results files.
     *
     * @return a list of junit XML results files
     */
    private List<File> findJunitResultFiles(String[] mvnOutput) {
        File surefileResultsDir = getSureFireResultsDir();

        File[] resultsFiles = surefileResultsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("TEST.*\\.xml");
            }
        });

        if (resultsFiles == null || resultsFiles.length == 0) {
            Assert.fail("No TCK test JUnit result files were found in the results directory (" + surefileResultsDir.toString() +
                        ") which suggests the TCK tests did not run\n"
                        + "Errors found in mvnOutput were:\n" +
                        getErrorsFromMvnOutput(mvnOutput));
        }

        return Arrays.asList(resultsFiles);
    }

    private static String getErrorsFromMvnOutput(String[] mvnOutput) {
        StringBuilder sb = new StringBuilder();
        sb.append("### maven test output:\n");
        for (String line : mvnOutput) {
            if (line.contains("[ERROR]")) {
                sb.append(line).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Get the absolute path of the component root directory ... i.e. the path to the tck FAT bucket when it is on the target system
     *
     * @return the FAT bucket component root directory
     */
    private String getComponentRootDir() {
        return Props.getInstance().getFileProperty(Props.DIR_COMPONENT_ROOT).getAbsolutePath();
    }

    /**
     * Generates a list of "-Djarname=path" type strings to add to the CLI. The path is resolved to existing
     * jar names that match the jarName but also include version numbers etc.
     *
     * @return           a list of strings that can be added to a ProcessRunner command
     * @throws Exception
     */
    private ArrayList<String> getJarCliProperties() throws Exception {
        Map<String, String> actualJarFiles = resolveJarPaths();
        ArrayList<String> addon = new ArrayList<>();

        for (Entry<String, String> entry : actualJarFiles.entrySet()) {
            String jarKey = entry.getKey();
            String jarPathName = entry.getValue();
            addon.add("-D" + jarKey + "=" + jarPathName);
        }
        return addon;
    }

    /**
     * Generate the array of Strings which will be used to run the given commands with all the appropriate parameters
     *
     * @return           an array of Strings representing the command to be run
     * @throws Exception thrown if there was a problem assembling the parameters to the mvn command
     */
    private String[] getMvnCommandArray(String... commands) throws Exception {
        String mvn = getMvn(getMvnWrapperDir());

        ArrayList<String> stringArrayList = new ArrayList<>();
        stringArrayList.add(mvn);

        stringArrayList.add("-s");
        stringArrayList.add(getSettingsFile().toString());

        for (String command : commands) {
            stringArrayList.add(command);
        }
        stringArrayList.add("-DoutputAbsoluteArtifactFilename");
        stringArrayList.add("-DtrimStackTrace=false"); //According to the mvn docs this should default to false, but we needed to set this to get full stacks.
        stringArrayList.add("-Dwlp=" + getWLPInstallRoot());
        stringArrayList.add("-Dtck_server=" + getServerName());
        stringArrayList.add("-Dtck_hostname=" + getServerHostName());
        stringArrayList.add("-Dtck_failSafeUndeployment=" + DEFAULT_FAILSAFE_UNDEPLOYMENT);
        stringArrayList.add("-Dtck_appDeployTimeout=" + DEFAULT_APP_DEPLOY_TIMEOUT);
        stringArrayList.add("-Dtck_appUndeployTimeout=" + DEFAULT_APP_UNDEPLOY_TIMEOUT);
        stringArrayList.add("-Dtck_port=" + getPort());
        stringArrayList.add("-Dtck_port_secure=" + getPortSecure());
        stringArrayList.add("-DtargetDirectory=" + getTargetDir().getAbsolutePath());
        stringArrayList.add("-DcomponentRootDir=" + getComponentRootDir());
        stringArrayList.add("-Dsun.rmi.transport.tcp.responseTimeout=" + DEFAULT_MBEAN_TIMEOUT);

        stringArrayList.addAll(getJarCliProperties());

        // The cmd below is a base for running the TCK as a whole for this project.
        // It is possible to use other Testng control xml files (and even generate them
        // based on examining the TCK jar) in which case the value for suiteXmlFile would
        // be different.
        if (isTestNG)
            stringArrayList.add("-DsuiteXmlFile=" + getSuiteFileName());

        // Batch mode, gives better output when logged to a file and allows timestamps to be enabled
        stringArrayList.add("-B");

        // add any additional properties passed
        for (Entry<String, String> prop : this.additionalMvnProps.entrySet()) {
            stringArrayList.add("-D" + prop.getKey() + "=" + prop.getValue());
        }

        String[] cmd = stringArrayList.toArray(new String[0]);
        return cmd;
    }

    /**
     * Get a File which represents the mvn output file when the tests are run. Typically ${component_Root_Directory}/results/mvnOutput_test_${suite_name}
     *
     * @return the mvn output file when running tests.
     */
    private File getMvnTestOutputFile() {
        return new File(getResultsDir(), getMvnTestOutputFileName());
    }

    /**
     * The filename for the mvn output file when running tests. Typically mvnOutput_test_${suite_name}
     *
     * @return The filename for the mvn output file when running tests.
     */
    private String getMvnTestOutputFileName() {
        return MVN_TEST_OUTPUT_FILENAME_PREFIX + getSuiteName();
    }

    /**
     * Get a File which represents the TCK pom.xml file. Typically ${component_Root_Directory}/publish/tckRunner/tck/pom.xml
     *
     * @return the pom.xml File
     */
    private File getPomXmlFile() {
        File pomXmlFile = new File(getTCKRunnerDir(), RELATIVE_POM_FILE);
        if (!pomXmlFile.exists()) {
            pomXmlFile = new File(getTCKRunnerDir(), RELATIVE_POM_FILE2);
        }
        return pomXmlFile;
    }

    /**
     * Get the standard http port for the Liberty server
     *
     * @return the http port number
     */
    private int getPort() throws Exception {
        return server.getPort(PortType.WC_defaulthost);
    }

    /**
     * Get the standard https port for the Liberty server
     *
     * @return the https port number
     */
    private int getPortSecure() throws Exception {
        return server.getPort(PortType.WC_defaulthost_secure);
    }

    /**
     * Get the name of the Liberty Server being used to test
     *
     * @return The Liberty Server name
     */
    private String getServerName() {
        return this.server.getServerName();
    }

    /**
     * Get the name of the Liberty Server hostName being used to test
     *
     * @return The Liberty Server hostName
     */
    private String getServerHostName() {
        return this.server.getHostname();
    }

    /**
     * Get the name of the suite xml file. Normally this defaults to tck-suite.xml.
     *
     * @return the name of the suite xml file
     */
    private String getSuiteFileName() {
        if (isTestNG)
            return this.suiteFileName;
        else
            throw new UnsupportedOperationException("Suite XML file was never set, therefore we assume this is a Junit test.");
    }

    /**
     * Get the name of the suite. This is generated from the suite xml file name and made unique by adding the repeat ID, if any.
     *
     * @return the name of the suite
     */
    private String getSuiteName() {
        if (isTestNG)
            return getSuiteFileName().replace(".xml", "") + RepeatTestFilter.getRepeatActionsAsString();
        else //When using junit just use the server name
            return getServerName() + RepeatTestFilter.getRepeatActionsAsString();
    }

    /**
     * Get a File which represents the raw reports folder from surefire ... may be either TestNG or Junit
     *
     * @return the raw reports directory File
     */
    private File getSureFireResultsDir() {
        File targetResultsDir = getTargetDir();
        File surefireResultsDir = new File(targetResultsDir, TESTNG_REPORTS); // TestNG result location
        if (!surefireResultsDir.exists()) {
            surefireResultsDir = new File(targetResultsDir, SUREFIRE_REPORTS); // JUnit result location
        }
        return surefireResultsDir;
    }

    /**
     * Get a File which represents the target output folder. Typically ${component_Root_Directory}/results/tck_${suite_name}
     *
     * The ${suite_name} may include the Repeat ID which means the folder will remain unique if RepeatTests is used
     *
     * @return a File which represents the target output folder
     */
    private File getTargetDir() {
        return new File(getResultsDir(), MVN_TARGET_FOLDER_PREFIX + getSuiteName());
    }

    /**
     * Get a File which represents the root of the TCK runner. Typically ${component_Root_Directory}/publish/tckRunner
     *
     * @return The TCK runner dir
     */
    private File getTCKRunnerDir() {
        return this.tckRunnerDir;
    }

    private File getMavenUserHome() {
        return this.mavenUserHome;
    }

    private File getMvnWrapperDir() {
        return this.mvnWrapperDir;
    }

    private File getSettingsFile() {
        return this.settingsFile;
    }

    private static File getM2Dir() throws IOException {
        String userDir = System.getProperty("user.dir");
        if (userDir == null) {
            throw new IOException("Could not determine user.dir");
        }
        File devFolder = new File(userDir);
        if (!devFolder.exists() || !devFolder.isDirectory()) {
            throw new IOException("user.dir does not exist or is not a directory: " + userDir);
        }

        //walk back from the user.dir until we reach the dev folder
        while (!devFolder.getName().equals("dev")) {
            devFolder = devFolder.getParentFile();
            if (devFolder == null) {
                throw new IOException("Could not determine dev folder from user.dir: " + userDir);
            }
        }

        File m2Dir = new File(devFolder, ".m2");
        if (m2Dir.exists() && !m2Dir.isDirectory()) {
            throw new IOException(m2Dir + " exists but is not a directory.");
        }
        return m2Dir;
    }

    /**
     * Get the Liberty Install root, e.g. /opt/ibm/wlp
     *
     * @return the path where Liberty is installed
     */
    private String getWLPInstallRoot() {
        return this.server.getInstallRoot();
    }

    /**
     * Get a File which represents the output folder for the processed junit html files. Typically ${component_Root_Directory}/results/junit
     *
     * @return
     */
    private static File getJunitResultsDir() {
        return new File(getResultsDir(), "junit");
    }

    /**
     * Prepare the TestNg/Junit Result XML files for inclusion in Simplicity html processing and return a list of failing tests
     *
     * @return                                      A list of non passing tests
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     */
    private List<String> postProcessTestResults(String[] mvnOutput) throws IOException, SAXException, XPathExpressionException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {

        List<File> resultsFiles = findJunitResultFiles(mvnOutput); //the raw input files
        File targetDir = getJunitResultsDir(); //the output dir

        copyResultsAndAppendId(targetDir, resultsFiles);

        // Get the failing tests out of the JUnit result files
        List<String> failingTestsList = getNonPassingTestsNamesList(resultsFiles);
        if (!failingTestsList.isEmpty()) {
            TCKUtilities.printStdOutAndScreenIfLocal("\nTCK TESTS THAT DID NOT PASS:");
            for (String failedTest : failingTestsList) {
                TCKUtilities.printStdOutAndScreenIfLocal("                               " + failedTest);
            }
            TCKUtilities.printStdOutAndScreenIfLocal("\n");
            new File("output").mkdirs();
            try (FileOutputStream fos = new FileOutputStream("output/fail.log")) {
                fos.write("Test FAILED".getBytes());
            }
        }
        return failingTestsList;
    }

    /**
     * Return a full path for a jar file name substr.
     * This function enables the Liberty build version which is often included
     * in jar names to increment and the FAT bucket to find the jar under the
     * new version.
     *
     * @param  jarName A fragment of a jar file name to be fully resolved
     * @return         The fully resolved path to the jar
     */
    private String resolveJarPath(String jarName) {
        String wlp = getWLPInstallRoot();
        String jarPath = genericResolveJarPath(jarName, wlp);
        return jarPath;
    }

    /**
     * Find a set of jars in a LibertyServer
     *
     * The pom.xml file for the TCK may have dependencies which look like this...
     *
     * <dependency>
     * <groupId>org.eclipse.microprofile.config</groupId>
     * <artifactId>microprofile-config-api</artifactId>
     * <version>${microprofile.config.version}</version>
     * <scope>system</scope>
     * <systemPath>${com.ibm.websphere.org.eclipse.microprofile.config.1.1_}</systemPath>
     * </dependency>
     *
     * This method looks for those systemPath entries which have ${xxx} variables in them and then tries to find corresponding jars in the Liberty installation
     *
     * @return           a Map that has the jars list parameter as the keySet and the resolved paths as entries.
     * @throws Exception thrown if a problem occurs in parsing the pom.xml file
     */
    private Map<String, String> resolveJarPaths() throws Exception {
        Map<String, String> mavenVersionBindingJarPatches = new HashMap<String, String>();

        //first find all the systemPath variables to be resolved
        Set<String> jarsFromWlp = new HashSet<>();
        try {
            DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
            DocumentBuilder bldr = fac.newDocumentBuilder();
            File pomXml = getPomXmlFile();
            Document doc = bldr.parse(pomXml.getAbsolutePath());
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xp = xpf.newXPath();
            // We are looking for <systemPath>${jar.name}</systemPath>
            XPathExpression expr = xp.compile("//systemPath");

            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                // turn "<systemPath>${jar.name}</systemPath>" into "jar.name"
                String jarKey = nl.item(i).getTextContent().replaceAll("\\$\\{", "").replaceAll("\\}", "".replaceAll("\\s+", ""));

                jarsFromWlp.add(jarKey);
                Log.finer(c, "resolveJarPaths", jarKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.warning(c, e.toString());
            throw e;
        }

        //then map all of those jar variables to actual paths
        Map<String, String> result = resolveJarPaths(jarsFromWlp, mavenVersionBindingJarPatches);
        return result;
    }

    /**
     * Resolve a given set of jar name fragments to actual jar paths in a Liberty installation
     *
     * @param  jarsFromWlp
     * @param  mavenVersionBindingJarPatches
     * @return
     * @throws Exception
     */
    private Map<String, String> resolveJarPaths(Set<String> jarsFromWlp, Map<String, String> mavenVersionBindingJarPatches) throws Exception {
        HashMap<String, String> result = new HashMap<String, String>(jarsFromWlp.size());

        for (String jarName : jarsFromWlp) {
            String jarPath;
            // Sometimes we can add a particular version postfix to the regex bases on a spec pom.xml
            if (mavenVersionBindingJarPatches.keySet().contains(jarName)) {
                jarPath = resolveJarPath(mavenVersionBindingJarPatches.get(jarName));
            } else {
                jarPath = resolveJarPath(jarName);
            }

            if (jarPath == null) {
                System.out.println("No jar found");
            }

            if (Boolean.valueOf(System.getProperty("fat.test.localrun"))) {
                // Developers laptop FAT
                Assert.assertNotNull(jarPath, "The resolved jarPath for " + jarName + " is null in " + getWLPInstallRoot());
            }
            result.put(jarName, jarPath);
        }
        return result;
    }

    /**
     * @return a File which represents the mvn output when "dependency:list" is run
     */
    private static File getMvnDependencyOutputFile() {
        return new File(getResultsDir(), "dependency.txt");
    }

    private static void assertTestsPassed(String bucketName, String testName, List<String> failingTestsList) {
        if (failingTestsList != null && failingTestsList.size() > 0) {
            String msg = bucketName + ":" + testName + " tests failed:";
            for (String test : failingTestsList) {
                msg += "\n - " + test;
            }
            throw new AssertionFailedError(msg);
        }
    }

    /**
     * Copy a list of result files to the target directory, appending the id string to both the file name and to test names inside the result XML.
     *
     * @param  targetDir            the target directory
     * @param  resultFiles          the result files to modify and copy
     * @throws TransformerException
     */
    private static void copyResultsAndAppendId(File targetDir,
                                               List<File> resultFiles) throws ParserConfigurationException, XPathExpressionException, TransformerFactoryConfigurationError, SAXException, IOException, TransformerException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression xpr = xpath.compile("//testcase/@name");

        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        String id = RepeatTestFilter.getRepeatActionsAsString();

        for (File file : resultFiles) {
            Document doc;
            try {
                doc = builder.parse(file);
            } catch (SAXException ex) {
                //Handle JSON-P TCK test that logs an invalid unicode char that was purposely used in the test.
                String xmlText = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                xmlText = xmlText.replaceAll("\uffff", "0xffff");
                Files.write(file.toPath(), xmlText.getBytes(StandardCharsets.UTF_8));
                doc = builder.parse(file);
            }
            NodeList nodes = (NodeList) xpr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Attr nameAttr = (Attr) nodes.item(i);
                nameAttr.setValue(nameAttr.getValue() + id);
            }

            int extensionStart = file.getName().lastIndexOf(".");

            StringBuilder targetNameBuilder = new StringBuilder();
            targetNameBuilder.append(file.getName().substring(0, extensionStart));
            targetNameBuilder.append(id);
            targetNameBuilder.append(file.getName().substring(extensionStart));

            File targetFile = new File(targetDir, targetNameBuilder.toString());

            transformer.transform(new DOMSource(doc), new StreamResult(targetFile));
        }
    }

    /**
     * This is more easily unit testable and reusable version of guts of resolveJarPath
     *
     * @param  jarName
     * @param  wlpPathName
     * @return             the path to the jar
     */
    private static String genericResolveJarPath(String jarName, String wlpPathName) {
        Log.entering(c, "genericResolveJarPath", new Object[] { jarName, wlpPathName });
        String dev = wlpPathName + "/dev/";
        String api = dev + "api/";
        String apiStable = api + "stable/";
        String apiSpec = api + "spec/";
        String lib = wlpPathName + "/lib/";

        ArrayList<String> places = new ArrayList<String>();
        places.add(apiStable);
        places.add(apiSpec);
        places.add(lib);

        String jarPath = null;
        for (String dir : places) {
            Log.finer(c, "genericResolveJarPath", "JAR: dir=" + dir);
            jarPath = jarPathInDir(jarName, dir);
            if (jarPath != null) {
                Log.finer(c, "genericResolveJarPath", "JAR: dir match=" + dir + jarPath);
                jarPath = dir + jarPath;
                break;
            }
        }
        return jarPath;
    }

    /**
     * Get the basic mvn command ... "mvn.cmd" on Windows, otherwise just "mvn"
     *
     * @return the mvn command
     */
    private static String getMvn(File mvnHomeDir) {
        String mvn = "mvnw";
        if (TCKUtilities.isWindows()) {
            mvn = mvn + ".cmd";
        }
        File mvnFile = new File(mvnHomeDir, mvn);
        String mvnPath = mvnFile.getAbsolutePath();
        return mvnPath;
    }

    /**
     * @return                              A list of non-PASSing test results
     * @throws SAXException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     */
    private static List<String> getNonPassingTestsNamesList(List<File> resultFiles) throws SAXException, IOException, XPathExpressionException, ParserConfigurationException {
        String notPassingTestsQuery = "//testcase[child::error or child::failure]/@name";
        HashSet<String> excludes = new HashSet<String>(Arrays.asList("arquillianBeforeTest", "arquillianAfterTest"));
        List<String> result = new ArrayList<>();
        for (File resultFile : resultFiles) {
            result.addAll(getQueryInXml(resultFile, notPassingTestsQuery, excludes));
        }
        return result;
    }

    /**
     * Return the result of a XPath query on a file
     *
     * @param  xml   file
     * @param  query as a XPath String
     * @return       result of query into the xml
     */
    private static List<String> getQueryInXml(File xml, String query, Set<String> excludes) {
        ArrayList<String> result = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xmlDoc = builder.parse(xml);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            XPathExpression xpr = xpath.compile(query);
            NodeList nodes = (NodeList) xpr.evaluate(xmlDoc, XPathConstants.NODESET);

            Log.finer(c, "getQueryInXml", "query " + query + " returned " + nodes.getLength() + " nodes");

            if (nodes.getLength() > 0) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    String value = nodes.item(i).getNodeValue();
                    if (value == null || value.length() == 0) {
                        value = nodes.item(i).getTextContent();
                    }
                    if (excludes == null || !excludes.contains(value)) {
                        result.add(value);
                    }
                }
            }

            Log.finer(c, "getQueryInXml", "results: {0}", result);

        } catch (Throwable t) {
            Log.warning(c, t.getMessage());
        }
        return result;
    }

    /**
     * Get a File which represents the results directory, typically ${component_Root_Directory}/results
     *
     * @return the results directory
     */
    private static File getResultsDir() {
        return Props.getInstance().getFileProperty(Props.DIR_LOG);
    }

    /**
     * Looks for a path in a directory from a sub set of the filename
     *
     * @param  jarNameFragment
     * @param  dir
     * @return
     */
    private static String jarPathInDir(String jarNameFragment, String dir) {
        String result = null;
        File dirFileObj = new File(dir);
        String[] files = dirFileObj.list();

        // for someone debugging with absolute paths, just ignore those, regex might not handle.
        if (jarNameFragment.toLowerCase().startsWith("c:") || jarNameFragment.startsWith("/")) {
            Log.finer(c, "jarPathInDir", "ignoring absolute path: " + jarNameFragment);
            return null;
        }

        //Allow for some users using ".jar" at the end and some not
        if (jarNameFragment.endsWith(".jar")) {
            jarNameFragment = jarNameFragment.substring(0, jarNameFragment.length() - ".jar".length());
        }

        //In pom.xml <systemPath>${wildcard}</systemPath>
        // "."-> matches literal "." which is "\\." in a regex and "\\\\\\." in a string
        // "DOT" is a regex "." which matches a single char
        // "STAR (or DOTSTAR) is ".*" which matches any sequence of chars.
        // "_" can be used and will match "_"
        // Other char sequences will be passed into the regex pattern but ONLY valid environment variable chars can
        // be passed to the mvn command line so chars like ^[]- are off limits
        //
        String expandedJarNameFragment = jarNameFragment.replaceAll("\\.", "\\\\\\.").replaceAll("DOTSTAR", ".*").replaceAll("DOT", "\\.").replaceAll("STAR", ".*");

        // Use [0-9_\\.]* to account for the release version for liberty
        // But avoid matching characters otherwise we may match against the wrong artifact
        // i.e. ${io.openliberty.org.jboss.weld5} will match io.openliberty.org.jboss.weld5.se_1.0.70.jar
        String stringPattern = ".*" + expandedJarNameFragment + "[0-9_\\.]*" + "\\.jar";
        Log.finer(c, "jarPathInDir", "looking for jar " + jarNameFragment + " using " + stringPattern + " in dir " + dir);

        // Looking for (for example):
        //              <systemPath>${api.stable}com.ibm.websphere.org.eclipse.microprofile.config.${mpconfig.version}_${mpconfig.bundle.version}.${version.qualifier}.jar</systemPath>
        //              <systemPath>${lib}com.ibm.ws.microprofile.config_${liberty.version}.jar</systemPath>
        //              <systemPath>${lib}com.ibm.ws.microprofile.config.cdi_${liberty.version}.jar</systemPath>

        Pattern p = Pattern.compile(stringPattern);
        for (int i = 0; i < files.length; i++) {
            Matcher m = p.matcher(files[i]);
            if (m.matches()) {
                result = files[i];
                Log.finer(c, "jarPathInDir", "dir " + dir + " matches " + stringPattern + " for " + jarNameFragment + " as " + result);
                return result;
            }
        }
        Log.finer(c, "jarPathInDir", "returning NOT FOUND for " + jarNameFragment + " " + expandedJarNameFragment + " " + stringPattern);
        return null;
    }

    private static Process startMvnProcess(String[] cmd, File workingDirectory, File mavenUserHome) throws IOException, InterruptedException {
        Map<String, String> mvnEnv = getMvnEnv(mavenUserHome);
        Process process = TCKUtilities.startProcess(cmd, workingDirectory, mvnEnv);
        return process;
    }

    private static Map<String, String> getMvnEnv(File mavenUserHome) throws IOException, InterruptedException {
        Map<String, String> mvnEnv = new HashMap<>();

        mvnEnv.put("MAVEN_OPTS", "-Dorg.slf4j.simpleLogger.showDateTime=true" +
                                 " -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss,SSS");

        String artifactoryServer = System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_SERVER_KEY);
        if (artifactoryServer == null) {
            Log.info(c, "getMvnEnv", "Artifactory Download Server not found");
        } else {
            Log.info(c, "getMvnEnv", "ARTIFACTORY_SERVER=" + artifactoryServer);
            mvnEnv.put("ARTIFACTORY_SERVER", artifactoryServer);
        }

        String artifactoryUser = System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_USER_KEY);
        if (artifactoryUser == null) {
            Log.info(c, "getMvnEnv", "Artifactory Download User not found");
        } else {
            Log.info(c, "getMvnEnv", "MVNW_USERNAME=" + artifactoryUser);
            mvnEnv.put("MVNW_USERNAME", artifactoryUser);
        }

        String artifactoryToken = System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_TOKEN_KEY);
        if (artifactoryToken == null) {
            Log.info(c, "getMvnEnv", "Artifactory Download Token not found");
        } else {
            //Log.info(c, "getMvnEnv", "MVNW_PASSWORD=XXXXXXX");
            mvnEnv.put("MVNW_PASSWORD", artifactoryToken);
        }

        String mvnUserHome = mavenUserHome.toString();
        Log.info(c, "getMvnEnv", "MAVEN_USER_HOME=" + mvnUserHome);
        mvnEnv.put("MAVEN_USER_HOME", mvnUserHome);
        mvnEnv.put("MVNW_VERBOSE", "true");

        return mvnEnv;
    }

    private static void assertNotTimedOut(boolean timeout, ArrayList<String> output, long hardTimeout, long softTimeout) {
        if (timeout) {
            ArrayList<String> lastLines = new ArrayList<String>();
            int numOfLinesToInclude = 7; // We will include the last 7 lines of the output file in the timeout message
            int lineNum = output.size() - numOfLinesToInclude;
            if (lineNum < 0) {
                lineNum = 0;
            }
            while (lineNum < output.size()) {
                lastLines.add(output.get(lineNum));
                lineNum++;
            }

            // Prepare the timeout message
            String timeoutMsg = "Timeout occurred. FAT timeout set to: " + hardTimeout + "ms (soft timeout set to " + softTimeout + "ms). The last " +
                                numOfLinesToInclude + " lines from the mvn logs are as follows:\n";
            boolean slowDownload = false;
            while (lineNum < output.size()) {
                String line = output.get(lineNum);
                timeoutMsg += line + "\n";

                if (lineNum > (output.size() - 3)) {
                    if (line.toLowerCase().matches(".* downloading .*|.* downloaded .*")) {
                        slowDownload = true;
                    }
                }

                lineNum++;
            }

            // Special Case: Check if the last or second line has the text "downloading" or "downloaded"
            if (slowDownload) {
                timeoutMsg += "It appears there were some issues gathering dependencies. This may be due to network issues such as slow download speeds.";
            }

            // Throw custom timeout error message rather then the one provided by the JUnitTask
            Log.info(c, "runCmd", timeoutMsg); // Log the timeout message into messages.log or the default log
            throw new AssertionFailedError(timeoutMsg);
        }
    }

    /**
     * Run a command using a ProcessBuilder.
     *
     * @param  cmd
     * @param  workingDirectory
     * @param  outputFile
     * @return                  The return code of the process. (TCKs return 0 if all tests pass and !=0 otherwise).
     * @throws Exception
     */
    private static String[] runMvnCmd(String[] cmd, File workingDirectory, File mavenUserHome) throws IOException, InterruptedException {
        //calculate the timeout
        int hardTimeout = Integer.parseInt(System.getProperty("fat.timeout", "10800000"));
        long softTimeout = -1;

        // We need to ensure that the hard timeout is large enough to avoid future issues
        if (hardTimeout >= 30000) {
            softTimeout = hardTimeout - 15000; // Soft timeout is 15 seconds less than hard timeout
        }
        boolean timeout = false;

        //start the process
        Process process = startMvnProcess(cmd, workingDirectory, mavenUserHome);

        //read the output
        ArrayList<String> output = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                output.add(line);
            }

            //wait for it to finish
            timeout = TCKUtilities.waitForProcess(process, softTimeout);
        }

        //if the process timed out throw an Error
        assertNotTimedOut(timeout, output, hardTimeout, softTimeout);

        String[] lines = output.toArray(new String[0]);
        return lines;
    }
}
