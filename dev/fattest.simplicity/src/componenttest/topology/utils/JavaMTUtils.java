/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.common.apiservices.Bootstrap;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class JavaMTUtils {

    private static final Class<?> c = JavaMTUtils.class;

    private static final String JAVA_VERSION =
                    AccessController.doPrivileged(new PrivilegedAction<String>() {

                        @Override
                        public String run() {
                            return System.getProperty("java.version");
                        }
                    });
    private static final String JAVA_VM_VENDOR =
                    AccessController.doPrivileged(new PrivilegedAction<String>() {

                        @Override
                        public String run() {
                            return System.getProperty("java.vm.vendor");
                        }
                    });
    private static final String OS_NAME =
                    AccessController.doPrivileged(new PrivilegedAction<String>() {

                        @Override
                        public String run() {
                            return System.getProperty("os.name");
                        }
                    });
    private static final String JVM_BIT_MODE =
                    AccessController.doPrivileged(new PrivilegedAction<String>() {

                        @Override
                        public String run() {
                            return System.getProperty("com.ibm.vm.bitmode");
                        }
                    });

    private static String javadDir = null;
    private static String javadFilePath = null;

    private static boolean isJava7; // -Xmt flag support did not make it to Java 8. Hence we need to only check with Java 7.
    private static boolean isIBMJDK;
    private static boolean isOSIBMiOrzOS;
    private static boolean is64BitMachine;

    private static String jvmOptionsFilePath = null;

    public static boolean checkSupportedEnvForMultiTenancy() {

        // MT is only available on IBM Java.  Check this first to avoid errors in parsing foreign JVM strings below.
        if (!JAVA_VM_VENDOR.contains("IBM"))
            return false;
        String method = "checkSupportedEnvForMultiTenancy";
        String[] version = JAVA_VERSION.split("\\.");
        Integer[] versionNum = new Integer[version.length];
        int i = 0;
        for (String str : version) {
            versionNum[i] = Integer.parseInt(str);
            i++;
        }
        if (versionNum[0] >= 1 && versionNum[1] == 7 && versionNum[2] >= 0) //Only IBM JRE v7R1 supports the -Xmt flag. The support was dropped in Java 8
            isJava7 = true;
        else
            isJava7 = false;
        //isJava7orAbove = JAVA_VERSION.matches("1\\.[7-9]\\.[0-9]");
        isIBMJDK = JAVA_VM_VENDOR.contains("IBM");
        isOSIBMiOrzOS = OS_NAME.contains("OS/400") || OS_NAME.contains("z/OS");
        is64BitMachine = JVM_BIT_MODE != null && JVM_BIT_MODE.matches("64");

        Log.info(c, method, "MT environment test properties..");
        Log.info(c, method, "isJava7orAbove : " + JAVA_VERSION);
        Log.info(c, method, "isIBMJDK : " + JAVA_VM_VENDOR);
        Log.info(c, method, "isOSIBMiOrzOS : " + OS_NAME);
        Log.info(c, method, "is64BitMachine : " + JVM_BIT_MODE);

        if (!isOSIBMiOrzOS && isJava7 && isIBMJDK && is64BitMachine) {
            if (supportsXmtFlag())
                return true;
            else
                return false;
        } else
            return false;
    }

    public static boolean supportsXmtFlag() {
        boolean supportsMT = true;

        Process javaMTCommandProcess = null;
        BufferedReader in = null;

        String method = "supportsXmtFlag";
        Log.entering(c, method);
        Log.info(c, method, "Testing for Java MT by issuing the java -version command");

        //Get machine java.home using bootstrap         
        Bootstrap b = null;
        try {
            b = Bootstrap.getInstance();
        } catch (Exception e1) {
            Log.error(c, method, e1, "Error while Getting the bootstrap instance");
        }
        String hostName = b.getValue("hostName");
        String machineJavaHome = b.getValue(hostName + ".JavaHome");
        Log.info(c, method, "machineJavaHome from Bootstrap : " + machineJavaHome);
        String JAVA_COMMAND = machineJavaHome + "/bin/java";
        String JAVA_MT_FLAG = "-Xmt";

        List<String> params = java.util.Arrays.asList(JAVA_COMMAND, JAVA_MT_FLAG);
        Log.info(c, method, " jvm params : " + params.toString());

        // Start the java process in multitenancy mode
        try {
            ProcessBuilder pb = new ProcessBuilder(params);
            pb.redirectErrorStream(true);
            javaMTCommandProcess = pb.start();
        } catch (IOException e3) {
            Log.error(c, method, e3, "Error while starting the Java MT Process");
            try {
                throw new Exception("Error while starting the Java MT Process");
            } catch (Exception e) {
                Log.error(c, method, e3, "Error while throwing new exception");
            }
        }

        in = new BufferedReader(new InputStreamReader(javaMTCommandProcess.getInputStream()));

        try {
            String line = null;
            long time = System.currentTimeMillis();
            long timeout = time + 120000; //wait max two minutes for java -Xmt to execute
            while (System.currentTimeMillis() <= timeout) {
                line = in.readLine();
                if (line != null) {
                    //Java v7R1 SR2 has changed the message to indicate non-support of -Xmt. The new message reads "Error: -Xmt no longer supported" 
                    if (line.contains("JVMJ9VM007E") || line.contains("JVMJ9VM143E") || line.contains("Error")) { //Java 8 gives error code JVMJ9VM143E to indicate no support for -Xmt flag
                        supportsMT = false;
                        Log.info(c, method, line);
                        break;
                    } else {
                        //wasn't the output we expected, but wasn't end of stream
                        //so just continue
                        continue;
                    }
                } else {
                    //reached the end of the stream, wait 1 second before trying again
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.error(c, "start", e, "Error occured while Thread.sleep()");
                    }
                }
            }
        } catch (IOException e2) {
            Log.error(c, "start", e2, "Error occured while reading the buffered reader");
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Log.error(c, "start", e, "Error occured while closing the buffered reader");
            }
        }
        if (!supportsMT)
            Log.info(c, method, "The Java version which is used does not support -Xmt flag.");
        Log.exiting(c, method);
        return supportsMT;
    }

    public static void createJavad(LibertyServer server) throws Exception {
        server.copyFileToLibertyServerRoot("../../javad", "javad.options");
        javadDir = server.getUserDir() + "/" + "javad";
        javadFilePath = javadDir + "/" + "javad.options";

        try {
            FileOutputStream out = new FileOutputStream(new File(javadFilePath), true);
            String javaDumpFlag = "-Xdump:java:label=" + javadDir
                                  + "/javacore.%pid.%seq.txt";
            String heapDumpFlag = "-Xdump:heap:label=" + javadDir
                                  + "/heapcore.%pid.%seq.txt";
            String systemDumpFlag = "-Xdump:system:label=" + javadDir
                                    + "/systemcore.%pid.%seq.txt";
            String newLine = System.getProperty("line.separator");
            out.write(javaDumpFlag.getBytes());
            out.write(newLine.getBytes());
            out.write(heapDumpFlag.getBytes());
            out.write(newLine.getBytes());
            out.write(systemDumpFlag.getBytes());
            out.close();
        } catch (IOException e) {
            throw new Exception("Error occured while updating the javad.options file.");
        }
    }

    public static void createJvmOptions(LibertyServer server) throws Exception {
        server.copyFileToLibertyServerRoot("jvm.options");
        jvmOptionsFilePath = server.getServerRoot() + "/" + "jvm.options";

        try {
            FileOutputStream out = new FileOutputStream(new File(jvmOptionsFilePath), true);
            String newLine = System.getProperty("line.separator");
            String javadHomePath = "-Djavad.home=" + server.getUserDir() + "/" + "javad";
            out.write(newLine.getBytes());
            out.write(javadHomePath.getBytes());
            out.close();
        } catch (IOException e) {
            throw new Exception("Error occured while updating the jvm.options file.");
        }
    }

}
