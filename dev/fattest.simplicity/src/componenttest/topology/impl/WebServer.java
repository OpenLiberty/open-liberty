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
package componenttest.topology.impl;

import com.ibm.websphere.simplicity.Cell;
import com.ibm.websphere.simplicity.ConnectorType;
import com.ibm.websphere.simplicity.Dmgr;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.Server;
import com.ibm.websphere.simplicity.ServerType;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.runtime.ProcessStatus;
import com.ibm.websphere.simplicity.server.WebServerCreationOptions;

/**
 * This class represents Server of type WebServer in liberty
 */
public class WebServer extends Server implements LogMonitorClient {

    private static final Class<?> c = WebServer.class;
    private String pluginInstallRoot;
    private String installRoot;
    private int portNumber;
    private String userName;
    private String password;
    private String hostName;
    private String tempDir;
    private WebServerCreationOptions webServerProperties;
    private ProcessStatus serverStatus = ProcessStatus.STOPPED;
    private boolean pluginSetUpSuccessfull;
    private Machine machine;
    private String pathSeparator = "/"; // This value is reset once the machine is specified - it is set according to the host type.
    private final LogMonitor logMonitor;
    private final String LOG_FILE = "http_plugin.log";
    private RemoteFile pluginLogFile = null;

    /**
     * @param options
     * @throws Exception
     */
    public WebServer(String name, WebServerCreationOptions webServerProperties) throws Exception {
        super(null, null, null, ServerType.WEB_SERVER);
        this.logMonitor = new LogMonitor(this);
        this.webServerProperties = webServerProperties;
        setName(name);
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the tempDir
     */
    public String getTempDir() {
        return tempDir;
    }

    /**
     * @param tempDir the tempDir to set
     */
    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    /**
     * @return the portNumber
     */
    public int getPortNumber() {
        return this.portNumber;
    }

    /**
     * @param portNumber the portNumber to set
     */
    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
        this.webServerProperties.getServerConfig().setWebPort(portNumber);
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @param hostName the hostName to set
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * @param serverStatus the serverStatus to set
     */
    public void setWebServerStatus(ProcessStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    /**
     * @param serverStatus the serverStatus to set
     */

    public ProcessStatus getWebServerStatus() {
        return this.serverStatus;
    }

    /**
     * @return the webServerProperties
     */
    public WebServerCreationOptions getWebServerProperties() {
        return webServerProperties;
    }

    /**
     * @param webServerProperties the webServerProperties to set
     */
    public void setWebServerProperties(WebServerCreationOptions webServerProperties) {
        this.webServerProperties = webServerProperties;
    }

    /**
     * @return the installRoot
     */
    public String getInstallRoot() {
        return installRoot;
    }

    /**
     * @param installRoot the installRoot to set
     */
    public void setInstallRoot(String installRoot) {
        this.installRoot = installRoot;
        this.webServerProperties.getServerConfig().setWebInstallRoot(installRoot);
    }

    /**
     * Returns the {@link Machine} that this server is installed on.
     *
     * @return the machine
     */
    public Machine getMachine() {
        return machine;

    }

    /**
     * Sets the {@link Machine} that this server is installed on.
     *
     * @param machine the machine to set
     */
    public void setMachine(Machine machine) {
        this.machine = machine;

        try {
            if (machine != null && OperatingSystem.WINDOWS.equals(machine.getOperatingSystem())) {
                pathSeparator = "\\";
            }
        } catch (Exception e) {
            Log.error(c, "setMachine", e);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.simplicity.Server#start()
     */
    @Override
    public void start() throws Exception {
//        this.instance.startServer();  wont work in web server
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.simplicity.Server#start(int)
     */
    @Override
    public void start(int mbeanWaitDuration) throws Exception {
//        this.instance.startServer(); will not work in web server
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.simplicity.Server#stop()
     */
    @Override
    public void stop() throws Exception {
//        this.instance.stopServer();

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.simplicity.Server#stop(long)
     */
    @Override
    public void stop(long timeout) throws Exception {
//        this.instance.stopServer();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.simplicity.Server#getServerStatus()
     */
    @Override
    public ProcessStatus getServerStatus() throws Exception {
        return this.serverStatus;
    }

    /**
     * Get the {@link ServerType} of this Server. The server type determines the capibilities of the
     * Server. For example, a {@link Dmgr} server manages the nodes within a {@link Cell}.
     *
     * @return The type of this Server
     */
    @Override
    public ServerType getServerType() {
        return ServerType.WEB_SERVER;
    }

    /**
     * Get the port number value for a {@link ConnectorType} for this Server
     *
     * @param connectorType The {@link ConnectorType} port value to get
     * @return An Integer representation of the port value
     * @throws Exception
     */
    @Override
    public Integer getPortNumber(ConnectorType connectorType) throws Exception {
        final String method = "getPortNumber";
        Log.entering(c, method, connectorType);
        Log.finer(c, method, "server is " + this.getNodeName() + ", " + this.getName());
        //PortType port = PortType.valueOf(connectorType.getEndpointName());
        Integer portNumber = getPortNumber(PortType.OSGi); // there is no RMI, SOAP, IPC, etc port in liberty (yet?), so assume OSGi
        Log.exiting(c, method, portNumber);
        return portNumber;
    }

    /**
     * Get a port value for a port for this Server
     *
     * @param port The port to get
     * @return An Integer representation of the port value
     * @throws Exception
     */
    @Override
    public Integer getPortNumber(PortType port) throws Exception {
        return this.portNumber;
    }

    /**
     * @return
     */
    public String getPluginInstallRoot() {
        return this.pluginInstallRoot;
    }

    public void setPluginInstallRoot(String pluginInstallRoot) {
        this.pluginInstallRoot = pluginInstallRoot;;
        this.webServerProperties.getServerConfig().setPluginInstallRoot(pluginInstallRoot);
    }

    /**
     * @param setUpSuccessfull
     */
    public void setPluginSetupSuccessfull(boolean pluginSetUpSuccessfull) {
        this.pluginSetUpSuccessfull = pluginSetUpSuccessfull;

    }

    /**
     * @param setUpSuccessfull
     */
    public boolean getPluginSetupSuccessfull() {
        return this.pluginSetUpSuccessfull;

    }

    /** Returns the dummy node name for this webserver, as Liberty has no concept of nodes. */
    @Override
    public String getNodeName() {
        return this.getName() + "Node";
    }

    public String getPathSeparator() {
        return pathSeparator;
    }

    public boolean isStarted() {
        return (ProcessStatus.RUNNING.equals(getWebServerStatus()));
    }

    //*******************************************************************************************************
    //*** Start of log monitoring methods
    //*******************************************************************************************************

    /**
     * Reset the mark and offset values for logs back to the start of the file.
     * <p>
     * Note: This method doesn't set the offset values to the beginning of the file per se,
     * rather this method sets the list of logs and their offset values to null. When one
     * of the findStringsInLogsAndTrace...(...) methods are called, it will recreate the
     * list of logs and set each offset value to 0L - the start of the file.
     */
    public void resetLogMarks() {
        logMonitor.resetLogMarks();
    }

    /**
     * Set the mark offset to the end of the log file.
     *
     * @param log files to mark. If none are specified, the default log file is marked.
     */
    public void setMarkToEndOfLog(RemoteFile... logFiles) throws Exception {
        logMonitor.setMarkToEndOfLog(logFiles);
    }

    /**
     * Wait for the specified regex in the default logs from the last mark.
     * <p>
     * This method will time out after a sensible period of
     * time has elapsed.
     * <p>The best practice for this method is as follows:
     * <tt><p>
     * // Set the mark to the current end of log<br/>
     * server.setMarkToEndOfLog();<br/>
     * // Do something, e.g. config change<br/>
     * server.setServerConfigurationFile("newServer.xml");<br/>
     * // Wait for message that was a result of the config change<br/>
     * server.waitForStringInLogUsingMark("CWWKZ0009I");<br/>
     * </p></tt></p>
     *
     * @param regexp a regular expression to search for
     * @return the matching line in the log, or null if no matches
     *         appear before the timeout expires
     */
    public String waitForStringInLogUsingMark(String regexp) {
        return logMonitor.waitForStringInLogUsingMark(regexp);
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.topology.impl.LogMonitorClient#lmcGetDefaultLogFile()
     */
    @Override
    public RemoteFile lmcGetDefaultLogFile() throws Exception {
        if (pluginLogFile == null) {
            String filePath = pluginInstallRoot + pathSeparator + "logs" + pathSeparator + getName() + pathSeparator + LOG_FILE;
            pluginLogFile = new RemoteFile(getMachine(), filePath);
        }
        return pluginLogFile;
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.topology.impl.LogMonitorClient#lmcClearLogOffsets()
     */
    @Override
    public void lmcClearLogOffsets() {
        // Do nothing, the use of offsets is not supported in this server type
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.topology.impl.LogMonitorClient#lmcUpdateLogOffset(java.lang.String, java.lang.Long)
     */
    @Override
    public void lmcUpdateLogOffset(String logFile, Long newLogOffset) {
        // Do nothing, the use of offsets is not supported in this server type
    }

    //*******************************************************************************************************
    //*** End of log monitoring methods
    //*******************************************************************************************************

}
