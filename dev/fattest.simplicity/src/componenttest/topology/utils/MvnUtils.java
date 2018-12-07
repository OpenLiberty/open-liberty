/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import org.junit.Assert;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.Props;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyServer;

/**
 * A set of mostly static utility functions.
 * This exists partly as there is more than one test class
 * in development that share these functions.
 */
public class MvnUtils {

    public static final Class<MvnUtils> c = MvnUtils.class;

    private static final String DEFAULT_FAILSAFE_UNDEPLOYMENT = "true";
    private static final String DEFAULT_APP_DEPLOY_TIMEOUT = "180";
    private static final String DEFAULT_APP_UNDEPLOY_TIMEOUT = "20";
    private static final int DEFAULT_MBEAN_TIMEOUT = 60000;
    public static File resultsDir;
    public static File componentRootDir;
    public static String wlp;
    public static File tckRunnerDir;
    public static File pomXml;
    public static boolean init;
    public static String[] mvnCliRaw;
    public static String[] mvnCliRoot;
    public static String[] mvnCliTckRoot;
    public static String mvnOutputFilename = "mvnOutput_TCK";
    public static String defaultSuiteFile = "tck-suite.xml";
    public static String defaultTargetFolder = "tck";
    public static String targetFolder = defaultTargetFolder;
    static List<String> jarsFromWlp = new ArrayList<String>(3);
    private static File mvnOutput;
    private static String apiVersion;
    private static String implVersion;
    private static String backStopImplVersion;
    private static Set<String> versionedJars;
    private static Map<String, String> mavenVersionBindingJarPatches = new HashMap<String, String>();
    private static String overrideSuiteFileName = null;
    private static String[] additionalMvnProps = null;

    /**
     * pass additional -Dproperty=value strings or other params to the maven command to run
     *
     * @param propDefs - each entry should have the form -Dproperty=value, or some other valid maven param.
     */
    public static void setAdditionalMvnProps(String[] propDefs, LibertyServer server) throws Exception {
        additionalMvnProps = propDefs;
        init(server);
    }

    /**
     * Changes the name of the suite file used from default value to a new name.
     * Changes maven output file from default value to default value + _ + new suite name
     * Changes maven reports folder name from default to default value + _ + new suite name
     *
     * @param newName - new suite file
     * @param server - server that will run this suite
     * @throws Exception
     */
    public static void setSuiteFileName(String newName, LibertyServer server) throws Exception {
        overrideSuiteFileName = newName;
        mvnOutputFilename = "mvnOutput_" + newName.replace(".xml", "");
        targetFolder = defaultTargetFolder + "_" + newName.replace(".xml", "");
        init(server);
    }

    /**
     * Initialise shared values for a particular server.
     * This enables us to set up things like the Liberty install
     * directory and jar locations once.
     *
     * @param server Simplicity LibertyServer
     * @throws Exception
     */
    public static void init(LibertyServer server) throws Exception {
        String pomRelativePath = "tck/pom.xml";
        init(server, pomRelativePath);
    }

    /**
     * Initialise shared values for a particular server.
     * This enables us to set up things like the Liberty install
     * directory and jar locations once.
     *
     * @param server Simplicity LibertyServer
     * @param pomRelativePath relative to "publish/tckRunner" path to pom - usually "tck/pom.xml"
     * @throws Exception
     */
    public static void init(LibertyServer server, String pomRelativePath) throws Exception {
        wlp = server.getInstallRoot();
        resultsDir = Props.getInstance().getFileProperty(Props.DIR_LOG); //typically ${component_Root_Directory}/results
        componentRootDir = Props.getInstance().getFileProperty(Props.DIR_COMPONENT_ROOT);

        tckRunnerDir = new File("publish/tckRunner");

        pomXml = new File(tckRunnerDir, pomRelativePath);

        if (!init) {
            populateJarsFromWlp(pomXml);
        }

        String mvn = "mvn";
        if (System.getProperty("os.name").contains("Windows")) {
            mvn = mvn + ".cmd";
        }
        mvnCliRaw = new String[] { mvn, "clean", "test", "-Dwlp=" + wlp, "-Dtck_server=" + server.getServerName(),
                                   "-Dtck_failSafeUndeployment=" + DEFAULT_FAILSAFE_UNDEPLOYMENT,
                                   "-Dtck_appDeployTimeout=" + DEFAULT_APP_DEPLOY_TIMEOUT,
                                   "-Dtck_appUndeployTimeout=" + DEFAULT_APP_UNDEPLOY_TIMEOUT,
                                   "-Dtck_port=" + server.getPort(PortType.WC_defaulthost),
                                   "-DtargetDirectory=" + resultsDir.getAbsolutePath() + "/" + targetFolder,
                                   "-DcomponentRootDir=" + componentRootDir,
                                   "-Dsun.rmi.transport.tcp.responseTimeout=" + DEFAULT_MBEAN_TIMEOUT
        };

        mvnCliRoot = concatStringArray(mvnCliRaw, getJarCliProperties(server, jarsFromWlp));

        // The cmd below is a base for running the TCK as a whole for this project.
        // It is possible to use other Testng control xml files (and even generate them
        // based on examining the TCK jar) in which case the value for suiteXmlFile would
        // be different.
        String suiteFile = overrideSuiteFileName == null ? defaultSuiteFile : overrideSuiteFileName;
        mvnCliTckRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=" + suiteFile });

        // add any properties passed in through addMvnProps()
        mvnCliTckRoot = concatStringArray(mvnCliTckRoot, additionalMvnProps);

        mvnOutput = new File(resultsDir, mvnOutputFilename);

        init = true;
    }

