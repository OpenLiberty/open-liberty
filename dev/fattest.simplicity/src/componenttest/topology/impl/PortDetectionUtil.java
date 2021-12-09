/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

/**
 *
 */
public abstract class PortDetectionUtil {
    private final static Class<?> c = PortDetectionUtil.class;
    private final static String LS = System.getProperty("line.separator");

    private static class NoopDetector extends PortDetectionUtil {

        /*
         * (non-Javadoc)
         *
         * @see componenttest.topology.impl.PortDetectionUtil#determineOwnerOfPort(int)
         */
        @Override
        public String determineOwnerOfPort(int port) throws UnsupportedOperationException {
            return "";
        }

        @Override
        public String determinePidForPort(int port) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String determineCommandLineForPid(String pid) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String listProcesses() throws Exception {
            // TODO Auto-generated method stub
            return null;
        }
    }

    private static class LinuxDetector extends PortDetectionUtil {
        private final static Class<?> c = LinuxDetector.class;
        private final Machine machine;
        private final String NETSTAT_CMD = "netstat";
        private final String[] NETSTAT_PARMS = new String[] { "-tulpn" };
        private final String PS_CMD = "ps";
        private final String[] PS_PARMS = new String[] { "-fp" };

        private LinuxDetector(Machine machine) {
            this.machine = machine;
        }

        /*
         * (non-Javadoc)
         *
         * @see componenttest.topology.impl.PortDetectionUtil#determineOwnerOfPort(int)
         */
        @Override
        public String determineOwnerOfPort(final int port) throws IOException {
            final String m = "determineOwnerOfPort";
            String pidInfo = "";
            try {
                ProgramOutput po = machine.execute(NETSTAT_CMD, NETSTAT_PARMS);
                String cmdOutput = po.getStdout();
                //Output should resemble:
//                Active Internet connections (only servers)
//                Proto Recv-Q Send-Q Local Address               Foreign Address             State       PID/Program name
//                tcp        0      0 0.0.0.0:139                 0.0.0.0:*                   LISTEN      -
//                tcp        0      0 0.0.0.0:111                 0.0.0.0:*                   LISTEN      -
//                tcp        0      0 127.0.0.1:8979              0.0.0.0:*                   LISTEN      -
//                tcp        0      0 0.0.0.0:48500               0.0.0.0:*                   LISTEN      -
//                tcp        0      0 192.168.122.1:53            0.0.0.0:*                   LISTEN      -
//                tcp        0      0 ::ffff:127.0.0.1:65341      :::*                        LISTEN      5124/sametime
//                tcp        0      0 :::445                      :::*                        LISTEN      -
//                tcp        0      0 ::ffff:127.0.0.1:38848      :::*                        LISTEN      4823/java
//                tcp        0      0 ::ffff:127.0.0.1:57248      :::*                        LISTEN      3944/symphony
                StringTokenizer st = new StringTokenizer(cmdOutput, "/-" + LS);
                int pid = -1;
                while (st.hasMoreTokens()) {
                    String s = st.nextToken().trim();
                    if (s.startsWith("tcp") && s.contains(":" + port + " ") && s.contains("LISTEN")) {
                        String pidString = s.substring(s.lastIndexOf(' ')).trim();
                        pid = Integer.parseInt(pidString);
                        break;
                    }
                }

                if (pid < 0) {
                    // we did not find the port in the netstat cmd output
                    String msg = "Could not find port (" + port + ") listed in netstat output";
                    Log.info(c, m, msg + ":" + LS + cmdOutput);
                    throw new Exception(msg);
                }

                String[] parms = Arrays.copyOf(PS_PARMS, PS_PARMS.length + 1);
                parms[PS_PARMS.length] = "" + pid;
                po = machine.execute(PS_CMD, parms);
                pidInfo = po.getStdout();

                if ("".equals(pidInfo)) {
                    // we didn't find the pid in the list of processes...
                    // possibly whatever process was listening on that port has exited...
                    String msg = "Could not find PID, " + pid + " in PID list - possibly that process already exited";
                    Log.info(c, m, msg + ":" + cmdOutput);
                    throw new Exception(msg);
                }
            } catch (Exception ex) {
                throw new IOException("Failed to determine owner of port " + port, ex);
            }
            return pidInfo;
        }

