/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.location.MalformedLocationException;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
public class WsLocationAdminImpl implements WsLocationAdmin {
    private static final TraceComponent tc = Tr.register(WsLocationAdminImpl.class);

    private static final String LOC_INTERNAL_LIB_DIR = "wlp.lib.dir";

    public static final String LOC_INTERNAL_WORKAREA_DIR = "wlp.workarea.dir";

    private static final String LOC_AREA_NAME_APPS = "shared/apps/",
                    LOC_AREA_NAME_CONFIG = "shared/config/",
                    LOC_AREA_NAME_RESC = "shared/resources/",
                    LOC_AREA_NAME_STATE = "logs/state/",
                    LOC_AREA_NAME_SERVERS = "servers/",
                    LOC_AREA_NAME_CLIENTS = "clients/",
                    LOC_AREA_NAME_WORKING = "workarea/",
                    LOC_AREA_NAME_EXTENSION = "extension/";

    private volatile static WsLocationAdminImpl instance;

    /**
     * Construct the WsLocationAdminService singleton based on a set of initial
     * properties provided by bootstrap or other initialization code (e.g. an
     * initializer in a test environment).
     *
     * @param initProps
     * @return WsLocationAdmin
     */
    public static WsLocationAdminImpl createLocations(Map<String, Object> initProps) {
        if (instance == null) {
            SymbolRegistry.getRegistry().clear();
            instance = new WsLocationAdminImpl(initProps);
        }

        return instance;
    }

    /**
     * Construct the WsLocationAdminService singleton based on a set of initial
     * properties provided by the bundle context when running in an osgi
     * framework.
     *
     * @param initProps
     * @return WsLocationAdmin
     */
    public static WsLocationAdminImpl createLocations(BundleContext ctx) {
        if (instance == null) {
            SymbolRegistry.getRegistry().clear();
            instance = new WsLocationAdminImpl(new BundleContextMap(ctx));
        }

        return instance;
    }

    /**
     * @return
     */
    public static WsLocationAdminImpl getInstance() {
        if (instance == null)
            throw new IllegalStateException("Location manager not initialized");

        return instance;
    }

    /**
     * Location of installation; usually the parent of bootstrapLib
     * (e.g. wlp/).
     *
     * @see WsLocationConstants#LOC_INSTALL_DIR
     */
    final protected SymbolicRootResource installRoot;

    /**
     * Parent of installation location.
     * (e.g. parent of wlp)
     *
     * @see WsLocationConstants#LOC_INSTALL_PARENT_DIR
     */
    final protected SymbolicRootResource installParentRoot;

    /**
     * Location of liberty instance; usually a child of the install root
     * (e.g. wlp/usr).
     *
     * @see WsLocationConstants#LOC_INSTANCE_DIR
     */
    final protected SymbolicRootResource userRoot;

    /**
     * Root directory of local repository (e.g. wlp/usr/extension).
     *
     * @see WsLocationConstants#LOC_USER_EXTENSION_DIR
     */
    final protected SymbolicRootResource usrExtensionRoot;

    /**
     * Location of active/current server configuration (e.g. wlp/usr/servers/serverName).
     *
     * @see WsLocationConstants#LOC_SERVER_DIR
     */
    final protected SymbolicRootResource serverConfigDir;

    /**
     * Location of active/current server output (e.g. wlp/usr/servers/serverName).
     *
     * @see WsLocationConstants#LOC_SERVER_DIR
     */
    final protected SymbolicRootResource serverOutputDir;

    /**
     * Location of bootstrap library; always set to location of launching
     * jar/class.
     */
    final protected InternalWsResource bootstrapLib;

    /**
     * Location of active/current server workarea; ALWAYS a child of the server
     * directory (e.g. wlp/usr/servers/serverName/workarea).
     */
    final protected InternalWsResource serverWorkarea;

    /**
     * Location of the current servers state directory. Always a child of the server output directory.
     */
    final protected InternalWsResource serverState;

    /**
     * Root directory of local repository (e.g. wlp/usr/shared/apps).
     *
     * @see WsLocationConstants#LOC_SHARED_APPS_DIR
     */
    final protected SymbolicRootResource sharedAppsRoot;

