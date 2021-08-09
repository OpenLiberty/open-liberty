/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

import org.junit.Assert;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.Props;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import junit.framework.AssertionFailedError;

/**
 * MvnUtils allows an arquillian based MicroProfile TCK suite to be launched via Maven. The results will then be converted to junit format and presented
 * as if they were the output of a normal FAT project.
 */
public class MvnUtils {

    private static final Class<MvnUtils> c = MvnUtils.class;

    private static final String DEFAULT_FAILSAFE_UNDEPLOYMENT = "true";
    private static final String DEFAULT_APP_DEPLOY_TIMEOUT = "180";
    private static final String DEFAULT_APP_UNDEPLOY_TIMEOUT = "60";
    private static final int DEFAULT_MBEAN_TIMEOUT = 60000;

    public static final String DEFAULT_SUITE_FILENAME = "tck-suite.xml";
    public static final String API_VERSION = "api.version";
    public static final String IMPL_VERSION = "impl.version";
    public static final String BACKSTOP_VERSION = "backstop.version";
    public static final String TCK_VERSION = "tck.version";

    private static final String MVN_FILENAME_PREFIX = "mvnOutput_";

    private static final String RELATIVE_POM_FILE = "tck/pom.xml";
    private static final String RELATIVE_TCK_RUNNER = "publish/tckRunner";
    private static final String MVN_CLEAN = "clean";
    private static final String MVN_TEST = "test";
    private static final String MVN_INSTALL = "install";

    private static final String SUREFIRE_REPORTS = "surefire-reports";
    private static final String TESTNG_REPORTS = SUREFIRE_REPORTS + "/junitreports";

    private static final String MVN_TEST_OUTPUT_FILENAME_PREFIX = MVN_FILENAME_PREFIX + MVN_TEST + "_";
    private static final String MVN_INSTALL_OUTPUT_FILENAME_PREFIX = MVN_FILENAME_PREFIX + MVN_INSTALL + "_";
    private static final String MVN_TARGET_FOLDER_PREFIX = "tck_";

    private final String bucketName;
    private final String testName;
    private final LibertyServer server;
    private final String suiteFileName;
    private final Set<String> versionedJars;
    private final Map<String, String> additionalMvnProps;

    /**
     * runs "mvn clean test" in the tck folder
     *
     * @param server     the liberty server which should be used to run the TCK
     * @param bucketName the name of the test project
     * @param testName   the name of the method that's being used to launch the TCK
     */
    public static int runTCKMvnCmd(LibertyServer server, String bucketName, String testName) throws Exception {
        return runTCKMvnCmd(server, bucketName, testName, DEFAULT_SUITE_FILENAME, Collections.<String, String> emptyMap(), Collections.<String> emptySet());
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     *
     * @param server          the liberty server which should be used to run the TCK
     * @param bucketName      the name of the test project
     * @param testName        the name of the method that's being used to launch the TCK
     * @param additionalProps java properties to set when running the mvn command
     */
    public static int runTCKMvnCmd(LibertyServer server, String bucketName, String testName, Map<String, String> additionalProps) throws Exception {
        return runTCKMvnCmd(server, bucketName, testName, DEFAULT_SUITE_FILENAME, additionalProps, Collections.<String> emptySet());
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     *
     * @param  server          the liberty server which should be used to run the TCK
     * @param  bucketName      the name of the test project
     * @param  testName        the name of the method that's being used to launch the TCK
     * @param  suiteFileName   the name of the suite xml file
     * @param  additionalProps java properties to set when running the mvn command
     * @param  versionedJars   A set of versioned jars
     * @return                 the integer return code from the mvn command. Anything other than 0 should be regarded as a failure.
     * @throws Exception       occurs if anything goes wrong in setting up and running the mvn command.
     */
    public static int runTCKMvnCmd(LibertyServer server, String bucketName, String testName, String suiteFileName, Map<String, String> additionalProps,
                                   Set<String> versionedJars) throws Exception {
        MvnUtils mvn = new MvnUtils(server, bucketName, testName, suiteFileName, additionalProps, versionedJars);
        return mvn.runCleanTest();
    }

    /**
     * Full constructor for MvnUtils. In most cases one of the static convenience methods should be used instead of calling this directly.
     *
     * @param server          the liberty server which should be used to run the TCK
     * @param bucketName      the name of the test project
     * @param testName        the name of the method that's being used to launch the TCK
     * @param suiteFileName   the name of the suite xml file
     * @param additionalProps java properties to set when running the mvn command
     * @param versionedJars   A set of versioned jars
     */
    public MvnUtils(LibertyServer server, String bucketName, String testName, String suiteFileName, Map<String, String> additionalMvnProps,
                    Set<String> versionedJars) {
        this.server = server;
        this.suiteFileName = suiteFileName;
        this.bucketName = bucketName;
        this.testName = testName;
        this.versionedJars = versionedJars;
        this.additionalMvnProps = additionalMvnProps;
    }

    /**
     * Get a list of Files which represent JUnit xml results files.
     *
     * @return a list of junit XML results files
     */
    private List<File> findJunitResultFiles() {
        File surefileResultsDir = getSureFireResultsDir();

        File[] resultsFiles = surefileResultsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("TEST.*\\.xml");
            }
        });

