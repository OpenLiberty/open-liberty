/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.ibm.websphere.simplicity.ProgramOutput;

import componenttest.topology.impl.LibertyServer;

public abstract class AbstractLooseConfigTest {
    protected static final long MS_IN_SEC = 1000;
    protected static final long NS_IN_SEC = 1000000000;

    // Local folders ...
    
    protected static final String BUILD_DIR = "build";
    protected static final String PUBLISH_RESOURCES = "publish/resources/";
    protected static final String CONFIG_SOURCE = PUBLISH_RESOURCES + "configs/";
    protected static final String TMP_SOURCE = PUBLISH_RESOURCES + "tmp/";

    // Liberty image folders ...

    protected static final String APPS_DIR = "apps";
    protected static final String DROPINS_DIR = "dropins";

    public abstract String getAppsTargetDir();
    
    // The liberty server used by these tests ...
    // Also, a liberty image folder.

    protected static final String SERVER_NAME = "com.ibm.ws.kernel.boot.loose.config.fat";
    protected static final String SERVER_NAME_JAR = SERVER_NAME + ".jar";
    protected static final String SERVER_NAME_ZIP = SERVER_NAME + ".zip";

    // Packaging values ...
  
    protected static final String ARCHIVE_NAME_ZIP = "MyPackage.zip";
    protected static final String ARCHIVE_NAME_1_JAR = "MyPackage1.jar";
    protected static final String ARCHIVE_NAME_2_JAR = "MyPackage2.jar";

    protected static final String SERVER_ROOT_DEFAULT = "wlp";
    protected static final String SERVER_ROOT = "MyRoot";

    /**
     * Environment variable used to specify where the liberty image
     * is extracted when launching a liberty runnable jar.
     */
    protected static final String WLP_EXTRACT_PROPERTY_NAME = "WLP_JAR_EXTRACT_ROOT";

    /**
     * Value to assign to the wlp-extract property.
     */
    protected static final String WLP_EXTRACT = "wlpExtract";

    //
    
    private LibertyServer server;

    protected LibertyServer getServer() {
        return server;
    }

    protected void setServer(LibertyServer server) {
        this.server = server;
    }

    //

    protected String packageRunnable(
        String moduleName, String packageName, String serverRoot)
        throws Exception {

        String[] packageCmd;

        if ( !serverRoot.equals(SERVER_ROOT_DEFAULT) ) {
            // The server root parameter is ignored!
            // stdout will display the following,
            // and 'wlp' will be used as the server root.
            //
            // Packaging server com.ibm.ws.kernel.boot.loose.config.fat.
            // CWWKE0948W: A --server-root option with --include=runnable
            //   is not an allowed combination. The default argument of wlp
            //   is used for the server root.
            // Server com.ibm.ws.kernel.boot.loose.config.fat package complete
            //   in C:\\dev\\repos-pub\\open-liberty\\dev\\build.image\\wlp\\usr\\servers\\com.ibm.ws.kernel.boot.loose.config.fat\\MyPackage2.jar.

            packageCmd = new String[] {
                "--archive=" + packageName,
                "--include=runnable",
                "--server-root=" + serverRoot
                };

        } else {
            packageCmd = new String[] {
                "--archive=" + packageName,
                "--include=runnable"
            };            
        }

        packageServer(moduleName, packageName, packageCmd);

        return getServer().getServerRoot() + '/' + packageName;
    }

    public String packageServer(
        String looseConfig, String archiveName,
        String[] packageCmd) throws Exception {

        String methodName = "packageServer";

        String looseConfigPath = getAppsTargetDir() + '/' + looseConfig;
        String archivePath = server.getServerRoot() + '/' + archiveName;        

        System.out.println(methodName + ": Server: " + server.getServerName());
        System.out.println(methodName + ": Loose config: " + looseConfig);
        System.out.println(methodName + ": Loose config path: " + looseConfigPath);
        System.out.println(methodName + ": Archive: " + archiveName);
        System.out.println(methodName + ": Archive path: " + archivePath);

        for ( String cmdArg : packageCmd ) {
            System.out.println(methodName + ": Package arg: " + cmdArg);
        }

        // 'lib/extract' need not be present when packaging to a ZIP archive.
        //
        // 'lib/extract' must be present when packaging to a JAR archive.
        // However, tests which package to a JAR have an early test for
        // the directory, and the test is skipped if the directory is
        // not present.
        //
        // The result is that in either case, this test is not needed.
        //
        // server.getFileFromLibertyInstallRoot("lib/extract");

        server.copyFileToLibertyServerRoot(CONFIG_SOURCE, getAppsTargetDir(), looseConfig);

        ProgramOutput output = server.executeServerScript("package", packageCmd);
        String stdout = output.getStdout();
        String stderr = output.getStderr();
        if ( !stdout.isEmpty() ) {
            System.out.print("stdout:");
            System.out.println(stdout);
        }
        if ( !stderr.isEmpty() ) {
            System.out.println("stderr:");        
            System.out.print(stderr);
        }
        if ( !stdout.contains("package complete") ) {
            fail("Package of [ " + server.getInstallRoot() + " ] into [ " + archiveName + " ] command did not complete. STDOUT = " + stdout);
        }

        return archivePath;
    }

