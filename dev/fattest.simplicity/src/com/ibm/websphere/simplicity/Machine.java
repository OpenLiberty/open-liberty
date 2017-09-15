/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.provider.commandline.CommandLineProvider;
import com.ibm.websphere.simplicity.provider.commandline.RemoteCommandFactory;
import com.ibm.websphere.simplicity.provider.commandline.local.LocalCommandLineProvider;
import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.LocalMachine;
import componenttest.common.apiservices.cmdline.LocalProvider;

/**
 * This class represents a physical machine. This might be a standalone machine or one that has
 * WebSphere installed. If the Machine is not the local machine, a remote connection may need to be
 * established to be able to perform Machine operations. If an operation is requested and a
 * connection is not established, the connection will be implicitly created and the operation
 * complete. Also, the connection can be managed manually using the {@link #connect()} and {@link #disconnect()} methods of this class.
 */
public class Machine {

    @SuppressWarnings("rawtypes")
    private static Class c = Machine.class;
    private static Map<ConnectionInfo, Machine> machineCache = new HashMap<ConnectionInfo, Machine>();

    private final ConnectionInfo connInfo;
    protected OperatingSystem os;
    private String bootstrapFileKey;
    private String osVersion;
    private String rawOSName;
    private String tempDir;
    private static int[] existingWindowsProcesses;
    protected String workDir;
    protected boolean isLocal = false;

    /**
     * {@link ConnectionInfo} constructor
     * 
     * @param connInfo The {@link ConnectionInfo} contains the information needed to make a
     *            connection to the machine. This includes the hostname, administrative username and
     *            password.
     * 
     * @throws Exception
     */
    protected Machine(ConnectionInfo connInfo) throws Exception {
        this.connInfo = connInfo;
    }

    /**
     * Factory method to get a Machine using a {@link ConnectionInfo} Object. This method
     * should be used by so that there is only one instance of a Machine per physical machine
     * existing at any one time.
     * 
     * @param connInfo The {@link ConnectionInfo} contains the information needed to make a
     *            connection to the machine. This includes the hostname, administrative username and
     *            password.
     * @return A Machine Object corresponding to the {@link ConnectionInfo}
     * @throws Exception
     */
    public static Machine getMachine(String hostname) throws Exception {
        Log.entering(c, "getMachine", hostname);
        ConnectionInfo connInfo = Bootstrap.getInstance().getMachineConnectionData(hostname);
        Machine ret = getMachine(connInfo);
        Log.exiting(c, "getMachine", ret);
        return ret;
    }

    /**
     * Factory method to get a Machine using a {@link ConnectionInfo} Object. This method
     * should be used by so that there is only one instance of a Machine per physical machine
     * existing at any one time.
     * 
     * @param connInfo The {@link ConnectionInfo} contains the information needed to make a
     *            connection to the machine. This includes the hostname, administrative username and
     *            password.
     * @return A Machine Object corresponding to the {@link ConnectionInfo}
     * @throws Exception
     */
    public static Machine getMachine(ConnectionInfo connInfo) throws Exception {
        Log.entering(c, "getMachine", connInfo);
        Machine machine = machineCache.get(connInfo);
        if (machine == null) {
            if (connInfo.getHost().contains("localhost"))
                machine = getLocalMachine();
            else
                machine = new Machine(connInfo);
            machineCache.put(connInfo, machine);
        }
        Log.exiting(c, "getMachine", machine);
        return machine;
    }

    /**
     * Get a Machine instance that represents the local physical machine. This method uses
     * java.net.InetAddress to obtain the local hostname and reads security information from the
     * bootstrapping properties file. If security information cannot be found in the bootstrapping
     * properties file based on the hostname returned by java.net.InetAddress, a second attempt will
     * be made using "localhost" as the hostname. If the second attempt fails, a Machine instance
     * will be returned using the hostname localhost with no security information. Note that without
     * security credentials, remote command line operations may not be possible.
     * 
     * @return A Machine representation for the local physical machine.
     * @throws Exception
     */
    public static Machine getLocalMachine() throws Exception {
        final String method = "getLocalMachine";
        Log.entering(c, method);
        Machine ret = LocalMachine.getInstance();
        Log.exiting(c, method, ret);
        return ret;
    }

    /**
     * Get a Machine instance that represents the local physical machine.
     * 
     * @param connInfo The {@link ConnectionInfo} Object which contains connection information for the local machine
     * @return A Machine representation for the local physical machine.
     * @throws Exception
     */
    public static Machine getLocalMachine(ConnectionInfo connInfo) throws Exception {
        return getMachine(connInfo);
    }

