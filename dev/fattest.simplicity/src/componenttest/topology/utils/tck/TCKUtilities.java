/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.tck.TCKResultsInfo.TCKJarInfo;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;

public class TCKUtilities {
    private static final Class<TCKUtilities> c = TCKUtilities.class;

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

    public static void writeStringsToFile(String[] strings, File outputFile) {
        try (Writer writer = new FileWriter(outputFile)) {
            for (String line : strings) {
                writer.write(line);
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.error(c, "writeStringsToFile", e);
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

    public static TCKJarInfo getTCKJarInfo(Type type, String[] dependencyOutput) {
        TCKJarInfo tckJar = parseTCKDependencies(type, dependencyOutput);

        if (tckJar != null) {
            File tckFile = new File(tckJar.jarPath);
            tckJar.sha1 = TCKUtilities.generateSHA1(tckFile);
            tckJar.sha256 = TCKUtilities.generateSHA256(tckFile);
        }

        return tckJar;
    }

    public static TCKJarInfo parseTCKDependencies(Type type, String[] dependencyOutput) {
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
}
