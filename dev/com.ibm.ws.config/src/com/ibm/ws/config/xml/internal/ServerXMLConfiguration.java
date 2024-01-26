/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;

import com.ibm.websphere.config.ConfigParserException;
import com.ibm.websphere.config.ConfigValidationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.config.xml.LibertyVariable;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;

/**
 *
 */
class ServerXMLConfiguration {

    private static final TraceComponent tc = Tr.register(ServerXMLConfiguration.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    /** The root XML document (server.xml) */
    private final WsResource configRoot;
    private final WsResource configDropinDefaults;
    private final WsResource configDropinOverrides;
    private final BundleContext bundleContext;

    private ServerConfiguration serverConfiguration;

    private static final String CONFIG_DROPINS = "configDropins";
    private static final String CONFIG_DROPIN_DEFAULTS = CONFIG_DROPINS + "/" + "defaults/";
    private static final String CONFIG_DROPIN_OVERRIDES = CONFIG_DROPINS + "/" + "overrides/";

    /**
     * last time config files(root config document and its included documents) are
     * read
     */
    private volatile long configReadTime = 0;

    private final XMLConfigParser parser;

    ServerXMLConfiguration(BundleContext bundleContext,
                           WsLocationAdmin locationService,
                           XMLConfigParser parser) {
        this.bundleContext = bundleContext;
        this.parser = parser;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "WsLocationAdmin locations=" + locationService.printLocations(false));
        }

        this.configRoot = locationService.resolveResource(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + "/" + WsLocationConstants.SYMBOL_PROCESS_TYPE + ".xml");
        this.configDropinDefaults = locationService.resolveResource(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + "/" + CONFIG_DROPIN_DEFAULTS);
        this.configDropinOverrides = locationService.resolveResource(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + "/" + CONFIG_DROPIN_OVERRIDES);