    /**
     * Get the {@link ConnectionInfo} Object that contains the information used to make a connection
     * to this Machine.
     * 
     * @return The {@link ConnectionInfo} for this Machine
     */
    public ConnectionInfo getConnInfo() {
        return this.connInfo;
    }

    /**
     * Get the bootstrapping file key used to cache this Machine. If the caching is not enabled,
     * this returns null
     * 
     * @return The key used to cache the machine in the bootstrapping file
     */
    public String getBootstrapFileKey() {
        return bootstrapFileKey;
    }

    /**
     * Set the boostrapping file key
     * 
     * @param bootstrapFileKey The boostrapping file key
     */
    protected void setBootstrapFileKey(String bootstrapFileKey) {
        this.bootstrapFileKey = bootstrapFileKey;
    }

    /**
     * Get the hostname or ip address of the Machine. The hostname can be used to communicate with
     * the Machine.
     * 
     * @return The hostname of the Machine
     */
    public String getHostname() {
        return this.getConnInfo().getHost();
    }

    /**
     * @return True if the current instance references the local machine.
     */
    public boolean isLocal() {
        return this.isLocal;
    }

    /**
     * Get the administrative username of the Machine. The username is used to establish a remote
     * connection to the machine.
     * 
     * @return The administrative username of the Machine
     */
    public String getUsername() {
        return this.getConnInfo().getUser();
    }

    /**
     * Get the administrative password of the Machine. The password is used to establish a remote
     * connection to the machine.
     * 
     * @return The administrative password of the Machine
     */
    public String getPassword() {
        return this.getConnInfo().getPassword();
    }

    /**
     * Constructs a RemoteFile object that represents the path to the remote file indicated by the
     * input path. Note that the actual file is not guaranteed to exist. The input path may
     * represent either a file or a directory.
     * 
     * @param path The absolute path to a file on the remote device.
     * @return A RemoteFile representing the input abstract path name
     */
    public RemoteFile getFile(String path) {
        return new RemoteFile(this, path);
    }

    /**
     * Constructs a RemoteFile object that represents the path to the remote file indicated by the
     * input path. Note that the actual file is not guaranteed to exist. The input path may
     * represent either a file or a directory.
     * 
     * @param parent The parent directory of the target file
     * @param name The name of the file
     * @return A RemoteFile representing the input abstract path name
     */
    public RemoteFile getFile(RemoteFile parent, String name) {
        return new RemoteFile(this, parent, name);
    }

    /**
     * Get the {@link OperatingSystem} that this Machine is running
     * 
     * @return A representation of the operating system of the remote machine
     */
    public OperatingSystem getOperatingSystem() throws Exception {
        if (this.os == null) {
            this.os = OperatingSystem.getOperatingSystem(LocalProvider.getOSName(this));
        }
        return this.os;
    }

    protected void setOperatingSystem(OperatingSystem os) {
        this.os = os;
    }

    /**
     * Get the version of the OperatingSystem that this Machine is running
     * 
     * @return A String value of the operating system returned by the operating system itself
     * @throws Exception
     */
    public String getOSVersion() throws Exception {
        final String method = "getOSVersion";
        Log.entering(c, method);
        if (this.osVersion == null) {
            String cmd = null;
            String[] params = null;
            if (getOperatingSystem() == OperatingSystem.WINDOWS) {
                cmd = "cmd.exe";
                params = new String[] { "/C", "ver" };
            } else {
                cmd = "/bin/sh";
                params = new String[] { "-c", "\"cat", "/proc/version\"" };
            }
            Log.finer(c, method, "Command to get OS version: " + cmd);
            this.osVersion = LocalProvider.executeCommand(this, cmd, params, null, null).getStdout().trim();
        }
        Log.exiting(c, method, this.osVersion);
        return this.osVersion;
    }

    /**
     * Set the OS version for this Machine
     * 
     * @param version The OS version to set
     */
    protected void setOSVersion(String version) {
        this.osVersion = version;
    }

    /**
     * Get a String representation of the raw name of operating system that this Machine is running
     * (as opposed to the {@link OperatingSystem} enum).
     * 
     * @return A String representation of the raw name of the operating system
     * @throws Exception
     */
    public String getRawOSName() throws Exception {
        if (this.rawOSName == null) {
            this.rawOSName = LocalProvider.getOSName(this);
        }
        return this.rawOSName;
    }

    /**
     * Set the raw OS name for this Machine
     * 
     * @param name The raw OS name to set
     */
    protected void setRawOSName(String name) {
        this.rawOSName = name;
    }

