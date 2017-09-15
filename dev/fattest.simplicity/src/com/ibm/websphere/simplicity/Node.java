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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.simplicity.exception.NotImplementedException;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.product.InstallationType;
import com.ibm.websphere.simplicity.product.InstalledWASProduct;
import com.ibm.websphere.simplicity.product.WASInstallation;
import com.ibm.websphere.simplicity.server.AppServerCreationOptions;
import com.ibm.websphere.simplicity.util.CollectionUtility;

/**
 * This class represents a WebSphere node. A Node is a logical grouping of application servers. A
 * Node instance of {@link Scope} has visibility to the servers within it.
 */
public class Node extends Scope {

    private static Class c = Node.class;
    protected static final String CHANGE_KEY_SERVERS = "servers";

    private Set<Server> servers;
    private Machine machine;
    private String profilePath;
    private String profileName;
    private String hostname;
    private WebSphereVersion baseProductVersion;

    /**
     * Nodes can only be created by instances of {@link Cell} and {@link Topology}.
     * 
     * @param configId The {@link ConfigIdentifier} for this node
     * @param cell The instance of {@link Cell} representing the cell to which this node belongs.
     */
    protected Node(Cell cell) throws Exception {
        super(cell, cell);
    }

    @Override
    public String getObjectNameFragment() {
        return this.cell.getObjectNameFragment() + ",node=" + this.name;
    }

    /**
     * Get the name of the {@link Cell} to which this Node belongs
     * 
     * @return The name of the Node's {@link Cell}
     */
    public String getCellName() {
        return this.cell.getName();
    }

    /**
     * Get the {@link Server}s that exist in this Node
     * 
     * @return A Set of {@link Server}s in the this Node
     * @throws Exception
     */
    public Set<Server> getServers() throws Exception {
        return new HashSet<Server>(this.servers);
    }

    /**
     * Get a sorted List of Servers in the Cell
     * 
     * @param c The Comparator that defines how to sort the Servers
     * @return A List containing the sorted Servers
     * @throws Exception
     */
    public List<Server> getServers(Comparator<Server> c) throws Exception {
        return CollectionUtility.sort(getServers(), c);
    }

    public ApplicationServer getManager() throws Exception {
        return (ApplicationServer) servers.iterator().next();
    }

    /**
     * Get a specific {@link Server} in this Node that has the specified name
     * 
     * @param name The name of the {@link Server} to get
     * @return The existing {@link Server} in this cell that has the specified name or null if no {@link Server} in the node exists with the name
     * @throws Exception
     */
    public Server getServerByName(String name) throws Exception {
        final String method = "getServerByName";
        Log.entering(c, method, name);
        for (Server server : getServers()) {
            Log.finest(c, method, "Current server name: " + server.getName());
            if (server.getName().equalsIgnoreCase(name)) {
                Log.finer(c, method, "Server with matching name found.");
                Log.exiting(c, method, server);
                return server;
            }
        }
        Log.finer(c, method, "No server with matching name found.");
        Log.exiting(c, method, null);
        return null;
    }

    /**
     * Retrieves all servers in the cell that match a certain server type.
     * 
     * @param name The name of the server to get
     * @return The {@link Server} with the specified name or null if no Server with the name exists
     * @throws Exception
     */
    public List<Server> getServersByType(ServerType type) throws Exception {
        final String method = "getServerByType";
        Log.entering(c, method, type);
        List<Server> ret = new ArrayList<Server>();
        Set<Server> servers = getServers();
        for (Server server : servers) {
            if (server.getServerType().equals(type)) {
                ret.add(server);
            }
        }
        Log.exiting(c, method, ret);
        return ret;
    }

    public OperationResults<ApplicationServer> createApplicationServer(AppServerCreationOptions options) throws Exception {
        return new OperationResults<ApplicationServer>(false);
    }

    public void deleteServer(String serverName) throws Exception {

    }

    public Set<InstalledWASProduct> getInstalledWASProducts() throws Exception {
        return new HashSet<InstalledWASProduct>();
    }

    public WASInstallation getWASInstall() throws Exception {
        return new WASInstallation(getMachine(), null, InstallationType.WAS_INSTALL);
    }

    /**
     * Get the profile directory of this Node. If the Node is not installed on the local machine
     * this will NOT refer to a path on the local machine. The path exists on the machine that the
     * Node is installed to.
     * 
     * @return The profile directory of the node
     * @throws Exception
     */
    public String getProfileDir() throws Exception {
        if (this.profilePath == null) {
            this.profilePath = expandVariable(VariableType.USER_INSTALL_ROOT);
            if (this.profilePath != null) // unmanaged nodes do not have a profile path
                this.profilePath = this.profilePath.replace('\\', '/');
        }
        return this.profilePath;
    }

    public String getJavaDir() throws Exception {
        String ret = getProfileDir();
        return ret.substring(0, ret.lastIndexOf("/")) + "/java/jre";
    }

    /**
     * Translates a predefined variable into its corresponding string value.
     * 
     * @param variable The variable to translate.
     * @return The value of the variable, or null if it does not exist.
     * @throws Exception
     */
    public String expandVariable(VariableType variable) throws Exception {
        return expandVariable(variable.getValue());
    }

