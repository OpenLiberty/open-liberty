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
package com.ibm.websphere.simplicity.application.tasks;

import com.ibm.websphere.simplicity.application.AppConstants;
import com.ibm.websphere.simplicity.application.types.AsyncRequestDispatchType;

/**
 * 
 * 
 * @author SterlingBates
 * 
 */
public class AppDeploymentOptionsTask extends ApplicationTask {

    private String defaultAppName;
    private boolean useDefaultBindings = false;
    private boolean appNameModified = false;
    private boolean asyncDispatchModified = false;
    private boolean ejbDeployModified = false;
    private boolean filePermissionModified = false;
    private boolean precompileJspsModified = false;
    private boolean processEmbeddedConfigModified = false;
    private boolean reloadIntervalModified = false;
    private boolean useAutoLinkModified = false;
    private boolean useBinaryConfigModified = false;
    private boolean useDefaultBindingsModified = false;
    private boolean wsDeployModified = false;
    private boolean allowDispatchRemoteIncludeModified = false;
    private boolean allowServiceRemoteIncludeModified = false;
    private boolean blaNameModified = false;
    private boolean installDirectoryModified = false;

    public AppDeploymentOptionsTask() {

    }

    public AppDeploymentOptionsTask(String[][] taskData) {
        super(AppConstants.AppDeploymentOptionsTask, taskData);
        defaultAppName = getString(AppConstants.APPDEPL_APPNAME, 1);
        if (defaultAppName != null && defaultAppName.length() == 0)
            defaultAppName = null;
    }

    public AppDeploymentOptionsTask(String[] columns) {
        super(AppConstants.AppDeploymentOptionsTask, columns);
    }

    public String getDefaultApplicationName() {
        return this.defaultAppName;
    }

    public boolean getUseDefaultBindings() {
        return this.useDefaultBindings;
    }

    public void setUseDefaultBindings(boolean value) {
        useDefaultBindingsModified = true;
        this.useDefaultBindings = value;
    }

    public boolean getUseAutoLink() {
        return getBoolean(AppConstants.APPDEPL_AUTOLINK, 1, AppConstants.APPDEPL_AUTOLINK_DEFAULT);
    }

    public void setUseAutoLink(boolean value) {
        useAutoLinkModified = true;
        setBoolean(AppConstants.APPDEPL_AUTOLINK, 1, value);
    }

    public boolean getEjbDeploy() {
        return getBoolean(AppConstants.APPDEPL_DEPLOYEJB_CMDARG, 1, AppConstants.APPDEPL_DEPLOYEJB_CMDARG_DEFAULT);
    }

    public void setEjbDeploy(boolean value) {
        ejbDeployModified = true;
        setBoolean(AppConstants.APPDEPL_DEPLOYEJB_CMDARG, 1, value);
    }

    public boolean getWsDeploy() {
        return getBoolean(AppConstants.APPDEPL_DEPLOYWS_CMDARG, 1, AppConstants.APPDEPL_DEPLOYWS_CMDARG_DEFAULT);
    }

    public void setWsDeploy(boolean value) {
        wsDeployModified = true;
        setBoolean(AppConstants.APPDEPL_DEPLOYWS_CMDARG, 1, value);
    }

    public boolean getPrecompileJsps() {
        return getBoolean(AppConstants.APPDEPL_PRECOMPILE_JSP, 1, AppConstants.APPDEPL_PRECOMPILE_JSP_DEFAULT);
    }

    public void setPrecompileJsps(boolean value) {
        precompileJspsModified = true;
        setBoolean(AppConstants.APPDEPL_PRECOMPILE_JSP, 1, value);
    }

    public boolean getUseBinaryConfig() {
        return getBoolean(AppConstants.APPDEPL_USE_BINARY_CONFIG, 1, AppConstants.APPDEPL_USE_BINARY_CONFIG_DEFAULT);
    }

    public void setUseBinaryConfig(boolean value) {
        useBinaryConfigModified = true;
        setBoolean(AppConstants.APPDEPL_USE_BINARY_CONFIG, 1, value);
    }

    public boolean getProcessEmbeddedConfig() {
        return getBoolean(AppConstants.APPDEPL_PROCESS_EMBEDDEDCFG_INSTALL, 1, AppConstants.APPDEPL_PROCESS_EMBEDDEDCFG_INSTALL_DEFAULT);
    }

    public void setProcessEmbeddedConfig(boolean value) {
        processEmbeddedConfigModified = true;
        setBoolean(AppConstants.APPDEPL_PROCESS_EMBEDDEDCFG_INSTALL, 1, value);
    }

    public String getFilePermission() {
        return getString(AppConstants.APPDEPL_FILEPERMISSION, 1, AppConstants.APPDEPL_FILEPERMISSION_DEFAULT);
    }

