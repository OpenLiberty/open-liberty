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

public class MapRolesToUsersTask extends MultiEntryApplicationTask {

    public MapRolesToUsersTask() {

    }

    public MapRolesToUsersTask(String[][] taskData) {
        super(AppConstants.MapRolesToUsersTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new MapRolesToUsersEntry(data, this));
        }
    }

    public MapRolesToUsersTask(String[] columns) {
        super(AppConstants.MapRolesToUsersTask, columns);
    }

    @Override
    public MapRolesToUsersEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (MapRolesToUsersEntry) entries.get(i);
    }

    public void deleteRoleMap(String role) {
        MapRolesToUsersEntry entry = getRoleMap(role);
        if (entry != null)
            entry.deleteEntry();
    }

    public MapRolesToUsersEntry getRoleMap(String role) {
        return (MapRolesToUsersEntry) getEntry(AppConstants.APPDEPL_ROLE, role);
    }

    public void clearRole(String role) throws Exception {
        MapRolesToUsersEntry entry = getRoleMap(role);
        if (entry != null)
            this.delete(entry);
    }

    public void addGroupToRole(String role, String group) throws Exception {
        MapRolesToUsersEntry entry = getRoleMapInternal(role);
        entry.addGroup(group);
    }

    public void addUserToRole(String role, String user) throws Exception {
        MapRolesToUsersEntry entry = getRoleMapInternal(role);
        entry.addUser(user);
    }

    public void removeGroupFromRole(String role, String group) throws Exception {
        MapRolesToUsersEntry entry = getRoleMapInternal(role);
        entry.removeGroup(group);
    }

    public void removeUserFromRole(String role, String user) throws Exception {
        MapRolesToUsersEntry entry = getRoleMapInternal(role);
        entry.removeUser(user);
    }

    public void setRoleToEveryone(String role) throws Exception {
        setRoleMap(role, true, false, false, null, null, null, null);
    }

    public void setRoleToAllAuthenticatedRealms(String role) throws Exception {
        setRoleMap(role, false, true, false, null, null, null, null);
    }

    public void setRoleToAllAuthenticatedUsers(String role) throws Exception {
        setRoleMap(role, false, false, true, null, null, null, null);
    }

    public void setRoleToGroup(String role, String group) throws Exception {
        setRoleMap(role, false, false, false, group, null, null, null);
    }

    public void setRoleToGroupAccessIds(String role, String groupAccessIds) throws Exception {
        setRoleMap(role, false, false, false, null, groupAccessIds, null, null);
    }

    public void setRoleToUser(String role, String user) throws Exception {
        setRoleMap(role, false, false, false, null, null, user, null);
    }

    public void setRoleToUserAccessIds(String role, String userAccessIds) throws Exception {
        setRoleMap(role, false, false, false, null, null, null, userAccessIds);
    }

    private void setRoleMap(String role, boolean everyone, boolean allAuthenticatedRealms, boolean allAuthenticatedUsers, String group, String groupAccessIds, String user,
                            String userAccessIds) throws Exception {
        modified = true;
        MapRolesToUsersEntry entry = getRoleMapInternal(role);

        entry.clearEveryone();
        entry.clearAllAuthenticatedRealms();
        entry.clearAllAuthenticatedUsers();
        entry.clearGroup();
        entry.clearGroupAccessIds();
        entry.clearUser();
        entry.clearUserAccessIds();

        if (everyone)
            entry.setEveryone(everyone);
        else if (allAuthenticatedRealms)
            entry.setAllAuthenticatedRealms(allAuthenticatedRealms);
        else if (allAuthenticatedUsers)
            entry.setAllAuthenticatedUsers(allAuthenticatedUsers);
        else if (group != null)
            entry.setGroup(group);
        else if (groupAccessIds != null)
            entry.setGroupAccessIds(groupAccessIds);
        else if (user != null)
            entry.setUser(user);
        else if (userAccessIds != null)
            entry.setUserAccessIds(userAccessIds);
    }

    private MapRolesToUsersEntry getRoleMapInternal(String role) {
        MapRolesToUsersEntry entry = getRoleMap(role);
        if (entry == null) {
            entry = new MapRolesToUsersEntry(new String[coltbl.size()], this);
            entry.setRole(role);
            entries.add(entry);
        }
        return entry;
    }

}
