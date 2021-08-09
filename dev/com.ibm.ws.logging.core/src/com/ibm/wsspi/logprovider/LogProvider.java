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
package com.ibm.wsspi.logprovider;

import java.io.File;
import java.util.Map;

import com.ibm.wsspi.logging.TextFileOutputStreamFactory;

/**
 * Interface for initializing WsLogProviders (e.g. RAS/FFDC, etc) using the
 * META-INF/service factory.
 * <p>
 * LogProviders should be defined in a file in the log provider jar:
 * META-INF/service/com.ibm.websphere.logging.LogProvider, with one log provider
 * per line.
 */
public interface LogProvider {
    /**
     * Initial configuration of the log provider: config is a map containing string key/object
     * value pairs. The values are pulled from bootstrap and system properties
     * in the appropriate order (command line values on top -> system properties
     * -> bootstrap.properties).
     * <p>
     * This method is called before the framework is started. The config map is
     * subsequently used to start the framework. The map is read-only, with
     * operations essentially limited to get/contains.
     * 
     * @param config
     *            Map containing configuration parameters: this map should/will
     *            be backed by system properties (wrapped w/ appropriate
     *            doPriv).
     * 
     * @param logLocation
     *            File specifying the location of the logs directory
     * @param factory
     *            Factory that should be used by the LogProvider to create
     *            text-based file output streams. This enables OS-specific
     *            file tagging.
     */
    void configure(Map<String, String> config, File logLocation, TextFileOutputStreamFactory factory);

    /**
     * Stop the log provider: this is called after the osgi framework has
     * stopped, as the runtime is shutting down. LogProviders should close any
     * open resources when this method is called.
     */
    void stop();
}
