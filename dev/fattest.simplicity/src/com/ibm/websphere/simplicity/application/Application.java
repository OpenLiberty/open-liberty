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
package com.ibm.websphere.simplicity.application;

import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.simplicity.Cluster;
import com.ibm.websphere.simplicity.Node;
import com.ibm.websphere.simplicity.OperationResults;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.Scope;
import com.ibm.websphere.simplicity.Server;
import com.ibm.websphere.simplicity.application.types.UpdateContentType;
import com.ibm.websphere.simplicity.application.types.UpdateType;
import com.ibm.websphere.simplicity.exception.NotImplementedException;
import com.ibm.websphere.simplicity.log.Log;

public abstract class Application {

    protected static Application create(ApplicationManager mgr, ApplicationType type, String name, Scope scope) throws Exception {
        Application ret = null;
        // Other application types?
        switch (type) {
            case EAR:
                ret = new EnterpriseApplication(mgr, name, scope);
        }
        return ret;
    }

    protected static Class c = Application.class;

    protected Scope scope;
    protected String name;
    protected ArchiveType archiveType = null;
    protected String installRoot;
    protected Boolean isInstalled = null;
    protected ApplicationType type;
    protected ApplicationManager applications;

    protected Application(ApplicationManager mgr, ApplicationType type, String name, Scope scope) throws Exception {
        Log.entering(c, "WebSphereApplication");
        this.scope = scope;
        this.type = type;
        this.name = name;
        this.archiveType = ArchiveType.EAR;

        this.applications = mgr;
        this.isInstalled = isInstalled();
    }

    public abstract OperationResults<Boolean> stop() throws Exception;

    public abstract OperationResults<Boolean> start() throws Exception;

    public abstract OperationResults<Boolean> update(UpdateWrapper options) throws Exception;

    public abstract OperationResults<Boolean> edit(EditWrapper options) throws Exception;

    public abstract UpdateWrapper getUpdateWrapper() throws Exception;

    public abstract UpdateWrapper getUpdateWrapper(UpdateType updateType, RemoteFile contents, UpdateContentType contentType) throws Exception;

    @Deprecated
    public abstract UpdateWrapper getUpdateWrapper(RemoteFile file) throws Exception;

    public abstract EditWrapper getEditWrapper() throws Exception;

    /**
     * @return The path to the binary files, with variables left intact.
     */
    public abstract String getDestinationPath();

    /**
     * @return The fully qualified path to the binary files, with variables expanded.
     */
    public abstract String getDestinationPath(Node node);

    /**
     * @return The name of the application defined during installation.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return an enum that describes the type of archive that contains this application.
     */
    public ArchiveType getArchiveType() {
        return this.archiveType;
    }

    /**
     * @return An enum that describes the type of application represented by this instance.
     */
    public ApplicationType getApplicationType() {
        return this.type;
    }

    public Set<AssetModule> getModules() throws Exception {
        return new HashSet<AssetModule>();
    }

    /**
     * Returns the root folder for the application installation. This is either
     * the default folder, or the one specified in AppDeploymentOptions during
     * installation.
     */
    public String getInstallLocation() {
        return getInstallLocation(null);
    }

    public String getInstallLocation(Node scope) {
        try {
            throw new NotImplementedException();
            //            if (installLocation == null)
            //                installLocation = this.getApplicationOptions().getAppDeploymentOptions().getInstallDirectory();
            //            
            //            if (scope != null)
            //                return scope.expandString(installLocation);
        } catch (Exception e) {
            Log.error(c, "getInstallLocation", e);
        }
        return null;
    }

    /**
     * @return A reference to the scope used to create this application.
     */
    public Scope getScope() {
        return this.scope;
    }

    /**
     * @return The full path of the archive on the cell's server.
     */
    public String getArchivePath() {
        return getInstallLocation() + "/" + getName() + ".ear";
    }

    /**
     * @return True if the application is installed.
     * @throws Exception
     */
    public boolean isInstalled() throws Exception {
        Log.entering(c, "isInstalled");
        isInstalled = true;
        Log.exiting(c, "isInstalled", isInstalled);
        return isInstalled;
    }

    /**
     * Attempts to launch the application, and -- if successful -- waits up to
     * five minutes for the app to finish starting on all targets. If the launch
     * operation is not successful, the call will return immediately.
     * 
     * @param wait True to wait synchronously for a successful launch.
     * @return An instance of OperationResults containing all startup information.
     * @throws Exception
     */
    public OperationResults<Boolean> start(boolean wait) throws Exception {
        // Default to three minutes
        return start(wait, 30000);
    }

    /**
     * Attempts to launch the application, and -- if successful -- waits up to
     * the specified timeout (ms) for the app to finish starting on all targets.
     * If the launch operation is not successful, the call will return immediately.
     * 
     * @param wait True to wait synchronously for a successful launch.
     * @param timeout The duration to wait for a successful launch in milliseconds.
     * @return An instance of OperationResults containing all startup information.
     * @throws Exception
     */
    public OperationResults<Boolean> start(boolean wait, long timeout) throws Exception {
        Log.entering(c, "start", new Object[] { wait, timeout });
        OperationResults<Boolean> result = start();
        if (!result.isSuccess())
            return result;

        if (wait) {
            ApplicationStatus state = waitForState(ApplicationStatus.STARTED, timeout);
            OperationResults<Boolean> tmp = new OperationResults<Boolean>(state == ApplicationStatus.STARTED);
            // Copy over notifications etc.
            OperationResults.setOperationResults(tmp, result);
            result = tmp;
        }
        Log.exiting(c, "start", result);
        return result;
    }

