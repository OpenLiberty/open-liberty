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
package componenttest.topology.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A class used for identifying properties of a JDK other
 * than the one that is currently being run.
 */
public class JavaInfo {
    private static Class<?> c = JavaInfo.class;
    private static Map<String, JavaInfo> cache = new HashMap<String, JavaInfo>();

    public static int JAVA_VERSION = JavaInfo.forCurrentVM().majorVersion();

    public static JavaInfo forCurrentVM() {
        String javaHome = System.getProperty("java.home");
        JavaInfo info = cache.get(javaHome);
        if (info == null) {
            info = new JavaInfo();
            cacheJavaInfo(javaHome, info);
        }
        return info;
    }

    /**
     * Get the JavaInfo for a given JDK path
     *
     * @param jdkPath The jdk path. For example: System.getProperty("java.home")
     */
    public static JavaInfo fromPath(String jdkPath) throws IOException {
        JavaInfo info = cache.get(jdkPath);
        if (info == null) {
            info = runJavaVersion(jdkPath);
            cacheJavaInfo(jdkPath, info);
        }
        return info;
    }

    private static void cacheJavaInfo(String jdkPath, JavaInfo info) {
        cache.put(jdkPath, info);
        if (jdkPath.contains("\\"))
            cache.put(jdkPath.replace("\\", "/"), info);
    }

    /**
     * Get the JavaInfo for the JDK that will be used for a given LibertyServer.
     * The priority is determined in the following way:
     * <ol>
     * <li> ${server.config.dir}/server.env
     * <li> ${wlp.install.dir}/etc/server.env
     * <li> JAVA_HOME returned by LibertyServer.getMachineJavaJDK()
     * </ol>
     */
    public static JavaInfo forServer(LibertyServer server) throws IOException {
        String serverJava = server.getServerEnv().getProperty("JAVA_HOME");
        return fromPath(serverJava);
    }

    /**
     * The java.vendor of the JDK. Note that Sun and Oracle JDKs are considered to be the same.
     */
    public static enum Vendor {
        IBM,
        OPENJ9,
        SUN_ORACLE,
        UNKNOWN
    }

    final String JAVA_HOME;
    final int MAJOR;
    final int MINOR;
    final int MICRO;
    final Vendor VENDOR;
    final int SERVICE_RELEASE;
    final int FIXPACK;

    private JavaInfo(String jdk_home, int major, int minor, int micro, Vendor v, int sr, int fp) {
        JAVA_HOME = jdk_home;
        MAJOR = major;
        MINOR = minor;
        MICRO = micro;
        VENDOR = v;
        SERVICE_RELEASE = sr;
        FIXPACK = fp;

        Log.info(c, "<init>", this.toString());
    }

