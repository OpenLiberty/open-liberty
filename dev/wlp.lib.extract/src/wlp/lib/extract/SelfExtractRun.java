/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Alternate jar main-class for Liberty jar created using server package <server> --include=runnable
 * Does following:
 * 1) extracts jar
 * 2) runs contained server
 * 3) deletes extraction directory
 */
public class SelfExtractRun extends SelfExtract {

    private static int platformType = SelfExtractUtils.getPlatform();

    /**
     * Determine user home based on platform type.
     * Java user.home property is correct in all cases except
     * for cygwin. For cygwin, user.home is Windows home,
     * so use HOME env var instead.
     *
     * @return user home directory
     */
    private static String getUserHome() {
        String home;

        if (platformType == SelfExtractUtils.PlatformType_CYGWIN) {

            home = System.getenv("HOME");
        } else {
            home = System.getProperty("user.home");
        }
        return home;
    }

    /**
     * Return server name from extractor
     *
     * @return server name
     */
    private static String getServerName() {

        return extractor.getServerName();
    }

    /**
     * Create input directory if it does not exist
     *
     * @return directory name
     */
    private static String createIfNeeded(String dir) {
        File f = new File(dir);
        if (f.exists()) {
            return dir;
        } else {
            boolean success = f.mkdirs();
            if (success)
                return dir;
            else
                return null;
        }
    }

    /**
     * Return jar file name from input archive
     *
     * @return <name> from path/<name>.jar
     */
    private static String jarFileName() {
        // do this first so we can access extractor, ok to invoke more than once
        createExtractor();
        // get <name> from path/<name>.jar
        String fullyQualifiedFileName = extractor.container.getName();
        int lastSeparator = fullyQualifiedFileName.lastIndexOf(File.separatorChar);
        String simpleFileName = fullyQualifiedFileName.substring(lastSeparator + 1);
        int dotIdx = simpleFileName.lastIndexOf('.');
        if (dotIdx != -1) {
            return simpleFileName.substring(0, simpleFileName.lastIndexOf('.'));
        }
        return simpleFileName;
    }

    /**
     * Generate unique directory name of form:
     * basedir/fileStem<time-in-nanos>
     *
     * @param baseDir
     * @param fileStem
     * @return unique dir name
     */
    private static String createTempDirectory(String baseDir, String fileStem) {
        Long nano = new Long(System.nanoTime());
        return baseDir + File.separator + fileStem + nano;
    }

    /**
     * Determine and return directory to extract into.
     * Three choices:
     * 1) ${WLP_JAR_EXTRACT_DIR}
     * 2) ${WLP_JAR_EXTRACT_ROOT}/<jar file name>_nnnnnnnnnnnnnnnnnnn
     * 3) default - <home>/wlpExtract/<jar file name>_nnnnnnnnnnnnnnnnnnn
     *
     * @return extraction directory
     */
    private static String getExtractDirectory() {
        createExtractor();
        String containerPath = extractor.container.getName();
        File containerFile = new File(containerPath);
        if (containerFile.isDirectory()) {
            extractor.allowNonEmptyInstallDirectory(true);
            return containerFile.getAbsolutePath();
        }
        // check if user specified explicit directory
        String extractDirVar = System.getenv("WLP_JAR_EXTRACT_DIR");

        // if so, return it and done
        if (extractDirVar != null && extractDirVar.length() > 0) {
            String retVal = createIfNeeded(extractDirVar.trim());
            return retVal;
        } else {

            String extractDirVarRoot = System.getenv("WLP_JAR_EXTRACT_ROOT");
            if (extractDirVarRoot == null || extractDirVarRoot.length() == 0) {
                extractDirVarRoot = getUserHome() + File.separator + "wlpExtract";
            }
            createIfNeeded(extractDirVarRoot);

            try {
                String basedir = (new File(extractDirVarRoot)).getAbsolutePath();
                return createTempDirectory(basedir, jarFileName() + "_");
            } catch (Exception e) {
                throw new RuntimeException("Could not create temp directory under: " + extractDirVarRoot);
            }
        }
    }