    public void setFilePermission(String value) {
        setItem(AppConstants.APPDEPL_FILEPERMISSION, 1, value);
    }

    public void addFilePermission(String filespec) {
        filePermissionModified = true;
        String s = getFilePermission() + "#" + filespec;
        setFilePermission(s);
    }

    public AsyncRequestDispatchType getAsyncRequestDispatch() {
        AsyncRequestDispatchType type = null;
        String s = getString(AppConstants.APPDEPL_ASYNC_REQUEST_DISPATCH, 1);
        if (s != null)
            type = AsyncRequestDispatchType.valueOf(s);
        return type;
    }

    public void setAsyncRequestDispatch(AsyncRequestDispatchType value) {
        asyncDispatchModified = true;
        setItem(AppConstants.APPDEPL_ASYNC_REQUEST_DISPATCH, 1, value.getValue());
    }

    public Integer getReloadInterval() {
        return getInteger(AppConstants.APPDEPL_RELOADINTERVAL, AppConstants.APPDEPL_RELOADINTERVAL_DEFAULT);
    }

    public void setReloadInterval(int value) {
        reloadIntervalModified = true;
        setInteger(AppConstants.APPDEPL_RELOADINTERVAL, 1, value);
    }

    public String getApplicationName() {
        return getString(AppConstants.APPDEPL_APPNAME, 1);
    }

    public void setApplicationName(String value) {
        appNameModified = true;
        setItem(AppConstants.APPDEPL_APPNAME, 1, value);
    }

    public boolean isAppNameModified() {
        return appNameModified;
    }

    public boolean isAsyncDispatchModified() {
        return asyncDispatchModified;
    }

    public boolean isEjbDeployModified() {
        return ejbDeployModified;
    }

    public boolean isFilePermissionModified() {
        return filePermissionModified;
    }

    public boolean isPrecompileJspsModified() {
        return precompileJspsModified;
    }

    public boolean isProcessEmbeddedConfigModified() {
        return processEmbeddedConfigModified;
    }

    public boolean isReloadIntervalModified() {
        return reloadIntervalModified;
    }

    public boolean isUseAutoLinkModified() {
        return useAutoLinkModified;
    }

    public boolean isUseBinaryConfigModified() {
        return useBinaryConfigModified;
    }

    public boolean isUseDefaultBindingsModified() {
        return useDefaultBindingsModified;
    }

    public boolean isWsDeployModified() {
        return wsDeployModified;
    }

    public boolean isAllowDispatchRemoteIncludeModified() {
        return this.allowDispatchRemoteIncludeModified;
    }

    public boolean isAllowServiceRemoteIncludeModified() {
        return this.allowServiceRemoteIncludeModified;
    }

    public boolean getAllowDispatchRemoteInclude() {
        return getBoolean(AppConstants.APPDEPL_DISPATCH_REMOTEINCLUDE, 1, AppConstants.APPDEPL_DISPATCH_REMOTEINCLUDE_DEFAULT);
    }

    public void setAllowDispatchRemoteInclude(boolean allowDispatchRemoteInclude) {
        allowDispatchRemoteIncludeModified = true;
        setBoolean(AppConstants.APPDEPL_DISPATCH_REMOTEINCLUDE, 1, allowDispatchRemoteInclude);
    }

    public boolean getAllowServiceRemoteInclude() {
        return getBoolean(AppConstants.APPDEPL_SERVICE_REMOTEINCLUDE, 1, AppConstants.APPDEPL_SERVICE_REMOTEINCLUDE_DEFAULT);
    }

    public void setAllowServiceRemoteInclude(boolean allowServiceRemoteInclude) {
        this.allowServiceRemoteIncludeModified = true;
        setBoolean(AppConstants.APPDEPL_SERVICE_REMOTEINCLUDE, 1, allowServiceRemoteInclude);
    }

    public String getBlaName() {
        return getString(AppConstants.APPDEPL_BLANAME, 1);
    }

    public void setBlaName(String value) {
        blaNameModified = true;
        setItem(AppConstants.APPDEPL_BLANAME, 1, value);
    }

    public boolean isBlaNameModified() {
        return blaNameModified;
    }

    public String getInstallDirectory() {
        return getString(AppConstants.APPDEPL_INSTALL_DIR, 1);
    }

    public void setInstallDirectory(String value) {
        installDirectoryModified = true;
        setItem(AppConstants.APPDEPL_INSTALL_DIR, 1, value);
    }

    public boolean isInstallDirectoryModified() {
        return installDirectoryModified;
    }

    @Override
    protected void init() {
        super.init();
        if (getReloadInterval() == null)
            setReloadInterval(0);
    }

}