        if (resultsFiles == null || resultsFiles.length == 0) {
            Assert.fail("No TCK test JUnit result files were found in the results directory which suggests the TCK tests did not run\n"
                        + "Errors found in mvnOutput were:\n" +
                        getErrorsFromMvnOutput());
        }

        return Arrays.asList(resultsFiles);
    }

    private String getErrorsFromMvnOutput() {
        StringBuilder sb = new StringBuilder();
        File installOutput = getMvnInstallOutputFile();
        if (installOutput.exists() && installOutput.canRead()) {
            sb.append("### maven install output:\n");
            try (Scanner s = new Scanner(installOutput)) {
                while (s.hasNextLine() && sb.length() < 20000) {
                    String line = s.nextLine();
                    if (line.contains("[ERROR]"))
                        sb.append(line).append("\n");
                }
            } catch (FileNotFoundException e) {
            }
        }

        File testOutput = getMvnTestOutputFile();
        if (testOutput.exists() && testOutput.canRead()) {
            sb.append("### maven test output:\n");
            try (Scanner s = new Scanner(testOutput)) {
                while (s.hasNextLine() && sb.length() < 20000) {
                    String line = s.nextLine();
                    if (line.contains("[ERROR]"))
                        sb.append(line).append("\n");
                }
            } catch (FileNotFoundException e) {
            }
        }
        return sb.toString();
    }

    /**
     * Get the map of additional properties which should be passed in to the mvn command as "-Dkey=value" parameters
     *
     * @return the additionalMvnProps
     */
    private Map<String, String> getAdditionalMvnProps() {
        return additionalMvnProps;
    }

    /**
     * Get the api.version value from within the additional Mvn properties. This is the version of the API which is to be tested.
     *
     * @return the api.version value from within the additional Mvn properties
     */
    private String getApiVersion() {
        return getAdditionalMvnProps().get(API_VERSION);
    }

    /**
     * Get the backstop.version value from within the additional Mvn properties. This is the default version of the Implementation which is to be tested if the impl.version was not
     * specified.
     *
     * @return the backstop.version value from within the additional Mvn properties
     */
    private String getBackStopImplVersion() {
        return getAdditionalMvnProps().get(BACKSTOP_VERSION);
    }

    /**
     * Get the name of the FAT bucket to use
     *
     * @return the FAT bucket name
     */
    private String getBucketName() {
        return this.bucketName;
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
     * Get the impl.version value from within the additional Mvn properties. This is the version of the Implementation which is to be tested.
     *
     * @return the impl.version
     */
    private String getImplVersion() {
        return getAdditionalMvnProps().get(IMPL_VERSION);
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
     * Generate the array of Strings which will be used to run the "mvn clean test" command with all the appropriate parameters
     *
     * @return           an array of Strings representing the command to be run
     * @throws Exception thrown if there was a problem assembling the parameters to the mvn command
     */
    private String[] getMvnTestCommandArray() throws Exception {
        String mvn = getMvn();

        ArrayList<String> stringArrayList = new ArrayList<>();
        stringArrayList.add(mvn);
        stringArrayList.add(MVN_CLEAN); //TODO do we always want to clean?
        stringArrayList.add(MVN_TEST);
        stringArrayList.add("-Dwlp=" + getWLPInstallRoot());
        stringArrayList.add("-Dtck_server=" + getServerName());
        stringArrayList.add("-Dtck_failSafeUndeployment=" + DEFAULT_FAILSAFE_UNDEPLOYMENT);
        stringArrayList.add("-Dtck_appDeployTimeout=" + DEFAULT_APP_DEPLOY_TIMEOUT);
        stringArrayList.add("-Dtck_appUndeployTimeout=" + DEFAULT_APP_UNDEPLOY_TIMEOUT);
        stringArrayList.add("-Dtck_port=" + getPort());
        stringArrayList.add("-DtargetDirectory=" + getTargetDir().getAbsolutePath());
        stringArrayList.add("-DcomponentRootDir=" + getComponentRootDir());
        stringArrayList.add("-Dsun.rmi.transport.tcp.responseTimeout=" + DEFAULT_MBEAN_TIMEOUT);

        stringArrayList.addAll(getJarCliProperties());

        // The cmd below is a base for running the TCK as a whole for this project.
        // It is possible to use other Testng control xml files (and even generate them
        // based on examining the TCK jar) in which case the value for suiteXmlFile would
        // be different.
        stringArrayList.add("-DsuiteXmlFile=" + getSuiteFileName());

        // Batch mode, gives better output when logged to a file and allows timestamps to be enabled
        stringArrayList.add("-B");

        // add any additional properties passed
        for (Entry<String, String> prop : getAdditionalMvnProps().entrySet()) {
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
        return new File(getTCKRunnerDir(), RELATIVE_POM_FILE);
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
     * Get the name of the Liberty Server being used to test
     *
     * @return The Liberty Server name
     */
    private String getServerName() {
        return this.server.getServerName();
    }

    /**
     * Get the name of the suite xml file. Normally this defaults to tck-suite.xml.
     *
     * @return the name of the suite xml file
     */
    private String getSuiteFileName() {
        return this.suiteFileName;
    }

    /**
     * Get the name of the suite. This is generated from the suite xml file name and made unique by adding the repeat ID, if any.
     *
     * @return the name of the suite
     */
    private String getSuiteName() {
        return getSuiteFileName().replace(".xml", "") + RepeatTestFilter.getRepeatActionsAsString();
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
        return new File(RELATIVE_TCK_RUNNER);
    }

    /**
     * Get the test name. This will have been provided by the caller and is only used as part of error messages.
     *
     * @return the test name
     */
    private String getTestName() {
        return this.testName;
    }

    /**
     * Get a Set of jar names which should be specifically versioned using either the impl.version supplied or the api version obtained from the pom.xml.
     * This helps to disambiguate when there are multiple versions of a jar in the installation.
     *
     * @return A Set of jars which should be specifically versioned
     */
    private Set<String> getVersionedJars() {
        return this.versionedJars;
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
    private File getJunitResultsDir() {
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
    private List<String> postProcessTestResults() throws IOException, SAXException, XPathExpressionException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {

        List<File> resultsFiles = findJunitResultFiles(); //the raw input files
        File targetDir = getJunitResultsDir(); //the output dir

        copyResultsAndAppendId(targetDir, resultsFiles);

        // Get the failing tests out of the JUnit result files
        List<String> failingTestsList = getNonPassingTestsNamesList(resultsFiles);
        if (!failingTestsList.isEmpty()) {
            printStdOutAndScreenIfLocal("\nTCK TESTS THAT DID NOT PASS:");
            for (String failedTest : failingTestsList) {
                printStdOutAndScreenIfLocal("                               " + failedTest);
            }
            printStdOutAndScreenIfLocal("\n");
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

            String implVersion = getImplVersion();

            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                // turn "<systemPath>${jar.name}</systemPath>" into "jar.name"
                String jarKey = nl.item(i).getTextContent().replaceAll("\\$\\{", "").replaceAll("\\}", "".replaceAll("\\s+", ""));

                jarsFromWlp.add(jarKey);
                Log.finer(c, "resolveJarPaths", jarKey);
                // For jars that have more than one version we try to add to the regex the api version
                if (getVersionedJars().contains(jarKey)) {
                    String versionedJarKey;
                    if (implVersion != null) {
                        // User has passed in impl.version
                        versionedJarKey = jarKey + "." + implVersion;
                    } else {
                        // Get version from pom.xml
                        String apiVersion = getApiVersion();
                        versionedJarKey = jarKey + "." + takeOffFinalOrSnapshot(apiVersion);
                    }
                    mavenVersionBindingJarPatches.put(jarKey, versionedJarKey);
                }
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

            String backStopImplVersion = getBackStopImplVersion();

            // We allow the situation were we want to test the current TCK version N versus a default level of impl
            // that is passed in from the testcase and used if no impl.version is set. This does not resolve the
            // server.xml features dynamically but does allow different maven systempath jars for the impl jar
            if (jarPath == null && backStopImplVersion != null && backStopImplVersion.length() > 0) {
                jarPath = resolveJarPath(jarName + "." + backStopImplVersion);
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
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     */
    public int runCleanTest() throws Exception {
        String[] testcmd = getMvnTestCommandArray();
        // Everything under autoFVT/results is collected from the child build machine
        int rc = runCmd(testcmd, getTCKRunnerDir(), getMvnTestOutputFile());

        List<String> failingTestsList = postProcessTestResults();

        // mvn returns 0 on success, anything else represents a failure.
        // Usually this is caused by failing tests, but if we didn't detect any failing tests then we should raise an exception
        if (rc != 0 && failingTestsList.isEmpty()) {
            Assert.fail("In " + getBucketName() + ":" + getTestName() + " the TCK (" + Arrays.toString(testcmd) + ") has returned non-zero return code of: " + rc + "\n"
                        + "but did not report any failing tests.\n"
                        + "see: ...autoFVT/results/" + getMvnTestOutputFileName() + " for more details");
        }

        return rc;
    }

    /**
     * @return a File which represents the mvn output when "install" is run
     */
    private static File getMvnInstallOutputFile() {
        return new File(getResultsDir(), MVN_INSTALL_OUTPUT_FILENAME_PREFIX + RepeatTestFilter.getRepeatActionsAsString());
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
            Document doc = builder.parse(file);
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
     * Return the version from the <repo>/spec/pom.xml
     *
     * @param  repo
     * @return
     */
    public static String getApiSpecVersionAfterClone(File repo) {
        return getPomVersionInDir(repo, "spec");
    }

    /**
     * Get the basic mvn command ... "mvn.cmd" on Windows, otherwise just "mvn"
     *
     * @return the mvn command
     */
    private static String getMvn() {
        String mvn = "mvn";
        if (System.getProperty("os.name").contains("Windows")) {
            mvn = mvn + ".cmd";
        }
        return mvn;
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
     * Return the project/version String of a directory's pom.xml file
     *
     * @param  repo
     * @param  subdir
     * @return
     */
    private static String getPomVersionInDir(File repo, String subdir) {
        Assert.assertTrue("The cloned into directory " + repo.getAbsolutePath() + " does not exist", repo != null && repo.exists());
        File dir = new File(repo, subdir);
        Assert.assertTrue("The pom.xml parent directory " + dir.getAbsolutePath() + " does not exist", dir.exists());
        File pomXml = new File(dir, "pom.xml");
        Assert.assertTrue("The pom.xml file " + pomXml.getAbsolutePath() + " does not exist", pomXml.exists());
        String query = "/project/version";
        List<String> projectVersion = getQueryInXml(pomXml, query, null);
        // Some pom.xml files have no version but inherit it from the
        // parent
        if (!projectVersion.isEmpty() && projectVersion.get(0).trim().length() > 0) {
            return projectVersion.get(0).trim();
        } else {
            query = "/project/parent/version";
            List<String> parentVersion = getQueryInXml(pomXml, query, null);
            return parentVersion.isEmpty() ? null : parentVersion.get(0).trim();
        }

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
     * Return the version from the <repo>/tck/pom.xml
     *
     * @param  repo
     * @return
     */
    public static String getTckVersionAfterClone(File repo) {
        return getPomVersionInDir(repo, "tck");
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
        String stringPattern = ".*" + expandedJarNameFragment + ".*" + "\\.jar";
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

    /**
     * Run "mvn clean install" to populate a given maven repository
     *
     * @param repo the maven repository directory
     */
    public static int mvnCleanInstall(File repo) {

        String mvn = getMvn();
        String[] mvnCleanInstall = new String[] { mvn, MVN_CLEAN, MVN_INSTALL };

        // Everything under autoFVT/results is collected from the child build machine
        int rc = -1;
        try {
            rc = runCmd(mvnCleanInstall, repo, getMvnInstallOutputFile());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // mvn returns 0 if all surefire tests pass and -1 otherwise - this Assert is enough to mark the build as having failed
        // the TCK regression
        Assert.assertEquals("maven clean install in " + repo + " has a non-zero return code of: "
                            + rc +
                            ".\nThis indicates build failure", 0, rc);

        return rc;

    }

    /**
     * This method will print a String reliably to the 'standard' Standard.out
     * (i.e. the developers screen when running locally)
     *
     * @param msg
     */
    private static void printStdOutAndScreenIfLocal(String msg) {
        // If running locally print to screen and stdout if different else print to 'stdout' only
        if (Boolean.valueOf(System.getProperty("fat.test.localrun"))) {
            // Developers laptop FAT
            PrintStream screen = new PrintStream(new FileOutputStream(FileDescriptor.out));
            screen.println(msg);
            if (!System.out.equals(screen)) {
                System.out.println(msg);
            }
        } else {
            // Build engine FAT
            System.out.println(msg);
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
    public static int runCmd(String[] cmd, File workingDirectory, File outputFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);

        // Enables timestamps in the mvnOutput logs
        pb.environment()
                        .put("MAVEN_OPTS", "-Dorg.slf4j.simpleLogger.showDateTime=true" +
                                           " -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss,SSS");

        pb.directory(workingDirectory);
        pb.redirectOutput(outputFile);
        pb.redirectErrorStream(true);

        Log.info(c, "runCmd", "Running command " + Arrays.asList(cmd));

        int hardTimeout = Integer.parseInt(System.getProperty("fat.timeout", "10800000"));
        long softTimeout = -1;

        // We need to ensure that the hard timeout is large enough to avoid future issues
        if (hardTimeout >= 30000) {
            softTimeout = hardTimeout - 15000; // Soft timeout is 15 seconds less than hard timeout
        }

        Process p = pb.start();
        int returnCode = 1;
        boolean returnStatus;
        if (softTimeout > -1) {
            returnStatus = p.waitFor(softTimeout, TimeUnit.MILLISECONDS); // Requires Java 8+
            if (returnStatus == false) {
                // Parse through the mvn logs
                if (outputFile.exists() && outputFile.canRead()) {
                    try (Scanner s = new Scanner(outputFile)) {
                        // Get the last few lines from the MVN log
                        ArrayList<String> lastLines = new ArrayList<String>();
                        int numOfLinesToInclude = 7; // We will include the last 7 lines of the output file in the timeout message
                        while (s.hasNextLine()) {
                            if (lastLines.size() < numOfLinesToInclude) {
                                lastLines.add(s.nextLine());
                            } else {
                                lastLines.remove(0);
                                lastLines.add(s.nextLine());
                            }
                        }

                        // Prepare the timeout message
                        String timeoutMsg = "Timeout occurred. FAT timeout set to: " + hardTimeout + "ms (soft timeout set to " + softTimeout + "ms). The last " +
                                            numOfLinesToInclude + " lines from the mvn logs are as follows:\n";
                        for (String line : lastLines) {
                            timeoutMsg += line + "\n";
                        }

                        // Special Case: Check if the last or second line has the text "downloading" or "downloaded"
                        if ((lastLines.get(lastLines.size() - 1).toLowerCase().matches(".* downloading .*|.* downloaded .*"))
                            || (lastLines.get(lastLines.size() - 2).toLowerCase().matches(".* downloading .*|.* downloaded .*"))) {
                            timeoutMsg += "It appears there were some issues gathering dependencies. This may be due to network issues such as slow download speeds.";
                        }

                        // Throw custom timeout error message rather then the one provided by the JUnitTask
                        Log.info(c, "runCmd", timeoutMsg); // Log the timeout message into messages.log or the default log
                        throw new AssertionFailedError(timeoutMsg);
                    } catch (FileNotFoundException FileError) {
                        // Do nothing as we can't look at the mvn log. This leads to hard timeout handled by the JUnit Task in p.waitFor()
                    }
                }
                // Return to normal behavior and let it timeout through the Junit Task using the hard timeout
                returnCode = p.waitFor();
            } else {
                returnCode = 0;
            }
        } else {
            // The soft timeout could not be used so return to normal behavior and let the Junit Task take care of the timeout
            returnCode = p.waitFor();
        }

        return returnCode;
    }

    /**
     * @param  version
     * @return
     */
    private static String takeOffFinalOrSnapshot(String version) {
        if (version != null && version.length() > 1) {
            // Remove anything after a dash
            String[] bits = version.split("-");
            version = bits[0];
            // Remove and Final specifier
            version = version.replace(".Final", "");
        }
        return version;
    }

}