    /**
     * Root directory of local repository (e.g. wlp/usr/shared/config).
     *
     * @see WsLocationConstants#LOC_SHARED_CONFIG_DIR
     */
    final protected SymbolicRootResource sharedConfigRoot;

    /**
     * Root directory of local repository (e.g. wlp/usr/shared/resources).
     *
     * @see WsLocationConstants#LOC_SHARED_RESC_DIR
     */
    final protected SymbolicRootResource sharedResourceRoot;

    /**
     * Temp directory
     *
     * @see WsLocationConstants#LOC_TMP_DIR
     */
    final protected SymbolicRootResource tmpRoot;

    /** Name of server instance */
    final protected String serverName;

    final protected List<File> resourcePaths = new ArrayList<File>(3);
    final protected HashMap<String, List<File>> resourceGroups = new HashMap<String, List<File>>();

    final protected VirtualRootResource commonRoot;

    /**
     * Initialize file locations based on provided initial properties (which
     * includes some set in response to command line argument parsing).
     *
     * @param config
     *            Map containing location service configuration information
     * @throws IllegalArgumentException
     *             if serverName, instanceRootStr, or bootstrapLibStr are empty or
     *             null
     * @throws IllegalStateException
     *             if bootstrap library location or instance root don't exist.
     */
    protected WsLocationAdminImpl(Map<String, Object> config) {
        String userRootStr = (String) config.get(WsLocationConstants.LOC_USER_DIR);
        String serverCfgDirStr = (String) config.get(WsLocationConstants.LOC_SERVER_CONFIG_DIR);
        String serverOutDirStr = (String) config.get(WsLocationConstants.LOC_SERVER_OUTPUT_DIR);
        String bootstrapLibStr = (String) config.get(LOC_INTERNAL_LIB_DIR);
        String workareaDirStr = (String) config.get(LOC_INTERNAL_WORKAREA_DIR);

        String processType = (String) config.get(WsLocationConstants.LOC_PROCESS_TYPE);
        boolean isClient = (WsLocationConstants.LOC_PROCESS_TYPE_CLIENT.equals(processType));

        serverName = (String) config.get(WsLocationConstants.LOC_SERVER_NAME);
        if (serverName == null || serverName.length() == 0)
            throwInitializationException(new IllegalStateException("The server name must be specified"));

        if (userRootStr == null || userRootStr.length() == 0)
            throwInitializationException(new IllegalStateException("The location of the server instance must be specified"));

        if (bootstrapLibStr == null || bootstrapLibStr.length() == 0)
            throwInitializationException(new IllegalStateException("The location of bootstrap libraries must be specified"));

        commonRoot = new VirtualRootResource();

        // bootstrap file -- the lib dir containing the Launcher..
        File bootstrapFile = new File(bootstrapLibStr);

        // the usr dir -- must exist
        userRoot = new SymbolicRootResource(userRootStr, WsLocationConstants.LOC_USER_DIR, commonRoot);
        String bootstrapFileParentDir = bootstrapFile.getParent();
        installRoot = new SymbolicRootResource(bootstrapFileParentDir, WsLocationConstants.LOC_INSTALL_DIR, commonRoot);

        File installFile = new File(bootstrapFileParentDir);
        String installFileParentDir = installFile.getParent();
        installParentRoot = new SymbolicRootResource(installFileParentDir, WsLocationConstants.LOC_INSTALL_PARENT_DIR, commonRoot);

        // bootstrapLib could be under installRoot or localRepoRoot
        bootstrapLib = resolveResource(bootstrapFile.getAbsolutePath() + File.separatorChar);

        if (!bootstrapFile.exists() || !userRoot.exists())
            throwInitializationException(new IllegalStateException("The locations for the bootstrap libraries and the server instance must exist"));

        usrExtensionRoot = new SymbolicRootResource(userRoot.getNormalizedPath() + "/" + LOC_AREA_NAME_EXTENSION, WsLocationConstants.LOC_USER_EXTENSION_DIR, commonRoot);

        String serversRootStr = userRoot.getNormalizedPath() + "/" + (isClient ? LOC_AREA_NAME_CLIENTS : LOC_AREA_NAME_SERVERS);

        if (serverCfgDirStr == null)
            serverCfgDirStr = serversRootStr + "/" + serverName;

        serverConfigDir = new SymbolicRootResource(serverCfgDirStr, WsLocationConstants.LOC_SERVER_CONFIG_DIR, commonRoot);

        if (serverOutDirStr == null || serverOutDirStr.equals(serverCfgDirStr)) {
            serverOutputDir = serverConfigDir;
            // add the config dir using the output dir variable as well
            SymbolRegistry.getRegistry().addRootSymbol(WsLocationConstants.LOC_SERVER_OUTPUT_DIR, serverConfigDir);
        } else {
            serverOutputDir = new SymbolicRootResource(serverOutDirStr, WsLocationConstants.LOC_SERVER_OUTPUT_DIR, commonRoot);
        }

        // Set the process type, either client or server
        SymbolRegistry.getRegistry().addStringSymbol(WsLocationConstants.LOC_PROCESS_TYPE, processType);

        // Workarea is a child of the server output directory
        if (workareaDirStr == null)
            workareaDirStr = LOC_AREA_NAME_WORKING;
        serverWorkarea = serverOutputDir.createDescendantResource(workareaDirStr);
        SymbolRegistry.getRegistry().addResourceSymbol(WsLocationConstants.LOC_SERVER_WORKAREA_DIR, serverWorkarea);

        // state dir is a child of the server output directory
        serverState = serverOutputDir.createDescendantResource(LOC_AREA_NAME_STATE);
        SymbolRegistry.getRegistry().addResourceSymbol(WsLocationConstants.LOC_SERVER_STATE_DIR, serverState);

        String sharedAppsStr = userRoot.getNormalizedPath() + LOC_AREA_NAME_APPS;
        sharedAppsRoot = new SymbolicRootResource(sharedAppsStr, WsLocationConstants.LOC_SHARED_APPS_DIR, commonRoot);

        String sharedConfigStr = userRoot.getNormalizedPath() + LOC_AREA_NAME_CONFIG;
        sharedConfigRoot = new SymbolicRootResource(sharedConfigStr, WsLocationConstants.LOC_SHARED_CONFIG_DIR, commonRoot);

        String sharedResourceStr = userRoot.getNormalizedPath() + LOC_AREA_NAME_RESC;
        sharedResourceRoot = new SymbolicRootResource(sharedResourceStr, WsLocationConstants.LOC_SHARED_RESC_DIR, commonRoot);

        // Find/resolve temp directory
        final String dirProp = PathUtils.normalize(System.getProperty("java.io.tmpdir") + '/');
        tmpRoot = new SymbolicRootResource(dirProp, "tmp", commonRoot);
        if (!tmpRoot.exists()) {
            if (!tmpRoot.create()) {
                Tr.error(tc, "missingTmpDir", tmpRoot.getNormalizedPath());
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Created tmp directory", tmpRoot.getNormalizedPath());
            }
        }
        if (tmpRoot.exists() && !PathUtils.pathIsAbsolute(dirProp) && tc.isAuditEnabled())
            Tr.audit(tc, "relativeTmpDir", tmpRoot.getNormalizedPath());

        // Register the server name as a variable
        SymbolRegistry.getRegistry().addStringSymbol(WsLocationConstants.LOC_SERVER_NAME, serverName);

        // Register the server UUID as a variable
        SymbolRegistry.getRegistry().addStringSymbol(WsLocationConstants.LOC_SERVER_UUID, getServerId().toString());

        addResourcePath(bootstrapLib.getNormalizedPath());

        SymbolRegistry.getRegistry().addStringSymbol(WsLocationConstants.LOC_SERVICE_BINDING_ROOT, (String) config.get(WsLocationConstants.LOC_SERVICE_BINDING_ROOT));
    }

