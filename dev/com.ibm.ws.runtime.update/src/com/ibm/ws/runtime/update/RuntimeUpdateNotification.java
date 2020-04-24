/*******************************************************************************
 * Copyright (c) 2014-2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.update;

import java.util.Map;
import java.util.concurrent.Future;

import com.ibm.ws.threading.listeners.CompletionListener;

/**
 * A runtime update notification
 *
 * Instances of this interface are created by calling the createNotification method
 * of the RuntimeUpdateManager. The notification name is provided as a parameter to
 * that method call. A set of core runtime notification names are provided.
 *
 * A notification consists of a name and a Future which will be set when the
 * notification is completed.
 */
public interface RuntimeUpdateNotification {
    /**
     * A common set of notification names
     */
    public final String APP_FORCE_RESTART = "AppForceRestart";
    public final String FEATURE_BUNDLES_RESOLVED = "FeatureBundlesResolved";
    public final String FEATURE_BUNDLES_PROCESSED = "FeatureBundlesProcessed";
    public final String FEATURE_UPDATES_COMPLETED = "FeatureUpdatesCompleted";
    public final String INSTALLED_BUNDLES_IN_UPDATE = "InstalledBundles";
    public final String REMOVED_BUNDLES_IN_UPDATE = "RemovedBundles";
    public final String CONFIG_UPDATES_DELIVERED = "ConfigUpdatesDelivered";
    public final String APPLICATIONS_STOPPED = "ApplicationsStopped";
    public final String APPLICATIONS_STARTING = "ApplicationsStarting";
    public final String APPLICATIONS_INSTALL_CALLED = "ApplicationsInstallCalled";
    public final String ORB_STARTED = "ORBStarting";

    /**
     * Get the name of this notification
     *
     * @return the notification name
     */
    public String getName();

    /**
     * The future associated with this notification
     *
     * @return the future for this notification
     */
    public Future<Boolean> getFuture();

    /**
     * Set the result of a successfully completed future
     *
     * @param result the result for the future
     */
    public void setResult(boolean result);

    /**
     * Set the result of a future when an error occurs
     *
     * @param t the Throwable to set as the result (failure) of the future
     */
    public void setResult(Throwable t);

    /**
     * Set a completion listener to be called when the future completes
     *
     * @param completionListener the completion listener to be registered
     *                               with the future
     */
    public void onCompletion(CompletionListener<Boolean> completionListener);

    /**
     * Wait for the future to complete
     */
    public void waitForCompletion();

    /**
     * Check if the future is done
     *
     * @return the result of calling isDone() on the underlying future
     */
    public boolean isDone();

    /**
     * Check if this notification can be ignored on server quiesce
     *
     * @return boolean
     */
    public boolean ignoreOnQuiesce();

    public Map<String, Object> getProperties();

    public void setProperties(Map<String, Object> props);
}
