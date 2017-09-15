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

public class MapWebModToVHEntry extends TaskEntry {

    public MapWebModToVHEntry(String[] data, MultiEntryApplicationTask task) {
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

    public String getVirtualHost() {
        return getString(AppConstants.APPDEPL_VIRTUAL_HOST);
    }

    public void setVirtualHost(String value) {
        task.setModified();
        setItem(AppConstants.APPDEPL_VIRTUAL_HOST, value);
    }

}
