/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.cmdline.LocalProvider;
import componenttest.topology.utils.tck.TCKResultsInfo.TCKJarInfo;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import junit.framework.AssertionFailedError;

public class TCKUtilities {
    private static final Class<TCKUtilities> c = TCKUtilities.class;

    public static final String FAT_TEST_PREFIX = "fat.test.";
    public static final String ARTIFACTORY_SERVER_KEY = "artifactory.download.server";
    public static final String ARTIFACTORY_USER_KEY = "artifactory.download.user";
    public static final String ARTIFACTORY_TOKEN_KEY = "artifactory.download.token";
    public static final String MVN_DISTRIBUTION_URL_KEY = "distributionUrl";
    public static final String MVN_WRAPPER_URL_KEY = "wrapperUrl";

    public static String generateSHA256(File file) {
        String sha256 = null;
        try (FileInputStream fis = new FileInputStream(file)) {
            sha256 = generateSHA256(fis);
        } catch (IOException e) {
            Log.error(c, "generateSHA256", e, "Could not read file: " + file);
            sha256 = "UNKNOWN";
        } catch (NoSuchAlgorithmException e) {
            Log.error(c, "generateSHA256", e, "Could not generate SHA-256 for: " + file);
            sha256 = "UNKNOWN";
        }

        return sha256;
    }

    public static String generateSHA1(File file) {

        String sha1 = null;
        try (FileInputStream fis = new FileInputStream(file)) {
            sha1 = generateSHA1(fis);
        } catch (IOException e) {
            Log.error(c, "generateSHA1", e, "Could not read file: " + file);
            sha1 = "UNKNOWN";
        } catch (NoSuchAlgorithmException e) {
            Log.error(c, "generateSHA1", e, "Could not generate SHA-1 for: " + file);
            sha1 = "UNKNOWN";
        }

        return sha1;
    }

    public static String generateSHA1(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String sha1 = getFileChecksum(md, inputStream);
        return sha1;
    }

    public static String generateSHA256(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String sha256 = getFileChecksum(md, inputStream);
        return sha256;
    }

