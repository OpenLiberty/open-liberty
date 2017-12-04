/**
 *
 */
package com.ibm.ws.microprofile.tck;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.ibm.websphere.simplicity.PortType;

import componenttest.topology.impl.LibertyServer;

/**
 * A set of mostly static utility fuctions.
 */
public class Utils {

    public static File home;
    public static String wlp;
    public static File tckRunnerDir;
    public static boolean init;
    public static String[] mvnCliRaw;
    public static String[] mvnCliRoot;
    public static String[] mvnCliTckRoot;
    public static String[] mvnCliMethodRoot;
    public static String[] mvnCliClassRoot;
    public static String[] mvnCliPackageRoot;
    static List<String> jarsFromWlp = new ArrayList<String>(3);

    public static void init(LibertyServer server) throws Exception {
        wlp = server.getInstallRoot();
        home = new File(System.getProperty("user.dir"));

        jarsFromWlp.add("com.ibm.websphere.org.eclipse.microprofile.config");
        jarsFromWlp.add("com.ibm.ws.microprofile.config.cdi");
        jarsFromWlp.add("com.ibm.ws.microprofile.config");

        mvnCliRaw = new String[] { "mvn", "clean", "test", "-Dwlp=" + wlp, "-Dtck_server=" + server.getServerName(),
                                   "-Dtck_port=" + server.getPort(PortType.WC_defaulthost) };

        mvnCliRoot = concatStringArray(mvnCliRaw, getJarCliEnvVars(server, jarsFromWlp));

        mvnCliTckRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=tck-suite.xml" });
        mvnCliMethodRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=method.xml" });
        mvnCliClassRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=class.xml" });
        mvnCliPackageRoot = concatStringArray(mvnCliRoot, new String[] { "-DsuiteXmlFile=package.xml" });
        tckRunnerDir = new File("publish/tckRunner");

        init = true;
    }

    /**
     * Resolves a set of "-Djarname=path" type strings to add to the CLI. The path is resolved to existing
     * jar names that match the jarName but also include version numbers etc.
     *
     * @param nonVersionedJars
     * @param mvnCliRoot2
     * @return
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
     * Smashes two String arrays into a returned third.
     *
     * @param a
     * @param b
     * @return
     */
    public static String[] concatStringArray(String[] a, String[] b) {
        Stream<String> streamA = Arrays.stream(a);
        Stream<String> streamB = Arrays.stream(b);
        return Stream.concat(streamA, streamB).toArray(String[]::new);
    }

    /**
     * @param cmd
     * @param workingDirectory TODO
     * @param outputFile TODO
     * @return
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
     * Return a full path for a jar file name substr
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
     * This is more easily unit testable than resolveJarPath
     *
     * @param jarName
     * @param wlpPathName
     * @return
     */
    public static String genericResolveJarPath(String jarName, String wlpPathName) {
        String dev = wlpPathName + "/dev/";
        String api = dev + "api/";
        String spi = dev + "spi/";
        String apiSpec = api + "spec/";
        String apiStable = api + "stable/";
        String apiThirdParty = api + "third-party/";
        String apiIbm = api + "ibm/";
        String spiSpec = spi + "spec/";
        String spiThirdParty = spi + "third-party/";
        String spiIbm = spi + "ibm/";
        String lib = wlpPathName + "/lib/";

        ArrayList<String> places = new ArrayList<String>();
        places.add(apiStable);
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
     * Looks for a path in a directory
     *
     * TODO support regexes?
     *
     * @param jarNameFragment
     * @param dir
     * @return
     */
    public static String jarPathInDir(String jarNameFragment, String dir) {
        String result = null;
        File dirFileObj = new File(dir);
        String[] files = dirFileObj.list();
        log("looking for jar " + jarNameFragment + " in dir " + dir);

// Looking for (for example):
//              <systemPath>${api.stable}com.ibm.websphere.org.eclipse.microprofile.config.${mpconfig.version}_${mpconfig.bundle.version}.${version.qualifier}.jar</systemPath>
//              <systemPath>${lib}com.ibm.ws.microprofile.config_${liberty.version}.jar</systemPath>
//              <systemPath>${lib}com.ibm.ws.microprofile.config.cdi_${liberty.version}.jar</systemPath>

        for (int i = 0; i < files.length; i++) {
            log(files[i]);
            if (files[i].contains(jarNameFragment)) {
                result = files[i];
                log("dir " + dir + " contains " + jarNameFragment + " as " + result);
                // We do not want to allow any prefixes or postfixes that contain letters.
                String r = result.replaceAll(".jar", "").replaceAll("[^a-zA-Z]", "");
                log("r " + r);
                if (r.length() == 0) {
                    // No extra letters in a prefix/postfix.
                    log("JAR found in dir " + jarNameFragment + " " + dir + " as " + result);
                    return result;
                }
            } else {
                log("JAR no jar in dir " + jarNameFragment + " " + dir);
            }
        }
        log("JAR returning" + jarNameFragment + " " + dir);
        return result;
    }

    /**
     * @param string
     */
    private static void log(String string) {
        System.out.println("GDH:" + string);
    }

}