        @Override
        public String determinePidForPort(int port) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String determineCommandLineForPid(String pid) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String listProcesses() throws Exception {
            // TODO Auto-generated method stub
            return null;
        }
    }

    private static class MacDetector extends PortDetectionUtil {
        private final static Class<?> c = MacDetector.class;
        private final Machine machine;
        private final String LSOF_CMD = "lsof";
        private final String[] LSOF_PARMS = new String[] { "-F", "p", "-i" };
        private final String PS_CMD = "ps";
        private final String[] PS_PARMS = new String[] { "-fp" };

        private MacDetector(Machine machine) {
            this.machine = machine;
        }

        /*
         * (non-Javadoc)
         *
         * @see componenttest.topology.impl.PortDetectionUtil#determineOwnerOfPort(int)
         */
        @Override
        public String determineOwnerOfPort(final int port) throws IOException {
            final String m = "determineOwnerOfPort";
            String pidInfo = "";
            try {
                String[] parms = Arrays.copyOf(LSOF_PARMS, LSOF_PARMS.length + 1);
                parms[LSOF_PARMS.length] = "tcp:" + port;
                ProgramOutput po = machine.execute(LSOF_CMD, parms);
                String cmdOutput = po.getStdout();
                if (cmdOutput == null) {
                    cmdOutput = "-1";
                } else {
                    // on Mac, the lsof command with the -F parm will generate output like this:
                    //    lsof -F p -i tcp:49961
                    //    p3069
                    // Note the "p" in front of the PID number, the code below handles that
                    cmdOutput = cmdOutput.trim();
                    if (cmdOutput.startsWith("p")) {
                        cmdOutput = cmdOutput.substring(1);
                    }
                    int newLineIdx = cmdOutput.indexOf("\n");
                    if (newLineIdx > 0) {
                        // in some cases, the output will contain a second line like so:
                        //
                        // $ lsof -F p -i tcp:8010
                        // p65655
                        // f53
                        //
                        // this second line needs to be removed
                        cmdOutput = cmdOutput.substring(0, newLineIdx).trim();
                    }
                }
                // Output should contain only the PID
                int pid = Integer.parseInt(cmdOutput);

                if (pid < 0) {
                    // we did not find the port in the lsof cmd output
                    String msg = "lsof did not show any processes bound to tcp port: " + port;
                    Log.info(c, m, msg + ":" + LS + cmdOutput);
                    throw new Exception(msg);
                }

                parms = Arrays.copyOf(PS_PARMS, PS_PARMS.length + 1);
                parms[PS_PARMS.length] = "" + pid;
                po = machine.execute(PS_CMD, parms);
                pidInfo = po.getStdout();

                if ("".equals(pidInfo)) {
                    // we didn't find the pid in the list of processes...
                    // possibly whatever process was listening on that port has exited...
                    String msg = "Could not find PID, " + pid + " in PID list - possibly that process already exited";
                    Log.info(c, m, msg + ":" + cmdOutput);
                    throw new Exception(msg);
                }
            } catch (Exception ex) {
                throw new IOException("Failed to determine owner of port " + port, ex);
            }
            return pidInfo;
        }

        @Override
        public String determinePidForPort(int port) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String determineCommandLineForPid(String pid) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String listProcesses() throws Exception {
            // TODO Auto-generated method stub
            return null;
        }
    }