    /**
     * Execute the specified command line command.
     * If you have parameters you should use the version of execute where you
     * give the parameters in an Array.
     * 
     * @param cmd The command to execute.
     * @return The result of the command
     * @throws ExecutionException
     */
    public ProgramOutput execute(String cmd) throws Exception {
        return this.execute(cmd, null, workDir, null);
    }

    /**
     * Execute the specified command line command.
     * If you have parameters you should use the version of execute where you
     * give the parameters in an Array.
     * 
     * @param cmd The command to execute.
     * @param timeout Execute the command with timeout
     * @param workDir The directory to execute the command from
     * @return The result of the command
     * @throws ExecutionException
     */
    public ProgramOutput execute(String cmd, int timeout) throws Exception {
        return this.execute(cmd, null, workDir, null, timeout);
    }

    /**
     * Execute the specified command line command.
     * If you have parameters you should use the version of execute where you
     * give the parameters in an Array.
     * 
     * @param cmd The command to execute.
     * @param workDir The directory to execute the command from
     * @return The result of the command
     * @throws ExecutionException
     */
    public ProgramOutput execute(String cmd, String workDir) throws Exception {
        return this.execute(cmd, null, workDir, null);
    }

    /**
     * Execute the specified command line command.
     * If you have parameters you should use the version of execute where you
     * give the parameters in an Array.
     * 
     * @param cmd The command to execute.
     * @param workDir The directory to execute the command from
     * @param envVars Environment variables to modify the default environment
     * @return The result of the command
     * @throws ExecutionException
     */
    public ProgramOutput execute(String cmd, String workDir, Properties envVars) throws Exception {
        return this.execute(cmd, null, workDir, envVars);
    }

    /**
     * Execute the specified command line command.
     * 
     * @param cmd The command to execute.
     * @param parameters The parameters to pass to the command. Spaces are allowed within arguments
     *            and path names. DO NOT use quotation marks around paths, they will be inserted
     *            automatically when needed.
     * @return The result of the command
     * @throws ExecutionException
     */
    public ProgramOutput execute(String cmd, String[] parameters) throws Exception {
        return this.execute(cmd, parameters, workDir, null);
    }

    /**
     * Execute the specified command line command.
     * 
     * @param cmd The command to execute.
     * @param parameters The parameters to pass to the command. Spaces are allowed within arguments
     *            and path names. DO NOT use quotation marks around paths, they will be inserted
     *            automatically when needed.
     * @param envVars Environment variables to modify the default environment
     * @return The result of the command
     * @throws ExecutionException
     */
    public ProgramOutput execute(String cmd, String[] parameters, Properties envVars) throws Exception {
        return this.execute(cmd, parameters, workDir, envVars);
    }

    /**
     * Execute the specified command line command.
     * 
     * @param cmd The command to execute.
     * @param parameters The parameters to pass to the command. Spaces are allowed within arguments
     *            and path names. DO NOT use quotation marks around paths, they will be inserted
     *            automatically when needed.
     * @param workDir The directory to execute the command from
     * @return The result of the command
     * @throws ExecutionException
     */
    public ProgramOutput execute(String cmd, String[] parameters, String workDir) throws Exception {
        return this.execute(cmd, parameters, workDir, null);
    }

    /**
     * Execute the specified command line command
     * 
     * @param cmd The command to execute
     * @param parameters The parameters to pass to the command. Spaces are allowed within arguments
     *            and path names. DO NOT use quotation marks around paths, they will be inserted
     *            automatically when needed.
     * @param workDir The directory to execute the command from
     * @param envVars A Properties Object which contain name/value pairs for environment variables
     *            needed to execute the command
     * @return The result of the command
     * @throws Exception
     */
    public ProgramOutput execute(String cmd, String[] parameters, String workDir, Properties envVars) throws Exception {
        return this.execute(cmd, parameters, workDir, envVars, 0);
    }

    /**
     * Execute the specified command line command
     * 
     * @param cmd The command to execute
     * @param parameters The parameters to pass to the command. Spaces are allowed within arguments
     *            and path names. DO NOT use quotation marks around paths, they will be inserted
     *            automatically when needed.
     * @param workDir The directory to execute the command from
     * @param envVars A Properties Object which contain name/value pairs for environment variables
     *            needed to execute the command
     * @param timeout Execute the command with timeout
     * @return The result of the command
     * @throws Exception
     */
    public ProgramOutput execute(String cmd, String[] parameters, String workDir, Properties envVars, int timeout) throws Exception {
        // On iSeries, we should be adding the qsh -c flag to the start of any command.
        // This means commands are executed in a native-like shell, rather than a
        // PASE environment.
        if (OperatingSystem.ISERIES.compareTo(getOperatingSystem()) == 0) {
            cmd = "qsh -c " + cmd;
        }
        return LocalProvider.executeCommand(this, cmd, parameters, workDir, envVars);
    }

