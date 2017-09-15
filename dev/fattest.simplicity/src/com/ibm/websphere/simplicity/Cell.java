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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.simplicity.application.Application;
import com.ibm.websphere.simplicity.application.ApplicationManager;
import com.ibm.websphere.simplicity.config.SecurityConfiguration;
import com.ibm.websphere.simplicity.config.securitydomain.SecurityDomain;
import com.ibm.websphere.simplicity.exception.NotImplementedException;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.provider.OperationsProviderFactory;
import com.ibm.websphere.simplicity.provider.websphere.ConfigurationOperationsProvider;
import componenttest.topology.impl.LibertyServer;

/**
 * This class represents a WebSphere cell. The Cell instance of {@link Scope}, being the highest,
 * can see and affect all other scopes in the topology. A Cell is logical structural entity used to
 * group other entities such as {@link Node}s, {@link Cluster}s, and {@link NodeGroup}s.
 */
public class Cell extends Scope {

    private static Class c = Cell.class;

    private Set<Node> nodes;
    private SecurityConfiguration securityConfiguration;
    private ApplicationManager applicationManager = null;
    protected Workspace workspace;

    /**
     * Constructor
     * 
     * @param configId The {@link ConfigIdentifier} that uniquely identifies this Cell
     * @param connInfo The {@link ConnectionInfo} that holds data on how to connect to the Cell
     */
    public Cell(ConnectionInfo connInfo) {
        super(connInfo, null);
        this.cell = this;
    }

    @Override
    public String getObjectNameFragment() {
        return "WebSphere:cell=" + this.getName();
    }

    /**
     * Get the {@link ApplicationManager} for the cell. The ApplicationManager can be used to obtain {@link Application} instances.
     * 
     * @return The {@link ApplicationManager} for the cell.
     * @throws Exception
     */
    public ApplicationManager getApplicationManager() throws Exception {
        if (applicationManager == null)
            applicationManager = new ApplicationManager(this);
        return applicationManager;
    }

    public void initApplicationManager(LibertyServer ls) throws Exception {
        applicationManager = new ApplicationManager(ls);
    }

    public WebSphereTopologyType getTopologyType() {
        return WebSphereTopologyType.BASE;
    }

    /**
     * Get the {@link Server}s that exist in this cell across all {@link Node}s in the cell.
     * 
     * @return A Set of {@link Server}s in the cell
     * @throws Exception
     */
    public Set<Server> getServers() throws Exception {
        final String method = "getServers";
        Log.entering(c, method);
        Set<Server> ret = new HashSet<Server>();
        for (Node n : this.getNodes()) {
            ret.addAll(n.getServers());
        }
        Log.exiting(c, method, ret);
        return ret;
    }

    public Set<Cluster> getClusters() throws Exception {
        return new HashSet<Cluster>();
    }

    public ApplicationServer getManager() throws Exception {
        return (ApplicationServer) getServers().iterator().next();
    }

    /**
     * Get a specific {@link Server}
     * 
     * @param name The name of the server to get
     * @return The {@link Server} with the specified name or null if no Server with the name exists
     * @throws Exception
     */
    public Server getServerByName(String name) throws Exception {
        final String method = "getServerByName";
        Log.entering(c, method, name);
        Set<Server> servers = getServers();
        for (Server server : servers) {
            if (server.getName().equals(name)) {
                Log.exiting(c, method, server);
                return server;
            }
        }
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
        Set<Node> nodes = getNodes();
        for (Node node : nodes) {
            ret.addAll(node.getServersByType(type));
        }
        Log.exiting(c, method, ret);
        return ret;
    }

    /**
     * Get the {@link Node}s that exist in this cell
     * 
     * @return A Set of {@link Node}s in the cell
     * @throws Exception
     */
    public Set<Node> getNodes() throws Exception {
        final String method = "getNodes";
        Log.entering(c, method);
        if (this.nodes == null) {
            Log.finer(c, method, "Initializing nodes.");
            loadNodes();
        }
        Log.exiting(c, method, this.nodes);
        return this.nodes;
    }