    /**
     * This method will add in all the ${variables} used in <systemPath> elements in the pom.xml passed in to those
     * that are searched for in the Liberty jar directories as held in jarsFromWlp
     *
     * @param pomXml the pom.xml file to search for <systemPath>${jar.name}</systemPath>
     * @throws Exception
     */
    private static void populateJarsFromWlp(File pomXml) throws Exception {

        try {
            DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
            DocumentBuilder bldr = fac.newDocumentBuilder();
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
                Log.finer(c, "populateJarsFromWlp", jarKey);
                // For jars that have more than one version we try to add to the regex the api version
                if (versionedJars != null && versionedJars.contains(jarKey)) {
                    String versionedJarKey;
                    if (implVersion != null) {
                        // User has passed in impl.version
                        versionedJarKey = jarKey + "." + implVersion;
                    } else {
                        // Get version from pom.xml
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

    }

    /**
     * @param version
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

    /**
     * Resolves a set of "-Djarname=path" type strings to add to the CLI. The path is resolved to existing
     * jar names that match the jarName but also include version numbers etc.
     *
     * @param server
     * @param nonVersionedJars
     * @return an array of string that can be added to a ProcessRunner command
     */
    private static String[] getJarCliProperties(LibertyServer server, List<String> nonVersionedJars) {

        Map<String, String> actualJarFiles = resolveJarPaths(nonVersionedJars, server);
        String[] addon = new String[] {};

        Set<String> jarsSet = actualJarFiles.keySet();
        for (Iterator<String> iterator = jarsSet.iterator(); iterator.hasNext();) {
            String jarKey = iterator.next();
            String jarPathName = actualJarFiles.get(jarKey);
            addon = concatStringArray(addon, new String[] { "-D" + jarKey + "=" + jarPathName });
        }
        return addon;
    }

    /**
     * Smashes two String arrays into a returned third. Useful for appending to the CLI
     * used for ProcessBuilder. This is not a very efficient way to do this but it is
     * easy to read/write and it is not a performance sensitive use case.
     *
     * @param a
     * @param b
     * @return cat a b
     */
    public static String[] concatStringArray(String[] a, String[] b) {
        if (a == null && b != null) {
            return Arrays.copyOf(b, b.length);
        } else if (a != null && b == null) {
            return Arrays.copyOf(a, a.length);
        } else if (a == null && b == null) {
            return new String[] {};
        } else {
            String[] result = new String[a.length + b.length];
            System.arraycopy(a, 0, result, 0, a.length);
            System.arraycopy(b, 0, result, a.length, b.length);
            return result;
        }
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     *
     * @param backStopImpl
     */
    public static int runTCKMvnCmdWithProps(LibertyServer server, String bucketName, String testName, Map<String, String> addedProps,
                                            Set<String> versionedJarKeys, String backStopImpl) throws Exception {
        apiVersion = addedProps.get("api.version");
        implVersion = addedProps.get("impl.version");
        backStopImplVersion = backStopImpl;

        versionedJars = versionedJarKeys;

        return runTCKMvnCmd(server, bucketName, testName, addedProps);
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     *
     * @see #runTCKMvnCmd(LibertyServer, String, String, Map, Map)
     */
    public static int runTCKMvnCmd(LibertyServer server, String bucketName, String testName) throws Exception {
        return runTCKMvnCmd(server, bucketName, testName, null);
    }

    /**
     * runs "mvn clean test" in the tck folder, passing through all the required properties
     *
     * @param server the liberty server which should be used to run the TCK
     * @param bucketName the name of the test project
     * @param testName the name of the method that's being used to launch the TCK
     * @param addedProps java properties to set when running the mvn command
     */
    public static int runTCKMvnCmd(LibertyServer server, String bucketName, String testName, Map<String, String> addedProps) throws Exception {
        if (!init) {
            init(server);
        }

        String[] cmd = mvnCliTckRoot;
        if (addedProps != null) {
            for (Iterator<Entry<String, String>> iterator = addedProps.entrySet().iterator(); iterator.hasNext();) {
                Entry<String, String> entry = iterator.next();
                cmd = concatStringArray(cmd, new String[] { "-D" + entry.getKey() + "=" + entry.getValue() });
            }
        }

        // Everything under autoFVT/results is collected from the child build machine
        int rc = runCmd(cmd, MvnUtils.tckRunnerDir, mvnOutput);
        List<String> failingTestsList = postProcessTestResults();

        // mvn returns 0 on success, anything else represents a failure.
        // Usually this is caused by failing tests, but if we didn't detect any failing tests then we should raise an exception
        if (rc != 0 && failingTestsList.isEmpty()) {
            Assert.fail("In " + bucketName + ":" + testName + " the TCK (" + cmd + ") has returned non-zero return code of: " + rc + "\n"
                        + "but did not report any failing tests.\n"
                        + "see: ...autoFVT/results/" + MvnUtils.mvnOutputFilename + " for more details");
        }

        return rc;
    }

    /**
     * Prepare the TestNg/Junit Result XML files for inclusion in Simplicity html processing and return a list of failing tests
     *
     * @return A list of non passing tests
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     */
    private static List<String> postProcessTestResults() throws IOException, SAXException, XPathExpressionException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {

        List<File> resultsFiles = findJunitResultFiles();
        File targetDir = new File(MvnUtils.resultsDir, "junit");

        String id;
        if (RepeatTestFilter.CURRENT_REPEAT_ACTION == null || RepeatTestFilter.CURRENT_REPEAT_ACTION.equals(EmptyAction.ID)) {
            id = "";
        } else {
            id = "_" + RepeatTestFilter.CURRENT_REPEAT_ACTION;
        }

        copyResultsAndAppendId(targetDir, resultsFiles, id);

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

    private static List<File> findJunitResultFiles() {
        File resultsDir = new File(MvnUtils.resultsDir, targetFolder + "/surefire-reports/junitreports"); // TestNG result location
        if (!resultsDir.exists()) {
            resultsDir = new File(MvnUtils.resultsDir, targetFolder + "/surefire-reports"); // JUnit result location
        }

        File[] resultsFiles = resultsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("TEST.*\\.xml");
            }
        });

        if (resultsFiles == null || resultsFiles.length == 0) {
            Assert.fail("No TCK test JUnit result files were found in the results directory which suggests the TCK tests did not run - check build logs.\n"
                        + "ResultsDir: " + resultsDir.getAbsolutePath());
        }

        return Arrays.asList(resultsFiles);
    }

    /**
     * Copy a list of result files to the target directory, appending the id string to both the file name and to test names inside the result XML.
     *
     * @param targetDir the target directory
     * @param resultFiles the result files to modify and copy
     * @param id the id string to append to the file names and test names
     * @throws TransformerException
     */
    private static void copyResultsAndAppendId(File targetDir, List<File> resultFiles,
                                               String id) throws ParserConfigurationException, XPathExpressionException, TransformerFactoryConfigurationError, SAXException, IOException, TransformerException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression xpr = xpath.compile("//testcase/@name");

        Transformer transformer = TransformerFactory.newInstance().newTransformer();

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
     * Run a command using a ProcessBuilder.
     *
     * @param cmd
     * @param workingDirectory
     * @param outputFile
     * @return The return code of the process. (TCKs return 0 if all tests pass and !=0 otherwise).
     * @throws Exception
     */
    public static int runCmd(String[] cmd, File workingDirectory, File outputFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDirectory);
        pb.redirectOutput(outputFile);
        pb.redirectErrorStream(true);

        Log.info(c, "runCmd", "Running command " + Arrays.asList(cmd));
        Process p = pb.start();
        int exitCode = p.waitFor();
        return exitCode;

    }

    /**
     * Find a set of jars in a LibertyServer
     *
     * @param jars
     * @param server
     * @return a Map that has the jars list parameter as the keySet and the resolved paths as entries.
     */
    public static Map<String, String> resolveJarPaths(List<String> jars, LibertyServer server) {
        HashMap<String, String> result = new HashMap<String, String>(jars.size());
        for (Iterator<String> iterator = jars.iterator(); iterator.hasNext();) {
            String jarName = iterator.next();

            String jarPath;
            // Sometimes we can add a particular version postfix to the regex bases on a spec pom.xml
            if (mavenVersionBindingJarPatches.keySet().contains(jarName)) {
                jarPath = resolveJarPath(mavenVersionBindingJarPatches.get(jarName), server);
            } else {
                jarPath = resolveJarPath(jarName, server);
            }

            // We allow the situation were we want to test the current TCK version N versus a default level of impl
            // that is passed in from the testcase and used if no impl.version is set. This does not resolve the
            // server.xml features dynamically but does allow different maven systempath jars for the impl jar
            if (jarPath == null && backStopImplVersion != null && backStopImplVersion.length() > 0) {
                jarPath = resolveJarPath(jarName + "." + backStopImplVersion, server);
            }

            if (jarPath == null) {
                System.out.println("No jar found");
            }

            if (Boolean.valueOf(System.getProperty("fat.test.localrun"))) {
                // Developers laptop FAT
                Assert.assertNotNull(jarPath, "The resolved jarPath for " + jarName + " is null in " + server.getInstallRoot());
            }
            result.put(jarName, jarPath);
        }
        return result;
    }

    /**
     * Return a full path for a jar file name substr.
     * This function enables the Liberty build version which is often included
     * in jar names to increment and the FAT bucket to find the jar under the
     * new version.
     *
     * @param jarName
     * @param server
     * @return
     */
    public static String resolveJarPath(String jarName, LibertyServer server) {
        String wlp = server.getInstallRoot();
        String jarPath = genericResolveJarPath(jarName, wlp);
        return jarPath;
    }

    /**
     * This is more easily unit testable and reusable version of guts of resolveJarPath
     *
     * @param jarName
     * @param wlpPathName
     * @return the path to the jar
     */
    public static String genericResolveJarPath(String jarName, String wlpPathName) {
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
        for (Iterator<String> iterator = places.iterator(); iterator.hasNext();) {
            String dir = iterator.next();
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
     * Looks for a path in a directory from a sub set of the filename
     *
     * @param jarNameFragment
     * @param dir
     * @return
     */
    public static String jarPathInDir(String jarNameFragment, String dir) {
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
     * @return A list of non-PASSing test results
     * @throws SAXException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     */
    public static List<String> getNonPassingTestsNamesList(List<File> resultFiles) throws SAXException, IOException, XPathExpressionException, ParserConfigurationException {
        String notPassingTestsQuery = "//testcase[child::error or child::failure]/@name";
        HashSet<String> excludes = new HashSet<String>(Arrays.asList("arquillianBeforeTest", "arquillianAfterTest"));
        List<String> result = new ArrayList<>();
        for (File resultFile : resultFiles) {
            result.addAll(getQueryInXml(resultFile, notPassingTestsQuery, excludes));
        }
        return result;
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
     * @param repo
     */
    public static int mvnCleanInstall(File dir) {

        String mvn = "mvn";
        if (System.getProperty("os.name").contains("Windows")) {
            mvn = mvn + ".cmd";
        }

        String[] mvnCleanInstall = new String[] { mvn, "clean", "install" };

        File results = Props.getInstance().getFileProperty(Props.DIR_LOG); //typically ${component_Root_Directory}/results
        File output = new File(results, "mvnCleanInstall.out");

        // Everything under autoFVT/results is collected from the child build machine
        int rc = -1;
        try {
            rc = runCmd(mvnCleanInstall, dir, output);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // mvn returns 0 if all surefire tests pass and -1 otherwise - this Assert is enough to mark the build as having failed
        // the TCK regression
        Assert.assertEquals("maven clean install in " + dir + " has a non-zero return code of: "
                            + rc +
                            ".\nThis indicates build failure", 0, rc);

        return rc;

    }

    /**
     * Return the version from the <repo>/spec/pom.xml
     *
     * @param repo
     * @return
     */
    public static String getApiSpecVersionAfterClone(File repo) {
        return getPomVersionInDir(repo, "spec");
    }

    /**
     * Return the version from the <repo>/tck/pom.xml
     *
     * @param repo
     * @return
     */
    public static String getTckVersionAfterClone(File repo) {
        return getPomVersionInDir(repo, "tck");
    }

    /**
     * Return the project/version String of a directory's pom.xml file
     *
     * @param repo
     * @param subdir
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
     * @param xml file
     * @param query as a XPath String
     * @return result of query into the xml
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
}