    public static String getFileChecksum(MessageDigest digest, InputStream fileInputStream) throws IOException {
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fileInputStream.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        } ;

        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    /**
     * This method will print a String reliably to the 'standard' Standard.out
     * (i.e. the developers screen when running locally)
     *
     * @param msg
     */
    public static void printStdOutAndScreenIfLocal(String msg) {
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

    public static boolean waitForProcess(Process process, long timeoutMS) throws InterruptedException {
        boolean timeout = false;
        if (timeoutMS > -1) {
            timeout = !process.waitFor(timeoutMS, TimeUnit.MILLISECONDS); // Requires Java 8+
            if (timeout) { //timeout!
                process.destroyForcibly(); //kill the process
                process.waitFor();
            }
        }
        return timeout;
    }

    public static void writeStringToFile(String string, File outputFile) {
        try (Writer writer = new FileWriter(outputFile)) {
            writer.write(string);
        } catch (IOException e) {
            e.printStackTrace();
            Log.error(c, "writeStringToFile", e);
        }
    }

    //org.eclipse.microprofile.config:microprofile-config-tck:jar:3.0.2:compile:/Users/tevans/.m2/repository/org/eclipse/microprofile/config/microprofile-config-tck/3.0.2/microprofile-config-tck-3.0.2.jar
    //jakarta.json:jakarta.json-tck-tests:jar:2.1.0:compile:/Users/tevans/.m2/repository/jakarta/json/jakarta.json-tck-tests/2.1.0/jakarta.json-tck-tests-2.1.0.jar

    private static final String basicRegEx = "[a-zA-Z0-9\\.\\-_]";
    private static final String mpGroupRegEx = basicRegEx + "*microprofile" + basicRegEx + "+";
    private static final String jakartaGroupRegEx = basicRegEx + "*jakarta" + basicRegEx + "+";
    private static final String artifactRegEx = basicRegEx + "+\\-tck|" + basicRegEx + "+\\-tck\\-tests";
    private static final String versionRegEx = basicRegEx + "+";
    private static final String typeRegEx = "jar";
    private static final String scopeRegEx = "compile|test";
    private static final String pathRegEx = ".+\\.jar";

    //(group):(artifact):(type):(version):(scope):(path)
    private static final String mpRegex = "(?<group>" + mpGroupRegEx + "):(?<artifact>" + artifactRegEx + "):(?<type>" + typeRegEx + "):(?<version>"
                                          + versionRegEx + "):(?<scope>" + scopeRegEx + "):(?<path>" + pathRegEx + ")";

    private static final String jakartaRegex = "(?<group>" + jakartaGroupRegEx + "):(?<artifact>" + artifactRegEx + "):(?<type>" + typeRegEx + "):(?<version>"
                                               + versionRegEx + "):(?<scope>" + scopeRegEx + "):(?<path>" + pathRegEx + ")";

    public static final Pattern MP_TCK_PATTERN = Pattern.compile(mpRegex, Pattern.DOTALL);
    public static final Pattern JAKARTA_TCK_PATTERN = Pattern.compile(jakartaRegex, Pattern.DOTALL);

    public static Pattern getTCKPatternMatcher(Type type) {
        if (type == Type.MICROPROFILE) {
            return MP_TCK_PATTERN;
        } else if (type == Type.JAKARTA) {
            return JAKARTA_TCK_PATTERN;
        }
        throw new IllegalArgumentException("Unknown type: " + type);
    }

    public static TCKJarInfo getTCKJarInfo(Type type, String dependencyOutput) {
        TCKJarInfo tckJar = parseTCKDependencies(type, dependencyOutput);

        if (tckJar != null) {
            File tckFile = new File(tckJar.jarPath);
            tckJar.sha1 = TCKUtilities.generateSHA1(tckFile);
            tckJar.sha256 = TCKUtilities.generateSHA256(tckFile);
        }

        return tckJar;
    }

    public static String[] splitLines(String text) {
        return text.split("\\r?\\n");
    }

    public static TCKJarInfo parseTCKDependencies(Type type, String dependencyOutput) {
        String lines[] = splitLines(dependencyOutput);
        Pattern tckPattern = getTCKPatternMatcher(type);
        TCKJarInfo tckJar = null;
        for (String sCurrentLine : lines) {
            if (sCurrentLine.contains("-tck:jar") || sCurrentLine.contains("-tck-tests:jar")) {
                Matcher nameMatcher = tckPattern.matcher(sCurrentLine);
                if (nameMatcher.find()) {
                    tckJar = new TCKJarInfo();
                    tckJar.group = nameMatcher.group("group");
                    tckJar.artifact = nameMatcher.group("artifact");
                    tckJar.version = nameMatcher.group("version");
                    tckJar.jarPath = nameMatcher.group("path");
                    Log.info(c, "getTCKJarInfo", "Group: " + tckJar.group);
                    Log.info(c, "getTCKJarInfo", "Artifact: " + tckJar.artifact);
                    Log.info(c, "getTCKJarInfo", "Version: " + tckJar.version);
                    Log.info(c, "getTCKJarInfo", "Path: " + tckJar.jarPath);
                    break;
                }
            }
        }

        return tckJar;
    }

    /**
     * On zos, tag a file as ascii.
     *
     * @param file
     */
    public static void zosTagASCII(File file) {
        try {
            String command = "chtag";
            String[] params = new String[] { "-tcISO8859-1", file.getCanonicalPath() };
            ProgramOutput output = startProcess(command, params, file.getParentFile(), new Properties(), 0);
            int exitValue = output.getReturnCode();
            Log.info(c, "zosTagASCII", "chtag RC = " + exitValue);

            if (exitValue != 0) {
                String stdout = output.getStdout();
                Log.info(c, "zosTagASCII", "SYSOUT:");
                Log.info(c, "zosTagASCII", stdout);

                String stderr = output.getStderr();
                Log.info(c, "zosTagASCII", "SYSERR:");
                Log.info(c, "zosTagASCII", stderr);

                throw new Exception("Process failure <chtag> see log for SYSOUT and SYSERR.");
            }
        } catch (Exception e) {
            Log.error(c, "Could not tag zos file as ASCII", e);
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static boolean isZos() {
        return System.getProperty("os.name").toLowerCase().contains("z/os");
    }

    public static ProgramOutput startProcess(String command, String[] params, File workingDirectory, Properties envProperties, long timeout) throws Exception {
        if (TCKUtilities.isZos()) {
            //The _BPXK_AUTOCVT environment variable is sent to "ON" to allow ASCII tagged
            //files to be read in the correct codepage on zos
            envProperties.put("_BPXK_AUTOCVT", "ON");
        }

        Machine machine = Machine.getLocalMachine();
        ProgramOutput output = machine.execute(command, params, workingDirectory.getAbsolutePath(), envProperties, timeout);

        return output;
    }

    public static void assertNotTimedOut(ProgramOutput output, long hardTimeout, long softTimeout) {

        boolean timeout = output.getReturnCode() == LocalProvider.TIMEOUT_RC;

        if (timeout) {
            String stdout = output.getStdout();
            String[] lines = TCKUtilities.splitLines(stdout);

            ArrayList<String> lastLines = new ArrayList<String>();
            int numOfLinesToInclude = 7; // We will include the last 7 lines of the output file in the timeout message
            int lineNum = lines.length - numOfLinesToInclude;
            if (lineNum < 0) {
                lineNum = 0;
            }
            while (lineNum < lines.length) {
                lastLines.add(lines[lineNum]);
                lineNum++;
            }

            // Prepare the timeout message
            String timeoutMsg = "Timeout occurred. FAT timeout set to: " + hardTimeout + "ms (soft timeout set to " + softTimeout + "ms). The last " +
                                numOfLinesToInclude + " lines from the mvn logs are as follows:\n";
            boolean slowDownload = false;
            while (lineNum < lines.length) {
                String line = lines[lineNum];
                timeoutMsg += line + "\n";

                if (lineNum > (lines.length - 3)) {
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
            Log.info(c, "assertNotTimedOut", timeoutMsg); // Log the timeout message into messages.log or the default log
            throw new AssertionFailedError(timeoutMsg);
        }
    }

    /**
     * Export a file from the fattest.simplicity jar to the local filesystem. If the local file
     * already exists then this method does nothing, it does not overwrite.
     *
     * @param  fileName     The name of the jar resource, relative to TCKRunner.class
     * @param  targetFolder The target local folder to export to
     * @return              the File of the exported file (or the existing one)
     * @throws IOException
     */
    public static File exportResource(File targetFolder, String fileName) throws IOException {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        File targetFile = null;
        if (targetFolder.exists() && targetFolder.isDirectory()) {
            targetFile = new File(targetFolder, fileName);
            if (targetFile.exists()) {
                Log.info(c, "exportResource", "Target File already exists: " + targetFile);
            } else {
                try {
                    stream = TCKRunner.class.getResourceAsStream(fileName);
                    if (stream == null) {
                        throw new IOException("Cannot get resource \"" + fileName + "\" from Jar file.");
                    }

                    int readBytes;
                    byte[] buffer = new byte[4096];
                    resStreamOut = new FileOutputStream(targetFile);
                    while ((readBytes = stream.read(buffer)) > 0) {
                        resStreamOut.write(buffer, 0, readBytes);
                    }
                    resStreamOut.flush();
                } catch (IOException e) {
                    Log.info(c, "exportResource", "Failed to export: " + fileName + ". Exception: " + e.getMessage());
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
                    Log.info(c, "exportResource", fileName + " exported to: " + targetFile);
                } else {
                    Log.info(c, "exportResource", "Failed to export " + fileName + " to: " + targetFile);
                }
            }
        } else {
            Log.info(c, "exportResource", "Target folder does not exist or is not a directory: " + targetFolder);
        }

        return targetFile;
    }

    //https://eu.artifactory.swg-devops.com:443/artifactory/wasliberty-maven-remote/org/apache/maven/apache-maven/3.9.0/apache-maven-3.9.0-bin.zip
    //${mavenUserHome}/wrapper/dists/apache-maven-3.9.0-bin/b0cac456/apache-maven-3.9.0-bin.zip
    /**
     * Download a maven distribution and put it where the maven wrapper would expect to find it. This allows the download to be re-tried if artifactory
     * is being slow.
     *
     * @param  distroURL     The URL to download from
     * @param  mavenUserHome The local .m2 folder to download to
     * @param  auth          The authenticator to use
     * @throws IOException
     */
    public static void downloadMavenDistro(String distroURL, File mavenUserHome, Authenticator auth) throws IOException {
        URI uri = URI.create(distroURL);
        String hash = Integer.toHexString(uri.hashCode());
        String fileName = distroURL.substring(distroURL.lastIndexOf('/') + 1);
        String distName = fileName.substring(0, fileName.indexOf(".zip"));
        File targetFolder = new File(mavenUserHome, "wrapper/dists/" + distName + "/" + hash);
        downloadFile(distroURL, new File(targetFolder, fileName), auth);
    }

    /**
     * Download a file. If the download fails, retry up to 5 times.
     *
     * @param  url           The url to download from
     * @param  targetFile    The file to download to
     * @param  authenticator The authenticator to use
     * @return
     * @throws IOException
     */
    public static File downloadFile(String url, File targetFile, Authenticator authenticator) throws IOException {
        Files.createDirectories(targetFile.getParentFile().toPath());
        Authenticator.setDefault(authenticator);
        int attempt = 0;
        boolean completed = false;
        while (!completed) {
            FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
            FileChannel fileChannel = fileOutputStream.getChannel();
            ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());
            try {

                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                completed = true;
            } catch (IOException e) {
                attempt++;
                if (attempt >= 5) {
                    throw e;
                }
            } finally {
                fileOutputStream.close();
                fileChannel.close();
                readableByteChannel.close();
            }
        }
        return targetFile;
    }

    /**
     * Export the maven wrapper scripts, jars and config to the TCK working directory
     * Updates the maven wrapper config to use the correct artifactory host
     *
     * @throws IOException
     */
    public static Properties exportMvnWrapper(File exportDir, Authenticator auth) throws IOException {

        if (TCKUtilities.isWindows()) {
            File mvnwCmdFile = TCKUtilities.exportResource(exportDir, "mvnw.cmd");
            mvnwCmdFile.setExecutable(true, false);
        } else {
            File mvnwFile = TCKUtilities.exportResource(exportDir, "mvnw");
            mvnwFile.setExecutable(true, false);
            if (TCKUtilities.isZos()) {
                TCKUtilities.zosTagASCII(mvnwFile);
            }
        }

        File targetFolder = new File(exportDir, ".mvn/wrapper");
        Files.createDirectories(targetFolder.toPath());

        File wrapperPropertiesFile = TCKUtilities.exportResource(targetFolder, "maven-wrapper.properties");
        Properties props = updatePropertiesFile(wrapperPropertiesFile);

        String wrapperURL = props.getProperty(MVN_WRAPPER_URL_KEY);
        File wrapperFile = TCKUtilities.downloadFile(wrapperURL, new File(targetFolder, "maven-wrapper.jar"), auth);

        return props;
    }

    public static void assertSHA256(File file, String expectedSha256) {
        String actualSha256 = generateSHA256(file);
        assertEquals(expectedSha256, actualSha256);
    }

    public static class ArtifactoryAuthenticator extends Authenticator {
        PasswordAuthentication authentication;
        String userName = getArtifactoryUser();
        String password = getArtifactoryToken();

        ArtifactoryAuthenticator() {
            if (userName == null || password == null) {
                authentication = null;
            } else {
                authentication = new PasswordAuthentication(userName, password.toCharArray());
            }
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return authentication;
        }
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
    public static Properties updatePropertiesFile(File wrapperPropertiesFile) throws IOException {
        Properties props = new Properties();
        //get the artifactory server
        String artifactoryServer = System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_SERVER_KEY);
        if (artifactoryServer != null) {
            //load the properties file

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
        return props;
    }

    public static String getArtifactoryServer() {
        return System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_SERVER_KEY);
    }

    public static String getArtifactoryUser() {
        return System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_USER_KEY);
    }

    public static String getArtifactoryToken() {
        return System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_TOKEN_KEY);
    }

    public static File getM2Dir() throws IOException {
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
        Files.createDirectories(m2Dir.toPath());
        return m2Dir;
    }

    /**
     * Export the mvnw scripts.
     * Download the maven-wrapper jar.
     * Download the maven distro.
     * Export the maven settings.xml file.
     *
     * @param  mavenUserHome
     * @param  tckRunnerDir
     * @param  artifactoryAuthenticator
     * @return                          the exported maven settings.xml file
     * @throws IOException
     */
    public static File setupMaven(File mavenUserHome, File tckRunnerDir, Authenticator authenticator) throws IOException {
        Properties props = TCKUtilities.exportMvnWrapper(tckRunnerDir, authenticator);
        String distroURL = props.getProperty(MVN_DISTRIBUTION_URL_KEY);

        TCKUtilities.downloadMavenDistro(distroURL, mavenUserHome, authenticator);

        //the local maven settings.xml file to use
        return TCKUtilities.exportResource(mavenUserHome, "settings.xml");
    }
}
