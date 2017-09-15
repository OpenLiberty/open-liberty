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

public class EJBDeployOptionsTask extends ApplicationTask {

    public EJBDeployOptionsTask() {

    }

    public EJBDeployOptionsTask(String[][] taskData) {
        super(AppConstants.EJBDeployOptionsTask, taskData);
    }

    public EJBDeployOptionsTask(String[] columns) {
        super(AppConstants.EJBDeployOptionsTask, columns);
    }

    public String getClasspath() {
        return getString(AppConstants.APPDEPL_DEPLOYEJB_CLASSPATH_OPTION, 1);
    }

    public void setClasspath(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DEPLOYEJB_CLASSPATH_OPTION, 1, value);
    }

    public String getRmic() {
        return getString(AppConstants.APPDEPL_DEPLOYEJB_RMIC_OPTION, 1);
    }

    public void setRmic(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DEPLOYEJB_RMIC_OPTION, 1, value);
    }

    public String getDbType() {
        return getString(AppConstants.APPDEPL_DEPLOYEJB_DBTYPE_OPTION, 1);
    }

    public void setDbType(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DEPLOYEJB_DBTYPE_OPTION, 1, value);
    }

    public String getDbSchema() {
        return getString(AppConstants.APPDEPL_DEPLOYEJB_DBSCHEMA_OPTION, 1);
    }

    public void setDbSchema(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DEPLOYEJB_DBSCHEMA_OPTION, 1, value);
    }

    public String getComplianceLevel() throws Exception {
        hasAtLeast(5);
        return getString(AppConstants.APPDEPL_DEPLOYEJB_COMPLIANCE_LEVEL_OPTION, 1);
    }

    public void setComplianceLevel(String value) throws Exception {
        hasAtLeast(5);
        modified = true;
        setItem(AppConstants.APPDEPL_DEPLOYEJB_COMPLIANCE_LEVEL_OPTION, 1, value);
    }

    public String getDbAccessType() throws Exception {
        hasAtLeast(5);
        return getString(AppConstants.APPDEPL_DEPLOYEJB_DBACCESSTYPE_OPTION, 1);
    }

    public void setDbAccessType(String value) throws Exception {
        hasAtLeast(5);
        modified = true;
        setItem(AppConstants.APPDEPL_DEPLOYEJB_DBACCESSTYPE_OPTION, 1, value);
    }

    public String getSqlJClasspath() throws Exception {
        hasAtLeast(5);
        return getString(AppConstants.APPDEPL_DEPLOYEJB_SQLJCLASSPATH_OPTION, 1);
    }

    public void setSqlJClasspath(String value) throws Exception {
        hasAtLeast(5);
        modified = true;
        setItem(AppConstants.APPDEPL_DEPLOYEJB_SQLJ_ACCESS_OPTION, 1, value);
    }

}