    //TODO: once we have more data on the output of these commands on AIX, Solaris, etc.
    //      we will need to create new detectors for each one (or re-use existing) so
    //      that we can better tailor the output.
    private static class GenericNixDetector extends PortDetectionUtil {
        private final Machine machine;
        private final String NETSTAT_CMD = "netstat";
        private final String[] NETSTAT_PARMS = new String[] { "-tulpn" };
        private final String PS_CMD = "ps";
        private final String[] PS_PARMS = new String[] { "-ef" };

        private GenericNixDetector(Machine machine) {
            this.machine = machine;
        }

        /*
         * (non-Javadoc)
         *
         * @see componenttest.topology.impl.PortDetectionUtil#determineOwnerOfPort(int)
         */
        @Override
        public String determineOwnerOfPort(int port) throws UnsupportedOperationException, IOException {
            String pidInfo = LS;
            try {
                ProgramOutput po = machine.execute(NETSTAT_CMD, NETSTAT_PARMS);
                String cmdOutput = po.getStdout();
                //Output type may depend on OS - so just print it all for now
                pidInfo += cmdOutput;

                // Next get a list of all running processes on the box
                cmdOutput = listProcesses();
                pidInfo += LS + LS + LS + cmdOutput;

            } catch (Exception ex) {
                throw new IOException("Failed to determine owner of port " + port, ex);
            }
            return pidInfo;
        }

        @Override
        public String determinePidForPort(int port) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String determineCommandLineForPid(String pid) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String listProcesses() throws Exception {
            return machine.execute(PS_CMD, PS_PARMS).getStdout();
        }
    }

    private static class ZOSDetector extends PortDetectionUtil {
        private final static Class<?> c = ZOSDetector.class;
        private final Machine machine;
        private final String NETSTAT_CMD = "netstat";
        private final String[] NETSTAT_PARMS = new String[] { "-P" };
        private final String PS_CMD = "ps";
        private final String[] PS_PARMS = new String[] { "-ef -o jobname,pid,xasid" };

        private ZOSDetector(Machine machine) {
            this.machine = machine;
        }

        /*
         * (non-Javadoc)
         *
         * @see componenttest.topology.impl.PortDetectionUtil#determineOwnerOfPort(int)
         */
        @Override
        public String determineOwnerOfPort(final int port) throws IOException {
            final String m = "determineOwnerOfPort";
            String pidInfo = "";
            String[] parms = Arrays.copyOf(NETSTAT_PARMS, NETSTAT_PARMS.length + 1);
            parms[NETSTAT_PARMS.length] = Integer.toString(port);
            try {
                ProgramOutput po = machine.execute(NETSTAT_CMD, parms);
                String cmdOutput = po.getStdout();
                //Output should resemble:
//                Active Internet connections (only servers)
//                MVS TCP/IP NETSTAT CS V1R10       TCPIP Name: TCPIP           13:47:39
//                User Id  Conn     State
//                -------  ----     -----
//                BBON001  000014F6 Establsh
//                  Local Socket:   ::ffff:9.57.165.181..9355
//                  Foreign Socket: ::ffff:9.57.165.181..4681
//                BBON001  000012D9 Listen
//                  Local Socket:   ::..9355
//                  Foreign Socket: ::..0
//                BBOS001  000014F5 Establsh
//                  Local Socket:   ::ffff:9.57.165.181..4681
//                  Foreign Socket: ::ffff:9.57.165.181..9355
                if (!!!cmdOutput.contains("Listen")) {
                    // we did not find the port in the netstat cmd output
                    String msg = "Could not find port (" + port + ") listed in netstat output";
                    Log.info(c, m, msg + ":" + LS + cmdOutput);
                    throw new Exception(msg);
                }

                // Just print it all for now
                pidInfo += cmdOutput;

                // Next get a list of all running processes on the box
//              BBON001    16842943
//              BBON001    50397376
//              BBOS001    50397397
//              BBOS001    16842966
                cmdOutput = listProcesses();
                pidInfo += LS + LS + LS + cmdOutput;
            } catch (Exception ex) {
                throw new IOException("Failed to determine owner of port " + port, ex);
            }
            return pidInfo;
        }

