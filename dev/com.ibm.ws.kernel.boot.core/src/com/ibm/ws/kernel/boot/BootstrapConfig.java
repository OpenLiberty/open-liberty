/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants.VerifyServer;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.ws.kernel.boot.internal.KernelResolver;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.boot.internal.PasswordGenerator;
import com.ibm.ws.kernel.boot.internal.ServerLock;

public class BootstrapConfig {
    /** ${} */
    final static Pattern SYMBOL_DEF = Pattern.compile("\\$\\{([^\\$\\{\\}]*?)\\}");

    /**
     * Location of bootstrap library; always set to location of launching
     * jar/class.
     */
    protected final File bootstrapLib;

    /**
     * Location of installation; usually the parent of bootstrapLib
     */
    protected File installRoot = null;

    /**
     * Location of instance; usually a child of the install root (e.g.
     * wlp/usr).
     */
    protected File userRoot = null;

    /**
     * Root directory containing configured servers or clients (e.g.
     * wlp/usr/servers, wlp/usr/clients).
     */
    protected File processesRoot = null;

    /**
     * Root directory containing working dirs for configured servers or clients (e.g.
     * wlp/usr/server-output, wlp/usr/client-output).
     */
    protected File outputRoot = null;

    /** Location of active/current server or client configuration (e.g. wlp/usr/servers/serverName, wlp/usr/clients/clientName). */
    protected File configDir = null;

    /** Location of active/current server or client output (e.g. wlp/usr/servers/serverName, wlp/usr/clients/clientName). */
    protected File outputDir = null;

    /** Location of active/current log output (e.g. wlp/usr/servers/serverName/logs). */
    protected File logDir = null;

    /**
     * Location of active/current log file (e.g., wlp/usr/servers/serverName/logs/console.log),
     * if the process was started using the server script.
     */
    protected File consoleLogFile;

    /**
     * Location of active/current server or client workarea; ALWAYS a child of the server/client OUTPUT
     * directory (e.g. wlp/usr/servers/serverName/workarea, wlp/usr/clients/clientName/workarea).
     */
    protected File workarea = null;

    protected String workareaDirStr = null;

    /** Name of server instance */
    protected String processName;

    /** Initial properties used to launch the platform/framework */
    protected Map<String, String> initProps;

    /** Command line arguments */
    protected List<String> cmdArgs;

    /**
     * Classloader created to launch the framework
     *
     * @see Launcher#buildClassLoader(List)
     */
    protected ClassLoader frameworkLaunchClassloader;

    protected Instrumentation instrumentation;

    /**
     * Kernel Bootstrap Resolver
     */
    protected KernelResolver kernelResolver;

    protected File serviceBindingRootDir = null;

    public BootstrapConfig() {
        File fbootstrapLib = null;
        try {
            fbootstrapLib = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<File>() {
                @Override
                public File run() throws Exception {
                    return KernelUtils.getBootstrapLibDir();
                }
            });
        } catch (Exception ex) {
        }