    private final void throwInitializationException(RuntimeException t) {
        if (t != null) {
            Tr.error(tc, "locationInitializationError", t.getMessage());
            throw t;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getBundleFile(Object caller, String relativeBundlePath) {
        final String mockBundlePrivate = "bundle-";

        if (caller == null)
            throw new NullPointerException("Caller to getBundleFile can not be null (path=" + relativeBundlePath + ")");

        String filePath = PathUtils.normalizeDescendentPath(relativeBundlePath);

        File bundleFile = null;

        // First, get Bundle used to load the caller
        Bundle b = FrameworkUtil.getBundle(caller.getClass());
        BundleContext bc = null;
        long id = 0;

        if (b != null) {
            // can return null
            bc = b.getBundleContext();
            id = b.getBundleId();
            if (bc != null) {
                bundleFile = bc.getDataFile(filePath);
            } else if (tc.isDebugEnabled()) {
                Tr.debug(tc, "BundleContext is null -- the bundle that loaded the class is in an invalid state", b);
            }
        }

        if (bundleFile == null) {
            // Unable to create a file in the bundle's private data area, potentially
            // because we aren't running in a framework at all, or the bundle couldn't
            // be found for the file, or because the bundle was in an invalid state.
            String dir = mockBundlePrivate + id + "/" + filePath;
            bundleFile = new File(serverWorkarea.getNormalizedPath(), dir);
        }

        return bundleFile;
    }

    /**
     * Lazy-loads a singleton that uniquely identifies the local server
     */
    private static final class ServerIdHolder {

        static final UUID SERVER_ID = getServerId();

        /**
         * <p>
         * Generates and caches an ID that uniquely identifies this server.
         * In an OSGi environment, this ID will not change until the server's work area is deleted.
         * </p>
         * <p>
         * The ID is cached inside a file named 'serverId' within the work area for this bundle.<ul>
         * <li>If this method is called when no such file exists, a new file will be created and populated with a random UUID.</li>
         * <li>If this method is called when the file does exist, the UUID will be read directly from the file.</li>
         * <li>If this method is called from a non-OSGi environment, or if the BundleContext can't be found, a random UUID is always returned.</li>
         * </ul>
         * </p>
         *
         * @return an ID that uniquely identifies this server
         * @see java.util.UUID#randomUUID()
         */
        private static UUID getServerId() {
            UUID id = readOrWriteId();
            if (id == null) {
                id = UUID.randomUUID();
            }
            return id;
        }

        /**
         * Reads a UUID from the serverId file. If the file does not exist,
         * generates a random UUID, writes it to the file, and returns the result.
         *
         * @return the UUID from the serverId file, or null if:<ol>
         *         <li>running in a non-osgi env (<code>FrameworkUtil.getBundle(Cache.class)==null</code>)</li>
         *         <li>the bundle has no context (<code>bundle.getBundleContext()==null</code>)</li>
         *         <li>bundleContext is no longer valid (<code>bundleContext.getDataFile("serverId") throws IllegalStateException</code>)</li>
         *         <li>the platform does not have file system support (<code>bundleContext.getDataFile("serverId")==null</code>)</li>
         *         <li>the file can't be read or contains an invalid UUID</li>
         *         </ol>
         */
        private static UUID readOrWriteId() {
            final Bundle bundle = FrameworkUtil.getBundle(ServerIdHolder.class);
            if (bundle == null) {
                return null;
            }
            return AccessController.doPrivileged(new PrivilegedAction<UUID>() {
                @Override
                public UUID run() {
                    BundleContext bundleContext = bundle.getBundleContext();
                    if (bundleContext == null) {
                        return null;
                    }
                    File file = null;
                    try {
                        file = bundleContext.getDataFile("serverId");
                    } catch (IllegalStateException e) {
                        // bundleContext is no longer valid ... log FFDC
                    }
                    if (file == null) {
                        return null;
                    }
                    if (file.exists()) {
                        // the ID was generated in a previous server cycle
                        return readId(file);
                    }
                    // the ID has not been generated yet
                    UUID id = UUID.randomUUID();
                    writeId(file, id);
                    return id;
                }
            });
        }

        private static UUID readId(File file) {
            UUID result = null;
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                String id = in.readLine();
                if (id != null) {
                    result = UUID.fromString(id);
                }
            } catch (IOException e) {
                // failed to read the file ... log FFDC
            } catch (IllegalArgumentException e) {
                // the contents of the file is not a UUID ... log FFDC
            } finally {
                tryToClose(in);
            }
            return result;
        }

        private static void writeId(File file, UUID id) {
            String idString = id.toString();
            BufferedWriter out = null;
            try {
                // if parent doesn't exist and can't be created, return early
                if (!FileUtils.ensureDirExists(file.getParentFile())) {
                    return;
                }
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8));
                out.write(idString);
            } catch (IOException e) {
                // failed to write the id to the file ... log FFDC
                tryToClose(out); // can't delete open files on Windows
                out = null; // don't tryToClose in finally
                boolean deleted = file.delete(); // if we fail to write, don't leave a half-written file
                if (!deleted && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to delete a file after a write operation failed", file);
                }
            } finally {
                tryToClose(out);
            }
        }