    @SuppressWarnings("unused")
    protected void verifyContents(
        String archivePath, String serverRoot,
        boolean includeUsr,
        String serverName, String moduleName) throws IOException {
        // Do nothing by default.
    }    
    
    protected static final boolean INCLUDE_USR = true;
    protected static final boolean VERIFY_APP = true;

    protected void verifyContents(
            String archivePath,
            String installRoot, boolean includeUsr, 
            String serverName, String appName, boolean verifyApp) throws IOException {

        String methodName = "verifyContents";

        System.out.println(methodName + ": Verifying [ " + archivePath + " ]");
        System.out.println(methodName + ":   Install root [ " + installRoot + " ]");
        System.out.println(methodName + ":   Server [ " + serverName + " ]");
        System.out.println(methodName + ":   Module [ " + appName + " ]");

        String appsDir = getAppsTargetDir();

        String serverPath;
        if ( includeUsr ) {
            serverPath = installRoot + "/usr/servers/" + serverName;
        } else {
            serverPath = installRoot + "/servers/" + serverName;
        }
        String appPath = serverPath + '/' + appsDir + '/' + appName;
        // The expanded application is always placed under 'apps', even if the
        // un-expanded application is under 'dropins'.
        String expandedAppPath = serverPath + "/apps/expanded/" + appName + '/';

        System.out.println(methodName + ":   Server path [ " + serverPath + " ]");
        
        if ( verifyApp ) {
            System.out.println(methodName + ":   App path [ " + appPath + " ]");
            System.out.println(methodName + ":   Expanded app path [ " + expandedAppPath + " ]");
        }

        try ( ZipFile zipFile = new ZipFile(archivePath) ) {
            boolean foundAll = false;
            boolean foundServerEntry = false;
            boolean foundAppEntry = !verifyApp; // Short circuit these if app validation is off.
            boolean foundExpandedAppEntry = !verifyApp;

            String lastEntry = null;
            int lastSlash = -1;

            Enumeration<? extends ZipEntry> en = zipFile.entries();
            while ( !foundAll && en.hasMoreElements() ) {
                ZipEntry entry = en.nextElement();
                String entryName = entry.getName();
                int slash = entryName.lastIndexOf('/');
                boolean doLog = (
                    (lastEntry == null) ||
                    (slash != lastSlash) ||
                    !entryName.regionMatches(0, lastEntry, 0, lastSlash) );
                if ( doLog ) {
                    lastEntry = entryName;
                    lastSlash = slash;
                    System.out.println("Entry [ " + entryName + " ]");
                }                    

                if ( !foundServerEntry ) {
                    foundServerEntry = entryName.startsWith(serverPath);
                }
                if ( !foundAppEntry ) {
                    foundAppEntry = entryName.startsWith(appPath);
                }
                if ( !foundExpandedAppEntry ) {
                    foundExpandedAppEntry = entryName.startsWith(expandedAppPath);
                }
                foundAll = ( foundServerEntry && foundAppEntry && foundExpandedAppEntry );                                                
            }

            if ( !foundServerEntry ) {
                fail("Package [ " + archivePath + " ] missing [ " + serverPath + " ]");
            }
            if ( !foundAppEntry ) {
                fail("Package [ " + archivePath + " ] missing [ " + appPath + " ]");
            }
            if ( !foundExpandedAppEntry ) {
                fail("Package [ " + archivePath + " ] missing [ " + expandedAppPath + " ]");
            }
        }
    }

