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

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.simplicity.Node;
import com.ibm.websphere.simplicity.OperationResults;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.Scope;
import com.ibm.websphere.simplicity.application.tasks.ApplicationTask;
import com.ibm.websphere.simplicity.application.types.UpdateContentType;
import com.ibm.websphere.simplicity.application.types.UpdateType;
import com.ibm.websphere.simplicity.exception.ApplicationNotInstalledException;
import com.ibm.websphere.simplicity.exception.NotImplementedException;
import com.ibm.websphere.simplicity.log.Log;

/**
 * Need to add code to request all installed modules and validation routines.
 * Port relevant code from following:
 * - GetLocs.java
 * ValidateUpdate.java
 * AppUpdateTest.java
 * ...
 * 
 * @author SterlingBates
 * 
 */
public class EnterpriseApplication extends Application {

    public EnterpriseApplication(ApplicationManager mgr, String name, Scope scope) throws Exception {
        super(mgr, ApplicationType.EAR, name, scope);
    }

    @Override
    public String getDestinationPath() {
        return getInstallLocation() + "/" + this.name + ".ear";
    }

    @Override
    public String getDestinationPath(Node node) {
        return getInstallLocation(node) + "/" + this.name + ".ear";
    }

    @Override
    public UpdateWrapper getUpdateWrapper(RemoteFile originalAppFile) throws Exception {
        return getUpdateWrapper();
    }

    @Override
    public UpdateWrapper getUpdateWrapper(UpdateType updateType, RemoteFile contents, UpdateContentType contentType) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public UpdateWrapper getUpdateWrapper() throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public OperationResults<Boolean> update(UpdateWrapper options) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public OperationResults<Boolean> start() throws Exception {
        Log.entering(c, "start");
        OperationResults<Boolean> results = new OperationResults<Boolean>(true);
        results.setResult(results.isSuccess());
        Log.exiting(c, "start", results.isSuccess());
        return results;
    }

    /**
     * Start an application on a specific {@link Scope}. The application will be started on that
     * Scope only.
     * 
     * @param target The target to start the application on
     * @return The {@link OperationResults} of the command
     */
    public OperationResults<Boolean> start(Scope target) throws Exception {
        final String method = "start";
        Log.entering(c, method, target);
        if (!isInstalled()) {
            Exception e = new ApplicationNotInstalledException(this.getName());
            Log.error(c, method, e);
            throw e;
        }

        OperationResults<Boolean> results = new OperationResults<Boolean>(true);
        results.setResult(results.isSuccess());

        Log.exiting(c, method, results.isSuccess());
        return results;
    }

    @Override
    public OperationResults<Boolean> stop() throws Exception {
        Log.entering(c, "stop");
        if (!isInstalled()) {
            Exception e = new ApplicationNotInstalledException(this.getName());
            Log.error(c, "stop", e);
            throw e;
        }
        OperationResults<Boolean> results = new OperationResults<Boolean>(false);
        results.setResult(results.isSuccess());
        Log.exiting(c, "stop", results.isSuccess());
        return results;
    }

    /**
     * Stop an application on a specific {@link Scope}. The application will be stopped on that
     * Scope only.
     * 
     * @param target The target to stop the application on
     * @return The {@link OperationResults} of the command
     */
    public OperationResults<Boolean> stop(Scope target) throws Exception {
        final String method = "stop";
        Log.entering(c, method, target);
        if (!isInstalled()) {
            Exception e = new ApplicationNotInstalledException(this.getName());
            Log.error(c, method, e);
            throw e;
        }

        OperationResults<Boolean> results = new OperationResults<Boolean>(false);
        results.setResult(results.isSuccess());

        Log.exiting(c, method, results.isSuccess());
        return results;
    }

    /**
     * Tests to see if the application has been distributed and is ready to be run.
     * TODO Rename this to isAppDistributed to reduce confusion with app state APIs
     * that identify started state.
     * 
     * @param ignoreUnknownState If true, any node or server with unknown state is not included in the final ready return
     * @return true if the application is ready; false otherwise.
     */
    public boolean isAppReady(boolean ignoreUnknownState) throws Exception {
        final String method = "isAppReady";
        Log.entering(c, method);

        boolean isReady = true;

        Log.exiting(c, method, isReady);
        return isReady;
    }

    @Override
    public OperationResults<Boolean> edit(EditWrapper options) throws Exception {
        Log.entering(c, "update", options);
        throw new NotImplementedException();
        //		Log.exiting(c, "update", results.getResult());
        //		return results;
    }

    @Override
    public EditWrapper getEditWrapper() throws Exception {
        List<ApplicationTask> tasks = new ArrayList<ApplicationTask>();
        return new EditWrapper(this, tasks, this.scope);
    }

}
