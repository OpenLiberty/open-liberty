/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.rules.repeater.FeatureSet;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.TCKJarInfo;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import junit.framework.AssertionFailedError;

public class TCKUtilities {

    private static final Class<TCKUtilities> c = TCKUtilities.class;

    static final String FAT_TEST_PREFIX = "fat.test.";
    static final String ARTIFACTORY_FORCE_EXTERNAL_KEY = "artifactory.force.external.repo";
    static final String ARTIFACTORY_SERVER_KEY = "artifactory.download.server";
    static final String ARTIFACTORY_USER_KEY = "artifactory.download.user";
    static final String ARTIFACTORY_TOKEN_KEY = "artifactory.download.token";
    static final String MVN_DISTRIBUTION_URL_KEY = "distributionUrl";
    static final String MVN_DISTRIBUTION_SHA_KEY = "distributionSha256Sum";
    static final String MVN_WRAPPER_URL_KEY = "wrapperUrl";
    static final String MVN_WRAPPER_SHA_KEY = "wrapperSha256Sum";
    /** The public repo containing the maven wrapper artifacts. Must match maven-wrapper.properties. */
    static final String MVN_WRAPPER_REPO = "https://repo.maven.apache.org/maven2/";
    static final String BUILD_CACHE_DIR_PROPERTY = "fat.test.build.cache.dir";

    // Convert from EBCDIC on z/OS
    static final Charset zosSafeCharset = TCKUtilities.isZos() ? Charset.forName("IBM1047") : Charset.defaultCharset();

