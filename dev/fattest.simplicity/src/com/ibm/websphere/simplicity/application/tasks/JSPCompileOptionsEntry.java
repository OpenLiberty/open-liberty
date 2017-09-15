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

public class JSPCompileOptionsEntry extends TaskEntry {

    public JSPCompileOptionsEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getWebModule() {
        return super.getWebModule();
    }

    protected void setWebModule(String value) {
        super.setWebModule(value);
    }

    public String getUri() {
        return super.getUri();
    }

    protected void setUri(String value) {
        super.setUri(value);
    }

    public String getJspClasspath() {
        return getString(AppConstants.APPDEPL_PRECMPJSP_CLASSPATH_OPTION);
    }

    public void setJspClasspath(String value) {
        task.setModified();
        setItem(AppConstants.APPDEPL_PRECMPJSP_CLASSPATH_OPTION, value);
    }

    public boolean getUseFullPackageNames() {
        return getBoolean(AppConstants.APPDEPL_PRECMPJSP_USEFULLPACKAGENAMES, AppConstants.APPDEPL_PRECMPJSP_USEFULLPACKAGENAMES_DEFAULT);
    }

    public void setUseFullPackageNames(boolean value) {
        task.setModified();
        setBoolean(AppConstants.APPDEPL_PRECMPJSP_USEFULLPACKAGENAMES, value);
    }

    public String getSourceLevel() {
        return getString(AppConstants.APPDEPL_PRECMPJSP_SOURCELEVEL, AppConstants.APPDEPL_PRECMPJSP_SOURCELEVEL_DEFAULT);
    }

    /**
     * Corresponds to the Java version used to compile the JSP. The
     * string is the version number without dots. So Java 1.4 is "14".
     * 
     * @param value
     */
    public void setSourceLevel(String value) {
        task.setModified();
        setItem(AppConstants.APPDEPL_PRECMPJSP_SOURCELEVEL, value);
    }

    public boolean getDisableRuntimeCompilation() {
        return getBoolean(AppConstants.APPDEPL_PRECMPJSP_DISABLERTCOMPILE, AppConstants.APPDEPL_PRECMPJSP_DISABLERTCOMPILE_DEFAULT);
    }

    public void setDisableRuntimeCompilation(boolean value) {
        task.setModified();
        setBoolean(AppConstants.APPDEPL_PRECMPJSP_DISABLERTCOMPILE, value);
    }

}