    /**
     * Write property into jvm.options to disable 2PC transactions.
     * 2PC transactions are disabled by default because default transaction
     * log is stored in extract directory and therefore foils transaction
     * recovery if the server terminates unexpectedly.
     *
     * @param extractDirectory
     * @param serverName
     * @throws IOException
     */
    private static void disable2PC(String extractDirectory, String serverName) throws IOException {
        String fileName = extractDirectory + File.separator + "wlp" + File.separator + "usr" + File.separator + "servers" + File.separator + serverName + File.separator
                          + "jvm.options";
        BufferedReader br = null;
        BufferedWriter bw = null;
        StringBuffer sb = new StringBuffer();

        try {

            String sCurrentLine;
            File file = new File(fileName);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                boolean success = file.createNewFile();
                if (!success) {
                    throw new IOException("Failed to create file " + fileName);
                }
            } else {
                // read existing file content

                br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
                while ((sCurrentLine = br.readLine()) != null) {
                    sb.append(sCurrentLine + "\n");
                }
            }

            // write property to disable 2PC commit
            String content = "-Dcom.ibm.tx.jta.disable2PC=true";

            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getAbsoluteFile()), "UTF-8"));
            bw.write(sb.toString());
            bw.write(content);

        } finally {
            if (br != null)
                br.close();
            if (bw != null)
                bw.close();

        }

    }

    /**
     * Run server extracted from jar
     * If environment variable WLP_JAR_DEBUG is set, use 'server debug' instead
     *
     * @param extractDirectory
     * @param serverName
     * @return server run return code
     * @throws IOException
     * @throws InterruptedException
     */
    private static int runServer(String extractDirectory, String serverName, String[] args) throws IOException, InterruptedException {
        int rc = 0;
        Runtime rt = Runtime.getRuntime();

        String action = "run";

        if (System.getenv("WLP_JAR_DEBUG") != null)
            action = "debug";

        // unless user specifies to enable 2PC,  disable it
        if (System.getenv("WLP_JAR_ENABLE_2PC") == null)
            disable2PC(extractDirectory, serverName);

        String cmd = extractDirectory + File.separator + "wlp" + File.separator + "bin" + File.separator + "server " + action + " " + serverName;
        if (args.length > 0) {
            StringBuilder appArgs = new StringBuilder(" --");
            for (String arg : args) {
                appArgs.append(" ").append(arg);
            }
            cmd += appArgs.toString();
        }

        System.out.println(cmd);

        if (platformType == SelfExtractUtils.PlatformType_UNIX) {
            // cmd ready as-is for Unix
        } else if (platformType == SelfExtractUtils.PlatformType_WINDOWS) {
            cmd = "cmd /k " + cmd;
        } else if (platformType == SelfExtractUtils.PlatformType_CYGWIN) {
            cmd = "bash -c  " + '"' + cmd.replace('\\', '/') + '"';
        }

        Process proc = rt.exec(cmd, SelfExtractUtils.runEnv(extractDirectory), null); // run server

        // setup and start reader threads for error and output streams
        StreamReader errorReader = new StreamReader(proc.getErrorStream(), "ERROR");
        errorReader.start();
        StreamReader outputReader = new StreamReader(proc.getInputStream(), "OUTPUT");
        outputReader.start();

        // now setup the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(platformType, extractDirectory, serverName, outputReader, errorReader)));

        // wait on server start process to complete, capture and pass on return code
        rc = proc.waitFor();

        return rc;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        String extractDirectory = getExtractDirectory();
        if (extractDirectory == null) {
            throw new RuntimeException("Failed to run jar because unable to create extraction directory.");
        } else {
            /*
             * call parent main to do extraction.
             * only arg is extractDirectory
             */
            String[] newArgs = { extractDirectory };
            int rc = doMain(newArgs);

            if (rc == 0) {
                try {
                    String serverName = getServerName();
                    if (shouldRunInJVM(extractDirectory, serverName)) {
                        rc = runServerInline(extractDirectory, serverName, args);
                    } else {
                        rc = runServer(extractDirectory, serverName, args);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to run jar due to error " + e.getMessage(), e);
                }
            }
            System.exit(rc);
        }

    }

    /**
     * @param extractDirectory
     * @param serverName
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private static int runServerInline(String extractDirectory,
                                       String serverName,
                                       String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        File serverLaunchJar = new File(extractDirectory, "wlp/bin/tools/ws-server.jar");
        JarFile jar = new JarFile(serverLaunchJar);
        String className = jar.getManifest().getMainAttributes().getValue("Main-Class");
        jar.close();
        URLClassLoader cl = new URLClassLoader(new URL[] { new URL("file:" + serverLaunchJar.getAbsolutePath()) });

        Properties props = System.getProperties();

        props.setProperty("user.dir", new File(extractDirectory, "wlp" + File.separator + "usr" + File.separator + "servers" + File.separator + serverName).getAbsolutePath());
        props.setProperty("LOG_DIR",
                          extractDirectory + File.separator + "wlp" + File.separator + "usr" + File.separator + "servers" + File.separator + serverName + File.separator + "logs");

        Class clazz = cl.loadClass(className);
        List<String> argList = new ArrayList<String>(args.length + 2);
        argList.add(serverName);
        if (args.length > 0) {
            argList.add("--");
            argList.addAll(Arrays.asList(args));
        }
        Method m = clazz.getDeclaredMethod("main", new Class[] { String[].class });

        attachJavaAgent(extractDirectory);

        m.invoke(null, new Object[] { argList.toArray(new String[0]) });

        return 0;
    }

    private static void attachJavaAgent(String extractDir) {

        File javaAgent = new File(extractDir, "wlp/bin/tools/ws-javaagent.jar");
        if (javaAgent.exists()) {
            String javaHome = System.getProperty("java.home");
            File f = new File(javaHome, "lib/tools.jar");
            if (!f.exists()) {
                f = new File(javaHome, "../lib/tools.jar");
            }

            if (f.exists()) {
                URL thisJar = SelfExtractRun.class.getProtectionDomain().getCodeSource().getLocation();
                try {
                    URL toolsJar = new URL("file:" + f.getCanonicalPath());
                    URLClassLoader cl = new URLClassLoader(new URL[] { thisJar, toolsJar }, null);
                    Class clazz = cl.loadClass("wlp.lib.extract.AgentAttach");
                    Method m = clazz.getDeclaredMethod("attach", new Class[] { String.class });
                    Object result = m.invoke(null, new String[] { javaAgent.getAbsolutePath() });
                    if (result != null) {
                        err("UNABLE_TO_ATTACH_AGENT", result);
                    }
                } catch (MalformedURLException mue) {
                    err("UNABLE_TO_ATTACH_AGENT", mue);
                } catch (ClassNotFoundException cnfe) {
                    err("UNABLE_TO_ATTACH_AGENT", cnfe);
                } catch (NoSuchMethodException nsme) {
                    err("UNABLE_TO_ATTACH_AGENT", nsme);
                } catch (IllegalAccessException iae) {
                    err("UNABLE_TO_ATTACH_AGENT", iae);
                } catch (InvocationTargetException ite) {
                    err("UNABLE_TO_ATTACH_AGENT", ite.getCause());
                } catch (IOException ioe) {
                    err("UNABLE_TO_ATTACH_AGENT", ioe);
                }
            } else {
                err("UNABLE_TO_FIND_TOOLS_JAR");
            }
        } else {
            err("UNABLE_TO_FIND_JAVA_AGENT");
        }
    }

    /**
     * @param serverName
     * @return
     */
    private static boolean shouldRunInJVM(String extractDir, String serverName) {

        // If WLP_JAR_DEBUG is set then we use 2 JVM's
        boolean result = System.getenv("WLP_JAR_DEBUG") == null;
        boolean outputMessage = true;

        // If we can find any jvm.options files we use 2 JVM's
        if (result) {
            File serverDir = new File(extractDir, "wlp/usr/servers/" + serverName);
            // check ${server.config.dir}/configDropins/defaults/
            result &= !new File(serverDir, "configDropins/defaults/jvm.options").exists();
            // check ${server.config.dir}/
            result &= !new File(serverDir, "jvm.options").exists();
            // check ${server.config.dir}/configDropins/overrides/
            result &= !new File(serverDir, "configDropins/overrides/jvm.options").exists();
            // check ${wlp.install.dir}/etc
            result &= !new File(extractDir, "wlp/etc/jvm.options").exists();
        } else if (outputMessage) {
            out("RUN_IN_CHILD_JVM_DEBUG");
            outputMessage = false;
        }

        // If on an IBM Java make sure -XX:+EnableHCR is configured since that is required to attach agent
        if (result) {
            RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
            String vendor = rt.getVmVendor();
            String version = rt.getSpecVersion();

            if (version.startsWith("1") &&
                vendor.toLowerCase().contains("ibm")) {
                result = rt.getInputArguments().contains("-XX:+EnableHCR");
            }
        } else if (outputMessage) {
            out("RUN_IN_CHILD_JVM_JVM_OPTIONS");
            outputMessage = false;
        }

        // Only run in 1 JVM if running on a Java SDK
        if (result) {
            String javaHome = System.getProperty("java.home");
            File f = new File(javaHome, "lib/tools.jar");
            boolean foundToolsJar = f.exists();
            if (!foundToolsJar) {
                f = new File(javaHome, "../lib/tools.jar");
                foundToolsJar = f.exists();
            }

            result &= foundToolsJar;
        } else if (outputMessage) {
            out("RUN_IN_CHILD_JVM_IBM_AGENT_ISSUE");
            outputMessage = false;
        }

        if (!result && outputMessage) {
            out("RUN_IN_CHILD_JVM_JRE");
        }

        return result;
    }
}