    static String generateSHA256(File file) {
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

    static String generateSHA1(File file) {
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

    static String generateSHA1(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String sha1 = getFileChecksum(md, inputStream);
        return sha1;
    }

    static String generateSHA256(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String sha256 = getFileChecksum(md, inputStream);
        return sha256;
    }

    static String getFileChecksum(MessageDigest digest, InputStream fileInputStream) throws IOException {
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
    static void printStdOutAndScreenIfLocal(String msg) {
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

    static final Pattern MP_TCK_PATTERN = Pattern.compile(mpRegex, Pattern.DOTALL);
    static final Pattern JAKARTA_TCK_PATTERN = Pattern.compile(jakartaRegex, Pattern.DOTALL);

    static Pattern getTCKPatternMatcher(Type type) {
        if (type == Type.MICROPROFILE) {
            return MP_TCK_PATTERN;
        } else if (type == Type.JAKARTA) {
            return JAKARTA_TCK_PATTERN;
        }
        throw new IllegalArgumentException("Unknown type: " + type);
    }

    static TCKJarInfo getTCKJarInfo(Type type, List<String> dependencyOutput) {
        TCKJarInfo tckJar = parseTCKDependencies(type, dependencyOutput);

        if (tckJar != null) {
            File tckFile = new File(tckJar.jarPath);
            tckJar.sha1 = TCKUtilities.generateSHA1(tckFile);
            tckJar.sha256 = TCKUtilities.generateSHA256(tckFile);
        }

        return tckJar;
    }

    static TCKJarInfo parseTCKDependencies(Type type, List<String> dependencyOutput) {
        Pattern tckPattern = getTCKPatternMatcher(type);
        TCKJarInfo tckJar = null;
        for (String sCurrentLine : dependencyOutput) {
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
    static void zosTagASCII(File file) {
        try {
            String command = "chtag";
            List<String> params = Arrays.asList("-tcISO8859-1", file.getCanonicalPath());
            ProcessResult output = startProcess(command, params, file.getParentFile(), Collections.emptyMap(), null, 20_000);
            int exitValue = output.getExitCode();
            Log.info(c, "zosTagASCII", "chtag RC = " + exitValue);

            if (exitValue != 0) {
                Log.info(c, "zosTagASCII", "OUTPUT:");
                for (String line : output.getOutput()) {
                    Log.info(c, "zosTagASCII", line);
                }

                throw new RuntimeException("Process failure <chtag>. Output: \n" + String.join("\n", output.getOutput()));
            }
        } catch (Exception e) {
            Log.error(c, "zosTagASCII", e, "Failed to tag file " + file + " as ASCII");
            throw new RuntimeException("Failed to tag " + file + " as ASCII: " + e, e);
        }
    }

    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    static boolean isZos() {
        return System.getProperty("os.name").toLowerCase().contains("z/os");
    }

    /**
     * Run a process and wait for it to complete.
     *
     * @param  command          the command to run
     * @param  params           the parameters to the command
     * @param  workingDirectory the working directory to use, may be {@code null} to use the working directory of the current process
     * @param  envProperties    the environment variables to set
     * @param  logFile          the file to log the output of the process to, may be {@code null} to not log to a file
     * @param  timeout          the timeout in ms. If the program takes longer to this to run then it will be terminated.
     * @return                  the result of running the process, caller should check {@link ProcessResult#isTimedOut()} and {@link ProcessResult#getExitCode()}
     * @throws Exception        if an unexpected exception occurs while starting or waiting for the process. Note that a non-zero exit code will <b>not</b> result in an exception.
     */
    public static ProcessResult startProcess(String command, List<String> params, File workingDirectory, Map<String, String> envProperties, File logFile,
                                             long timeout) throws Exception {
        assertThat("Process timeout", timeout, greaterThan(0L));
        if (TCKUtilities.isZos()) {
            //The _BPXK_AUTOCVT environment variable is sent to "ON" to allow ASCII tagged
            //files to be read in the correct codepage on zos
            envProperties = new HashMap<>(envProperties);
            envProperties.put("_BPXK_AUTOCVT", "ON");
        }

        List<String> commandLine = new ArrayList<>();
        if (TCKUtilities.isWindows()) {
            // Windows won't run a batch file directly as the command and needs "cmd /c" at the start
            commandLine.add("cmd");
            commandLine.add("/c");
        }
        commandLine.add(command);
        commandLine.addAll(params);

        Log.info(c, "startProcess", "Running command with timeout of " + timeout + "ms: " + command + " " + String.join(" ", params));

        ProcessBuilder pb = new ProcessBuilder(commandLine);
        pb.environment().putAll(envProperties);
        pb.directory(workingDirectory);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        Reader processReader = new InputStreamReader(process.getInputStream(), zosSafeCharset);

        StreamMonitor monitor = new StreamMonitor(processReader);
        monitor.logToFile(logFile);
        monitor.logAs("process-output");

        int rc = -1;
        boolean timedout = false;

        try (AutoCloseable c = monitor.start()) {
            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                timedout = true;
                process.destroy();
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(10, TimeUnit.SECONDS);
                }
            }
            rc = process.exitValue();
        } finally {
            if (process.isAlive()) {
                // This should only occur if there was an unexpected exception before we reached destroyForcibly above
                process.destroyForcibly();
            }
        }

        return new ProcessResult(timedout, monitor.getLines(), rc);
    }

    /**
     * Holds the result of executing a process
     */
    public static class ProcessResult {
        private final boolean timedOut;
        private final List<String> output;
        private final int exitCode;

        private ProcessResult(boolean timedOut, List<String> output, int exitCode) {
            this.timedOut = timedOut;
            this.output = output;
            this.exitCode = exitCode;
        }

        /**
         * @return whether the process was timed out
         */
        public boolean isTimedOut() {
            return timedOut;
        }

        /**
         * @return the merged output of stdout and stderr, split into lines
         */
        public List<String> getOutput() {
            return output;
        }

        /**
         * @return the exit code
         */
        public int getExitCode() {
            return exitCode;
        }
    }

    /**
     * Assert that a process did not time out and create a helpful message if it did
     *
     * @param output      the process output
     * @param hardTimeout a hard timeout value in ms that we're trying to avoid hitting
     * @param softTimeout the timeout value in ms that we used when running the process
     */
    public static void assertNotTimedOut(ProcessResult output, long hardTimeout, long softTimeout) {

        if (output.isTimedOut()) {
            List<String> lines = output.getOutput();

            int numOfLinesToInclude = 7; // We will include the last 7 lines of the output file in the timeout message
            int lastLinesStart = Math.max(0, lines.size() - numOfLinesToInclude);
            int slowDownloadSearchStart = Math.max(0, lines.size() - 3);

            // Prepare the timeout message
            String timeoutMsg = "Timeout occurred. FAT timeout set to: " + hardTimeout + "ms (soft timeout set to " + softTimeout + "ms). The last " +
                                numOfLinesToInclude + " lines from the mvn logs are as follows:\n";
            for (String line : lines.subList(lastLinesStart, lines.size())) {
                timeoutMsg += line + "\n";
            }

            // Special Case: Check if the last three lines has the text "downloading" or "downloaded"

            Pattern downloadingPattern = Pattern.compile("downloading|downloaded", Pattern.CASE_INSENSITIVE);

            boolean slowDownload = lines
                            .subList(slowDownloadSearchStart, lines.size())
                            .stream()
                            .anyMatch(l -> downloadingPattern.matcher(l).find());

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
     * already exists then it is overwritten.
     *
     * @param  fileName     The name of the jar resource, relative to TCKRunner.class
     * @param  targetFolder The target local folder to export to
     * @return              the File of the exported file (or the existing one)
     * @throws IOException  If there is a problem exporting the resource
     */
    static File exportResource(File targetFolder, String fileName) throws IOException {
        try {
            requireDirectory(targetFolder);
            File targetFile = new File(targetFolder, fileName);

            if (targetFile.exists()) {
                Log.info(c, "exportResource", "Target File already exists, deleting it: " + targetFile);
                targetFile.delete();
            }

            try (InputStream stream = TCKRunner.class.getResourceAsStream(fileName)) {
                if (stream == null) {
                    throw new IOException("Cannot get resource \"" + fileName + "\" from Jar file.");
                }

                try (FileOutputStream resStreamOut = new FileOutputStream(targetFile)) {
                    int readBytes;
                    byte[] buffer = new byte[4096];
                    while ((readBytes = stream.read(buffer)) > 0) {
                        resStreamOut.write(buffer, 0, readBytes);
                    }
                }
            }

            if (targetFile.exists()) {
                Log.info(c, "exportResource", fileName + " exported to: " + targetFile);
            } else {
                throw new IOException("Writing target file did not throw an exception, but the target file was not created");
            }
            return targetFile;
        } catch (Exception e) {
            String msg = "Failed to export " + fileName + " to " + targetFolder + ": " + e.getMessage();
            Log.error(c, "exportResource", e, msg);
            throw new IOException(msg, e);
        }
    }

    /**
     * Download a maven distribution and put it where the maven wrapper would expect to find it. This allows the download to be re-tried if artifactory
     * is being slow.
     *
     * @param  distroURL     The URL to download from
     * @param  distroHash    The expected SHA-256 hash of the distro file
     * @param  mavenUserHome The local .m2 folder to download to
     * @param  auth          The authenticator to use
     * @throws IOException
     */
    static void downloadMavenDistro(String distroURL, String distroHash, File mavenUserHome, Authenticator auth) throws IOException {
        // Example:
        // URL:    https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.0/apache-maven-3.9.0-bin.zip
        // Target: ${mavenUserHome}/wrapper/dists/apache-maven-3.9.0-bin/b0cac456/apache-maven-3.9.0-bin.zip
        URI uri = URI.create(distroURL);
        String hash = Integer.toHexString(uri.hashCode());
        String fileName = distroURL.substring(distroURL.lastIndexOf('/') + 1);
        String distName = fileName.substring(0, fileName.indexOf(".zip"));
        File targetFolder = new File(mavenUserHome, "wrapper/dists/" + distName + "/" + hash);
        downloadFile(distroURL, new File(targetFolder, fileName), auth, distroHash);
    }

    /**
     * Download a file. If the download fails, retry up to 5 times.
     * <p>
     * If the file exists and has the correct SHA-256 hash, the download is skipped.
     * <p>
     * If the file exists but does not have the correct SHA-256 hash, it is deleted and downloaded again.
     *
     * @param  url           The url to download from
     * @param  targetFile    The file to download to
     * @param  authenticator The authenticator to use
     * @param  sha256        The expected SHA-256 hash of the file. Used to check if download is required and to validate downloaded file.
     * @return               {@code targetFile}
     * @throws IOException   if there is an error downloading the file
     */
    static File downloadFile(String url, File targetFile, Authenticator authenticator, String sha256) throws IOException {
        try {
            Objects.requireNonNull(sha256, "sha256 hash");
            // If target exists, check whether it's correct
            if (targetFile.exists() && sha256 != null) {
                String actualSha256 = generateSHA256(targetFile);
                if (sha256.equals(actualSha256)) {
                    Log.info(c, "downloadFile", "Target file with correct hash already exists, skipping download for " + targetFile);
                    return targetFile;
                } else {
                    Log.info(c, "downloadFile", "Removing target file with incorrect hash: " + targetFile);
                    targetFile.delete();
                }
            }

            Log.info(c, "downloadFile", "Downloading file from " + url);
            Log.info(c, "downloadFile", "Download target: " + targetFile);
            Files.createDirectories(targetFile.getParentFile().toPath());
            Authenticator.setDefault(authenticator);
            int attempt = 0;
            boolean completed = false;
            while (!completed) {
                try {
                    URLConnection connection = new URL(url).openConnection();
                    connection.setConnectTimeout(60000);
                    connection.setReadTimeout(60000);
                    try (
                                    FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
                                    FileChannel fileChannel = fileOutputStream.getChannel();
                                    ReadableByteChannel readableByteChannel = Channels.newChannel(connection.getInputStream())) {
                        // Call transferFrom until we reach the end of the stream
                        while (fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE) > 0);
                    }

                    // Download complete, check hash
                    String actualSha256 = generateSHA256(targetFile);
                    if (!sha256.equals(actualSha256)) {
                        throw new IOException("Hash of downloaded file did not match. Expected: " + sha256 + ", actual: " + actualSha256);
                    }

                    completed = true;
                } catch (IOException e) {
                    attempt++;
                    if (attempt >= 5) {
                        throw e;
                    }
                }
            }
            return targetFile;
        } catch (Exception e) {
            String msg = "Error downloading file " + url + ": " + e.toString();
            Log.error(c, "downloadFile", e, msg);
            throw new IOException(msg, e);
        }
    }

    /**
     * Export the maven wrapper scripts and config to the TCK working directory
     *
     * @return             the maven wrapper properties file
     * @throws IOException
     */
    public static File exportMvnWrapper(File tckWorkingDir) throws IOException {

        if (TCKUtilities.isWindows()) {
            File mvnwCmdFile = TCKUtilities.exportResource(tckWorkingDir, "mvnw.cmd");
            mvnwCmdFile.setExecutable(true, false);
        } else {
            File mvnwFile = TCKUtilities.exportResource(tckWorkingDir, "mvnw");
            mvnwFile.setExecutable(true, false);
            if (TCKUtilities.isZos()) {
                TCKUtilities.zosTagASCII(mvnwFile);
            }
        }

        File targetFolder = new File(tckWorkingDir, ".mvn/wrapper");
        Files.createDirectories(targetFolder.toPath());

        File wrapperPropertiesFile = TCKUtilities.exportResource(targetFolder, "maven-wrapper.properties");
        return wrapperPropertiesFile;
    }

    /**
     * Download the maven wrapper jar to the correct directory within the TCK working directory
     *
     * @param  tckWorkingDir the TCK working directory
     * @param  wrapperProps  the properties from the wrapper config file
     * @param  authenticator the authenticator to use for the download
     * @throws IOException
     */
    public static void downloadMvnWrapperJar(File tckWorkingDir, Properties wrapperProps, Authenticator authenticator) throws IOException {

        String wrapperURL = wrapperProps.getProperty(MVN_WRAPPER_URL_KEY);
        String wrapperHash = wrapperProps.getProperty(MVN_WRAPPER_SHA_KEY);
        File targetFolder = new File(tckWorkingDir, ".mvn/wrapper");
        Files.createDirectories(targetFolder.toPath());

        TCKUtilities.downloadFile(wrapperURL, new File(targetFolder, "maven-wrapper.jar"), authenticator, wrapperHash);
    }

    static void assertSHA256(File file, String expectedSha256) {
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
     * Update the mvnw wrapper properties file to use the given artifactory server.
     * <p>
     * Specifically, this looks for the "distributionUrl" and "wrapperUrl" properties
     * and replaces the public repository URL with the artifactory URL.
     *
     * @param  wrapperPropertiesFile the wrapper properties file to update
     * @param  artifactoryServer     the artifactory server hostname
     * @return                       the new set of properties
     * @throws IOException
     */
    public static Properties updateWrapperPropertiesFile(File wrapperPropertiesFile, String artifactoryServer) throws IOException {
        Objects.requireNonNull(artifactoryServer, "artifactoryServer");

        Properties props = new Properties();
        String artifactoryRepoURL = "https://" + artifactoryServer + "/artifactory/wasliberty-maven-remote/";

        //load the properties file
        try (FileInputStream fis = new FileInputStream(wrapperPropertiesFile)) {
            props.load(fis);
        }

        //get the existing value of the distributionUrl property
        String distributionURL = props.getProperty(MVN_DISTRIBUTION_URL_KEY);
        //substitute the server string
        assertThat("distributionURL update failed", distributionURL, containsString(MVN_WRAPPER_REPO));
        distributionURL = distributionURL.replace(MVN_WRAPPER_REPO, artifactoryRepoURL);
        //set it back into the properties
        props.setProperty(MVN_DISTRIBUTION_URL_KEY, distributionURL);
        Log.info(c, "updatePropertiesFile", MVN_DISTRIBUTION_URL_KEY + "=" + distributionURL);

        //get the existing value of the wrapperUrl property
        String wrapperURL = props.getProperty(MVN_WRAPPER_URL_KEY);
        //substitute the server string
        assertThat("wrapperURL update failed", wrapperURL, containsString(MVN_WRAPPER_REPO));
        wrapperURL = wrapperURL.replace(MVN_WRAPPER_REPO, artifactoryRepoURL);
        //set it back into the properties
        props.setProperty(MVN_WRAPPER_URL_KEY, wrapperURL);
        Log.info(c, "updatePropertiesFile", MVN_WRAPPER_URL_KEY + "=" + wrapperURL);

        //write the properties back out into the original file
        try (FileOutputStream fos = new FileOutputStream(wrapperPropertiesFile)) {
            props.store(fos, "MVN Wrapper Properties");
        }
        return props;
    }

    /**
     * Checks whether we are configured to use artifactory or not
     *
     * @return {@code true} if we are configured to use artifactory, {@code false} if not
     */
    static boolean useArtifactory() {
        boolean forceExternal = isArtifactoryForceExternal();
        String artifactoryServer = getArtifactoryServer();

        boolean useArtifactory = Objects.nonNull(artifactoryServer) && !forceExternal;

        Log.info(c, "useArtifactory", "Use artifactory = " + useArtifactory + " ("
                                      + ARTIFACTORY_SERVER_KEY + "=" + artifactoryServer + ", "
                                      + ARTIFACTORY_FORCE_EXTERNAL_KEY + "=" + forceExternal
                                      + ")");
        return useArtifactory;
    }

    /**
     * Get the flag for forcing artfiactory usage
     *
     * @return true if flag was true, false otherwise
     */
    static boolean isArtifactoryForceExternal() {
        return Boolean.parseBoolean(getArtifactoryForceExternal());
    }

    /**
     * Get the flag for forcing artfiactory usage
     *
     * @return true if flag was true, false otherwise
     */
    static String getArtifactoryForceExternal() {
        return normalizeStringProperty(System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_FORCE_EXTERNAL_KEY));
    }

    /**
     * Get the artifactory server to use
     *
     * @return the artifactory server or {@code null} if none is configured
     */
    static String getArtifactoryServer() {
        return normalizeStringProperty(System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_SERVER_KEY));
    }

    /**
     * Get the username to access artifactory
     *
     * @return the username, or {@code null} if none is configured
     */
    static String getArtifactoryUser() {
        return normalizeStringProperty(System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_USER_KEY));
    }

    /**
     * Get the token to use to access artifactory
     *
     * @return the token, or {@code null} if none is configured
     */
    static String getArtifactoryToken() {
        return normalizeStringProperty(System.getProperty(FAT_TEST_PREFIX + ARTIFACTORY_TOKEN_KEY));
    }

    /**
     * This validates the result string from using System.getProperty().
     * See launch.xml line 322 we first check if a property was set via a command line property
     * If not we assume the property was set as an environment property.
     *
     * Which mean fat.test.artifactory.download.server could equal "${env.artifactory.download.server}"
     *
     * @param  result the string to validate
     * @return        null if the string was invalid, itself otherwise.
     */
    static String normalizeStringProperty(String result) {
        if (result == null)
            return result;

        if (result.isEmpty()) {
            return null;
        }

        if (result.startsWith("${"))
            return null;

        return result;
    }

    /**
     * Get the location within the {@linkplain #getBuildCacheDir() build cache directory} to create a temporary maven home directory.
     *
     * @return             {@code .m2} within the build cache directory
     * @throws IOException
     */
    public static File getTemporaryMavenHomeDir() throws IOException {
        File m2Dir = new File(getBuildCacheDir(), ".m2");
        if (!m2Dir.exists()) {
            Files.createDirectory(m2Dir.toPath());
        }
        return m2Dir;
    }

    /**
     * Get a location to store files which should be cached and shared between FAT test runs
     * <p>
     * If the directory does not exist it will be created
     * <p>
     * In the development environment, this will be the workspace {@code dev} directory
     *
     * @return             the build cache directory
     * @throws IOException
     */
    static File getBuildCacheDir() throws IOException {
        String buildCacheDirPath = System.getProperty(BUILD_CACHE_DIR_PROPERTY);

        File cacheDir;
        if (buildCacheDirPath != null && !buildCacheDirPath.isEmpty()) {
            // If we've been told to use a particular directory, use that
            cacheDir = new File(buildCacheDirPath).getAbsoluteFile();
            // Create it if required
            if (!cacheDir.exists()) {
                Files.createDirectory(cacheDir.toPath());
            }
        } else {
            // Otherwise, try to walk back from the working directory to find the dev folder
            File workingDir = new File("").getAbsoluteFile();

            File devFolder = workingDir;
            while (!devFolder.getName().equals("dev")) {
                devFolder = devFolder.getParentFile();
                if (devFolder == null) {
                    throw new IOException("Could not find dev folder above working directory: " + workingDir);
                }
            }
            cacheDir = devFolder;
        }

        if (!cacheDir.isDirectory()) {
            throw new IOException("Build cache location is not a directory: " + cacheDir);
        }
        return cacheDir;
    }

    /**
     * Create a temporary maven home directory to use for the build.
     * <p>
     * Download the maven distribution as the maven wrapper would and stores it in the expected location.
     * <p>
     * Exports a maven settings.xml file with the artifactory mirror configuration
     *
     * @param  mavenUserHome     the directory to use
     * @param  wrapperProperties the properties read from the maven-wrapper.properties file
     * @param  authenticator     the authenticator to use when downloading the maven distribution.
     * @return                   the exported maven settings.xml file
     * @throws IOException
     */
    public static File setupMavenHome(File mavenUserHome, Properties wrapperProperties, Authenticator authenticator) throws IOException {

        String distroURL = wrapperProperties.getProperty(MVN_DISTRIBUTION_URL_KEY);
        String distroHash = wrapperProperties.getProperty(MVN_DISTRIBUTION_SHA_KEY);

        TCKUtilities.downloadMavenDistro(distroURL, distroHash, mavenUserHome, authenticator);

        //the local maven settings.xml file to use
        return TCKUtilities.exportResource(mavenUserHome, "settings.xml");
    }

    /**
     * Ensure that a directory exists
     *
     * @param  directory   the file to check
     * @throws IOException if the file does not exist or is not a directory
     */
    public static void requireDirectory(File directory) throws IOException {
        requireExists(directory);
        if (!directory.isDirectory()) {
            throw new IOException(directory + " is not a directory");
        }
    }

    /**
     * Ensure that a file exists
     *
     * @param  file        the file to check
     * @throws IOException if the file does not exist
     */
    static void requireExists(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException(file + " does not exist");
        }
    }

    /**
     * Assert that the server has started successfully and hasn't hit any known problems.
     *
     * @param  server    the server to check
     * @throws Exception
     */
    static void assertServerHappy(LibertyServer server) throws Exception {
        // LibertyServer.start will already have checked for the server started successfully message
        // Look for errors that indicate a failure to start correctly but don't prevent liberty reporting a successful start
        assertThat("Server log has errors after starting", server.findStringsInLogs("CWWKF0001E"), empty()); // A feature definition could not be found for ...
        assertThat("Server log has errors after starting", server.findStringsInLogs("CWWKF0002E"), empty()); // A bundle could not be found for ...
        assertThat("Server log has errors after starting", server.findStringsInLogs("CWWKE0702E"), empty()); // Could not resolve module ...
    }

    private static Set<String> detectedShortNames = null;

    private static Set<String> getDetectedShortNames() {
        if (detectedShortNames != null) {
            return detectedShortNames;
        }

        detectedShortNames = new HashSet<String>();
        Bootstrap b;
        try {
            b = Bootstrap.getInstance();
        } catch (Exception e) {
            Log.info(c, "getDetectedShortNames", "Bootstrap.getInstance() failed. This will likely cause the current repeat to skip " + e.toString());
            detectedShortNames = null; //if bootstrap is broken we've got bigger problems, but we can avoid caching an exception
            throw new RuntimeException(e);
        }

        String installRoot = b.getValue("libertyInstallPath");
        String libPath = installRoot + "/lib/features/";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(libPath), "*.mf")) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {

                    try (Stream<String> lineStream = Files.lines(path, zosSafeCharset)) {
                        lineStream
                                        .filter(line -> line.startsWith("IBM-ShortName: "))
                                        .map(line -> line.replaceAll("IBM-ShortName: ", ""))
                                        .forEach(detectedShortNames::add);
                    }
                }
            }
        } catch (IOException e) {
            Log.info(c, "getDetectedShortNames", "IO exception while reading .mf Files. This will likely cause the current repeat to skip " + e.toString());
            detectedShortNames = null; //avoid caching an exception
            throw new RuntimeException(e);
        }

        return detectedShortNames;

    }

    /**
     * Checks if a feature or features are present in wlp/lib/features by reading the mf files
     * and checking if all the provided IBM-ShortName are present.
     *
     * @param  featureSet A FeatureSet, all the names returned by getFeatures() will be checked for a matching IBM-ShortName in a mf file
     * @return            true if all provided IBM-ShortName can be found in a .mf file in wlp/lib/features.
     */
    public static boolean areAllFeaturesPresent(FeatureSet featureSet) {
        return areAllFeaturesPresent(featureSet.getFeatures());
    }

    /**
     * Checks if a feature or features are present in wlp/lib/features by reading the mf files
     * and checking if all the provided IBM-ShortName are present.
     *
     * @param  featuresToCheck IBM-ShortNames that are required to be available on the server
     * @return                 true if all provided IBM-ShortName can be found in a .mf file in wlp/lib/features.
     */
    public static boolean areAllFeaturesPresent(Set<String> featuresToCheck) {

        final String m = "areAllFeaturesPresent";
        Log.info(c, m, "checking for " + String.join(", ", featuresToCheck));
        Set<String> shortNames = getDetectedShortNames();

        for (String s : featuresToCheck) {
            boolean result = shortNames.contains(s);
            if (!result) {
                Log.info(c, m, "checked for " + s + " found " + result);
                return false;
            }
        }

        Log.info(c, m, "found all features");
        return true;
    }
}