    /**
     * Attempts to stop the application, and -- if successful -- waits up to
     * five minutes for the app to finish stopping on all targets. If the stop
     * operation is not successful, the call will return immediately.
     * 
     * @param wait True to wait synchronously for a complete stop.
     * @return An instance of OperationResults containing all shutdown information.
     * @throws Exception
     */
    public OperationResults<Boolean> stop(boolean wait) throws Exception {
        // Default to three minutes
        return stop(wait, 30000);
    }

    /**
     * Attempts to stop the application, and -- if successful -- waits up to
     * the specified timeout (ms) for the app to finish stopping on all targets.
     * If the stop operation is not successful, the call will return immediately.
     * 
     * @param wait True to wait synchronously for a complete stop.
     * @param timeout The duration to wait for a complete stop in milliseconds.
     * @return An instance of OperationResults containing all shutdown information.
     * @throws Exception
     */
    public OperationResults<Boolean> stop(boolean wait, long timeout) throws Exception {
        Log.entering(c, "stop", new Object[] { wait, timeout });
        OperationResults<Boolean> result = stop();
        if (!result.isSuccess())
            return result;

        if (wait) {
            ApplicationStatus state = waitForState(ApplicationStatus.STOPPED, timeout);
            OperationResults<Boolean> tmp = new OperationResults<Boolean>(state == ApplicationStatus.STOPPED);
            // Copy over notifications etc.
            OperationResults.setOperationResults(tmp, result);
            result = tmp;
        }
        Log.exiting(c, "stop", result);
        return result;
    }

    /**
     * This method returns true if the application is running on any of its deployed targets. Use
     * the {@link #getApplicationStatus()} to get the aggregate status of the application across all
     * deployed targets and the {@link #getApplicationStatus(Scope)} method to get the
     * status of the application on a particular target.
     * TODO This should call getApplicationStatus instead.
     * 
     * @return True if the application is running on any of its deployed targets
     * @throws Exception
     */
    public boolean isStarted() throws Exception {
        Log.entering(c, "isStarted");
        ApplicationStatus result = this.getApplicationStatus();
        Log.exiting(c, "isStarted", (result == ApplicationStatus.STARTED));
        return (result == ApplicationStatus.STARTED);
    }

    /**
     * Check the status of an application on a particular server. This method returns {@link ApplicationStatus#STARTED} if the application is running on the server.
     * {@link ApplicationStatus#STOPPED} is
     * returned if the application is not running on the target and the server or the node manager
     * is running. {@link ApplicationStatus#UNKNOWN} is returned if the application is not running on the
     * server and the node manager is not available.
     * 
     * @param server The {@link Server} to get the status of the application on
     * @return The status of the application on the particular server
     * @throws Exception
     */
    protected ApplicationStatus getApplicationStatus(Server server) throws Exception {
        final String method = "getServerSpecificApplicationStatus";
        Log.entering(c, method, server);
        ApplicationStatus ret = ApplicationStatus.STARTED;
        Log.exiting(c, method, ret);
        return ret;
    }

    /**
     * Get the status of the application on the specified {@link Scope}. This method returns {@link ApplicationStatus#STARTED} if the application is running on all the servers in
     * the scope. If the
     * Scope is a {@link Cluster} all the members of the cluster are checked. This method returns {@link ApplicationStatus#STOPPED} if the application is not running on any of the
     * servers in the Scope
     * and at least one of the node manager servers is running. This method returns {@link ApplicationStatus#UNKNOWN} if the application is not running on any of the servers in the
     * Scope
     * and the node manager is not available. This method returns {@link ApplicationStatus#PARTIALLY_STARTED} if the application is running on at least one of the servers in the
     * scope but not all.
     * 
     * @param scope The {@link Scope} to query
     * @return The aggregate status of the application on the specified Scope
     * @throws Exception
     */
    public ApplicationStatus getApplicationStatus(Scope scope) throws Exception {
        final String method = "getApplicationStatus";
        Log.entering(c, method, scope);
        ApplicationStatus status = ApplicationStatus.STARTED;
        Log.exiting(c, method, status);
        return status;
    }

    /**
     * Get the aggregate status of an application across all deployed targets. This method returns {@link ApplicationStatus#STARTED} if the application is running on all deployed
     * targets. This method
     * returns {@link ApplicationStatus#STOPPED} if the application is not running on any of the deployed
     * targets and the application status is not {@link ApplicationStatus#UNKNOWN} on at least one of the
     * targets. {@link ApplicationStatus#UNKNOWN} is returned if the manager server is not running on all the
     * nodes of all the deployed targets. {@link ApplicationStatus#PARTIALLY_STARTED} is returned if the
     * application is running on at least one target but not all targets.
     * 
     * @return The status aggregate status of the application
     * @throws Exception
     */
    public ApplicationStatus getApplicationStatus() throws Exception {
        final String method = "getApplicationStatus";
        Log.entering(c, method);
        ApplicationStatus status = ApplicationStatus.STARTED;
        Log.exiting(c, method, status);
        return status;
    }

    /**
     * @return The ApplicationManager instance that manages this application.
     */
    public ApplicationManager getApplicationManager() {
        return this.applications;
    }

    private ApplicationStatus waitForState(ApplicationStatus expected, long timeout) throws Exception {
        ApplicationStatus result = ApplicationStatus.UNKNOWN;

        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end && result != expected) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
            result = getApplicationStatus();
        }

        return result;
    }

}