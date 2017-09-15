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

public class WSDeployOptionsTask extends ApplicationTask {

    public WSDeployOptionsTask() {

    }

    public WSDeployOptionsTask(String[] columns) {
        super(AppConstants.WSDeployOptionsTask, columns);
    }

    public WSDeployOptionsTask(String[][] data) {
        super(AppConstants.WSDeployOptionsTask, data);
    }

    public String getDeploywsClasspath() {
        return getString(AppConstants.APPDEPL_DEPLOYWS_CLASSPATH, 1);
    }

    public void setDeployWsClasspath(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DEPLOYWS_CLASSPATH, 1, value);
    }

    public String getDeploywsJarDirs() {
        return getString(AppConstants.APPDEPL_DEPLOYWS_JARDIRS, 1);
    }

    public void setDeployWsJarDirs(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DEPLOYWS_JARDIRS, 1, value);
    }
}