    /**
     * Get a specific {@link Node} in this cell that has the specified name
     * 
     * @param name The name of the node to get
     * @return The existing {@link Node} in this cell that has the specified name or null if there
     *         is no {@link Node} with the name
     * @throws Exception
     */
    public Node getNodeByName(String name) throws Exception {
        final String method = "getNodeByName";
        Log.entering(c, method, name);
        for (Node n : this.getNodes()) {
            Log.finest(c, method, "Current node name: " + n.getName());
            if (n.getName().equalsIgnoreCase(name)) {
                Log.finer(c, method, "Found a node with matching name.");
                Log.exiting(c, method, n);
                return n;
            }
        }
        Log.finer(c, method, "No node found with matching name.");
        Log.exiting(c, method, null);
        return null;
    }

    /**
     * Get the {@link SecurityConfiguration} for this cell. The {@link SecurityConfiguration} can be used to configure security settings
     * for the cell
     * 
     * @return A {@link SecurityConfiguration} instance
     */
    public SecurityConfiguration getSecurityConfiguration() throws Exception {
        if (this.securityConfiguration == null)
            this.securityConfiguration = new SecurityConfiguration(this);
        return this.securityConfiguration;
    }

    /**
     * Start all the servers in the cell. The Cell manager is started first. For each node, the node
     * manager is started followed by the remaining servers on the node.
     * 
     * @throws Exception
     */
    public void start() throws Exception {
        final String method = "start";
        Log.entering(c, method);

        for (Node node : this.getNodes()) {
            for (Server server : node.getServers())
                server.start();
        }
        Log.exiting(c, method);
    }

    /**
     * Start all the servers in the cell. The Cell manager is started first. For
     * each node, the node manager is started followed by the remaining servers
     * on the node.
     * 
     * @param mbeanWaitDuration The maximum time to wait (in seconds) for the
     *            server to activate mbeans (slower servers take longer). This
     *            is a per server value
     * @throws Exception
     */
    public void start(int mbeanWaitDuration) throws Exception {
        final String method = "start";
        Log.entering(c, method, mbeanWaitDuration);

        Log.finer(c, method, "Starting Cell " + this.getName() + ".");
        Exception e1 = null;

        // first start the cell manager
        Log.finer(c, method, "Staring the cell manager.");
        ApplicationServer cellManager = this.getManager();
        try {
            cellManager.start(mbeanWaitDuration);
        } catch (Exception e) {
            e1 = e;
        }

        ApplicationServer nodeManager = null;
        for (Node node : this.getNodes()) {
            nodeManager = node.getManager();
            // first start the node manager
            if (nodeManager != cellManager) {
                Log.finer(c, method, "Starting the manager for node " + node.getName());
                try {
                    nodeManager.start(mbeanWaitDuration);
                } catch (Exception e) {
                    if (e1 == null)
                        e1 = e;
                }
            }
            Log.finer(c, method, "Starting any remaining servers on the node.");
            for (Server server : node.getServers()) {
                if (server != nodeManager && server != cellManager) {
                    try {
                        server.start(mbeanWaitDuration);
                    } catch (Exception e) {
                        if (e1 == null)
                            e1 = e;
                    }
                }
            }
        }

        if (e1 != null)
            throw e1;

        Log.finer(c, method, "Cell " + this.getName() + " started.");

        Log.exiting(c, method);
    }

    /**
     * Stop all the servers in the cell.
     * 
     * @throws Exception
     */
    public void stop() throws Exception {
        final String method = "stop";
        Log.entering(c, method);

        for (Node node : this.getNodes()) {
            Log.finer(c, method, "Stopping all the non-manager servers for node " + node.getName());
            for (Server server : node.getServers()) {
                server.stop();
            }
        }
        // lastly the cell manager
        Log.exiting(c, method);
    }

    /**
     * Stop all the servers in the cell.
     * 
     * @param timeout The time to wait (in seconds). This is a per server value.
     * @throws Exception
     */
    public void stop(long timeout) throws Exception {
        final String method = "stop";
        Log.entering(c, method, timeout);

        Log.finer(c, method, "Stopping Cell " + this.getName());
        Exception e1 = null;

        // this is going to make us lose our admin connection. We need to stop
        // non-managers first, followed by node managers, followed by the cell
        // manager
        ApplicationServer cellManager = this.getManager();
        ApplicationServer nodeManager = null;

        for (Node node : this.getNodes()) {
            nodeManager = node.getManager();
            // first non-manager servers
            Log.finer(c, method, "Stopping all the non-manager servers for node " + node.getName());
            for (Server server : node.getServers()) {
                if (server != nodeManager && server != cellManager) {
                    try {
                        server.stop(timeout);
                    } catch (Exception e) {
                        if (e1 == null)
                            e1 = e;
                    }
                }
            }
            // now the node manager
            if (nodeManager != cellManager) {
                Log.finer(c, method, "Stopping the manager for node " + node.getName());
                try {
                    nodeManager.stop(timeout);
                } catch (Exception e) {
                    if (e1 == null)
                        e1 = e;
                }
            }
        }
        // lastly the cell manager
        Log.finer(c, method, "Stopping the cell manager.");
        try {
            cellManager.stop(timeout);
        } catch (Exception e) {
            if (e1 == null)
                e1 = e;
        }

        if (e1 != null)
            throw e1;

        Log.finer(c, method, "Cell " + this.getName() + " stopped.");

        Log.exiting(c, method);
    }

