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
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Properties;

import com.ibm.ws.kernel.boot.BootstrapConfig;

/**
 * Wrapper around the Liberty defaults file (in wlp/lib/platform/defaults).
 */
public class BootstrapDefaults {
    /** Manifest header for the default kernel version */
    static final String MANIFEST_KERNEL = "WebSphere-DefaultKernel";

    /** Manifest header for the default log provider */
    static final String MANIFEST_LOG_PROVIDER = "WebSphere-DefaultLogProvider";

    /** Manifest header for the default OS Extensions */
    static final String MANIFEST_OS_EXTENSION = "WebSphere-DefaultExtension-";

    /** bootstrap property to override kernel version */
    static final String BOOTPROP_KERNEL = "websphere.kernel";

    /** bootstrap property to override log provider */
    static final String BOOTPROP_LOG_PROVIDER = "websphere.log.provider";

    /** bootstrap property to override os extension */
    static final String BOOTPROP_OS_EXTENSIONS = "websphere.os.extension";

    /** The properties that we read from the defaults file. */
    private final Properties defaults;

    /**
     * Read the defaults from the file
     */
    BootstrapDefaults(BootstrapConfig bootConfig) throws java.io.IOException {
        // Read the default values
        FileInputStream fis = new FileInputStream(new File(bootConfig.getInstallRoot(), "lib/platform/defaults"));
        Properties defaultProps = null;
        try {
            defaultProps = new Properties();
            defaultProps.load(fis);
        } finally {
            fis.close();
        }

        // Read platform-specific default values, if they exist.
        String normalizedOsName = getNormalizedOperatingSystemName(bootConfig.get("os.name"));
        File osDefaults = new File(bootConfig.getInstallRoot(), "lib/platform/defaults-" + normalizedOsName);
        if ((osDefaults.exists()) && (osDefaults.isFile())) {
            fis = new FileInputStream(osDefaults);
            try {
                defaults = new Properties(defaultProps);
                defaults.load(fis);
            } finally {
                fis.close();
            }
        } else {
            defaults = defaultProps;
        }
    }

    /**
     * Find and return the name of the core/kernel feature. Look in
     * bootstrap properties first, if not explicitly defined there, get
     * the default from the manifest.
     *
     * @return the selected kernel version
     */
    public String getKernelDefinition(BootstrapConfig bootProps) {
        String kernelDef = bootProps.get(BOOTPROP_KERNEL);

        if (kernelDef == null)
            kernelDef = defaults.getProperty(MANIFEST_KERNEL);

        if (kernelDef != null)
            bootProps.put(BOOTPROP_KERNEL, kernelDef);

        return kernelDef;
    }

    /**
     * Find and return the name of the log provider. Look in
     * bootstrap properties first, if not explicitly defined there, get
     * the default from the manifest.
     *
     * @return the selected log provider
     */
    public String getLogProviderDefinition(BootstrapConfig bootProps) {
        String logProvider = bootProps.get(BOOTPROP_LOG_PROVIDER);

        if (logProvider == null)
            logProvider = defaults.getProperty(MANIFEST_LOG_PROVIDER);

        if (logProvider != null)
            bootProps.put(BOOTPROP_LOG_PROVIDER, logProvider);

        return logProvider;
    }

    /**
     * Find and return the name of the os extension. Look in
     * bootstrap properties first, if not explicitly defined there, get
     * the default from the manifest.
     *
     * @return the selected log provider
     */
    public String getOSExtensionDefinition(BootstrapConfig bootProps) {
        String osExtension = bootProps.get(BOOTPROP_OS_EXTENSIONS);

        if (osExtension == null) {
            String normalizedName = getNormalizedOperatingSystemName(bootProps.get("os.name"));
            osExtension = defaults.getProperty(MANIFEST_OS_EXTENSION + normalizedName);
        }

        if (osExtension != null)
            bootProps.put(BOOTPROP_OS_EXTENSIONS, osExtension);

        return osExtension;
    }

    /**
     * Normalize the value associated with the &quot;os.name&quot; system
     * property by putting it in lower case and removing characters that
     * cannot be used with {@link java.util.jar.Attributes.Name}.
     *
     * @return the normalized OS name
     */
    static String getNormalizedOperatingSystemName(final String osName) {
        String name = osName.toLowerCase(Locale.ENGLISH);
        name = name.replaceAll("[^0-9a-zA-Z_-]", "");
        return name;
    }
}
