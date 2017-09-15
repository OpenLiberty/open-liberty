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

import java.util.HashMap;
import java.util.Set;

import com.ibm.websphere.simplicity.exception.NotImplementedException;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.runtime.ProcessStatus;
import componenttest.topology.impl.LibertyServer;

/**
 * This abstract class is the parent for all Servers in the Topology. This includes application
 * servers, web servers, and generic servers.
 */
public abstract class Server extends Scope {

    private static final Class c = Server.class;

    protected LibertyServer instance;
    protected Node node = null;
    protected ServerType serverType = null;

    /**
     * Constructor to create a new Server
     * 
     * @param configId The {@link ConfigIdentifier} for the Server
     * @param cell The {@link Cell} that this Server belongs to
     * @param node The {@link Node} that the Server belongs to
     * @param server The underlying server instance
     * @param serverType The type of this server
     */
    protected Server(Cell cell, Node node, componenttest.topology.impl.LibertyServer server, ServerType serverType) throws Exception {
        super(node, cell);
        this.node = node;
        this.serverType = serverType;
        this.instance = server;
    }

    @Override
    public String getObjectNameFragment() {
        return this.node.getObjectNameFragment() + ",process=" + this.getName();
    }

    /**
     * Get the name of the parent {@link Cell} for this Server
     * 
     * @return The name of the parent {@link Cell} of this Server
     */
    public String getCellName() {
        return this.cell.getName();
    }

    /**
     * Get the parent {@link Node} for this Server
     * 
     * @return The parent {@link Node} of this Server
     */
    public Node getNode() {
        return this.node;
    }

    public componenttest.topology.impl.LibertyServer getBackend() {
        return this.instance;
    }

    /**
     * Get the name of the parent {@link Node} for this Server
     * 
     * @return The name of the parent {@link Node} of this Server
     */
    public String getNodeName() {
        return this.node.getName();
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
     * This method starts the Server
     * 
     * @throws Exception
     */
    public abstract void start() throws Exception;

    /**
     * This method starts the Server
     * 
     * @throws Exception
     */
    public abstract void start(int mbeanWaitDuration) throws Exception;

    /**
     * This method stops the Server
     * 
     * @throws Exception
     */
    public abstract void stop() throws Exception;

    /**
     * This method stops the Server
     * 
     * @throws Exception
     */
    public abstract void stop(long timeout) throws Exception;

    /**
     * Get the {@link ServerType} of this Server. The server type determines the capibilities of the
     * Server. For example, a {@link Dmgr} server manages the nodes within a {@link Cell}.
     * 
     * @return The type of this Server
     */
    public ServerType getServerType() {
        return this.serverType;
    }

    ////   public String getProfile() {
    ////       return this.instance.getProfileName();
    ////   }

    /**
     * This method clears all cached data for this Server. Further requests for
     * information will be loaded fresh from the WAS instance.
     * <p>
     * WARNING!!! Calling this method invalidates all pointers to any Simplicity
     * objects obtained from the Server and its children. Only call this method if
     * you absolutely want to reset the object model underneath this Server.
     * 
     * @throws Exception
     */
    public void resetServer() throws Exception {
        // nothing to do; ports are handled by underlying componenttest.topolgy.impl.LibertyServer instance
    }

    /**
     * Get the port number value for a {@link ConnectorType} for this Server
     * 
     * @param connectorType The {@link ConnectorType} port value to get
     * @return An Integer representation of the port value
     * @throws Exception
     */
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
    public Integer getPortNumber(PortType port) throws Exception {
        return this.instance.getPort(port);
    }

    /**
     * Get the host that the {@link ConnectorType} is bound to
     * 
     * @param connectorType The {@link ConnectorType} to get the host for
     * @return The host of the port
     * @throws Exception
     */
    ////    public String getPortHost(ConnectorType connectorType) throws Exception {
    ////        PortType port = PortType.valueOf(connectorType.getEndpointName());
    ////        return getPortHost(port);
    ////    }

    /**
     * Get the host that the {@link PortType} is bound to
     * 
     * @param connectorType The {@link PortType} to get the host for
     * @return The host of the port
     * @throws Exception
     */
    ////    public String getPortHost(PortType port) throws Exception {
    ////    	return this.instance.getHostname(); // why would some ports have a different host name?
    ////    }

    /**
     * This method returns the {@link ProcessStatus} of the Server. This method can be used to
     * determine whether or not a server is running. There are cases where the status of a server
     * may not be able to be determined. In these cases a status of {@link ProcessStatus#STOPPED} is
     * returned.
     * 
     * @return The status of the server
     * @throws Exception
     */
    public abstract ProcessStatus getServerStatus() throws Exception;

    /**
     * Determine whether or not the target port is currently listening.
     * This method is useful for determining if the server is really ready
     * to accept requests on its ports. Occasionally, a port is not ready
     * immediately after the server is started. If a request to a server port
     * is made before it is ready, unexpected failures can occur.
     * 
     * @param port The port whose status you want to check
     * @return true if the port is listening, otherwise false
     * @throws Exception if port status cannot be established
     */
    public boolean isPortListening(PortType port) throws Exception {
        ////return this.instance.isPortListening(PortType.convert(port));
        return true;
    }

    /**
     * Determine whether or not the target ports are currently listening.
     * 
     * @param ports The ports whose status you want to check
     * @return true if the ports are listening, otherwise false
     * @throws Exception if port status cannot be established
     */
    public boolean isPortListening(Set<PortType> ports) throws Exception {
        ////        final String method = "isPortListening";
        ////        Log.entering(c, method, ports);
        ////        if(ports==null) {
        ////            throw new IllegalArgumentException("Unable to determine if ports are listening since the input Set of PortType objects was null");
        ////        }
        ////        for(PortType portType : ports) {
        ////            if(!this.isPortListening(portType)) {
        ////                Log.exiting(c, method, false);
        ////                return false;
        ////            }
        ////       }
        ////        Log.exiting(c, method, true);
        return true;
    }

    /**
     * Modify a port for this Server
     * 
     * @param port The port to modify
     * @param host The hostname to set for the port
     * @param value The value to set for the port
     * @param modifyShared
     * @throws Exception
     */
    ////    public void modifyServerPort(PortType port, String host, Integer value, Boolean modifyShared) throws Exception {
    ////        final String method = "modifyServerPort";
    ////        Log.entering(c, method, new Object[]{host, value, modifyShared});
    ////        if(value==null) {
    ////        	throw new IllegalArgumentException("Unable to modify the port named "+port+" on the server named "+this.getName()+" because the specified port value is null");
    ////        }
    ////        this.instance.setPort(PortType.convert(port), value.intValue());
    ////        Log.exiting(c, method);
    ////    }

    public void commit(HashMap<String, Object> values) throws Exception {}

    @SuppressWarnings("unchecked")
    public void rollback(HashMap<String, Object> values) throws Exception {}

}