    private JavaInfo() {
        JAVA_HOME = System.getProperty("java.home");

        String version = System.getProperty("java.version");
        String[] versionElements = version.split("\\D"); // split on non-digits

        // Pre-JDK 9 the java.version is 1.MAJOR.MINOR
        // Post-JDK 9 the java.version is MAJOR.MINOR
        int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
        MAJOR = Integer.valueOf(versionElements[i++]);

        if (i < versionElements.length)
            MINOR = Integer.valueOf(versionElements[i++]);
        else
            MINOR = 0;

        if (i < versionElements.length)
            MICRO = Integer.valueOf(versionElements[i]);
        else
            MICRO = 0;

        String vendor = System.getProperty("java.vendor").toLowerCase();
        if (vendor.contains("openj9"))
            VENDOR = Vendor.OPENJ9;
        else if (vendor.contains("ibm") || vendor.contains("j9"))
            VENDOR = Vendor.IBM;
        else if (vendor.contains("oracle"))
            VENDOR = Vendor.SUN_ORACLE;
        else {
            vendor = System.getProperty("java.vm.name", "unknown").toLowerCase();
            if (vendor.contains("openj9"))
                VENDOR = Vendor.OPENJ9;
            else if (vendor.contains("ibm") || vendor.contains("j9"))
                VENDOR = Vendor.IBM;
            else if (vendor.contains("oracle") || vendor.contains("openjdk"))
                VENDOR = Vendor.SUN_ORACLE;
            else {
                Log.info(c, "<init>", "WARNING: Found unknown java vendor: " + vendor);
                VENDOR = Vendor.UNKNOWN;
            }
        }

        // Parse service release
        String buildInfo = System.getProperty("java.runtime.version");
        int sr = 0;
        int srloc = buildInfo.toLowerCase().indexOf("sr");
        if (srloc > (-1)) {
            srloc += 2;
            if (srloc < buildInfo.length()) {
                int len = 0;
                while ((srloc + len < buildInfo.length()) && Character.isDigit(buildInfo.charAt(srloc + len))) {
                    len++;
                }
                sr = Integer.parseInt(buildInfo.substring(srloc, srloc + len));
            }
        }
        SERVICE_RELEASE = sr;

        // Parse fixpack
        int fp = 0;
        int fploc = buildInfo.toLowerCase().indexOf("fp");
        if (fploc > (-1)) {
            fploc += 2;
            if (fploc < buildInfo.length()) {
                int len = 0;
                while ((fploc + len < buildInfo.length()) && Character.isDigit(buildInfo.charAt(fploc + len))) {
                    len++;
                }
                fp = Integer.parseInt(buildInfo.substring(fploc, fploc + len));
            }
        }
        FIXPACK = fp;

        Log.info(c, "<init>", this.toString());
    }

    public int majorVersion() {
        return MAJOR;
    }

    public int minorVersion() {
        return MINOR;
    }

    public int microVersion() {
        return MICRO;
    }

    private static final Map<String, Boolean> systemClassAvailability = new ConcurrentHashMap<>();