    protected void verifyExpandedContents(
        String archivePath,
        String serverRoot, boolean includeUsr, String serverName,
        String moduleName) throws IOException {
        
        String methodName = "verifyExpandedContents";

        String packedPath = serverRoot;
        if ( includeUsr ) {
            packedPath += "/usr";
        }
        packedPath +=
            "/servers/" + serverName + '/' +
            getAppsTargetDir() + '/' +
            moduleName;

        String unpackedPrefix = serverRoot;
        if ( includeUsr ) {
            unpackedPrefix += "/usr";
        }
        unpackedPrefix +=
            "/servers/" + serverName + '/' +
            getAppsTargetDir() + "/expanded/" +
            moduleName + '/';
        int unpackedPrefixLen = unpackedPrefix.length();

        System.out.println(methodName + ":  Packed archive [ " + packedPath + " ]");
        System.out.println(methodName + ":  Unpacked archive [ " + unpackedPrefix + " ]");

        Map<String, Integer> packedMapping = null;
        Map<String, Integer> unpackedMapping = null;

        try ( ZipFile packageZip = new ZipFile(archivePath) ) {
            int unpackedOffset = 0;

            String lastEntry = null;
            int lastSlash = -1;
            
            Enumeration<? extends ZipEntry> entries = packageZip.entries();
            while ( entries.hasMoreElements() ) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                int slash = entryName.lastIndexOf('/');
                boolean doLog = (
                    (lastEntry == null) ||
                    (slash != lastSlash) ||
                    !entryName.regionMatches(0, lastEntry, 0, lastSlash) );
                if ( doLog ) {
                    lastEntry = entryName;
                    lastSlash = slash;
                    System.out.println("Entry [ " + entryName + " ]");
                }                

                if ( entryName.equals(packedPath) ) {
                    if ( packedMapping != null ) {
                        fail("Archive [ " + archivePath + " ] has duplicates of entry [ " + packedPath + " ]");
                        return;
                        // Never used; added to avoid a compiler null value warning:
                        // The compiler doesn't know that 'fail' never returns.                        
                    }
                    packedMapping = new HashMap<String, Integer>();

                    try ( InputStream nestedStream = packageZip.getInputStream(entry);
                          ZipInputStream nestedZipStream = new ZipInputStream(nestedStream); ) {

                        ZipEntry nestedEntry;
                        for ( int offset = 0;
                              (nestedEntry = nestedZipStream.getNextEntry()) != null;
                              offset++ ) { 
                            packedMapping.put( nestedEntry.getName(), Integer.valueOf(offset) ); 
                        }
                    }

                } else {
                    // '<=' is deliberate: We don't want the entry for
                    // the directory of the unpacked archive.
                    if ( entryName.length() <= unpackedPrefixLen ) {
                        // ignore this entry
                    } else  if ( entryName.startsWith(unpackedPrefix) ) {
                        if ( unpackedMapping == null ) {
                            unpackedMapping = new HashMap<String, Integer>();
                        }
                        String suffix = entryName.substring(unpackedPrefix.length());
                        unpackedMapping.put(suffix, Integer.valueOf(unpackedOffset++));
                    } else {
                        // ignore this entry ...
                    }
                }
            }
        }
        
        if ( unpackedMapping == null ) {
            fail("Archive [ " + archivePath + " ] has no unpacked module entries [ " + unpackedPrefix + " ]");
            return;
            // Never used; added to avoid a compiler null value warning:
            // The compiler doesn't know that 'fail' never returns.
        }

        if ( packedMapping == null ) {
            fail("Archive [ " + archivePath + " ] has no packed module [ " + packedPath + " ]");
            return;
            // Never used; added to avoid a compiler null value warning:
            // The compiler doesn't know that 'fail' never returns.
        }

        int failures = 0;

        for ( Map.Entry<String, Integer> packedEntry : packedMapping.entrySet() ) {
            String packedName = packedEntry.getKey();
            Integer packedOffset = packedEntry.getValue();
            
            Integer unpackedOffset = unpackedMapping.get(packedName);
            
            if ( unpackedOffset == null ) {
                System.out.println("Extra packed entry [ " + packedName + " ]");
                failures++;
            } else {
                if ( packedOffset.intValue() != unpackedOffset.intValue() ) {
                    System.out.println("Packed entry [ " + packedName + " ] changed offset from [ " + packedOffset.intValue() + " ] to [ " + unpackedOffset.intValue() + " ]");
                    failures++;
                }
            }
        }
        
        for ( String unpackedName : unpackedMapping.keySet() ) {
            if ( !packedMapping.containsKey(unpackedName) ) {
                System.out.println("Extra unpacked entry [ " + unpackedName + " ]");
                failures++;
            } else {
                // The offsets were already verified
            }
        }
            