    /**
     * Asynchronously execute the specified command line command.
     * If you have parameters you should use the version of execute where you
     * give the parameters in an Array.
     * 
     * @param cmd The command to execute.
     * @return The result of the command
     * @throws ExecutionException
     */
    public void executeAsync(String cmd) throws Exception {
        this.executeAsync(cmd, new String[0]);
    }

    /**
     * Asynchronously execute the specified command line command.
     * 
     * 
     * @param cmd The command to execute.
     * @param parameters The parameters to pass to the command. Spaces are allowed within arguments
     *            and path names. DO NOT use quotation marks around paths, they will be inserted
     *            automatically when needed.
     * @return The result of the command
     * @throws ExecutionException
     */
    public AsyncProgramOutput executeAsync(String cmd, String[] parameters) throws Exception {
        return LocalProvider.executeCommandAsync(this, cmd, parameters, workDir, null);
    }

    /**
     * Execute a command line command asynchronously. This method creates a {@link RemoteCommand} Object and starts the command execution in a separate thread. The
     * {@link RemoteCommand} is
     * returned for querying.
     * 
     * @param cmd The command to execute
     * @param parameters The parameters to pass to the command. Spaces are allowed within arguments
     *            and path names. DO NOT use quotation marks around paths, they will be inserted
     *            automatically when needed.
     * @param workDir The directory to execute the command from
     * @param envVars A Properties Object which contain name/value pairs for environment variables
     *            needed to execute the command
     * @return The {@link RemoteCommand} Object
     * @throws Exception
     */
    public RemoteCommand executeAsync(String cmd, String[] parameters, String workDir, Properties envVars) throws Exception {
        RemoteCommand remoteCommand = RemoteCommandFactory.getInstance(cmd, parameters, workDir, envVars, this);
        remoteCommand.setDaemon(true);
        remoteCommand.start();
        return remoteCommand;
    }

    /**
     * Get the temporary directory for the Machine defined by the Machines operating system.
     * 
     * @return A {@link RemoteFile} representation of the temporary directory
     * @throws Exception
     */
    public RemoteFile getTempDir() throws Exception {
        if (this.tempDir == null) {
            this.tempDir = LocalProvider.getTempDir();
        }
        return new RemoteFile(this, this.tempDir);
    }

    protected void setTempDir(String dir) {
        this.tempDir = dir;
    }

    /**
     * Establish a remote connection if needed by the {@link CommandLineProvider} implementation.
     * 
     * @throws Exception
     */
    public void connect() throws Exception {
        //LocalProvider.connect(this.getConnInfo());
    }

    /**
     * Close a remote connection to this Machine if there is one established
     * 
     * @throws Exception
     */
    public void disconnect() throws Exception {
        //LocalProvider.disconnect(this.getConnInfo());
    }

    /**
     * Return true if a remote connection is currently established to the machine. The return value
     * is specific to the {@link CommandLineProvider} implementation. For example, the {@link LocalCommandLineProvider} will always return true since no connection is needed to
     * interact with the local machine.
     * 
     * @return true if a connection to the machine is currently established
     * @throws Exception
     */
    public boolean isConnected() throws Exception {
        return LocalProvider.isConnected();
    }

    /**
     * Kill the process specified by the process id. The process is not killed "gracefully."
     * It is immediately stopped.
     * 
     * @param processId The id of the process to kill
     */
    public void killProcess(int processId) throws Exception {
        killProcessRemote(processId, 0);
    }

    private void killProcessRemote(int processID, int attemptNumber) throws Exception {
        ProgramOutput po = LocalProvider.killProcess(this, processID);
        Thread.sleep(1000);
        if (processStillRunning(processID)) {
            if (attemptNumber >= 3) {//so has tried 3 times
                String ErrorMessage = "The processID " + processID + " could not be killed.  The command ran was " + po.getCommand() + " The Std Output is: \'"
                                      + po.getStdout() + "\' and the Std Error was \'" + po.getStderr() + "\'";
                throw new Exception(ErrorMessage);
            } else {
                Log.warning(c, "Unable to kill process " + processID + " - Retrying, this is attempt number " + attemptNumber);
                killProcessRemote(processID, ++attemptNumber);
            }
        }
    }