        bootstrapLib = fbootstrapLib;

    }

    /**
     * Light processing: find main locations
     *
     */
    protected void findLocations(BootstrapLocations locations) throws LocationException {

        // Server name only found via command line
        setProcessName(locations.getProcessName());

        // always use the parent of the lib dir as WLP_INSTALL_ROOT
        installRoot = bootstrapLib.getParentFile();

        // WLP_USER_DIR = /wlp/usr
        if (locations.getUserDir() == null)
            userRoot = new File(installRoot, BootstrapConstants.LOC_AREA_NAME_USR);
        else
            userRoot = assertDirectory(FileUtils.normalize(locations.getUserDir()), BootstrapConstants.ENV_WLP_USER_DIR);

        // /wlp/usr/servers
        processesRoot = new File(userRoot, getProcessesSubdirectory());
        // /wlp/usr/servers/serverName
        configDir = new File(processesRoot, processName);

        // Canonicalize server name for case-insensitive file systems.
        // UNLESS it is a symlink, in which case we just try to match case.
        String canonicalServerName = processName;
        try {
            // canonicalServerName = configDir.getCanonicalFile().getName();
            File parentDir = configDir.getParentFile();
            if (!isSymbolicLink(configDir, parentDir)) {
                canonicalServerName = configDir.getCanonicalFile().getName();
                if (!processName.equals(canonicalServerName)) {
                    processName = canonicalServerName;
                    // Recreate configDir (rather than using the result of
                    // getCanonicalFile above) to preserve symlinks.
                    configDir = new File(processesRoot, processName);
                }
            } else {
                // Find exact match, OR find case-variant if exact fails.
                File candidate = null;
                File[] siblings = parentDir.listFiles();
                File canonicalConfigDir = configDir.getCanonicalFile();
                for (int i = 0; i < siblings.length; ++i) {
                    File sibling = siblings[i];
                    if (!sibling.isDirectory())
                        continue;
                    String sibname = sibling.getCanonicalFile().getName();
                    if (sibname.equals(processName)) {
                        candidate = sibling;
                        break; // exact match exists, use as it stands
                    } else if (sibname.equalsIgnoreCase(processName)) {
                        if (sibling.getCanonicalFile().equals(canonicalConfigDir))
                            candidate = sibling; // Not exact match, but same file.
                        // Continue scanning in case exact match also exists.
                        // If several exist with varying case but nothing is exact...
                        // we currently take last-found; could  instead order lexically, or
                        // could tell the user to stop typing nonsense.
                    }
                }
                if (candidate != null) {
                    processName = candidate.getName();
                    // Recreate configDir (rather than using the result of
                    // getCanonicalFile above) to preserve symlinks.
                    configDir = new File(processesRoot, processName);
                }
            }
        } catch (IOException e) {
            // Ignore.
        }

        if (locations.getServerDir() == null) {
            outputRoot = processesRoot;
            outputDir = configDir;
        } else {
            // separate output dir, WLP_OUTPUT_DIR
            outputRoot = assertDirectory(FileUtils.normalize(locations.getServerDir()), getOutputDirectoryEnvName());
            outputDir = new File(outputRoot, processName);
        }

        // Logs could be redirected to a place other than the server output dir (like /var/log.. )
        if (locations.getLogDir() == null)
            logDir = new File(outputDir, BootstrapConstants.LOC_AREA_NAME_LOGS);
        else
            logDir = assertDirectory(FileUtils.normalize(locations.getLogDir()), BootstrapConstants.ENV_LOG_DIR);
        consoleLogFile = new File(logDir, locations.getConsoleLogFile() != null ? locations.getConsoleLogFile() : BootstrapConstants.CONSOLE_LOG);

        // Server workarea always a child of outputDir
        if (locations.getWorkAreaDir() == null)
            this.workareaDirStr = BootstrapConstants.LOC_AREA_NAME_WORKING;
        else
            this.workareaDirStr = BootstrapConstants.LOC_AREA_NAME_WORKING + "/" + locations.getWorkAreaDir();
        workarea = new File(outputDir, this.workareaDirStr);

        String serviceBindingRootStr = locations.getServiceBindingRoot();
        if (serviceBindingRootStr == null) {
            this.serviceBindingRootDir = new File(configDir, "bindings");
        } else {
            this.serviceBindingRootDir = new File(serviceBindingRootStr);
        }
    }

    /**
     * Given a file and it's parent file, determine if the file is a symbolic
     * link. This will not check for a symbolic anywhere in the file's path,
     * only the explicit file referred to.
     *
     * Swiped from PathUtils, which isn't currently exposed to BootstrapConfig
     *
     * @param file file to check if is a symbolic link
     * @param parentFile parent of the file to check
     * @return whether the given file refers to a symbolic link
     *
     * @throws PrivilegedActionException
     * @throws IOException
     */
    static boolean isSymbolicLink(final File file, File parentFile) throws IOException {
        File canonicalParentDir = parentFile.getCanonicalFile();
        File fileInCanonicalParentDir = new File(canonicalParentDir, file.getName());
        File canonicalFile = fileInCanonicalParentDir.getCanonicalFile();

        return !canonicalFile.equals(fileInCanonicalParentDir.getAbsoluteFile());
    }

    /**
     * Return the root directory name of the processes.
     *
     * @return BootstrapConstants.LOC_AREA_NAME_SERVERS
     */
    protected String getProcessesSubdirectory() {
        return BootstrapConstants.LOC_AREA_NAME_SERVERS;
    }

    /**
     * Return the output directory name value set in the environment variable WLP_OUTPUT_DIR.
     *
     * @return BootstrapConstants.ENV_WLP_OUTPUT_DIR
     */
    protected String getOutputDirectoryEnvName() {
        return BootstrapConstants.ENV_WLP_OUTPUT_DIR;
    }

    /**
     * Use command line arguments and provided launch properties (i.e. system
     * properties) to determine configured bootstrap locations.
     * <p>
     * (bottom) bootstrap.properties file <- command line args <- system properties (top)
     * <p>
     * This is package protected: this is not intended to be called by anything other
     * than the Launcher.
     *
     * @param initProps
     *            Initial set of properties we're working with, contains some
     *            properties populated by command line parser
     * @param instanceDirStr Value of WLP_USER_DIR environment variable
     * @param outputDirStr Value of WLP_OUTPUT_DIR environment variable
     * @param logDirStr Value of X_LOG_DIR or LOG_DIR environment variable
     *
     * @throws LocationException
     */
    protected void configure(Map<String, String> initProps) throws LocationException {
        if (initProps == null)
            throw new IllegalArgumentException("Initial properties can not be null");

        this.initProps = initProps;

        // Set the process type. e.g., server or client
        initProps.put(BootstrapConstants.LOC_PROPERTY_PROCESS_TYPE, getProcessType());

        // Update for setSystemProperties.
        initProps.put(BootstrapConstants.INTERNAL_SERVER_NAME, processName);

        // Check for / read bootstrap props
        File f = getConfigFile(BootstrapConstants.BOOTSTRAP_PROPERTIES);
        if (f.exists()) {
            mergeProperties(initProps, null, f.toURI().toString());
        }

        boolean userRootIsDefault = installRoot.equals(userRoot.getParentFile());

        // Set locations into initProps
        initProps.put(BootstrapConstants.LOC_PROPERTY_INSTALL_DIR, getPathProperty(installRoot));
        initProps.put(BootstrapConstants.LOC_PROPERTY_INSTANCE_DIR, getPathProperty(userRoot));
        initProps.put(BootstrapConstants.LOC_PROPERTY_INSTANCE_DIR_IS_DEFAULT, Boolean.toString(userRootIsDefault));

        initProps.put(BootstrapConstants.LOC_INTERNAL_WORKAREA_DIR, workareaDirStr + "/");

        initProps.put(BootstrapConstants.LOC_PROPERTY_SRVCFG_DIR, getPathProperty(configDir));
        initProps.put(BootstrapConstants.LOC_PROPERTY_SRVOUT_DIR, getPathProperty(outputDir));
        initProps.put(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR, getPathProperty(outputDir,
                                                                                  workareaDirStr,
                                                                                  BootstrapConstants.LOC_AREA_NAME_TMP));

        if (BootstrapConstants.LOC_PROCESS_TYPE_CLIENT.equals(getProcessType())) {
            initProps.put(BootstrapConstants.LOC_PROPERTY_CLIENTCFG_DIR, getPathProperty(configDir));
            initProps.put(BootstrapConstants.LOC_PROPERTY_CLIENTOUT_DIR, getPathProperty(outputDir));
            initProps.put(BootstrapConstants.LOC_PROPERTY_CLIENTTMP_DIR, getPathProperty(outputDir,
                                                                                         workareaDirStr,
                                                                                         BootstrapConstants.LOC_AREA_NAME_TMP));
            initProps.put(BootstrapConstants.LOC_PROPERTY_CLIENTTMP_DIR, getPathProperty(workarea,
                                                                                         BootstrapConstants.LOC_AREA_NAME_TMP));
        }

        initProps.put(BootstrapConstants.LOC_INTERNAL_LIB_DIR, getPathProperty(bootstrapLib));
        initProps.put(BootstrapConstants.LOC_PROPERTY_SHARED_APP_DIR, getPathProperty(userRoot,
                                                                                      BootstrapConstants.LOC_AREA_NAME_SHARED,
                                                                                      BootstrapConstants.LOC_AREA_NAME_APP));
        initProps.put(BootstrapConstants.LOC_PROPERTY_SHARED_CONFIG_DIR, getPathProperty(userRoot,
                                                                                         BootstrapConstants.LOC_AREA_NAME_SHARED,
                                                                                         BootstrapConstants.LOC_AREA_NAME_CFG));
        initProps.put(BootstrapConstants.LOC_PROPERTY_SHARED_RES_DIR, getPathProperty(userRoot,
                                                                                      BootstrapConstants.LOC_AREA_NAME_SHARED,
                                                                                      BootstrapConstants.LOC_AREA_NAME_RES));

        initProps.put(BootstrapConstants.LOC_PROPERTY_SERVICE_BINDING_ROOT, getPathProperty(serviceBindingRootDir));
        // Wait to look for symbols until we have location properties set
        substituteSymbols(initProps);
    }

    protected String getPathProperty(File file, String... dirs) {
        StringBuilder b = new StringBuilder().append(FileUtils.normalize(file.getAbsolutePath())).append('/');
        for (String dir : dirs) {
            b.append(dir).append('/');
        }

        return b.toString();
    }

    /**
     * Ensure that the given directory either does not yet exists,
     * or exists as a directory.
     *
     * @param dirName
     *            Name/path to directory
     * @param locName
     *            Symbol/location associated with directory
     * @return File for directory location
     * @throws LocationException
     *             if dirName references an existing File (isFile).
     */
    protected File assertDirectory(String dirName, String locName) {
        File d = new File(dirName);
        if (d.isFile())
            throw new LocationException("Path must reference a directory", MessageFormat.format(BootstrapConstants.messages.getString("error.specifiedLocation"), locName,
                                                                                                d.getAbsolutePath()));
        return d;
    }

    /**
     * A process (server/client) name must be specified on the command line.
     * It must be looked for (and set as a member variable) before we calculate locations.
     *
     * This method looks at the provided process name, if it is not null it checks to make
     * sure the provided process name contains only valid characters. If the process name is
     * specified and uses valid characters, it will be used as the process name, if it contains
     * invalid characters, an exception will be thrown preventing the process from starting.
     *
     * If no process name was specified, "defaultServer" or "defaultClient" will be used.
     *
     * @param newProcessName A process name being used.
     */
    protected void setProcessName(String newProcessName) {
        if (newProcessName == null) {
            processName = getDefaultProcessName();
            return;
        } else if (!newProcessName.matches("[\\p{L}\\p{N}\\+\\_][\\p{L}\\p{N}\\-\\+\\.\\_]*")) {
            // Translation of the above regex for the faint of heart:
            // The first character can be a +, an _, a unicode letter \p{L}, or a unicode number \\p{N}
            // Subsequent characters can be any of the above, in addition to a . and a -
            throw new LocationException("Bad server name: " + newProcessName, MessageFormat.format(BootstrapConstants.messages.getString(getErrorProcessNameCharacterMessageKey()),
                                                                                                   newProcessName));
        } else
            // use the new server name
            processName = newProcessName;
    }

    /**
     * Set configured attributes from bootstrap.properties as system properties.
     * <p>
     * This is a separate step from config to ensure it is called once (by the launcher),
     * rather than every/any time configure is called (could be more than once, some future
     * nested environment.. who knows?).
     */
    public void setSystemProperties() {
        // copy all other initial properties to system properties

        for (Map.Entry<String, String> entry : initProps.entrySet()) {
            if (!entry.getKey().equals("websphere.java.security")) {
                final String key = entry.getKey();
                final String value = entry.getValue();
                try {
                    AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Void>() {
                        @Override
                        public Void run() throws Exception {
                            System.setProperty(key, value);
                            return null;
                        }
                    });
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
     * Get value from initial configuration properties. If property is not
     * present in initial/framework properties, try finding it in system
     * properties.
     *
     * @param key
     *            Property key
     * @return Object value, or null if not found.
     */
    public String get(final String key) {
        if (key == null || initProps == null)
            return null;

        String value = initProps.get(key);
        if (value == null) {
            try {
                value = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws Exception {
                        return System.getProperty(key);
                    }
                });
            } catch (Exception ex) {
            }
        }

        return value;
    }

    /**
     * Set new property into set of initial properties. No effect (and returns
     * null) if key is null.
     *
     * @param key
     *            Property key string
     * @param value
     *            Property value object
     * @return current/replaced value
     */
    public String put(final String key, String value) {
        if (key == null || initProps == null)
            return null;

        return initProps.put(key, value);
    }

    /**
     * Set a new property into the set of initial properties only if the
     * key does not already have an existing value.
     *
     * @param key the key to set
     * @param value the value to set
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     */
    public String putIfAbsent(String key, String value) {
        String current = get(key);
        return current == null ? put(key, value) : current;
    }

    /**
     * Clear property
     *
     * @param key
     *            Key of property to clear
     * @return current/removed value
     */
    public String remove(final String key) {
        if (key == null)
            return null;

        return initProps.remove(key);
    }

    /**
     * @param useLineBreaks
     *            If true, line breaks will be used when displaying
     *            configured locations; locations will otherwise be separated by
     *            commas.
     * @return Display string describing configured bootstrap locations.
     */
    public String printLocations(boolean formatOutput) {
        if (!isConfigured())
            return super.toString() + " (not configured)";

        StringBuilder sb = new StringBuilder();

        if (formatOutput) {
            java.util.Formatter f = new java.util.Formatter();

            String value = null;
            if (value == null) {
                try {
                    value = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<String>() {
                        @Override
                        public String run() throws Exception {
                            return System.getenv("X_CMD");
                        }
                    });
                } catch (Exception ex) {
                }
            }

            String cmd = value;
            //String cmd = System.getenv("X_CMD");
            if (cmd != null)
                f.format("%26s:  %s%n", "Command", cmd);

            String java_home_value = null;
            try {
                java_home_value = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws Exception {
                        return System.getProperty("java.home");
                    }
                });
            } catch (Exception ex) {
            }

            f.format("%26s:  %s%n", "Java home", java_home_value);
            f.format("%26s:  %s%n", "Install root", getPathProperty(installRoot));
            f.format("%26s:  %s%n", "System libraries", getPathProperty(bootstrapLib));
            f.format("%26s:  %s%n", "User root", getPathProperty(userRoot));
            f.format("%26s:  %s%n", "Config", getPathProperty(configDir));
            f.format("%26s:  %s%n", "Output", getPathProperty(outputDir));
            sb.append(f.toString());
            f.close();
        } else {
            sb.append("installRoot=").append(getPathProperty(installRoot)).append(",");
            sb.append("bootstrapLib=").append(getPathProperty(bootstrapLib)).append(",");
            sb.append("instanceRoot=").append(getPathProperty(userRoot)).append(",");
            sb.append("configDir=").append(getPathProperty(configDir)).append(",");
            sb.append("outputDir=").append(getPathProperty(outputDir)).append(",");
        }

        return sb.toString();
    }

    public boolean isConfigured() {
        return (userRoot != null);
    }

    /**
     * @return server name
     */
    public String getProcessName() {
        if (processName == null)
            processName = getDefaultProcessName();
        return processName;
    }

    /**
     * @return log directory
     */
    public File getLogDirectory() {
        return logDir;
    }

    /**
     * @return console log file
     */
    public File getConsoleLogFile() {
        return consoleLogFile;
    }

    /**
     * Allocate a file in the user output directory, e.g.
     * WLP_OUTPUT_DIR/relativePath
     *
     * @param relativePath
     *            relative path of file to create in the WLP_OUTPUT_DIR directory
     * @return File object for relative path, or for the WLP_OUTPUT_DIR directory itself
     *         if the relative path argument is null
     */
    public File getUserOutputFile(String relativePath) {
        if (relativePath == null)
            return outputRoot;
        else
            return new File(outputRoot, relativePath);
    }

    /**
     * Allocate a file in the server config directory, e.g.
     * usr/servers/serverName/relativeServerPath
     *
     * @param relativeServerPath
     *            relative path of file to create in the server directory
     * @return File object for relative path, or for the server directory itself
     *         if the relative path argument is null
     */
    public File getConfigFile(String relativeServerPath) {
        if (relativeServerPath == null)
            return configDir;
        else
            return new File(configDir, relativeServerPath);
    }

    /**
     * Allocate a file in the server output directory, e.g.
     * server-data/serverName/relativeServerPath
     *
     * @param relativeServerPath
     *            relative path of file to create in the server directory
     * @return File object for relative path, or for the server directory itself
     *         if the relative path argument is null
     */
    public File getOutputFile(String relativeServerPath) {
        if (relativeServerPath == null)
            return outputDir;
        else
            return new File(outputDir, relativeServerPath);
    }

    /**
     * Allocate a file in the server directory, e.g.
     * usr/servers/serverName/workarea/relativeServerWorkareaPath
     *
     * @param relativeServerWorkareaPath
     *            relative path of file to create in the server's workarea
     * @return File object for relative path, or for the server workarea itself if
     *         the relative path argument is null
     */
    public File getWorkareaFile(String relativeServerWorkareaPath) {
        if (relativeServerWorkareaPath == null)
            return workarea;
        else
            return new File(workarea, relativeServerWorkareaPath);
    }

    /**
     * Merge properties from resource specified by urlStr (which is resolved
     * against the given
     * baseURL, in the case of relative paths) into the target map.
     *
     * @param target
     *            Target map to populate with new properties
     * @param baseURL
     *            Base location used for resolving relative paths
     * @param urlStr
     *            URL string describing the properties resource to load
     * @param recurse
     *            Whether or not to follow any included bootstrap resources
     *            (bootstrap.includes).
     */
    protected void mergeProperties(Map<String, String> target, URL baseURL, String urlStr) {
        String includes = null;
        URL url;
        try {
            if (baseURL != null && urlStr == null)
                url = baseURL;
            else
                url = new URL(baseURL, urlStr);

            // Read properties from file then trim trailing white spaces
            Properties props = KernelUtils.getProperties(url.openStream());

            includes = (String) props.remove(BootstrapConstants.BOOTPROP_INCLUDE);

            // First value to be set wins. Add values in the current file before
            // looking at included files.
            addMissingProperties(props, target);

            if (includes != null)
                processIncludes(target, url, includes);
        } catch (MalformedURLException e) {
            Debug.printStackTrace(e);
            throw new LocationException("Bad bootstrap.properties URI: " + urlStr, MessageFormat.format(BootstrapConstants.messages.getString("error.bootPropsURI"), urlStr, e), e);
        } catch (IOException e) {
            throw new LocationException("IOException reading bootstrap.properties: " + urlStr, MessageFormat.format(BootstrapConstants.messages.getString("error.bootPropsStream"),
                                                                                                                    urlStr, e), e);
        }
    }

    /**
     * Add properties from source to target if the target map does not
     * already contain a property with that value. When a new attribute
     * is discovered, System properties are checked to see if an override
     * has been specified. If the property is present as a System property
     * (from the command line or jvm.options), that value is used instead
     * of bootstrap.properties.
     *
     * @param source The source properties file (bootstrap.properties or include)
     * @param target The target map
     */
    protected void addMissingProperties(Properties source, Map<String, String> target) {
        if (source == null || source.isEmpty() || target == null)
            return;

        // only add "new" properties (first value wins)
        for (String key : source.stringPropertyNames()) {
            if (!target.containsKey(key) && key.length() > 0) {
                // Check for a System property override first.
                String value = System.getProperty(key);
                if (value == null) {
                    value = source.getProperty(key);
                }
                // store the value in the target map
                target.put(key, value);
            }
        }
    }

    /**
     * Process included/referenced bootstrap properties. Properties resources
     * should be specified using a) relative path from the containing
     * bootstrap.properties,
     * b) absolute path, or c) full URI/URLs.
     *
     * @param mergeProps
     * @param includeProps
     */
    protected void processIncludes(Map<String, String> mergeProps, URL rootURL, String includeProps) {
        if (includeProps == null)
            return;

        String props[] = includeProps.trim().split("\\s*,\\s*");
        for (String pname : props) {
            mergeProperties(mergeProps, rootURL, pname);
        }
    }

    /**
     * Perform substitution of symbols used in config
     *
     * @param initProps
     */
    protected void substituteSymbols(Map<String, String> initProps) {
        for (Entry<String, String> entry : initProps.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                String strValue = (String) value;
                Matcher m = SYMBOL_DEF.matcher(strValue);
                int i = 0;
                while (m.find() && i++ < 4) {
                    String symbol = m.group(1);
                    Object expansion = initProps.get(symbol);
                    if (expansion != null && expansion instanceof String) {
                        strValue = strValue.replace(m.group(0), (String) expansion);
                        entry.setValue(strValue);
                    }
                }
            }
        }
    }

    /**
     * Look for symbols in the given string, if that symbol is present in
     * the map of initial properties (or in system properties), replace the
     * symbol with the initial property value.
     *
     * @param str
     *            String to evaluate for symbols
     * @return String with known symbols replaced by the associated values.
     * @see #get(String)
     */
    public String replaceSymbols(String str) {
        Matcher m = SYMBOL_DEF.matcher(str);
        if (m.find()) {
            String symbol = m.group(1);
            String expansion = get(symbol);
            if (expansion != null) {
                return str.replace(m.group(0), expansion);
            }
        }

        return str;
    }

    /**
     * If necessary, verify the existence of the server. If --create was
     * specified indicating a new server should be created, then verify that the
     * server does not already exist. Otherwise, verify that the server already
     * exists; if the server is 'defaultServer' and is being started, create it
     * if it doesn't already exist.
     *
     * If the server is created, default bootstrap.properties and server.xml files will be
     * created unless the files to use were specified explicitly on the command line.
     *
     * @param verifyServerString
     *            A value from the {@link BootstrapConstants.VerifyServer} enum: describes
     *            whether or not a server should be created if it does not exist.
     * @param createOptions
     *            Other launch arguments, namely template options for use with create
     * @throws LaunchException
     *             If server does not exist and --create was not specified and
     *             it is not the defaultServer.
     */
    void verifyProcess(VerifyServer verifyServer, LaunchArguments createOptions) throws LaunchException {
        if (verifyServer == null || verifyServer == VerifyServer.SKIP) {
            return;
        }

        boolean generatePassword = createOptions == null || createOptions.getOption("no-password") == null;
        if (!configDir.exists()) {
            if (verifyServer == VerifyServer.CREATE ||
                (verifyServer == VerifyServer.CREATE_DEFAULT && getDefaultProcessName().equals(processName))) {

                // start creating server and copy boot/config files as needed
                // first make sure we can find the source files we need...
                //
                // CREATE_DEFAULT will only appear here if someone launches
                // without the server script or uses a non-default
                // WLP_OUTPUT_DIR (otherwise, the server script will have
                // already created the directory).  In any case, we should only
                // respect --template for --create.
                File template = findProcessTemplate(verifyServer == VerifyServer.CREATE ? createOptions : null);

                // Assuming we can find the files we need to create the server, let's create the server directory
                if (configDir.mkdirs()) {
                    try {
                        createConfigDirectory(template);
                        generateServerEnv(generatePassword);
                    } catch (IOException e) {
                        throw new LocationException("Error occurred while trying to create new process "
                                                    + processName, MessageFormat.format(BootstrapConstants.messages.getString(getErrorCreatingNewProcessMessageKey()), processName,
                                                                                        configDir.getAbsolutePath(),
                                                                                        e.getMessage()), e);
                    }
                } else {
                    //if the location hasn't been created since our last exist check
                    if (!!!configDir.exists()) {
                        throw new LocationException("Unable to create process config directory " + configDir.getAbsolutePath(), MessageFormat.format(
                                                                                                                                                     BootstrapConstants.messages.getString(getErrorCreatingNewProcessMkDirFailMessageKey()),
                                                                                                                                                     processName,
                                                                                                                                                     configDir.getAbsolutePath()), null);
                    } else {
                        //something else created the server location AFTER we did the exists check at the start of this method
                        throw new LocationException("Something else has been detected creating the process config directory "
                                                    + configDir.getAbsolutePath(), MessageFormat.format(BootstrapConstants.messages.getString(getErrorCreatingNewProcessExistsMessageKey()),
                                                                                                        processName,
                                                                                                        configDir.getAbsolutePath()), null);

                    }
                }
            } else {
                // Throw exception for missing server config.
                LaunchException le = new LaunchException("Process config directory does not exist, and --create option not specified for "
                                                         + processName, MessageFormat.format(BootstrapConstants.messages.getString(getErrorNoExistingProcessMessageKey()),
                                                                                             processName,
                                                                                             configDir.getAbsolutePath()));
                le.setReturnCode(ReturnCode.SERVER_NOT_EXIST_STATUS);
                throw le;
            }
        } else {
            if (verifyServer == BootstrapConstants.VerifyServer.CREATE) {
                LaunchException le = new LaunchException("Unable to create the process " + processName
                                                         + " because the process config directory already exists", MessageFormat.format(BootstrapConstants.messages.getString(getErrorProcessDirExistsMessageKey()),
                                                                                                                                        processName, configDir));
                le.setReturnCode(ReturnCode.REDUNDANT_ACTION_STATUS);
                throw le;
            }

            // make sure we have a server.xml file
            File f = getConfigFile(getProcessXMLFilename());

            // The server directory already exists, but server.xml might not
            // exist for defaultServer.  This happens when the server script
            // creates an empty output directory (which is the same as the
            // config directory by default) so it can use the output directory
            // as the current working directory.
            if (!f.exists() && (verifyServer == VerifyServer.CREATE_DEFAULT && getDefaultProcessName().equals(processName))) {
                try {
                    createConfigDirectory(findProcessTemplate(null));
                    generateServerEnv(generatePassword);
                } catch (IOException e) {
                    // We can ignore this because a message will be output in a moment.
                }
            }

            if (!f.exists() || !f.canRead()) {
                throw new LocationException("Not Found " + f.getAbsolutePath(), MessageFormat.format(BootstrapConstants.messages.getString("error.badConfigRoot"),
                                                                                                     f.getAbsolutePath(), "file not found"));
            }
        }
    }

    protected void createConfigDirectory(File template) throws IOException {
        // And here we are! Make sure we can find a server.xml file using the template..
        File serverConfig = template == null ? null : new File(template, getProcessXMLFilename());
        if (template != null && template.exists() && template.isDirectory() && serverConfig.exists() && serverConfig.isFile()) {
            FileUtils.copyDir(template, configDir);
        } else {
            File defaultServerXML = getConfigFile(getProcessXMLFilename());
            FileUtils.createFile(defaultServerXML, BootstrapConfig.class.getResourceAsStream(getProcessXMLResourcePath()));
        }

        // Create the .sLock file, which is used to detect if the server workarea is valid.
        ServerLock.createServerLock(this);

        // All done! WOOHOO
        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString(getInfoNewProcessCreatedMessageKey()), processName));
    }

    /**
     * @param args Arguments passed on the command line for create...
     * @return File (server.xml file or template directory) that should be used as source for new image
     * @throws FileNotFoundException
     * @throws IOException
     */
    private File findProcessTemplate(LaunchArguments args) {

        String templateExtension = null;
        if (args != null)
            templateExtension = args.getOption("template");

        File f = getInstallRoot();

        if (templateExtension == null) {
            f = new File(f, getProcessesTemplateDir() + getDefaultProcessName());

        } else {
            if (templateExtension.contains(":")) {
                String[] templateArgs = templateExtension.split(":");
                String extensionName = templateArgs[0];
                String templateName = templateArgs[1];

                File extensionProp = new File(f, "etc/extensions/" + extensionName + ".properties");

                if (extensionProp.exists()) {
                    String directory = null;
                    try {
                        Properties prop = KernelUtils.getProperties(new FileInputStream(extensionProp.getAbsoluteFile()));
                        directory = prop.getProperty("com.ibm.websphere.productInstall");
                    } catch (Exception e) {
                        throw new LaunchException("Unable to load property: com.ibm.websphere.productInstall", MessageFormat.format(BootstrapConstants.messages.getString("error.unable.load.property"),
                                                                                                                                    "com.ibm.websphere.productInstall",
                                                                                                                                    extensionProp.getAbsolutePath()));
                    }
                    if (directory != null) {
                        File productInstallPath = new File(directory);
                        if (!productInstallPath.isAbsolute()) {
                            productInstallPath = new File(f.getParent(), directory);
                        }
                        f = new File(productInstallPath, getProcessesTemplateDir() + templateName);
                    }
                } else {
                    throw new LaunchException("Not Found " + extensionProp.getAbsolutePath(), MessageFormat.format(BootstrapConstants.messages.getString("error.fileNotFound"),
                                                                                                                   extensionProp.getAbsolutePath()));
                }
            } else {
                f = new File(f, getProcessesTemplateDir() + templateExtension);
            }

            File serverConfig = new File(f, getProcessXMLFilename());
            if (!serverConfig.isFile()) {
                throw new LaunchException("Not Found " + serverConfig.getAbsolutePath(), MessageFormat.format(BootstrapConstants.messages.getString("error.fileNotFound"),
                                                                                                              serverConfig.getAbsolutePath()));
            }
        }

        return f;
    }

    /**
     * This gives a handle to the properties used to initialize the framework (not
     * the framework's copy).
     * This set of properties is passed (for example) to log providers, who can
     * then add/remove additional
     * framework properties.
     *
     * @return the properties used to start the framework.
     */
    public Map<String, String> getFrameworkProperties() {
        return initProps;
    }

    /**
     * @return the command line arguments
     */
    public List<String> getCmdArgs() {
        return cmdArgs;
    }

    /**
     * @param cmdArgs
     *            command line arguments (post-parse)
     */
    public void setCmdArgs(List<String> cmdArgs) {
        this.cmdArgs = cmdArgs;
    }

    /**
     * @return the frameworkLaunchClassloader
     */
    public ClassLoader getFrameworkClassloader() {
        return frameworkLaunchClassloader;
    }

    /**
     * @param frameworkLaunchClassloader
     *            the frameworkLaunchClassloader to set
     */
    public void setFrameworkClassloader(ClassLoader frameworkLaunchClassloader) {
        this.frameworkLaunchClassloader = frameworkLaunchClassloader;
    }

    public KernelResolver getKernelResolver() {
        return kernelResolver;
    }

    public void setKernelResolver(KernelResolver resolver) {
        this.kernelResolver = resolver;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        if (instrumentation != null) {
            put("java.lang.instrument", "true");
        } else {
            remove("java.lang.instrument");
        }
    }

    /**
     * @return the install root, ${wlp.install.dir}
     */
    public File getInstallRoot() {
        return installRoot;
    }

    /**
     * @return the userRoot directory, ${wlp.user.dir}
     */
    public File getUserRoot() {
        return userRoot;
    }

    /**
     * @return the wlp output directory which is not server specific
     */
    public File getCommonOutputRoot() {
        return outputRoot;
    }

    /**
     * Check osgi clean start properties, ensure set correctly for clean start
     *
     * @see forceCleanStart
     */
    public boolean checkCleanStart() {
        String fwClean = get(BootstrapConstants.INITPROP_OSGI_CLEAN);
        if (fwClean != null && fwClean.equals(BootstrapConstants.OSGI_CLEAN_VALUE)) {
            return true;
        }

        String osgiClean = get(BootstrapConstants.OSGI_CLEAN);
        return Boolean.valueOf(osgiClean);
    }

    /**
     * Force a framework clean start
     *
     * @see #checkCleanProperties(Map)
     */
    public void forceCleanStart() {
        put(BootstrapConstants.INITPROP_OSGI_CLEAN, BootstrapConstants.OSGI_CLEAN_VALUE);
    }

    protected String getDefaultProcessName() {
        return BootstrapConstants.DEFAULT_SERVER_NAME;
    }

    protected String getProcessXMLFilename() {
        return BootstrapConstants.SERVER_XML;
    }

    protected String getProcessXMLResourcePath() {
        return "/OSGI-OPT/websphere/server/server.xml";
    }

    protected String getErrorCreatingNewProcessMessageKey() {
        return "error.creatingNewServer";
    }

    protected String getErrorCreatingNewProcessMkDirFailMessageKey() {
        return "error.creatingNewServerMkDirFail";
    }

    protected String getErrorCreatingNewProcessExistsMessageKey() {
        return "error.creatingNewServerExists";
    }

    protected String getErrorNoExistingProcessMessageKey() {
        return "error.noExistingServer";
    }

    protected String getErrorProcessDirExistsMessageKey() {
        return "error.serverDirExists";
    }

    protected String getErrorProcessNameCharacterMessageKey() {
        return "error.serverNameCharacter";
    }

    protected String getInfoNewProcessCreatedMessageKey() {
        return "info.newServerCreated";
    }

    protected String getProcessesTemplateDir() {
        return "templates/servers/";
    }

    public String getProcessType() {
        return BootstrapConstants.LOC_PROCESS_TYPE_SERVER;
    }

    public void addBootstrapJarURLs(List<URL> urlList) {
        urlList.add(KernelUtils.getBootstrapJarURL());
    }

    /**
     * For Java 8 and newer JVMs, the PermGen command line parameter is no
     * longer supported. This method checks the Java level and if it is
     * less than Java 8, it simply returns OK. If it is Java 8 or higher,
     * this method will attempt to create a server.env file with
     *
     * @param bootProps
     * @return
     */
    protected ReturnCode generateServerEnv(boolean generatePassword) {
        BufferedWriter bw = null;
        File serverEnv = getConfigFile("server.env");
        try {
            char[] keystorePass = PasswordGenerator.generateRandom();
            String serverEnvContents = FileUtils.readFile(serverEnv);
            String toWrite = "";
            if (generatePassword && (serverEnvContents == null || !serverEnvContents.contains("keystore_password="))) {
                if (serverEnvContents != null)
                    toWrite += System.getProperty("line.separator");
                toWrite += "keystore_password=" + new String(keystorePass);
            }

            if (serverEnvContents == null)
                FileUtils.createFile(serverEnv, new ByteArrayInputStream(toWrite.getBytes(StandardCharsets.UTF_8)));
            else
                FileUtils.appendFile(serverEnv, new ByteArrayInputStream(toWrite.getBytes(StandardCharsets.UTF_8)));

        } catch (IOException ex) {
            throw new LaunchException("Failed to create/update the server.env file for this server", MessageFormat.format(BootstrapConstants.messages.getString("error.create.java8serverenv"),
                                                                                                                          serverEnv.getAbsolutePath()), ex, ReturnCode.LAUNCH_EXCEPTION);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ex) {
                }
            }
        }
        return ReturnCode.OK;
    }

    public File getServiceBindingRoot() {
        return this.serviceBindingRootDir;
    }
}