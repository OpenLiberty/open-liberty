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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.provider.OperationsProviderFactory;
import com.ibm.websphere.simplicity.provider.websphere.WebSphereOperationsProvider;
import componenttest.common.apiservices.Bootstrap;
import componenttest.exception.TopologyException;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * The Topology class is the entry point into the Simplicity WebSphere topology Object model. A
 * WebSphere topology is the collection of all servers underneath a top-level server, such as a
 * deployment manager, admin agent, or job manager. This class provides access to that information,
 * as well as client-side operations such as tracing. Additionally, this class provides methods to
 * programmatically set Simplicity settings and prefernces.
 */
public class Topology {

    /**
     * This is the default configuration file in which settings such which {@link WebSphereOperationsProvider} should be used. The configuration file to use can be
     * specified using the jvm arg <b>configProps</b>.<br/>ex: -DconfigProps=myConfig.props
     */
    public static final String DEFAULT_CONFIG_FILE = "simplicityConfig.props";

    /**
     * Simplicity version number
     */
    public static final String SIMPLICITY_VERSION = "1.0.1.5";

    private static Class c = Topology.class;
    private static List<Cell> cells = new ArrayList<Cell>();
    private static boolean inited = false;
    private static boolean topologyCachingEnabled = false;
    private static WebSphereOperationsProvider operationsProvider;
    private static LibertyServer libServer;

    /**
     * @return the libServer
     */
    public static LibertyServer getLibertyServer() {
        return libServer;
    }

    /**
     * Default path to published servers
     */
    private static final String PATH_TO_AUTOFVT_SERVERS = "publish/servers/";

    private Topology() {}

    /**
     * This method returns true if the topology has already been initialized using one of the init
     * methods of this class
     * 
     * @return true if the Simplicity Object model has already been initialized.
     */
    public static boolean isInited() {
        return inited;
    }

    /**
     * Returns a List of initialized {@link Cell}s. The {@link Cell} Object provides access to all
     * other objects in the topology object model. This method returns the Cells in the order they
     * are defined in the bootstrapping file if one is being used, or in the order that they are
     * initialized.
     * 
     * @return A List of initialized {@link Cell}s
     */
    public static List<Cell> getCells() {
        return cells;
    }

    /**
     * Get a sorted List of {@link Cell}s.
     * 
     * @param c The Comparator that defines how to sort the {@link Cell}s
     * @return A List containing the Cells in sorted order
     */
    public static List<Cell> getCells(Comparator<Cell> c) {
        List<Cell> cells = getCells();
        Collections.sort(cells, c);
        return cells;
    }

    /**
     * Get a {@link Cell} with a specific name. Note that if multiple cells have been initialized
     * with the same name, this method will return the first {@link Cell} found.
     * 
     * @param name The name of the {@link Cell} to retrieve
     * @return The {@link Cell} with the specified name, or null if no {@link Cell} exists with the
     *         name
     */
    public static Cell getCellByName(String name) {
        for (Cell c : cells)
            if (c.getName().equalsIgnoreCase(name))
                return c;
        return null;
    }

    /**
     * Get all the cells that have the specific {@link WebSphereTopologyType}
     * 
     * @param type The type to get
     * @return All the cells with the specified {@link WebSphereTopologyType}
     * @throws Exception
     */
    public static Set<Cell> getCellsByType(WebSphereTopologyType type) throws Exception {
        Set<Cell> ret = new HashSet<Cell>();
        for (Cell c : cells)
            if (c.getTopologyType().equals(type))
                ret.add(c);
        return ret;
    }

    /**
     * @param cellKey
     * @return
     */
    public static Cell getCellByBootstrapKey(String bootstrapKey) {
        for (Cell cell : cells) {
            if (cell.getBootstrapFileKey().equals(bootstrapKey)) {
                return cell;
            }
        }
        return null;
    }

