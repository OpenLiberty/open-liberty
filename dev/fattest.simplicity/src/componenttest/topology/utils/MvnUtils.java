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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.ibm.websphere.simplicity.PortType;
import com.ibm.ws.fat.util.Props;

import componenttest.topology.impl.LibertyServer;

/**
 * A set of mostly static utility functions.
 * This exists partly as there is more than one test class
 * in development that share these functions.
 */
public class MvnUtils {

    public static File resultsDir;
    public static String wlp;
    public static File tckRunnerDir;
    public static File pomXml;
    public static boolean init;
    public static String[] mvnCliRaw;
    public static String[] mvnCliRoot;
    public static String[] mvnCliTckRoot;
    public static String mvnOutputFilename = "mvnOutput_TCK";
    static List<String> jarsFromWlp = new ArrayList<String>(3);
    private static File mvnOutput;

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

        tckRunnerDir = new File("publish/tckRunner");

        pomXml = new File(tckRunnerDir, pomRelativePath);

        populateJarsFromWlp(pomXml);

        String mvn = "mvn";
        if (System.getProperty("os.name").contains("Windows")) {
            mvn = mvn + ".cmd";
        }
        mvnCliRaw = new String[] { mvn, "clean", "test", "-Dwlp=" + wlp, "-Dtck_server=" + server.getServerName(),
                                   "-Dtck_port=" + server.getPort(PortType.WC_defaulthost), "-DtargetDirectory=" + resultsDir.getAbsolutePath() + "/tck" };

        mvnCliRoot = concatStringArray(mvnCliRaw, getJarCliEnvVars(server, jarsFromWlp));

        // The cmd below is a base for running the TCK as a whole for this project.
        // It is possible to use other Testng control xml files (and even generate them
        // based on examining the TCK jar) in which case the value for suiteXmlFile would
        // be different.
        mvnCliTckRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=tck-suite.xml" });

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
                log(jarKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log(e.toString());
            throw e;
        }

    }

    /**
     * Resolves a set of "-Djarname=path" type strings to add to the CLI. The path is resolved to existing
     * jar names that match the jarName but also include version numbers etc.
     *
     * @param server
     * @param nonVersionedJars
     * @return an array of string that can be added to a ProcessRunner command
     */
    private static String[] getJarCliEnvVars(LibertyServer server, List<String> nonVersionedJars) {

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
     */
    public static int runTCKMvnCmd(LibertyServer server, String bucketName, String testName) throws Exception {
        if (!init) {
            init(server);
        }
        // Everything under autoFVT/results is collected from the child build machine

        int rc = runCmd(MvnUtils.mvnCliTckRoot, MvnUtils.tckRunnerDir, mvnOutput);
        Assert.assertEquals("mvn returned non-zero: " + rc, 0, rc);
        File src = new File(MvnUtils.resultsDir, "tck/surefire-reports/junitreports");
        File tgt = new File(MvnUtils.resultsDir, "junit");
        try {
            Files.walkFileTree(src.toPath(), new MvnUtils.CopyFileVisitor(src.toPath(), tgt.toPath()));
        } catch (java.nio.file.NoSuchFileException nsfe) {
            Assert.assertNull(
                              "The TCK tests' results directory does not exist which suggests the TCK tests did not run - check build logs."
                              + src.getAbsolutePath(), nsfe);
        }

        // mvn returns 0 if all surefire tests pass and -1 otherwise - this Assert is enough to mark the build as having failed
        // the TCK regression
        Assert.assertEquals(bucketName + ":" + testName + ":TCK has returned non-zero return code of: " + rc +
                            " This indicates test failure, see: ...autoFVT/results/" + MvnUtils.mvnOutputFilename +
                            " and ...autoFVT/results/tck/surefire-reports/index.html", 0, rc);

        return rc;
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
        log("Running command " + Arrays.asList(cmd));
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
            String jarPath = resolveJarPath(jarName, server);
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
            log("JAR: dir=" + dir);
            jarPath = jarPathInDir(jarName, dir);
            if (jarPath != null) {
                log("JAR: dir match=" + dir + jarPath);
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
        log("looking for jar " + jarNameFragment + " using " + stringPattern + " in dir " + dir);

        // Looking for (for example):
        //              <systemPath>${api.stable}com.ibm.websphere.org.eclipse.microprofile.config.${mpconfig.version}_${mpconfig.bundle.version}.${version.qualifier}.jar</systemPath>
        //              <systemPath>${lib}com.ibm.ws.microprofile.config_${liberty.version}.jar</systemPath>
        //              <systemPath>${lib}com.ibm.ws.microprofile.config.cdi_${liberty.version}.jar</systemPath>

        Pattern p = Pattern.compile(stringPattern);
        for (int i = 0; i < files.length; i++) {
            Matcher m = p.matcher(files[i]);
            if (m.matches()) {
                result = files[i];
                log("dir " + dir + " matches " + stringPattern + " for " + jarNameFragment + " as " + result);
                return result;
            }
        }
        log("returning NOT FOUND for " + jarNameFragment + " " + expandedJarNameFragment + " " + stringPattern);
        return null;
    }

    /**
     * A simple log abstraction to enable easy grepping of the logs.
     *
     * @param string
     */
    public static void log(String string) {
        System.out.println("TCK:" + string);
    }

    /**
     * A fairly standard fileTreeWalker Visitor that copies files to a new directory.
     * Used for copying testng results to the simplicity junit directory
     *
     */
    public static class CopyFileVisitor extends SimpleFileVisitor<Path> {
        private final Path src, tgt;

        public CopyFileVisitor(Path src, Path tgt) {
            this.src = src;
            this.tgt = tgt;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
            Path dest = tgt.resolve(src.relativize(path));
            try {
                Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return FileVisitResult.CONTINUE;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.nio.file.SimpleFileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes fileAttributes) {
            Path dest = tgt.resolve(src.relativize(path));
            try {
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return FileVisitResult.CONTINUE;
        }

    }

}
