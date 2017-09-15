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

public class MapRunAsRolesToUsersTask extends MultiEntryApplicationTask {

    public MapRunAsRolesToUsersTask() {

    }

    public MapRunAsRolesToUsersTask(String[][] taskData) {
        super(AppConstants.MapRunAsRolesToUsersTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new MapRunAsRolesToUsersEntry(data, this));
        }
    }

    public MapRunAsRolesToUsersTask(String[] columns) {
        super(AppConstants.MapRunAsRolesToUsersTask, columns);
    }

    @Override
    public MapRunAsRolesToUsersEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (MapRunAsRolesToUsersEntry) entries.get(i);
    }

    public void deleteRoleMap(String role) {
        modified = true;
        MapRunAsRolesToUsersEntry entry = getRoleMap(role);
        if (entry != null)
            entry.deleteEntry();
    }

    public MapRunAsRolesToUsersEntry getRoleMap(String role) {
        return (MapRunAsRolesToUsersEntry) getEntry(AppConstants.APPDEPL_ROLE, role);
    }

    public void setRoleMap(String role, String user, String password) throws Exception {
        modified = true;
        MapRunAsRolesToUsersEntry entry = getRoleMap(role);
        if (entry == null) {
            entry = new MapRunAsRolesToUsersEntry(new String[coltbl.size()], this);
            entry.setRole(role);
            entries.add(entry);
        }
        entry.setUser(user);
        entry.setPassword(password);
    }

}