    /**
     * Translates a custom variable into its corresponding string value.
     * 
     * @param variable The variable name to translate.
     * @return The value of the variable, or null if it does not exist.
     * @throws Exception
     */
    public String expandVariable(String variable) throws Exception {
        // Remove the standard prefix & postfix, if any
        variable = variable.replace("${", "").replace("$(", "").replace("}", "").replace(")", "");
        throw new NotImplementedException();
    }

    /**
     * Recursively expands all variables in the string to the corresponding value
     * from the server, and replaces backslashes with forward slashes. The
     * resulting string does not contain any variables.
     * 
     * @param str A string that contains zero or more variables.
     * @return The fully-expanded version of the string.
     * @throws Exception
     */
    public String expandString(String str) throws Exception {
        return Scope.expandString(this, str);
    }

    /**
     * Set the profile directory for this Node
     * 
     * @param path The profile directory path
     */
    protected void setProfileDir(String path) {
        this.profilePath = path;
    }

    /**
     * The hostname of the machine that the Node is installed on
     * 
     * @return The hostname of the machine that the node is installed on
     * @throws Exception
     */
    public String getHostname() throws Exception {
        return hostname;
    }

    public void setHostname(String hostname) throws Exception {
        this.hostname = hostname;
    }

    /**
     * Get a {@link Machine} Object that represents the machine that this Node is installed on
     * 
     * @return The {@link Machine} of this Node
     * @throws Exception
     */
    public Machine getMachine() throws Exception {
        final String method = "getMachine";
        Log.entering(c, method);
        if (this.machine == null) {
            ConnectionInfo connInfo = Topology.getBootstrapMgr().getMachineConnectionData(hostname);
            Log.finer(c, method, "Constructing the machine.");
            this.machine = Machine.getMachine(connInfo);
        }
        Log.exiting(c, method, this.machine);
        return this.machine;
    }

    protected void setMachine(Machine machine) {
        this.machine = machine;
    }

    public WebSphereVersion getBaseProductVersion() throws Exception {
        final String method = "getBaseProductVersion";
        Log.entering(c, method);
        if (this.baseProductVersion == null) {
            // To avoid stack overflow, assign a default "8.0" version
            this.baseProductVersion = new WebSphereVersion("8.0");
            // TODO Call Liberty to get the product version
            //            String versionString = (String)task.run(this).getResult();
            //            Log.finer(c, method, "version: " + versionString);
            //            this.baseProductVersion = new WebSphereVersion(versionString.trim());
        }
        Log.exiting(c, method, this.baseProductVersion);
        return this.baseProductVersion;
    }

    /**
     * Get the name of the profile that this Node belongs to.<br/>
     * IMPORTANT!!!!<br/>
     * This method requires the use of a command line provider. If this method is being run against
     * a node on a remote machine, the command line provider must have remote execution
     * capabilities. ex: RXA provider
     * 
     * @return The profile name for the node
     * @throws Exception
     */
    public String getProfileName() throws Exception {
        final String method = "getProfileName";
        Log.entering(c, method);
        if (this.profileName == null) {
            Log.finer(c, method, "Obtaining profile name.");
            Machine m = getMachine();
            String[] params = new String[3];
            params[0] = "-getName";
            params[1] = "-profilePath";
            params[2] = getProfileDir();
            if (params[1].indexOf(" ") != -1) {
                params[1] = "\"" + params[1] + "\"";
            }
            String cmd = getProfileDir() + "/bin/manageprofiles" + m.getOperatingSystem().getDefaultScriptSuffix();
            Log.finer(c, method, "cmd: " + cmd);
            Log.finer(c, method, "params: " + params);
            this.profileName = m.execute(cmd, params).getStdout().trim();
        }
        Log.exiting(c, method, this.profileName);
        return this.profileName;
    }

    /**
     * Set the profile name of the node
     * 
     * @param profileName The profile name to set
     */
    protected void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    /**
     * Add a {@link Server} to this Node's private Set
     * 
     * @param ep The {@link Server} to add
     * @throws Exception
     */
    protected void addServer(Server ep) throws Exception {
        if (this.servers == null) {
            this.servers = new HashSet<Server>();
        }
        this.servers.add(ep);
    }

    /**
     * Remove a {@link Server} from this Node's private Set
     * 
     * @param server The {@link Server} to remove
     * @throws Exception
     */
    protected void removeServer(Server server) throws Exception {
        final String method = "removeServer";
        Log.entering(c, method, server);
        Server s = this.getServerByName(server.getName());
        if (s == null) {
            return;
        }
        this.servers.remove(s);
        Log.finer(c, method, "Server is removed: " + !this.servers.contains(s));
        Log.exiting(c, method);
    }

    public void commit(HashMap<String, Object> values) throws Exception {}

    @SuppressWarnings("unchecked")
    public void rollback(HashMap<String, Object> values) throws Exception {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.equals(CHANGE_KEY_SERVERS)) {
                this.servers = (Set) value;
            }
        }
    }

    public void sync() {
    // do nothing
    }

}