    /**
     * Get a java.util.Date representation of the current time on the Machine
     * 
     * @return A Date repesentation of the current time on the Machine
     * @throws Exception
     */
    public Date getDate() throws Exception {
        return LocalProvider.getDate(this);
    }

    /**
     * Two Machines are equal if the hostname and credentials they are initialized with are the
     * same.
     */
    @Override
    public boolean equals(Object o) {
        final String method = "equals";
        Log.entering(c, method, o);
        if (o == null) {
            Log.finer(c, method, "Input was null");
            Log.exiting(c, method, false);
            return false;
        }
        if (o == this) {
            Log.finer(c, method, "Same instance as this");
            return true;
        }
        if (!(o instanceof Machine)) {
            Log.finer(c, method, "Input was not a Machine");
            return false;
        }

        Machine other = (Machine) o;
        boolean equals = this.getHostname().equalsIgnoreCase(other.getHostname()) &&
                         this.getConnInfo().equals(other.getConnInfo());

        Log.exiting(c, method, equals);
        return equals;
    }

    public int[] getJavaProcesses(String installPath, String serverName) throws Exception {
        String command;
        String[] parameters;
        if (os == OperatingSystem.WINDOWS) {
            command = "tasklist";
            parameters = new String[] { "/NH", "/FI", "\"IMAGENAME eq java.exe\"" };
            if (serverName != null)
                Log.warning(c, "Cannot distinguish server only processes running on Windows");
        } else {
            command = "ps";

            if (serverName == null) {
                parameters = new String[] { "-ef", "|", "grep", installPath };
            } else {
                parameters = new String[] { "-ef", "|", "grep", installPath, "|", "grep", serverName };
            }

        }
        ProgramOutput result = this.execute(command, parameters);

        String ps = result.getStdout();
        String[] split = ps.split("\n");
        ArrayList<String> splitted = new ArrayList<String>();
        for (String s : split)
            splitted.add(s);
        if (os == OperatingSystem.WINDOWS)
            splitted.remove(0); //to get rid of the first blank line

        // Assume one of our entries is our command
        // Split always returns at least one entry so the subtraction is safe
        List<Integer> processes = new ArrayList<Integer>();
        for (String s : splitted) {
            // Don't count our break statement

            if (!s.contains("grep")) {
                String[] columns = s.trim().split("\\s+");
                if (columns.length >= 2) {
                    // The pid will be the second column
                    int pid = Integer.parseInt(columns[1]);
                    if (!!!isExcludedProcess(pid))
                        processes.add(pid);
                }
            }
        }

        // Convert the list to an array
        int[] pss = new int[processes.size()];
        int i = 0;
        for (int p : processes) {
            pss[i] = p;
            i++;
        }
        return pss;
    }

    private boolean isExcludedProcess(int pid) {
        if (existingWindowsProcesses != null) {
            for (int i : existingWindowsProcesses) {
                if (i == pid)
                    return true;
            }
            return false;
        } else {
            //If the existingWindowsProcesses is null it means it is setting up
            //the list so we don't want any exceptions...
            return false;
        }

    }

    /**
     * Returns whether the given processID is running on the system or not
     * 
     * @param processID The process ID to check for
     * @return true - it is running
     *         false - it is not running
     * @throws Exception
     */
    public boolean processStillRunning(int processID) throws Exception {
        String command;
        String[] parameters;
        if (os == OperatingSystem.WINDOWS) {
            command = "tasklist";
            parameters = new String[] { "/NH", "/FI", "\"PID eq " + processID + "\"" };
            ProgramOutput result = this.execute(command, parameters);
            String ps = result.getStdout();
            String[] split = ps.split("\n");
            ArrayList<String> splitted = new ArrayList<String>();
            for (String s : split)
                splitted.add(s);
            splitted.remove(0); //to get rid of the first blank line + INFO (if no process)
            if (splitted.isEmpty())
                return false;
            else
                return true; //as we have done the filtering above if there is a result left after removing blank line the PID is still there
        } else {
            command = "ps";
            parameters = new String[] { "-e" };
            ProgramOutput result = this.execute(command, parameters);
            String ps = result.getStdout();
            String[] split = ps.split("\n");
            for (int i = 1; i < split.length; i++) //We do start at 1 as the first line (0) is headers
            {
                String s = split[i];
                s = s.trim();
                String[] columns = s.split("\\s+");
                if (columns.length >= 2) {
                    // The pid will be the first column
                    int pid = Integer.parseInt(columns[0]);
                    if (pid == processID)
                        return true;
                }
            }
            //if we dont find the process its not running so return false
            return false;
        }
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }
}