    /**
     * Returns true if there is currently a live connection to administer the Cell
     * 
     * @return true If there is currently a live connection to administer the Cell
     * @throws Exception
     */
    public boolean isConnected() throws Exception {
        final String method = "isConnected";
        Log.entering(c, method);
        throw new NotImplementedException();
        //        Log.exiting(c, method, connected);
        //        return connected;
    }

    /**
     * 
     */
    public void popAllConnections() {
    // do nothing

    }

    /**
     * @param none
     * @param user
     * @param password
     */
    public void pushConnection(ConnectorType none, String user, String password) {
    // do nothing
    }

    public void pushConnection(ConnectionInfo info) {
    // do nothing
    }

    public void pushConnection(ConnectorType connType,
                               Integer port,
                               String username,
                               String password) throws Exception {
// do nothing
    }

    @Override
    public Workspace getWorkspace() throws Exception {
        return new Workspace(this);
    }

    /**
     * This method clears all cached data for this Cell. Further requests for
     * information will be loaded fresh from the WAS instance.
     * <p>
     * WARNING!!! Calling this method invalidates all pointers to any Simplicity
     * objects obtained from the Cell and its children. Only call this method if
     * you absolutely want to reset the topology object model underneath this Cell.
     * 
     * @throws Exception
     */
    public void resetCell() throws Exception {
        final String method = "resetCell";
        Log.entering(c, method);

        Log.finer(c, method, "Resetting all the \"child\" private data of this Cell.");
        this.nodes = null;

        Log.exiting(c, method);
    }

    @Override
    public SecurityDomain getSecurityDomain() throws Exception {
        return super.getSecurityDomain();
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
     * <p>
     * Note that the cell does not contain many variables, so most expansions
     * will result in empty values.
     * 
     * @param str A string that contains zero or more variables.
     * @return The fully-expanded version of the string.
     * @throws Exception
     */
    public String expandString(String str) throws Exception {
        return Scope.expandString(this, str);
    }

    /**
     * Add a {@link Node} to this cells private Set
     * 
     * @param node The {@link Node} to add
     * @throws Exception
     */
    protected void addNode(Node node) throws Exception {
        if (this.getNodeByName(node.getName()) != null)
            return;

        this.nodes.add(node);
    }

    /**
     * Delegate to the {@link ConfigurationOperationsProvider} to obtain a list of {@link ConfigIdentifier}s
     * for the {@link Node}s in the cell
     * 
     * @throws Exception
     */
    private void loadNodes() throws Exception {
        final String method = "loadNodes";
        Log.entering(c, method);
        if (this.nodes == null)
            this.nodes = new HashSet<Node>();
        Log.exiting(c, method);
    }

    public void commit(HashMap<String, Object> values) throws Exception {}

    public void rollback(HashMap<String, Object> values) throws Exception {}

    /**
     * Shut down the administrative connection to manage the Cell.<br/>
     * Note that if the administrative connection is closed and a method call is
     * made in which a connection is needed, the providers will implicitly open
     * a connection using the current connection information.
     * 
     * @throws Exception
     */
    public void disconnect() throws Exception {
        final String method = "disconnect";
        Log.entering(c, method);
        if (isConnected()) {
            Log.finer(c, method, "Disconnecting the cell.");
            OperationsProviderFactory.getProvider().getCellOperationsProvider()
                            .disconnect(this, getConnInfo());
        }
        Log.exiting(c, method);
    }

    public void connect() {
    // do nothing

    }

    public void deleteCluster(String name) {
    // do nothing

    }

}