        // Determines if any of the configuration files used by current server has
        // been updated since the last run.
        this.configReadTime = getInitialConfigReadTime(bundleContext);

    }

    boolean hasConfigRoot() {
        return configRoot != null;
    }

    private static long getInitialConfigReadTime(BundleContext bundleContext) {
        if (bundleContext == null) {
            return 0;
        }
        File configStamp = bundleContext.getDataFile("configStamp");
        if (configStamp != null && configStamp.exists() && configStamp.canRead()) {
            return TimestampUtils.readTimeFromFile(configStamp);
        } else {
            return 0;
        }
    }

    /**
     * Set a server's base configuration by loading the server's root
     * configuration resource (server.xml) and any included configuration
     * resources, but not individual bundle default configurations
     * (for example, bundle.cfg).
     * <p>
     * This should be only done once, before any bundle default configurations
     * are loaded.
     *
     * @throws ConfigValidationException
     * @throws ConfigParserException
     */
    @FFDCIgnore(ConfigParserTolerableException.class)
    public void loadInitialConfiguration(ConfigVariableRegistry variableRegistry) throws ConfigValidationException, ConfigParserException {

        // If possible, load the server configuration.
        //
        // The load can fail in three ways:
        // 1 A null server configuration can be obtained.
        // 2 A tolerable parser exception was thrown.
        // 3 A non-tolerable parser exception was thrown.
        //
        // (1) Happens only if there is a parser error and onError has
        // been set to IGNORE or WARN.  Set an empty configuration, avoiding an NPE.
        // Proceeding with an empty configuration is less than idea, in particular, when
        // onError is set to IGNORE.  However, its what the user asked for.
        //
        // (2) Happens only if onError is set to FAIL.  In this case,
        // rethrow the exception: The caller will handle this and will
        // cause the server to shut down.

        if ((configRoot != null) && configRoot.exists()) {
            try {
                serverConfiguration = loadServerConfiguration();
                if (serverConfiguration == null) {
                    serverConfiguration = new ServerConfiguration();
                }
            } catch (ConfigParserTolerableException ex) {
                throw ex;
            } catch (ConfigParserException ex) {
                Tr.error(tc, "error.config.update.init", ex.getMessage());
                serverConfiguration = new ServerConfiguration();
                if (ErrorHandler.INSTANCE.fail())
                    throw ex;
            }

            serverConfiguration.setDefaultConfiguration(new BaseConfiguration());
        }

        try {
            variableRegistry.updateSystemVariables(getVariables());
            // Register the ConfigVariables service now that we have populated the registry
            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            properties.put("service.vendor", "IBM");
            bundleContext.registerService(ConfigVariables.class, variableRegistry, properties);
        } catch (ConfigMergeException e) {
            // Rethrow if onError=FAIL. An error message has already been issued otherwise.
            if (ErrorHandler.INSTANCE.fail()) {
                throw new ConfigParserTolerableException(e);
            }
        }
    }

    public void setConfigReadTime() {
        setConfigReadTime(getLastResourceModifiedTime());

    }

    public void setConfigReadTime(long time) {
        // Update time stamp for configReadTime on next run.
        TimestampUtils.writeTimeToFile(bundleContext.getDataFile("configStamp"), time);
        configReadTime = time;
    }

    private long getLastResourceModifiedTime() {
        long lastModified = configRoot.getLastModified();

        if (serverConfiguration != null) {
            for (WsResource resource : serverConfiguration.getIncludes()) {
                long modified = resource.getLastModified();
                if (modified > lastModified) {
                    lastModified = modified;
                }
            }
        }

        if (configDropinDefaults != null) {
            File[] defaultFiles = getChildXMLFiles(configDropinDefaults);
            if (defaultFiles != null) {
                for (File f : defaultFiles) {
                    String name = f.getName();
                    WsResource resource = configDropinDefaults.resolveRelative(name);
                    long modified = resource.getLastModified();
                    if (modified > lastModified) {
                        lastModified = modified;
                    }
                }
            }

        }

        if (configDropinOverrides != null) {
            File[] overrideFiles = getChildXMLFiles(configDropinOverrides);
            if (overrideFiles != null) {
                for (File f : overrideFiles) {
                    String name = f.getName();
                    WsResource resource = configDropinOverrides.resolveRelative(name);
                    long modified = resource.getLastModified();
                    if (modified > lastModified) {
                        lastModified = modified;
                    }
                }
            }

        }

        return lastModified;
    }

    // Remove milliseconds from timestamp values to address inconsistencies in container file systems
    long reduceTimestampPrecision(long value) {
        return (value / 1000) * 1000;
    }

    public boolean isModified() {
        return reduceTimestampPrecision(getLastResourceModifiedTime()) != reduceTimestampPrecision(configReadTime);
    }

    public Collection<String> getFilesToMonitor() {
        Collection<String> files = new HashSet<String>();
        files.add(configRoot.toRepositoryPath());

        for (WsResource resource : serverConfiguration.getIncludes()) {
            String path = resource.toRepositoryPath();
            if (path != null) {
                files.add(path);
            }
        }

        return files;
    }

    /**
     * Get the directories that should be monitored for changes. At the moment, this is
     * configDropins/defaults and configDropins/overrides
     */
    public Collection<String> getDirectoriesToMonitor() {
        Collection<String> files = new HashSet<String>();
        if (configDropinDefaults != null) {
            files.add(configDropinDefaults.toRepositoryPath());
        }

        if (configDropinOverrides != null) {
            files.add(configDropinOverrides.toRepositoryPath());
        }

        return files;
    }

    /**
     * To maintain the same order across platforms, we have to implement our own comparator.
     * Otherwise, "aardvark.xml" would come before "Zebra.xml" on windows, and vice versa on unix.
     */
    private static class AlphaComparator implements Comparator<File> {

        /*
         * (non-Javadoc)
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(File o1, File o2) {
            return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        }

    }

    /**
     * Load the entire configuration.
     *
     * Load dropins defaults resources, then load the root resource and
     * its included resources, then load dropins overrides. Finally, record
     * that the configuration is up to date relative to the last modification
     * time of the root configuration resource.
     *
     * @param configuration The configuration which is to be parsed.
     *
     * @throws ConfigValidationException Thrown in case of a problem in
     *             the parsed XML data.
     * @throws ConfigParserException Thrown if the XML parse fails.
     */
    private void load(ServerConfiguration configuration) throws ConfigValidationException, ConfigParserException {
        parseDirectoryFiles(configDropinDefaults, configuration);
        parser.parseServerConfiguration(configRoot, configuration);
        parseDirectoryFiles(configDropinOverrides, configuration);

        configuration.updateLastModified(configRoot.getLastModified());
    }

    @FFDCIgnore({ ConfigParserException.class, ConfigParserTolerableException.class })
    private ServerConfiguration loadServerConfiguration() throws ConfigValidationException, ConfigParserException {
        ServerConfiguration configuration = null;

        try {
            try {
                // Initialize the configuration object here, so that as the parser progresses
                // we maintain the information if an exception is thrown.
                configuration = new ServerConfiguration();
                load(configuration);

            } catch (ConfigParserTolerableException ex) {
                // We know what this is, so no need to retry
                throw ex;

            } catch (ConfigParserException cpe) {
                // Wait a short period of time and retry. This is to attempt to handle the case where we
                // parse the configuration in the middle of a file update.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore

                } finally {
                    configuration = new ServerConfiguration();
                    load(configuration);
                }
            }

        } catch (ConfigParserException ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while parsing root and referenced config documents.  Message=" + ex.getMessage());
            }

            parser.handleParseError(ex, null);

            if (ErrorHandler.INSTANCE.fail()) {
                throw ex;

            } else if (ex instanceof ConfigParserTolerableException) {
                // Discard the exception: Any messaging must be performed
                // when throwing the exception.
                //
                // Mark the configuration as up to date relative to the configuration resource.
                configuration.updateLastModified(configRoot.getLastModified());
            } else {
                // onError isn't set to FAIL, but we can't tolerate this exception either
                // so null the configuration reference
                configuration = null;
            }
        }

        return configuration;
    }

    private File[] getChildXMLFiles(WsResource directory) {
        File defaultsDir = directory.asFile();
        if (defaultsDir == null || !defaultsDir.exists())
            return null;

        File[] defaultFiles = defaultsDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                if (file != null && file.isFile()) {
                    String name = file.getName().toLowerCase();
                    return name.endsWith(".xml");
                }
                return false;
            }
        });
        return defaultFiles;
    }

    /**
     * Parse all of the config files in a directory in platform insensitive alphabetical order
     */
    private void parseDirectoryFiles(WsResource directory, ServerConfiguration configuration) throws ConfigParserException, ConfigValidationException {
        if (directory != null) {
            File[] defaultFiles = getChildXMLFiles(directory);
            if (defaultFiles == null)
                return;

            Arrays.sort(defaultFiles, new AlphaComparator());

            for (int i = 0; i < defaultFiles.length; i++) {
                File file = defaultFiles[i];
                if (!file.isFile())
                    continue;

                WsResource defaultFile = directory.resolveRelative(file.getName());
                if (defaultFile == null) {
                    // This should never happen, but it's conceivable that someone could remove a file
                    // after listFiles and before getChild
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, file.getName() + " was not found in directory " + directory.getName() + ". Ignoring. ");
                    }
                    continue;
                }
                Tr.audit(tc, "audit.dropin.being.processed", defaultFile.asFile());
                try {
                    parser.parseServerConfiguration(defaultFile, configuration);
                } catch (ConfigParserException ex) {
                    parser.handleParseError(ex, null);

                    if (ErrorHandler.INSTANCE.fail()) {
                        // if onError=FAIL, bubble the exception up the stack
                        throw ex;
                    } else {
                        // Mark the last update for the configuration so that we don't try to load it again
                        configuration.updateLastModified(configRoot.getLastModified());
                    }
                }
            }
        }
    }

    @FFDCIgnore(ConfigParserTolerableException.class)
    ServerConfiguration loadNewConfiguration() {
        ServerConfiguration newConfiguration = null;
        if (configRoot.exists()) {

            try {
                newConfiguration = loadServerConfiguration();
                setConfigReadTime();
            } catch (ConfigParserTolerableException e) {
                // This is only thrown if OnError = FAIL
                String message = e.getMessage() == null ? "Parser Failure" : e.getMessage();
                Tr.error(tc, "error.config.update.init", new Object[] { message });
            } catch (ConfigParserException e) {
                Tr.error(tc, "error.config.update.init", new Object[] { e.getMessage() });
            } catch (ConfigValidationException e) {
                Tr.warning(tc, "warn.configValidator.refreshFailed");
            }

            if (newConfiguration == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "doRefreshConfiguration(): Error loading new configuration - leaving existing configuration unchanged");
                }
//                Tr.error(tc, "error.config.update.init", new Object[] { e.getMessage() });
                return null;
            }
        } else {
            newConfiguration = new ServerConfiguration();
        }

        newConfiguration.setDefaultConfiguration(serverConfiguration.getDefaultConfiguration());
        return newConfiguration;
    }

    public ServerConfiguration getConfiguration() {
        return serverConfiguration;
    }

    public BaseConfiguration getDefaultConfiguration() {
        return serverConfiguration.getDefaultConfiguration();
    }

    @SuppressWarnings("unused")
    public Map<String, LibertyVariable> getVariables() throws ConfigMergeException {
        return serverConfiguration.getVariables();
    }

    public void setNewConfiguration(ServerConfiguration newConfiguration) {
        this.serverConfiguration = newConfiguration;
    }

    public ServerConfiguration copyConfiguration() {
        ServerConfiguration copy = new ServerConfiguration();
        BaseConfiguration dflt = new BaseConfiguration();
        copy.add(getConfiguration());
        dflt.add(getDefaultConfiguration());
        copy.setDefaultConfiguration(dflt);
        return copy;
    }

}
