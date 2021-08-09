/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;

import com.ibm.websphere.simplicity.ConnectionInfo;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;

/**
 *
 */
public class LibertyServerUtils {

    private static final Class<?> c = LibertyServerUtils.class;

    /**
     * Makes the String compatible with Java, this problem only exists where
     * Windows uses \ for a path seperator and Java uses /
     *
     * @param s
     *            the string to change
     */
    public static String makeJavaCompatible(String s) {
        try {
            File f = new File(s);
            return f.getCanonicalPath().replace('\\', '/');
        } catch (IOException e) {
            Log.info(c, "makeJavaCompatible", "Unable to normalize the path: " + s + ". Error: " + e.getMessage());
            e.printStackTrace();
        }

        return s.replace('\\', '/');
    }

    /**
     * Makes the String compatible with Java, this problem only exists where
     * Windows uses \ for a path seperator and Java uses /
     *
     * @param s
     *            the string to change
     */
    public static String makeJavaCompatible(String s, Machine machine) {
        try {
            if (machine.isLocal()) {
                File f = new File(s);
                return f.getCanonicalPath().replace('\\', '/');
            } else {
                return s.replace('\\', '/');
            }
        } catch (Exception e) {
            Log.info(c, "makeJavaCompatible", "Unable to normalize the path: " + s + ". Error: " + e.getMessage());
            e.printStackTrace();
        }

        return s.replace('\\', '/');
    }

    /*
     * Testing of process status removed, simple hard-coded wait of 5 seconds.
     */
    public static boolean osgiShutdown(Machine machine, int osgiPort) throws Exception {
        String method = "osgiShutdown";
        Log.entering(c, method);

        // let exceptions percolate up the stack as not all will be errors
        executeOsgiCommand(machine, osgiPort, "shutdown", false);

        Thread.sleep(5000);

        Log.exiting(c, method, true);
        return true;
    }

    private static String executeOsgiCommand(Machine machine, int osgiPort,
                                             String cmd, boolean waitForReply) throws Exception {
        String method = "executeOsgiCommand";
        String ret = "";
        SocketChannel channel = null;
        try {
            channel = SocketChannel.open(new InetSocketAddress(machine
                            .getHostname(), osgiPort));

            channel.configureBlocking(false);

            ret = readFrom(channel, "osgi>", 500);
            if (ret.contains("osgi>")) {
                writeCommandTo(channel, cmd);
                if (waitForReply)
                    ret = readFrom(channel, "osgi>", 1000);
                else
                    ret = "";
            }
        } catch (Exception e) {
            //absorb as this may not be an error
        } finally {
            //channel may be null if open failed
            if (channel != null)
                channel.close();
        }

        if (ret.contains("Connection refused")) {
            throw new Exception("Another client is using telnet. This can also be caused by other servers listening on this telnet port ("
                                + osgiPort
                                + "). Please ensure there are no other running instances.");
        }

        ret = ret.replace("osgi>", "");

        Log.exiting(c, method, ret);
        return ret;
    }

    /**
     * This method attempts to read, up to a certain point, for a specified time
     * period. It will return what is read in the return value.
     *
     * @param channel
     *            the socket channel to read from.
     * @param prompt
     *            the prompt to look for.
     * @param timeout
     *            how long to read for.
     * @return what was read.
     * @throws IOException
     */
    private static String readFrom(SocketChannel channel, String prompt, long timeout) throws IOException {
        StringBuilder builder = new StringBuilder();

        long startTime = System.currentTimeMillis();
        int osgiIndex = -1;

        do {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            channel.read(buffer);
            builder.append(new String(buffer.array()));
            osgiIndex = builder.indexOf(prompt);
        } while (osgiIndex == -1
                 && System.currentTimeMillis() - startTime < timeout);

        if (osgiIndex != -1) {
            builder.delete(osgiIndex + 5, builder.length());
        }
        return builder.toString().trim();
    }

    /**
     * We want to send a command to the OSGi console.
     *
     * @param channel
     *            the channel to write to.
     * @param command
     *            the command to send
     * @throws IOException
     */
    private static void writeCommandTo(SocketChannel channel, String command) throws IOException {
        command = command + "\r\n";
        byte[] bytes = command.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        channel.write(buffer);
        buffer.clear();
    }

    public static Machine createMachine(Bootstrap b) throws Exception {
        String hostName = b.getValue("hostName");
        String user = b.getValue(hostName + ".user");
        String password = b.getValue(hostName + ".password");
        String keystore = b.getValue("keystore");

        ConnectionInfo machineDetails = new ConnectionInfo(hostName, user, password);
        if ((password == null || password.length() == 0) && keystore != null && keystore.length() != 0) {
            File keyfile = new File(keystore);
            machineDetails = new ConnectionInfo(hostName, keyfile, user, password);
        }
        return Machine.getMachine(machineDetails);
    }

    public static ProgramOutput execute(Machine machine, String javaHome, String command, String... parms) throws Exception {
        return execute(machine, javaHome, null, command, parms);
    }

    /**
     * Execute a command on the file system. The javaHome parameter can be null if
     * the command being executed does not need a JAVA_HOME env variable to be set.
     *
     * @param machine
     * @param javaHome
     * @param command
     * @param parms
     * @return
     * @throws Exception
     */
    public static ProgramOutput execute(Machine machine, String javaHome, Properties envVars, String command, String... parms) throws Exception {
        final String method = "execute";
        Log.finer(c, method, "Executing: " + command, parms);

        //Need to ensure JAVA_HOME is set correctly - can't rely on user's environment to be set to the same Java as the build/runtime environment
        Properties _envVars = new Properties();
        _envVars.setProperty("JAVA_HOME", javaHome);
        if (envVars != null)
            _envVars.putAll(envVars);
        Log.finer(c, method, "Using additional env props: " + _envVars.toString());

        parms = parms != null ? parms : new String[] {};
        ProgramOutput output = machine.execute(command, parms, _envVars);
        String stdout = output.getStdout();
        int rc = output.getReturnCode();
        // Skip logging if rc=0 (success) or rc=1 (server not running)
        if (rc != 0 && rc != 1) {
            Log.info(c, method, "Server script output: " + stdout);
            Log.info(c, method, "Return code from script is: " + output.getReturnCode());
        }
        return output;
    }

    public static ProgramOutput executeLibertyCmd(Bootstrap bootstrap, String command, String... parms) throws Exception {
        Machine machine = createMachine(bootstrap);
        String hostName = bootstrap.getValue("hostName");
        String javaHome = bootstrap.getValue(hostName + ".JavaHome");
        String cmd = bootstrap.getValue("libertyInstallPath") + "/bin/" + command;
        return execute(machine, javaHome, null, cmd, parms);
    }
}
