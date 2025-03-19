/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
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
import componenttest.topology.utils.tck.TCKUtilities.ArtifactoryAuthenticator;
import componenttest.topology.utils.tck.TCKUtilities.ProcessResult;

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

    private static final String RELATIVE_POM_FILE = "tck/pom.xml";
    private static final String RELATIVE_POM_FILE2 = "pom.xml";

    private static final String MVN_CLEAN = "clean";
    private static final String MVN_TEST = "test";
    private static final String MVN_DEPENDENCY = "dependency:list";

    private static final String SUREFIRE_REPORTS = "surefire-reports";
    private static final String TESTNG_REPORTS = SUREFIRE_REPORTS + "/junitreports";

    private static final String MVN_FILENAME_PREFIX = "mvnOutput_";
    private static final String MVN_TEST_OUTPUT_FILENAME_PREFIX = MVN_FILENAME_PREFIX + MVN_TEST + "_";
    private static final String MVN_TARGET_FOLDER_PREFIX = "tck_";

    private static final long startNanos = System.nanoTime();

    // Builder settings
    private final LibertyServer server; // Required
    private final Type type; // Required
    private final String specName; // Required

    private String suiteFileName; // Optional
    private Map<String, String> additionalMvnProps = Collections.emptyMap(); // Optional
    private List<String> additionalProfiles = Collections.emptyList(); // Optional
    private String platformVersion = ""; // Optional
    private String[] qualifiers = new String[] {}; //Optional
    private Properties loggingProperties; //Optional

    //Artifactory settings
    private File mavenUserHome;
    private File settingsFile;
    private Authenticator artifactoryAuthenticator;

    // Default settings
    private boolean isTestNG = false;
    private File tckRunnerDir = new File("publish/tckRunner").getAbsoluteFile();
    private File loggingPropertiesFile = new File(tckRunnerDir, "logging.properties");

    /////////// Builder methods //////////////////

    /**
     * The starter point for constructing a TCKRunner
     *
     * @return an non-configured TCKRunner
     */
    public static TCKRunner build(LibertyServer server, Type type, String specName) {
        return new TCKRunner(server, type, specName);
    }

    /**
     * @param  suiteFileName the name of the suite xml file
     * @return               this TCKRunner
     */
    public TCKRunner withSuiteFileName(String suiteFileName) {
        Objects.requireNonNull(suiteFileName);

        this.suiteFileName = suiteFileName;
        this.isTestNG = true;
        return this;
    }

    /**
     * Sets suiteFileName to tck-suite.xml
     *
     * @return this TCKRunner
     */
    public TCKRunner withDefaultSuiteFileName() {
        this.suiteFileName = "tck-suite.xml";
        this.isTestNG = true;
        return this;
    }

    /**
     * @param  additionalMvnProps java properties to set when running the mvn command
     * @return                    this TCKRunner
     */
    public TCKRunner withAdditionalMvnProps(Map<String, String> additionalMvnProps) {
        Objects.requireNonNull(additionalMvnProps);

        this.additionalMvnProps = additionalMvnProps;
        return this;
    }

    /**
     * Set additional maven profiles to enable. This is equivalent to the {@code -P} command line option.
     *
     * @param  profiles the profile names to enable
     * @return          this TCKRunner
     */
    public TCKRunner withProfiles(String... profiles) {
        Objects.requireNonNull(profiles);

        this.additionalProfiles = Arrays.asList(profiles);
        return this;
    }

    /**
     * @param  relativeTCKRunner the relative path to the TCK runner
     * @return                   this TCKRunner
     */
    public TCKRunner withRelativeTCKRunner(String relativeTCKRunner) {
        Objects.requireNonNull(relativeTCKRunner);

        this.tckRunnerDir = new File(relativeTCKRunner).getAbsoluteFile();
        this.loggingPropertiesFile = new File(this.tckRunnerDir, "logging.properties");
        return this;
    }

    /**
     * @param  platformVersion the version of the Jakarta EE Platform this TCK tests
     * @return                 this TCKRunner
     */
    public TCKRunner withPlatfromVersion(String platformVersion) {
        Objects.requireNonNull(platformVersion);

        this.platformVersion = platformVersion;
        return this;
    }

    /**
     * @param  qualifiers necessary to distinguish repeats of a TCK not represented by a RepeatAction, Such as core / web profile.
     * @return            this TCKRunner
     */
    public TCKRunner withQualifiers(String... qualifiers) {
        Objects.requireNonNull(qualifiers);

        this.qualifiers = qualifiers;
        return this;
    }

    /**
     * Writes a logging.properties file to the tckRunnerDir, and adds 'logging.config.file' property to maven command.
     *
     * The resulting log file will be output to 'autoFVT/results' and will have the form '<spec-name>[-qualifiers]-tck-client.log'
     *
     * @param  logs zero or more additional packages and levels to be logged
     * @return      this TCKRunner
     */
    public TCKRunner withLogging(Map<String, Level> logs) {
        Objects.requireNonNull(logs);

        loggingProperties = new Properties();

        // Handlers we plan to use
        loggingProperties.put("handlers", "java.util.logging.FileHandler,java.util.logging.ConsoleHandler");

        // Global logger - By default only log warnings
        loggingProperties.put(".level", "WARNING");

        // Log from packages
        for (Map.Entry<String, Level> log : logs.entrySet()) {
            loggingProperties.put(log.getKey() + ".level", log.getValue().getName());
        }

        // Configure a simple formatter
        loggingProperties.put("java.util.logging.SimpleFormatter.format", "[%1$tm/%1$te/%1$tY %1$tT:%1$tM %1$tZ] %2$s %4$.1s %5$s %6$s %n");

        // Log warning to console
        loggingProperties.put("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
        loggingProperties.put("java.util.logging.ConsoleHandler.level", "WARNING");

        // Log everything else to file
        loggingProperties.put("java.util.logging.FileHandler.limit", "0");
        loggingProperties.put("java.util.logging.FileHandler.count", "1");
        loggingProperties.put("java.util.logging.FileHandler.formatter", "java.util.logging.SimpleFormatter");
        loggingProperties.put("java.util.logging.FileHandler.level", "ALL");

        return this;
    }

    /////////// Run method //////////////////

    /**
     * End point of constructing a TCKRunner.
     *
     * Validates configuration and
     * runs "mvn clean test" in the tck folder,
     * passing through all the required properties
     *
     * @throws IOException If any error occurs writing tck results to the file system.
     * @throws Exception   If any error occurs running mvn commands.
     */
    public void runTCK() throws IOException, Exception {
        // Assumptions that will prevent TCK from running
        Assume.assumeThat(System.getProperty("os.name"), CoreMatchers.not("OS/400")); // skip tests on IBM i due to mvn issue.

        // Validate configured settings
        TCKUtilities.requireDirectory(tckRunnerDir);

        // Validate server started ok (for tests that start the server in advance)
        if (server.isStarted()) {
            TCKUtilities.assertServerHappy(server);
        }

        // Configure Artifactory
        File wrapperPropertiesFile = TCKUtilities.exportMvnWrapper(this.tckRunnerDir);

        if (TCKUtilities.useArtifactory()) {
            Properties wrapperProperties = TCKUtilities.updateWrapperPropertiesFile(wrapperPropertiesFile, TCKUtilities.getArtifactoryServer());

            // Pre-download wrapper jar and set up a temporary maven home directory (if not already created)
            this.artifactoryAuthenticator = new ArtifactoryAuthenticator();
            TCKUtilities.downloadMvnWrapperJar(tckRunnerDir, wrapperProperties, this.artifactoryAuthenticator);
            this.mavenUserHome = TCKUtilities.getTemporaryMavenHomeDir();
            this.settingsFile = TCKUtilities.setupMavenHome(this.mavenUserHome, wrapperProperties, this.artifactoryAuthenticator);
            Log.info(c, "TCKRunner", "Using maven settings file: " + this.settingsFile);
        } else {
            // Don't override settings or maven home
            this.artifactoryAuthenticator = null;
            this.mavenUserHome = null;
            this.settingsFile = null;
            Log.info(c, "TCKRunner", "Using default maven environment");
        }

        // Output logging.properties
        if (loggingProperties != null) {
            String logOutputFileName = Props.getInstance().getFileProperty(Props.DIR_LOG).getAbsolutePath() + '/';
            logOutputFileName += this.specName.toLowerCase().replace(' ', '-') + '-';
            logOutputFileName += String.join("-", qualifiers);
            logOutputFileName += "-tck-client.log";

            loggingProperties.put("java.util.logging.FileHandler.pattern", logOutputFileName);
            loggingProperties.store(new FileOutputStream(loggingPropertiesFile), "Modified by TCKRunner");
        }

        // Run mvn clean test
        ProcessResult testOutput = runCleanTestCmd();
        List<String> failingTestsList = postProcessTestResults(testOutput.getOutput());
        if (failingTestsList.isEmpty()) {
            assertEquals("No tests failed but maven exit code was non-zero", 0, testOutput.getExitCode());
        }

        // Run mvn dependency:list
        List<String> dependencyOutput = runDependencyCmd();

        // Compile and publish results
        TCKJarInfo tckJarInfo = TCKUtilities.getTCKJarInfo(this.type, dependencyOutput);
        TCKResultsInfo resultsInfo = new TCKResultsInfo(this.type, this.specName, this.server, tckJarInfo);
        if (qualifiers.length > 0)
            resultsInfo.withQualifiers(this.qualifiers);
        if (!platformVersion.isEmpty())
            resultsInfo.withPlatformVersion(this.platformVersion);

        TCKResultsWriter.preparePublicationFile(resultsInfo);
    }

    /////////// Constructor //////////////////

    private TCKRunner(LibertyServer server, Type type, String specName) {
        // Validate required settings
        Objects.requireNonNull(server, "TCKRunner was built without a configured server");
        Objects.requireNonNull(type, "TCKRunner was built without a configured type");
        Objects.requireNonNull(specName, "TCKRunner was built without a configured specName");

        // Validate spec name field
        if (!Pattern.compile("^[ A-Za-z]+$").matcher(specName).matches()) {
            throw new IllegalArgumentException("The specName " + specName
                                               + " is an invalid name as it contains characters other than letters and spaces, or no information at all.");
        }

        this.server = server;
        this.type = type;
        this.specName = specName;
    }

    /////////// Private helper methods //////////////////

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     */
    private ProcessResult runCleanTestCmd() throws Exception {

        List<String> testcmd = getMvnCommandParams(MVN_CLEAN, MVN_TEST);
        ProcessResult mvnResult = runMvnCmd(testcmd, getTCKRunnerDir(), getMavenUserHome(), getMvnTestOutputFile());
        return mvnResult;
    }

    /**
     * runs "mvn dependency:list" in the tck folder, passing through all the required properties
     */
    private List<String> runDependencyCmd() throws Exception {
        List<String> dependencyCmd = getMvnCommandParams(MVN_DEPENDENCY);
        ProcessResult mvnOutput = runMvnCmd(dependencyCmd, getTCKRunnerDir(), getMavenUserHome(), getMvnDependencyOutputFile());
        return mvnOutput.getOutput();
    }

    /**
     * Get a list of Files which represent JUnit xml results files.
     *
     * @return a list of junit XML results files
     */
    private List<File> findJunitResultFiles(List<String> mvnOutput) {
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

    private static String getErrorsFromMvnOutput(List<String> mvnOutput) {
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
    private List<String> getMvnCommandParams(String... commands) throws Exception {
        ArrayList<String> stringArrayList = new ArrayList<>();

        if (getSettingsFile() != null) {
            stringArrayList.add("-s");
            stringArrayList.add(getSettingsFile().toString());
        }

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
            stringArrayList.add("-DsuiteXmlFile=" + this.suiteFileName);

        if (loggingProperties != null) {
            stringArrayList.add("-Dlogging.config.file=" + this.loggingPropertiesFile.getAbsolutePath());
        }

        // Batch mode, gives better output when logged to a file and allows timestamps to be enabled
        stringArrayList.add("-B");

        // add any additional properties passed
        for (Entry<String, String> prop : this.additionalMvnProps.entrySet()) {
            stringArrayList.add("-D" + prop.getKey() + "=" + prop.getValue());
        }

        if (!additionalProfiles.isEmpty()) {
            stringArrayList.add("-P");
            stringArrayList.add(String.join(",", additionalProfiles));
        }

        return stringArrayList;
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
     * Get the name of the suite. This is generated from the suite xml file name and made unique by adding the repeat ID, if any.
     *
     * @return the name of the suite
     */
    private String getSuiteName() {
        if (isTestNG)
            return this.suiteFileName.replace(".xml", "") + RepeatTestFilter.getRepeatActionsAsString();
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

    private File getSettingsFile() {
        return this.settingsFile;
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
    private List<String> postProcessTestResults(List<String> mvnOutput) throws IOException, SAXException, XPathExpressionException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {

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
        String thirdParty = api + "third-party/";
        String apiStable = api + "stable/";
        String apiSpec = api + "spec/";
        String lib = wlpPathName + "/lib/";

        ArrayList<String> places = new ArrayList<String>();
        places.add(apiStable);
        places.add(apiSpec);
        places.add(lib);
        places.add(thirdParty);

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
     * Get the full path to the mvnw command (or "mvnw.cmd" on Windows)
     *
     * @param  testDirectory the test directory
     * @return               the mvnw command with the full path
     */
    private static String getMvnw(File testDirectory) {
        String mvnw = "mvnw";
        if (TCKUtilities.isWindows()) {
            mvnw = mvnw + ".cmd";
        }
        File mvnFile = new File(testDirectory, mvnw);
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

        //Find all files that match, then sort in reverse and return the first to ensure we get the latest version (if multiple exist)
        result = Arrays.asList(files)
                        .stream()
                        .filter(file -> p.matcher(file).matches())
                        .sorted(Comparator.reverseOrder())
                        .findFirst()
                        .orElse(null);

        if (result == null) {
            Log.finer(c, "jarPathInDir", "returning NOT FOUND for " + jarNameFragment + " " + expandedJarNameFragment + " " + stringPattern);
        } else {
            Log.finer(c, "jarPathInDir", "dir " + dir + " matches " + stringPattern + " for " + jarNameFragment + " as " + result);
        }

        return result;
    }

    private static Map<String, String> getMvnEnv(File mavenUserHome) throws IOException, InterruptedException {
        Map<String, String> mvnEnv = new HashMap<>();

        mvnEnv.put("MAVEN_OPTS", "-Dorg.slf4j.simpleLogger.showDateTime=true" +
                                 " -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss,SSS");

        if (mavenUserHome != null) {
            // If we have created a custom user home dir, we need to set some environment variables
            // to correspond with the placeholders in the settings.xml
            String artifactoryServer = TCKUtilities.getArtifactoryServer();
            if (artifactoryServer == null) {
                Log.info(c, "getMvnEnv", "Artifactory Download Server not found");
            } else {
                Log.info(c, "getMvnEnv", "ARTIFACTORY_SERVER=" + artifactoryServer);
                mvnEnv.put("ARTIFACTORY_SERVER", artifactoryServer);
            }

            String artifactoryUser = TCKUtilities.getArtifactoryUser();
            if (artifactoryUser == null) {
                Log.info(c, "getMvnEnv", "Artifactory Download User not found");
            } else {
                Log.info(c, "getMvnEnv", "MVNW_USERNAME=" + artifactoryUser);
                mvnEnv.put("MVNW_USERNAME", artifactoryUser);
            }

            String artifactoryToken = TCKUtilities.getArtifactoryToken();
            if (artifactoryToken == null) {
                Log.info(c, "getMvnEnv", "Artifactory Download Token not found");
            } else {
                //Log.info(c, "getMvnEnv", "MVNW_PASSWORD=XXXXXXX");
                mvnEnv.put("MVNW_PASSWORD", artifactoryToken);
            }

            String mvnUserHome = mavenUserHome.toString();
            Log.info(c, "getMvnEnv", "MAVEN_USER_HOME=" + mvnUserHome);
            mvnEnv.put("MAVEN_USER_HOME", mvnUserHome);
        }

        mvnEnv.put("MVNW_VERBOSE", "true");

        return mvnEnv;
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
    private static ProcessResult runMvnCmd(List<String> mvnParams, File workingDirectory, File mavenUserHome, File logFile) throws Exception {

        //calculate the timeout
        long hardTimeout = Long.parseLong(System.getProperty("fat.timeout", "10800000"));
        assertThat("FAT timeout too short to run a TCK", hardTimeout, greaterThan(120_000L));

        // calculate the time elapsed so far (as best we can)
        long elapsedTime = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        // calculate a soft timeout for running the TCK bucket
        // This is 2 minutes less than we think is left to account for activity before this class can record the start time
        // and for the server to shut down cleanly afterwards
        long softTimeout = hardTimeout - elapsedTime - 120_000;

        assertThat("Not enough remaining time to start running a TCK. Elapsed: " + elapsedTime + "ms", softTimeout, greaterThan(0L));

        Map<String, String> mvnEnv = getMvnEnv(mavenUserHome);
        String mvn = getMvnw(workingDirectory);
        ProcessResult output = TCKUtilities.startProcess(mvn, mvnParams, workingDirectory, mvnEnv, logFile, softTimeout);

        //if the process timed out throw an Error
        TCKUtilities.assertNotTimedOut(output, hardTimeout, softTimeout);

        return output;
    }
}
