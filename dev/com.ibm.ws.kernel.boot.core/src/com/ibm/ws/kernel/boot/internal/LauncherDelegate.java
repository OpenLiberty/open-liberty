/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.IOException;
import java.util.Set;

import com.ibm.ws.kernel.boot.LaunchException;

/**
 * Interface between the framework launcher entry point and the framework
 * manager entry point, which are loaded by separate class loaders.
 */
public interface LauncherDelegate {

    /**
     * This method delegates the actual framework launch to the constructed
     * FrameworkManager. Before it does that, it initializes any configured
     * LogProviders (Tr and FFDC), and finds and constructs a
     * FrameworkConfigurator based on values present in the provided
     * BootstrapConfiguration.
     * 
     * @throws LaunchException
     *             If the framework can not be launched; this is propagated from
     *             the delegated call to the FrameworkManager
     * @throws RuntimeException
     *             If an unexpected runtime exception occurred while launching
     *             the framework; this may be propagated from the delegated call
     *             to the FrameworkManager. Uncaught Throwables are also mapped
     *             to RuntimeExceptions and are then re-thrown to the caller.
     */
    void launchFramework();

    /**
     * Wait for the framework to become fully started.
     * 
     * @return true if the framework was started successfully
     * @throws InterruptedException
     *             If the thread is interrupted before the framework launch
     *             status is determined
     */
    boolean waitForReady() throws InterruptedException;

    /**
     * Shutdown the framework
     * 
     * @throws InterruptedException
     *             If the thread is interrupted before the framework stop
     * @return true if the framework was shutdown successfully
     */
    boolean shutdown() throws InterruptedException;

    /**
     * Query feature info.
     * 
     * Used for minify.
     * 
     * @param osRequest contains any os filtering request information.
     * @return set of absolute paths representing all files used by configured features for the server
     */
    Set<String> queryFeatureInformation(String osRequest) throws IOException;

    /**
     * Query feature names.
     * 
     * @return set of feature names of the configured features for the server
     */
    Set<String> queryFeatureNames();
}
