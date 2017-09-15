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

import java.util.List;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.Cluster;
import com.ibm.websphere.simplicity.Scope;
import com.ibm.websphere.simplicity.exception.InvalidTargetException;

public class MapModulesToServersEntry extends TaskEntry {

    public MapModulesToServersEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getModule() {
        return super.getModule();
    }

    protected void setModule(String value) {
        super.setModule(value);
    }

    public String getUri() {
        return super.getUri();
    }

    protected void setUri(String value) {
        super.setUri(value);
    }

    public String getTarget() {
        return super.getServer();
    }

    public void setTarget(Scope value) throws Exception {
        validateTarget(value);
        task.setModified();
        if (value instanceof ApplicationServer)
            super.setServer(((ApplicationServer) value).getMappingName());
        else
            super.setServer(((Cluster) value).getMappingName());
    }

    public void setTarget(ApplicationServer value) {
        task.setModified();
        super.setServer(value.getMappingName());
    }

    public void setTargets(List<Scope> values) throws Exception {
        task.setModified();
        String target = "";
        for (int i = 0; i < values.size(); ++i) {
            validateTarget(values.get(i));
            if (values.get(i) instanceof ApplicationServer) {
                target += (((ApplicationServer) values.get(i)).getMappingName() + "+");
            } else {
                target += (((Cluster) values.get(i)).getMappingName() + "+");
            }
        }
        super.setServer(target.substring(0, target.length() - 1));
    }

    public void setTarget(String value) {
        task.setModified();
        super.setServer(value);
    }

    private void validateTarget(Scope target) throws Exception {
        if (!(target instanceof ApplicationServer || target instanceof Cluster))
            throw new InvalidTargetException();
    }

}