    /**
     * Loads all the {@link Cell}s defined in bootstrapping properties file. If no bootstrapping
     * properties file exists, an Exception is thrown. The {@link Cell}s are then accessible via
     * the getCell* methods of this class.
     * 
     * @throws Exception
     */
    public static void init() throws Exception {
        Log.entering(c, "init");
        if (inited) {
            Log.exiting(c, "init (already initialized)");
            return;
        }

        //As Liberty doesn't have a concept of cells and nodes we will just mock up a cell and node.
        //This is required so that we can run both tWAS FAT suites and our own Liberty FAT suites
        cells = new ArrayList<Cell>();

        //Determine serverName to use from the publish directory...
        String serverName = "";

        //Get the location of the local.properties file - default to current directory
        String localPropsPath = System.getProperty("local.properties", "./local.properties");
        java.util.Properties localProps = new java.util.Properties();
        localProps.load(new java.io.FileInputStream(localPropsPath));

        //Get the autoFVT root - dir.component.root used in FAT launch.xml so this must be set
        String absolutePathToPublishDirectory = localProps.getProperty("dir.component.root", ".") + "/" + PATH_TO_AUTOFVT_SERVERS;

        Log.info(c, "init", "Path to publish directory from Local Properties: " + absolutePathToPublishDirectory);
        //Get the name for the server from the publish directory
        File pubDir = new File(absolutePathToPublishDirectory);
        File[] files = pubDir.listFiles();
        // We only support one server at the moment so choose the first one.
        if (files != null && files.length > 0) {
            serverName = files[0].getName();
        } else {
            Log.info(c, "init", "No servers found in publish directory: " + absolutePathToPublishDirectory);
            throw new TopologyException("No servers found in publish directory: " + absolutePathToPublishDirectory);
        }
        Log.info(c, "init", "Using serverName: " + serverName);

        //Use the factory to get our Liberty server
        libServer = LibertyServerFactory.getLibertyServer(serverName, null, false);

        Cell cell = new Cell(null);// add all Nodes to the same "cell"
        cell.initApplicationManager(libServer);
        cell.setBootstrapFileKey(libServer.getBootstrapKey());

        Machine machine = libServer.getMachine();
        String hostname = machine.getHostname();

        cell.setName("Cell " + hostname);
        Node node = new Node(cell);
        node.setBootstrapFileKey(libServer.getBootstrapKey());
        node.setName("Node " + hostname);
        node.setHostname(hostname);
        node.setProfileDir(libServer.getServerRoot());

        ApplicationServer appServer = new ApplicationServer(libServer, node);
        appServer.setBootstrapFileKey(libServer.getBootstrapKey());
        appServer.setName(libServer.getServerName()); // cached in Scope, not in Server, so we have to cache the name redundantly
        node.addServer(appServer);
        cell.addNode(node);
        cells.add(cell);

        inited = true;
        Log.exiting(c, "init");
    }

    /**
     * This method clears the internal cell cache and reloads the cells defined in the bootstrapping
     * properties file.<br/>
     * WARNING!! Calling this method invalidates all pointers to any
     * Simplicity Objects. Only call this method if you absolutely want to reset the topology Object
     * model.
     * 
     * @throws Exception
     */
    public static void reset() throws Exception {
        cells = new ArrayList<Cell>();
        inited = false;
        init();
    }

    /**
     * Set the bootstrapping file to read from when initializing the WebSphere topology Object model.
     * 
     * @param bootStrappingFile The file that contains the {@link Cell} definitions
     */
    public static void setBootStrappingFile(File bootStrappingFile) {
        if (bootStrappingFile != null) {
            //FIXME: the import statement might trigger the static block of com.ibm.liberty.Topology too early for this to work
            System.setProperty("bootstrapping.properties", bootStrappingFile.getPath());
        }
    }

    /**
     * Get the bootstrapping file used to initialize the WebSphere topology Object model
     * 
     * @return The bootstrapping file
     * @throws Exception
     */
    public static Bootstrap getBootstrapMgr() throws Exception {
        return Bootstrap.getInstance();
    }

    public static BootStrappingFileOperations getBootstrapFileOps() throws Exception {
        File bootStrappingFile = getBootStrappingFile();
        if (null != bootStrappingFile && bootStrappingFile.canRead()) {
            return new BootStrappingFileOperations(bootStrappingFile);
        }
        return null;
    }

    public static File getBootStrappingFile() {
        try {
            return getBootstrapMgr().getFile();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Set if topology caching should be used when initializing the topology Object model. If true, {@link Cell}, {@link Node}, {@link Server}, {@link Cluster}, and {@link Machine}
     * data are
     * cached to the bootstrapping properties file. These properties can then be used externally or
     * used to reinitalized the topology Object model without incurring the cost of a wsadmin or JMX
     * connection.
     * 
     * @param enabled true if topology caching should be used
     */
    public static void setTopologyCaching(boolean enabled) {
        topologyCachingEnabled = enabled;
    }

    /**
     * @return true if topology caching is enabled
     */
    public static boolean isTopologyCachingEnabled() {
        return topologyCachingEnabled;
    }

    /**
     * Set the default {@link OperationsProviderType} to use to perform
     * WebSphere operations. This type of operations provider will be used by
     * the {@link WebSphereOperationsProvider}.
     * 
     * @param type The {@link OperationsProviderType} to use when performing
     *            WebSphere operations.
     * @throws Exception
     */
    public static void setDefaultOperationsProvider(OperationsProviderType type) throws Exception {
        OperationsProviderFactory.setDefaultCommandProvider(type);
        operationsProvider = null;
    }

    /**
     * Get the {@link WebSphereOperationsProvider} used to perform WebSphere
     * operations.
     * 
     * @return The {@link WebSphereOperationsProvider}
     * @throws Exception
     */
    public static WebSphereOperationsProvider getOperationsProvider() throws Exception {
        if (operationsProvider == null) {
            operationsProvider = OperationsProviderFactory.getProvider();
        }
        return operationsProvider;
    }

}