        @Override
        public String determinePidForPort(int port) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String determineCommandLineForPid(String pid) throws Exception {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String listProcesses() throws Exception {
            return machine.execute(PS_CMD, PS_PARMS).getStdout();
        }
    }

    private static class WindowsDetector extends PortDetectionUtil {
        private final static Class<?> c = WindowsDetector.class;
        private final Machine machine;
        private final String NETSTAT_CMD = "netstat";
        private final String[] NETSTAT_PARMS = new String[] { "-ano" };
        private final String PS_CMD = "wmic";
        private final String[] PS_PARMS = new String[] { "process", "get", "processid,commandline" };

        private WindowsDetector(Machine machine) {
            this.machine = machine;
        }

        @Override
        public String determineOwnerOfPort(final int port) throws Exception {

            String commandLine = determineCommandLineForPid(determinePidForPort(port));

            if (commandLine == null || commandLine.isEmpty()) {
                return "Couln't determine process holding port " + port;
            } else {
                return commandLine;
            }
        }

        @Override
        public String determinePidForPort(final int port) throws Exception {
            final String m = "determinePidForPort";
            ProgramOutput po = machine.execute(NETSTAT_CMD, NETSTAT_PARMS);
            String cmdOutput = po.getStdout();
            StringTokenizer st = new StringTokenizer(cmdOutput, "/-" + LS);
            String pattern = "^tcp.*:" + port + "\\s.*listen.*";
            while (st.hasMoreTokens()) {
                String s = st.nextToken().trim().toLowerCase();
                Log.finer(c, m, s);
                if (s.matches(pattern)) {
                    return s.substring(s.lastIndexOf(' ')).trim();
                }
            }

            return null;
        }

        @Override
        public String determineCommandLineForPid(String pid) throws Exception {
            final String m = "determineCommandLineForPid";

            if (pid != null && !pid.isEmpty()) {
                String cmdOutput = listProcesses();
                StringTokenizer st = new StringTokenizer(cmdOutput, LS);
                String pattern = ".*\\s" + pid + "\\s*$";
                Log.finer(c, m, "Looking for " + pattern);
                while (st.hasMoreTokens()) {
                    String s = st.nextToken().trim();
                    Log.finer(c, m, s);
                    if (s.matches(pattern)) {
                        return s.substring(0, s.lastIndexOf(pid)).trim();
                    }
                }
            }

            return null;
        }

        @Override
        public String listProcesses() throws Exception {
            return machine.execute(PS_CMD, PS_PARMS).getStdout();
        }
    }

    public static PortDetectionUtil getPortDetector(Machine machine) {
        try {
            switch (machine.getOperatingSystem()) {
                case WINDOWS:
                    return new WindowsDetector(machine);
                case LINUX:
                    return new LinuxDetector(machine);
                case MAC:
                    return new MacDetector(machine);
                case AIX:
                case HP:
                case SOLARIS:
                    return new GenericNixDetector(machine);
                case ZOS:
                    return new ZOSDetector(machine);
                default: // ISeries
                    return new NoopDetector();
            }
        } catch (Exception e) {
            // caught exception in getOperatingSystem().... just ignore and return the NoopDetector
            Log.info(c, "getPortDetector", "Caught exception while trying to determing host operating system: " + machine);
            return new NoopDetector();
        }
    }

    /**
     * Determine the process who is listening on the specified port.
     *
     * @param  port        - the port to check - valid entries are 1-64535
     * @return             a string that should include OS-specific info about the process
     *                     that is listening on the specified port - or an empty string if
     *                     no data could be collected.
     * @throws IOException - if a failure occurs while trying to detect the process
     */
    public abstract String determineOwnerOfPort(int port) throws Exception;

    public abstract String determinePidForPort(int port) throws Exception;

    public abstract String determineCommandLineForPid(String pid) throws Exception;

    public abstract String listProcesses() throws Exception;
}