    /**
     * In rare cases where different behaviour is performed based on the JVM vendor
     * this method should be used to test for a unique JVM class provided by the
     * vendor rather than using the vendor method. For example if on JVM provides a
     * different Kerberos login module testing for that login module being loadable
     * before configuring to use it is preferable to using the vendor data.
     *
     * New users of this method should consider adding their class name in
     * JavaInfoTest in the com.ibm.ws.java11_fat project.
     *
     * @param  className the name of a class in the JVM to test for
     * @return           true if the class is available, false otherwise.
     */
    public static boolean isSystemClassAvailable(String className) {
        return systemClassAvailability.computeIfAbsent(className, (k) -> AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            @FFDCIgnore(ClassNotFoundException.class)
            public Boolean run() {
                try {
                    // Using ClassLoader.findSystemClass() instead of
                    // Class.forName(className, false, null) because Class.forName with a null
                    // ClassLoader only looks at the boot ClassLoader with Java 9 and above
                    // which doesn't look at all the modules available to the findSystemClass.
                    systemClassAccessor.getSystemClass(className);
                    return true;
                } catch (ClassNotFoundException e) {
                    //No FFDC needed
                    return false;
                }
            }
        }));
    }

    private static final SystemClassAccessor systemClassAccessor = new SystemClassAccessor();

    private static final class SystemClassAccessor extends ClassLoader {
        public Class<?> getSystemClass(String className) throws ClassNotFoundException {
            return findSystemClass(className);
        }
    }

    /**
     * @deprecated
     *             This method should not be used to determine behaviour based on the Java vendor.
     *             Instead if there are behaviour differences between JVMs a test should be performed
     *             to detect the actual capability used before making a decision. For example if there
     *             is a different class on one JVM that needs to be used vs another an attempt should
     *             be made to load the class and take the code path.
     *
     * @return     the detected vendor of the JVM
     */
    @Deprecated
    public Vendor vendor() {
        return VENDOR;
    }

    public String javaHome() {
        return JAVA_HOME;
    }

    public int serviceRelease() {
        return SERVICE_RELEASE;
    }

    public int fixpack() {
        return FIXPACK;
    }

    /**
     * For debug purposes only
     *
     * @return a String containing basic info about the JDK
     */
    public String debugString() {
        return "Vendor = " + vendor() + ", Version = " + majorVersion() + "." + minorVersion();
    }

    private static JavaInfo runJavaVersion(String javaHome) throws IOException {
        final String m = "runJavaVersion";

        // output for 'java -version' is always as follows:
        // line 1: java version "1.MAJOR.MINOR"
        // line 2: build info
        // line 3: vendor info
        //
        // For example:
        //      java version "1.7.0"
        //      Java(TM) SE Runtime Environment (build pxi3270_27sr3fp50-20160720_02(*SR3fp50*))
        //      IBM J9 VM (build 2.7, JRE 1.7.0 Linux x86-32 20160630_309914 (JIT enabled, AOT enabled)
        //
        // Major version = 7
        // Minor version = 0
        // Service release (IBM JDK specific) = 3
        // Fixpack (IBM JDK specific) = 50
        // Vendor = IBM

        ProcessBuilder pb = new ProcessBuilder(javaHome + "/bin/java", "-version");
        Process p = pb.start();
        try {
            p.waitFor();
        } catch (InterruptedException e) {
        }
        InputStreamReader isr = new InputStreamReader(p.getErrorStream());
        BufferedReader br = new BufferedReader(isr);
        String versionInfo = br.readLine(); // 1st line has version info
        String buildInfo = br.readLine(); // 2nd line has service release and fixpack info
        String vendorInfo = br.readLine().toLowerCase();;

        br.close();
        isr.close();

        Log.info(c, m, versionInfo);
        Log.info(c, m, vendorInfo);

        // Parse vendor
        Vendor v;
        if (vendorInfo.contains("ibm") || vendorInfo.contains("j9")) {
            v = Vendor.IBM;
        } else if (vendorInfo.contains("oracle") || vendorInfo.contains("hotspot")) {
            v = Vendor.SUN_ORACLE;
        } else {
            v = Vendor.UNKNOWN;
        }

        // Parse major/minor versions
        versionInfo = versionInfo.substring(versionInfo.indexOf('"') + 1, versionInfo.lastIndexOf('"'));
        String[] versions = versionInfo.split("[^0-9]"); // split on non-numeric chars

        // Offset for 1.MAJOR.MINOR vs. MAJOR.MINOR version syntax
        int offset = "1".equals(versions[0]) ? 1 : 0;
        if (versions.length <= offset)
            throw new IllegalStateException("Bad Java runtime version string: " + versionInfo);
        int major = Integer.parseInt(versions[offset]);
        int minor = versions.length < (2 + offset) ? 0 : Integer.parseInt(versions[(1 + offset)]);
        int micro = versions.length < (3 + offset) ? 0 : Integer.parseInt(versions[(2 + offset)]);

        // Parse service release
        int sr = 0;
        int srloc = buildInfo.toLowerCase().indexOf("sr");
        if (srloc > (-1)) {
            srloc += 2;
            if (srloc < buildInfo.length()) {
                int len = 0;
                while ((srloc + len < buildInfo.length()) && Character.isDigit(buildInfo.charAt(srloc + len))) {
                    len++;
                }
                sr = Integer.parseInt(buildInfo.substring(srloc, srloc + len));
            }
        }

        // Parse fixpack
        int fp = 0;

        int fploc = buildInfo.toLowerCase().indexOf("fp");
        if (fploc > (-1)) {
            fploc += 2;
            if (fploc < buildInfo.length()) {
                int len = 0;
                while ((fploc + len < buildInfo.length()) && Character.isDigit(buildInfo.charAt(fploc + len))) {
                    len++;
                }
                fp = Integer.parseInt(buildInfo.substring(fploc, fploc + len));
            }
        }

        return new JavaInfo(javaHome, major, minor, micro, v, sr, fp);
    }

    @Override
    public String toString() {
        return "major=" + MAJOR + "  minor=" + MINOR + " service release=" + SERVICE_RELEASE + " fixpack=" + FIXPACK + "  vendor=" + VENDOR + "  javaHome=" + JAVA_HOME;
    }
}