        private static void tryToClose(Closeable stream) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // failed to close the stream ... log FFDC
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getServerId() {
        return ServerIdHolder.SERVER_ID; // Initialization on Demand Holder (IODH) idiom
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerName() {
        return serverName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InternalWsResource getServerResource(String relativeServerPath) {
        return serverConfigDir.createDescendantResource(relativeServerPath);
    }

    @Override
    public InternalWsResource getServerOutputResource(String relativeServerPath) {
        return serverOutputDir.createDescendantResource(relativeServerPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InternalWsResource getServerWorkareaResource(String relativeServerWorkareaPath) {
        String filePath = PathUtils.normalizeDescendentPath(relativeServerWorkareaPath);

        String repPath = serverWorkarea.toRepositoryPath();
        if (repPath != null)
            repPath += filePath;

        return LocalFileResource.newResourceFromResource(serverWorkarea.getNormalizedPath() + filePath, repPath, serverWorkarea);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String printLocations(boolean formatOutput) {
        StringBuilder sb = new StringBuilder();

        if (formatOutput) {
            java.util.Formatter f = new java.util.Formatter();
            f.format("%26s:  %s%n", "Install root", installRoot.getNormalizedPath());
            f.format("%26s:  %s%n", "System libraries", bootstrapLib.getNormalizedPath());
            f.format("%26s:  %s%n", "User root", userRoot.getNormalizedPath());
            f.format("%26s:  %s%n", "Server config", serverConfigDir.getNormalizedPath());
            f.format("%26s:  %s%n", "Server output", serverOutputDir.getNormalizedPath());

            sb.append(f.toString());
        } else {
            sb.append("installRoot=").append(installRoot.getNormalizedPath()).append(",");
            sb.append("bootstrapLib=").append(bootstrapLib.getNormalizedPath()).append(",");
            sb.append("userRoot=").append(userRoot.getNormalizedPath()).append(",");
            sb.append("serverConfigDir=").append(serverConfigDir.getNormalizedPath()).append(",");
            sb.append("serverOutputDir=").append(serverOutputDir.getNormalizedPath()).append(",");
        }

        return sb.toString();
    }

    /**
     * @return Dictionary containing attributes for the registration of the
     *         WsLocationAdmin service in the OSGi service registry.
     */
    public Dictionary<String, ?> getServiceProps() {
        Hashtable<String, Object> d = new Hashtable<String, Object>();

        d.put(WsLocationConstants.LOC_INSTALL_DIR, installRoot.getNormalizedPath());
        d.put(WsLocationConstants.LOC_USER_DIR, userRoot.getNormalizedPath());
        d.put(WsLocationConstants.LOC_SERVER_CONFIG_DIR, serverConfigDir.getNormalizedPath());
        d.put(WsLocationConstants.LOC_SERVER_OUTPUT_DIR, serverOutputDir.getNormalizedPath());
        d.put(WsLocationConstants.LOC_SHARED_APPS_DIR, sharedAppsRoot.getNormalizedPath());
        d.put(WsLocationConstants.LOC_SHARED_CONFIG_DIR, sharedConfigRoot.getNormalizedPath());
        d.put(WsLocationConstants.LOC_SHARED_RESC_DIR, sharedResourceRoot.getNormalizedPath());
        d.put(LOC_INTERNAL_LIB_DIR, bootstrapLib.getNormalizedPath());
        d.put(WsLocationConstants.LOC_SERVER_NAME, serverName);
        d.put(WsLocationConstants.LOC_TMP_DIR, tmpRoot.getNormalizedPath());
        d.put("service.vendor", "IBM");

        return d;
    }

    protected void addResourcePath(String path) {
        // check for symbols (implies pre-resolved resource)
        if (PathUtils.containsSymbol(path)) {
            WsResource ar = SymbolRegistry.getRegistry().resolveSymbolicResource(path);
            path = ar.toExternalURI().getPath();
        } else {
            path = PathUtils.normalize(new File(path).getAbsolutePath());
        }

        File dir = new File(path);
        if (dir.exists()) {
            List<File> group = new ArrayList<File>();
            group.add(new File(dir, "features"));
            resourceGroups.put("feature", group);
            resourcePaths.add(dir);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<WsResource> matchResource(String resourceGroupName, String resourceRegex, int limit) {
        if (resourceGroupName == null)
            throw new NullPointerException("Resource group required");

        List<File> path = null;
        path = resourceGroups.get(resourceGroupName);

        if (path == null) {
            String resolveName = "${" + resourceGroupName + "}";
            String resourceListNames = SymbolRegistry.getRegistry().resolveSymbolicString(resolveName);
            if (resourceListNames != null && (resourceListNames.equals(resolveName) == false)) {
                String[] names = resourceListNames.split("\\s*,\\s*");
                path = new ArrayList<File>();
                for (String filePath : names) {
                    path.add(new File(filePath));
                }
                resourceGroups.put(resourceGroupName, path);
            } else {
                path = resourcePaths;
            }
        }

        List<File> matches = Collections.emptyList();

        if (path.size() > 1) {
            matches = new ArrayList<File>();
            for (File dir : path) {
                matches.addAll(FileLocator.getMatchingFiles(dir, resourceRegex));
            }
        } else if (path.size() == 1) {
            matches = FileLocator.getMatchingFiles(path.get(0), resourceRegex);
        }

        return new IteratorWrapper(matches.iterator(), bootstrapLib, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InternalWsResource resolveResource(String resourceURI) {
        if (resourceURI == null || resourceURI.length() == 0)
            return null;

        String normalPath = PathUtils.normalize(resourceURI);

        if (PathUtils.containsSymbol(normalPath)) {
            return SymbolRegistry.getRegistry().resolveSymbolicResource(normalPath);
        }

        // *nix absolute path: /something -- implies file
        if (normalPath.length() >= 1 && normalPath.charAt(0) == '/') {
            SymbolicRootResource root = resolveRoot(normalPath);
            return LocalFileResource.newResource(normalPath, null, root);
        }

        // Absolute windows pathname: c:/something -- implies file
        if (normalPath.length() > 2 && normalPath.charAt(1) == ':') {
            SymbolicRootResource root = resolveRoot(normalPath);
            return LocalFileResource.newResource(normalPath, null, root);
        }

        // resolve relative paths against server root
        if (!PathUtils.pathIsAbsolute(normalPath))
            return serverConfigDir.createDescendantResource(normalPath);

        // Use URI to determine type of resource
        URI uri = null;

        // check for valid URI
        try {
            // try straight construction of a URI (will fail on spaces, illegal
            // characters, etc)
            uri = new URI(normalPath);
        } catch (URISyntaxException e) {
            try {
                // try constructing a no-scheme URI based on the given string
                // (relative path version)
                uri = new URI(null, null, normalPath, null);
            } catch (URISyntaxException e1) {
                MalformedLocationException e3;
                e3 = new MalformedLocationException("Could not construct URI to resolve resource (path=" + normalPath + ")");
                e3.initCause(e1); // report the original problem with the URI string
                throw e3;
            }
        }

        return resolveResource(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveString(String resourceURI) {
        // This method expands all symbols that the location service/symbol registry
        // knows about. It is not guaranteed to resolve a variable with known
        // symbols only in the middle to any given location
        if (resourceURI == null || resourceURI.length() == 0)
            return null;

        final String normalPath;

        if (PathUtils.containsSymbol(resourceURI)) {
            normalPath = SymbolRegistry.getRegistry().resolveSymbolicString(resourceURI);
        } else {
            normalPath = PathUtils.normalize(resourceURI);
        }

        // *nix absolute path: /something -- implies file
        if (normalPath.length() >= 1 && normalPath.charAt(0) == '/') {
            // Commenting out for now since an error will not be thrown from resolveRoot
            //        resolveRoot(normalPath); // Double check accessibility
            return normalPath;
        }

        // Absolute windows pathname: c:/something -- implies file
        if (normalPath.length() > 2 && normalPath.charAt(1) == ':') {
            // Commenting out for now since an error will not be thrown from resolveRoot
            //         resolveRoot(normalPath); // Double check accessibility
            return normalPath;
        }

        // NO URI Processing for this path: just fill in known symbols

        return normalPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InternalWsResource resolveResource(URI resourceURI) {
        if (resourceURI == null)
            return null;

        String scheme = resourceURI.getScheme();
        if (scheme == null || scheme.equals("file")) {
            // make sure to normalize the resourceURI to ensure consistent path
            // construction
            // i.e. no leading slash for windows absolute path, etc.
            String normalPath = PathUtils.normalize(resourceURI.getPath());
            SymbolicRootResource root = resolveRoot(normalPath);
            return LocalFileResource.newResource(normalPath, null, root);
        } else {
            try {
                return new RemoteResource(resourceURI.toURL());
            } catch (MalformedURLException e) {
                return null;
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InternalWsResource asResource(File file, boolean isFile) {
        if (file == null)
            return null;

        String normalPath = PathUtils.normalize(file.getAbsolutePath());
        if (!isFile)
            normalPath += "/";

        SymbolicRootResource root = resolveRoot(normalPath);
        return LocalFileResource.newResource(normalPath, null, root);
    }

    SymbolicRootResource resolveRoot(String normalPath) {
        return SymbolRegistry.getRegistry().findRoot(normalPath);
    }

    private static class BundleContextMap implements Map<String, Object> {
        private final BundleContext wrappedContext;

        BundleContextMap(final BundleContext ctx) {
            wrappedContext = ctx;
        }

        /** {@inheritDoc} **/
        @Override
        public void clear() {
            throw new UnsupportedOperationException("Can not clear attributes of bundle context");
        }

        /** {@inheritDoc} **/
        @Override
        public boolean containsKey(Object key) {
            return wrappedContext.getProperty((String) key) != null;
        }

        /** {@inheritDoc} **/
        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException("Can not iterate over bundle context property values");
        }

        /** {@inheritDoc} **/
        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet() {
            throw new UnsupportedOperationException("Can not obtain entry set for bundle context properties");
        }

        @Override
        public Object get(Object key) {
            return wrappedContext.getProperty((String) key);
        }

        /** {@inheritDoc} **/
        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException("Can not count bundle context properties");
        }

        /** {@inheritDoc} **/
        @Override
        public Set<String> keySet() {
            throw new UnsupportedOperationException("Can not obtain set of bundle context property keys");
        }

        @Override
        public Object put(String key, Object value) {
            throw new UnsupportedOperationException("Can not clear attributes of bundle context");
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> map) {
            throw new UnsupportedOperationException("Can not clear attributes of bundle context");
        }

        /** {@inheritDoc} **/
        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException("Can not clear attributes of bundle context");
        }

        /** {@inheritDoc} **/
        @Override
        public int size() {
            throw new UnsupportedOperationException("Can not count bundle context properties");
        }

        /** {@inheritDoc} **/
        @Override
        public Collection<Object> values() {
            throw new UnsupportedOperationException("Can not create collection of bundle context property values");
        }
    }

    protected static final class IteratorWrapper implements Iterator<WsResource> {

        private final Iterator<File> fileIterator;
        private final InternalWsResource related;
        private final int limit;
        private int index;

        IteratorWrapper(Iterator<File> fileIterator, InternalWsResource related, int limit) {
            this.fileIterator = fileIterator;
            this.related = related;
            this.limit = limit == 0 ? Integer.MAX_VALUE : limit;
            this.index = 0;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return index <= limit && fileIterator.hasNext();
        }

        /** {@inheritDoc} */
        @Override
        public WsResource next() {
            index++;
            File f = fileIterator.next();
            final String normalPath = PathUtils.normalize(f.getAbsolutePath());
            return LocalFileResource.newResourceFromResource(normalPath, null, related);
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /** {@inheritDoc} */
    @Override
    public WsResource getRuntimeResource(String relativeRuntimePath) {
        return installRoot.resolveRelative(relativeRuntimePath);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.kernel.service.location.WsLocationAdmin#addLocation(java.lang.String, java.lang.String)
     */
    @Override
    public WsResource addLocation(String fileName, String symbolicName) {
        return new SymbolicRootResource(fileName, symbolicName, commonRoot);
    }
}