        if ( failures != 0 ) {
            fail("Archive [ " + archivePath + " ] packed archive [ " + packedPath + " ] has [ " + failures + " ] content errors"); 
        }
    }
    
    
    protected void verifyFilteredContents(
        String archivePath,
        String serverRoot, boolean includeUsr, String serverName,
        String moduleName,
        Collection<String> requiredEntries,
        Collection<String> forbiddenEntries) throws IOException {

        String methodName = "verifyFilteredContents";

        String packedPath = serverRoot;
        if ( includeUsr ) {
            packedPath += "/usr";
        }
        packedPath +=
            "/servers/" + serverName + '/' +
            getAppsTargetDir() + '/' +
            moduleName;

        String unpackedPrefix = serverRoot;
        if ( includeUsr ) {
            unpackedPrefix += "/usr";
        }
        unpackedPrefix +=
            "/servers/" + serverName + '/' +
            getAppsTargetDir() + "/expanded/" +
            moduleName + '/';
        int unpackedPrefixLen = unpackedPrefix.length();

        System.out.println(methodName + ":  Packed archive [ " + packedPath + " ]");
        System.out.println(methodName + ":  Unpacked archive [ " + unpackedPrefix + " ]");

        System.out.println(methodName + ":  Forbidden entries: " + forbiddenEntries);
        System.out.println(methodName + ":  Required entries: " + requiredEntries);

        Set<String> forbidden = new HashSet<String>(forbiddenEntries);
        Set<String> forbiddenButPresentPacked = null;
        Set<String> forbiddenButPresentUnpacked = null;

        Set<String> requiredButAbsentPacked = new HashSet<String>(requiredEntries);
        Set<String> requiredButAbsentUnpacked = new HashSet<String>(requiredEntries);

        try ( ZipFile packageZip = new ZipFile(archivePath) ) {
            String lastEntry = null;
            int lastSlash = -1;
                
            Enumeration<? extends ZipEntry> entries = packageZip.entries();
            while ( entries.hasMoreElements() ) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                int slash = entryName.lastIndexOf('/');
                boolean doLog = (
                    (lastEntry == null) ||
                    (slash != lastSlash) ||
                    !entryName.regionMatches(0, lastEntry, 0, lastSlash) );
                if ( doLog ) {
                    lastEntry = entryName;
                    lastSlash = slash;
                    System.out.println("Entry [ " + entryName + " ]");
                }                

                if ( entryName.equals(packedPath) ) {
                    try ( InputStream nestedStream = packageZip.getInputStream(entry);
                          ZipInputStream nestedZipStream = new ZipInputStream(nestedStream); ) {

                        ZipEntry nestedEntry;
                        while ( (nestedEntry = nestedZipStream.getNextEntry()) != null ) {
                            String nestedEntryName = nestedEntry.getName();
                            if ( forbidden.contains(nestedEntryName) ) {
                                if ( forbiddenButPresentPacked == null ) {
                                    forbiddenButPresentPacked = new HashSet<String>(1);
                                }
                                forbiddenButPresentPacked.add(nestedEntryName);
                            }
                            requiredButAbsentPacked.remove(nestedEntryName);
                        }
                    }
                } else {
                    // '<=' is deliberate: We don't want the entry for
                    // the directory of the unpacked archive.
                    if ( entryName.length() <= unpackedPrefixLen ) {
                        // ignore this entry
                    } else  if ( entryName.startsWith(unpackedPrefix) ) {
                        String suffix = entryName.substring(unpackedPrefix.length());
                        if ( forbidden.contains(suffix) ) {
                            if ( forbiddenButPresentUnpacked == null ) {
                                forbiddenButPresentUnpacked = new HashSet<String>(1);
                            }                            
                            forbiddenButPresentUnpacked.add(suffix);
                        }
                        requiredButAbsentUnpacked.remove(suffix);

                    } else {
                        // ignore this entry ...
                    }
                }
            }

            String error1 = null;
            if ( forbiddenButPresentPacked != null ) {
                error1 = "Archive has extra packed module entries [ " + packedPath + " ]: " + forbiddenButPresentPacked;
                System.out.println(error1);
            }
            String error2 = null;
            if ( !requiredButAbsentPacked.isEmpty() ) {
                error2 = "Archive has missing packed module entries [ " + packedPath + " ]: " + requiredButAbsentPacked;
                System.out.println(error2);                
            }
            String error3 = null;
            if ( forbiddenButPresentUnpacked != null ) {
                error3 = "Archive has extra unpacked module entries [ " + unpackedPrefix + " ]: " + forbiddenButPresentUnpacked;
                System.out.println(error3);
            }
            String error4 = null;
            if ( !requiredButAbsentUnpacked.isEmpty() ) {
                error4 = "Archive has missing unpacked module entries [ " + unpackedPrefix + " ]: " + requiredButAbsentUnpacked;
                System.out.println(error4);
            }            
            
            if ( error1 != null ) {
                fail(error1);
            }
            if ( error2 != null ) {
                fail(error2);
            }
            
            if ( error3 != null ) {
                fail(error3);
            }            
            if ( error4 != null ) {
                fail(error4);
            }
        }    
    }

    //

    protected static class UnblockedReader implements Closeable {
        @SuppressWarnings("unused")
        private final InputStream inputStream;
        private final BufferedReader inputReader;

        private final BlockingQueue<String> inputQueue;
        private final Thread inputThread;

        public UnblockedReader(InputStream inputStream) {
            this.inputStream = inputStream;
            this.inputReader =  new BufferedReader( new InputStreamReader(inputStream) );

            this.inputQueue = new LinkedBlockingDeque<>();
            this.inputThread = new Thread(this::postAll);

            this.inputThread.start();
        }

        public void close() {
            stop();
        }

        @SuppressWarnings("deprecation")
        public void stop() {
            inputThread.stop();
        }

        private void postAll() {
            try {
                for (String line; (line = inputReader.readLine()) != null;) {
                    inputQueue.put(line);
                }
            } catch ( Exception e ) {
                System.out.println("Unexpected read exception: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        public String poll() {
            String line = inputQueue.poll();
            if ( line == null ) {
                Thread.yield();
                line = inputQueue.poll();
            }
            return line;
        }
    }

    protected static class SafeProcess implements Closeable {
        public SafeProcess(String[] cmd, String[] envp) throws IOException {
            this.process = Runtime.getRuntime().exec(cmd, envp, null);
        }        
        
        public SafeProcess(String[] cmd) throws IOException {
            this.process = Runtime.getRuntime().exec(cmd);
        }

        private final Process process;

        public InputStream getStdoutStream() {
            // Note: This is correct; the names are just messed up.
            return process.getInputStream();
        }

        public UnblockedReader createStdoutReader() {
            return new UnblockedReader( getStdoutStream() );
        }

        public InputStream getStderrStream() {
            return process.getErrorStream();
        }

        public UnblockedReader createStderrReader() {
            return new UnblockedReader( getStderrStream() );
        }

        public void close() {
            process.destroy();
        }
    }

    // Expected output:
    //
    // java -jar runnablePackage.jar
    // Extracting files to C:\\Users\\...\\wlpExtract\\runnablePackage_260686838796000\\wlp
    // Successfully extracted all product files.
    // A Java runtime environment installation is being used. The server will run in a separate Java virtual machine.
    // C:\\Users\\ThomasBitonti\\wlpExtract\\runnablePackage_260686838796000\\wlp\\bin\\server run com.ibm.ws.kernel.boot.loose.config.fat
    // Launching com.ibm.ws.kernel.boot.loose.config.fat (Open Liberty 21.0.0.3/wlp-1.0.50.202102222233) on IBM J9 VM, version 8.0.5.10 - pwa6480sr5fp10-20180214_01(SR5 FP10) (en_US)
    // [AUDIT   ] CWWKE0001I: The server com.ibm.ws.kernel.boot.loose.config.fat has been launched.
    // [WARNING ] CWWKF0009W: The server has not been configured to install any features.
    // [AUDIT   ] CWWKF0011I: The com.ibm.ws.kernel.boot.loose.config.fat server is ready to run a smarter planet. The com.ibm.ws.kernel.boot.loose.config.fat server started in 58.478 seconds.

    private static final Pattern launchPattern =
        Pattern.compile("^.* CWWKE0001I: .* " + SERVER_NAME + " .*$");
    private static final Pattern readyPattern =
        Pattern.compile(".* CWWKF0011I: .* " + SERVER_NAME + " .*$");
    
    // The windows defender will slow down the runnable server,
    // increasing the time taken for the the server to start from
    // just a few seconds to nearly a minute.  To avoid this
    // slowdown, and as a generally better build practice, the
    // extraction is redirected to the local build folder.
    //
    // From: 
    // https://www.ibm.com/support/knowledgecenter/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/rwlp_setup_jarserver.html
    //
    // When the JAR file runs, it gets extracted to a temporary location and then
    // the server runs in the foreground, started by the Liberty server run command.
    // All output is written to stdout or stderr. By default, files are extracted to
    // temporary locations:
    //
    // For Windows: %HOMEPATH%/wlpExtract/<jar file name>_nnnnnnnnnnnnnnnnnnn
    // For all other platforms: $HOME/wlpExtract/<jar file name>_nnnnnnnnnnnnnnnnnnn
    //
    // You can control the output location by using the WLP_JAR_EXTRACT_ROOT or
    // WLP_JAR_EXTRACT_DIR environment variable.

    // When running on windows using Java11, for example:
    //
    // openjdk version "11.0.4" 2019-07-16
    // OpenJDK Runtime Environment AdoptOpenJDK (build 11.0.4+11)
    // Eclipse OpenJ9 VM AdoptOpenJDK (build openj9-0.15.1, JRE 11 Windows 10 amd64-64-Bit 20190717_36 (JIT enabled, AOT enabled)
    // OpenJ9   - 0f66c6431
    // OMR      - ec782f26
    // JCL      - fa49279450 based on jdk-11.0.4+11)
    //
    // The extraction code generates illegal access warnings:
    //
    // WARNING: An illegal reflective access operation has occurred
    // WARNING: Illegal reflective access by org.eclipse.osgi.storage.FrameworkExtensionInstaller
    //   (file:/C:/Users/ThomasBitonti/wlpExtract/runnablePackage_258869724879800/wlp/lib/org.eclipse.osgi_3.15.0.jar)
    //   to method java.net.URLClassLoader.addURL(java.net.URL)
    // WARNING: Please consider reporting this to the maintainers of org.eclipse.osgi.storage.FrameworkExtensionInstaller
    // WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
    // WARNING: All illegal access operations will be denied in a future release
    //
    // For now, the messages are warnings.  Later, they may be errors,
    // in which case a switch will be necessary to turn them back into
    // being warnings:
    // "--illegal-access=warn"
    
    protected void launchRunnable(String archivePath) throws Exception {        
        String[] launchCmd = {
            "java",
            "-jar", archivePath,
        };

        String[] launchEnv = {
            WLP_EXTRACT_PROPERTY_NAME + "=" + BUILD_DIR + '/' + WLP_EXTRACT
        };

        try ( SafeProcess proc = new SafeProcess(launchCmd, launchEnv);
              UnblockedReader stdoutReader = proc.createStdoutReader();
              UnblockedReader stderrReader = proc.createStderrReader(); ) {

            boolean serverDidLaunch = false;
            boolean serverIsReady = false;

            long timeStart = System.nanoTime();
            System.out.println("Start time: " + timeStart);

            for (;;) {
                System.out.println("Polling for errors ...");
                String error;
                while ( (error = stderrReader.poll()) != null ) {
                    if ( !isReflectiveAccessWarning(error) ) {
                        System.out.println("Launch error: " + error);
                        fail("Server launch error: " + error);
                    } else {
                        System.out.println("Ignoring launch error: " + error);
                    }
                }

                System.out.println("Polling for output ...");                    
                String line;
                while ( (line = stdoutReader.poll()) != null ) {
                    System.out.println("Launch output: " + line);
                    if ( !serverDidLaunch ) {
                        serverDidLaunch = launchPattern.matcher(line).matches();
                        if ( serverDidLaunch ) {
                            System.out.println("Server did launch");
                        }
                    }
                    if ( !serverIsReady ) {
                        serverIsReady = readyPattern.matcher(line).matches();
                        if ( serverIsReady ) {
                            System.out.println("Server is ready");
                        }
                    }
                }

                if ( serverDidLaunch && serverIsReady ) {
                    break;
                }

                long timeNext = System.nanoTime();
                long timeElapsed = timeNext - timeStart;
                System.out.println("Poll time: " + timeNext + ": Elapsed: " + timeElapsed);
                if ( timeElapsed > NS_IN_SEC * 20 ) {
                    if ( !serverDidLaunch ) {
                        fail("Server package " + archivePath + " did not launch in time.");
                    }
                    if ( !serverIsReady ) {
                        fail("Server package " + archivePath + " was not ready in time.");
                    }
                }

                Thread.sleep(MS_IN_SEC);
            }
        }
    }

    private boolean isReflectiveAccessWarning(String line) {
        return ( line.contains("reflective access") ||
                 line.contains("consider reporting") ||
                 line.contains("illegal access") );
    }
}
