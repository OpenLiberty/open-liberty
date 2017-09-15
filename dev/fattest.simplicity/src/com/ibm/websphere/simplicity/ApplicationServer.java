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

import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.provider.OperationsProviderFactory;
import com.ibm.websphere.simplicity.provider.websphere.ConfigurationOperationsProvider;
import com.ibm.websphere.simplicity.runtime.ProcessStatus;
import componenttest.topology.impl.LibertyServer;

/**
 * This class represents a WebSphere application server. This is the parent class for specialized
 * application servers such as {@link Dmgr}.
 */
public class ApplicationServer extends Server {

    private static final Class c = ApplicationServer.class;

    /**
     * Constructor to create an existing instance
     * 
     * @param configId The {@link ConfigIdentifier} for the ApplicationServer
     * @param scope The {@link Cell} that this ApplicationServer belongs to
     * @param node The {@link Node} that the ApplicationServer belongs to
     */
    protected ApplicationServer(LibertyServer server, Node node) throws Exception {
        this(server, node.getCell(), node, ServerType.APPLICATION_SERVER);
    }

    /**
     * Constructor to create a new ApplicationServer
     * 
     * @param configId The {@link ConfigIdentifier} for the ApplicationServer
     * @param cell The {@link Cell} that this ApplicationServer belongs to
     * @param node The {@link Node} that the ApplicationServer belongs to
     * @param serverType The type of server
     */
    protected ApplicationServer(LibertyServer server, Cell cell, Node node, ServerType serverType) throws Exception {
        super(cell, node, server, serverType);
    }

    /**
     * Starts the server using the underlying com.ibm.liberty.Server instance.
     * 
     * @throws Exception If the server did not start successfully and is not already started.
     */
    @Override
    public void start() throws Exception {
        this.instance.startServer();
    }

    /**
     * Starts the server using the underlying com.ibm.liberty.Server instance.
     * 
     * @param mbeanWaitDuration This value is ignored.
     * 
     * @throws Exception If the server did not start successfully and is not already started.
     */
    @Override
    public void start(int mbeanWaitDuration) throws Exception {
        this.instance.startServer();
    }

    /**
     * Stops the server using the underlying com.ibm.liberty.Server instance.
     * 
     * @throws Exception If the server did not stop successfully and is not already stopped.
     */
    @Override
    public void stop() throws Exception {
        this.instance.stopServer();
    }

    /**
     * Stops the server using the underlying com.ibm.liberty.Server instance.
     * 
     * @param timeout This value is ignored.
     * 
     * @throws Exception If the server did not stop successfully and is not already stopped.
     */
    @Override
    public void stop(long timeout) throws Exception {
        this.instance.stopServer();
    }

    /**
     * Equivalent to calling stop() followed by start()
     * 
     * @throws Exception
     */
    public void restart() throws Exception {
        this.stop();
        this.start();
    }

    /**
     * Equivalent to calling stop() followed by start()
     * 
     * @param timeout This value is ignored.
     * @throws Exception
     */
    public void restart(long timeout) throws Exception {
        this.restart();
    }

    public Set<Cluster> getClusters() throws Exception {
        Set<Cluster> ret = new HashSet<Cluster>();
        return ret;
    }

    @Override
    public ProcessStatus getServerStatus() throws Exception {
        ////return ProcessStatus.convert(this.instance.getServerStatus());
        return null;
    }

    /**
     * Get the short name of this server. This name can contain one to eight
     * uppercase alphanumeric characters, but it cannot start with a numeral.
     * This field only applies to the z/OS(R) platform. On other platforms, this
     * method will typically return null.
     * 
     * @return The process ID
     */
    public String getShortName() throws Exception {
        final String method = "getShortName";
        Log.entering(c, method);
        String name;
        try {
            ConfigurationOperationsProvider provider =
                            OperationsProviderFactory.getProvider().getConfigurationOperationsProvider();
            // AbstractSession session = getWorkspace().getSession();
            ConfigIdentifier configId = this.getConfigId();
            Object attribute = provider.getAttribute(getNode(), null, configId, "shortName");
            name = (String) attribute;
        } catch (Exception e) {
            throw new Exception("Unable to detect the shortName of the Server named " + this
                            .getName(), e);
        }
        Log.exiting(c, method, name);
        return name;
    }

    public String getMappingName() {
        return "WebSphere:cell=" + getCellName() + ",node=" + getNode().getName() + ",server=" + getName();
    }

    /**
     * Get the OS process ID for this ApplicationServer
     * 
     * @return The process ID
     */
    public int getProcessID() throws Exception {
        return 0;
    }

    /**
     * Causes the current Thread to sleep until a specific set of ports
     * associated with this Server are listening.
     * 
     * @param timeout
     *            The number of milliseconds to wait for port activation before
     *            timing out
     * @param pollInterval
     *            The number of milliseconds to wait between each round of
     *            status checking
     * @param ports
     *            The ports to wait for
     * @return true if all ports started listening before the timeout expired,
     *         otherwise false
     * @throws Exception
     *             if port status cannot be established, or if this thread is
     *             interrupted
     */
    public boolean waitForPortActivation(long timeout, long pollInterval, Set<PortType> ports) throws Exception {
        final String method = "waitForPortActivation";
        Log.entering(c, method, new Object[] { timeout, pollInterval, ports });
        if (ports == null) {
            throw new IllegalArgumentException("Unable to determine if ports are listening since the input Set of PortType objects was null");
        }
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < timeout) {
            if (this.isPortListening(ports)) {
                Log.exiting(c, method, true);
                return true;
            }
            Thread.sleep(pollInterval);
            Log.finer(c, method, "This thread has waited " + (System.currentTimeMillis() - start) + " milliseconds for the ports to become active.  Will wait up to " + timeout
                                 + " milliseconds.");
        }
        Log.exiting(c, method, false);
        return false;
    }

}