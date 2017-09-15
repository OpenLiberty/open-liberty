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

public class MapRunAsRolesToUsersEntry extends TaskEntry {

    public MapRunAsRolesToUsersEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getRole() {
        return super.getRole();
    }

    protected void setRole(String value) {
        super.setRole(value);
    }

    public String getUser() {
        return super.getUser();
    }

    public void setUser(String value) {
        task.setModified();
        super.setUser(value);
    }

    public String getPassword() {
        return super.getPassword();
    }

    public void setPassword(String value) {
        task.setModified();
        super.setPassword(value);
    }

}
