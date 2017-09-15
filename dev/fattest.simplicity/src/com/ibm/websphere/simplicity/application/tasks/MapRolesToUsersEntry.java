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

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.simplicity.application.AppConstants;

/**
 * Represents a single mapping of a role (specific in application.xml)
 * to an arbitrary target.
 */
public class MapRolesToUsersEntry extends TaskEntry {

    public MapRolesToUsersEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    /**
     * @return The role represented by this instance.
     */
    public String getRole() {
        return super.getRole();
    }

    protected void setRole(String value) {
        super.setRole(value);
    }

    /**
     * Clear the "everyone" field.
     */
    public void clearEveryone() {
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_EVERYONE, "");
    }

    /**
     * @return True if the role applies to both authenticated and unauthenticated users.
     */
    public boolean getEveryone() {
        return getBoolean(AppConstants.APPDEPL_ROLE_EVERYONE);
    }

    /**
     * @param value True if the role should apply to both authenticated and unauthenticated users.
     */
    public void setEveryone(boolean value) {
        task.setModified();
        setBoolean(AppConstants.APPDEPL_ROLE_EVERYONE, value);
    }

    /**
     * Clear the "all-authenticated-users" field.
     */
    public void clearAllAuthenticatedUsers() {
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_ALL_AUTH_USER, "");
    }

    /**
     * @return True if the role applies to all authenticated users.
     */
    public boolean getAllAuthenticatedUsers() {
        return getBoolean(AppConstants.APPDEPL_ROLE_ALL_AUTH_USER);
    }

    /**
     * @param value True if the role should apply to all authenticated users.
     */
    public void setAllAuthenticatedUsers(boolean value) {
        task.setModified();
        setBoolean(AppConstants.APPDEPL_ROLE_ALL_AUTH_USER, value);
    }

    /**
     * Clear the "user" field.
     */
    public void clearUser() {
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_USER, "");
    }

    /**
     * @param value Adds the specified user to the list without replacing the existing users.
     */
    public void addUser(String value) {
        task.setModified();
        String users = getUser();
        if (users == null)
            users = "";

        if (users != "")
            users += "|";
        users += value;
        setUser(users);
    }

    /**
     * @return The user to which the role has been mapped. If multiple users are mapped, this string will be pipe-delimited.
     */
    public String getUser() {
        return super.getRoleUser();
    }

    /**
     * @return The users to which the role has been mapped.
     */
    public List<String> getUsers() {
        String[] users = super.getRoleUser().split("\\|");
        List<String> ret = new ArrayList<String>(users.length);
        for (String user : users)
            ret.add(user);
        return ret;
    }

    /**
     * @param value The user to remove from the list of configured users.
     */
    public void removeUser(String value) {
        task.setModified();
        List<String> users = getUsers();
        List<String> res = new ArrayList<String>();
        for (String s : users)
            if (!s.equalsIgnoreCase(value))
                res.add(s);
        setUsers(res);
    }

    /**
     * @param value The user to which the role should apply. Replaces any existing configured user.
     */
    public void setUser(String value) {
        task.setModified();
        super.setRoleUser(value);
    }

    /**
     * @param users The users to which the role should apply. Replaces any existing configured users.
     */
    public void setUsers(List<String> users) {
        clearUser();
        for (String user : users)
            addUser(user);
    }

    /**
     * Clear the "group" field.
     */
    public void clearGroup() {
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_GROUP, "");
    }

    /**
     * @param value Adds the specified group to the list without replacing the existing groups.
     */
    public void addGroup(String value) {
        task.setModified();
        String groups = getGroup();
        if (groups == null)
            groups = "";

        if (groups != "")
            groups += "|";
        groups += value;
        setGroup(groups);
    }

    /**
     * @return The group to which the role has been mapped.
     */
    public String getGroup() {
        return getString(AppConstants.APPDEPL_ROLE_GROUP);
    }

    /**
     * @return The groups to which the role has been mapped.
     */
    public List<String> getGroups() {
        String[] groups = getGroup().split("\\|");
        List<String> ret = new ArrayList<String>(groups.length);
        for (String group : groups)
            ret.add(group);
        return ret;
    }

    /**
     * @param value The group to remove from the list of configured groups.
     */
    public void removeGroup(String value) {
        task.setModified();
        List<String> groups = getGroups();
        List<String> res = new ArrayList<String>();
        for (String s : groups)
            if (!s.equalsIgnoreCase(value))
                res.add(s);
        setGroups(res);
    }

    /**
     * @param value The group to which the role should apply.
     */
    public void setGroup(String value) {
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_GROUP, value);
    }

    /**
     * @param groups The groups to which the role should apply. Replaces any existing configured groups.
     */
    public void setGroups(List<String> groups) {
        clearGroup();
        for (String user : groups)
            addGroup(user);
    }

    /**
     * Clear the "all-authenticated-realms" field.
     */
    public void clearAllAuthenticatedRealms() {
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_ALL_AUTH_REALMS, "");
    }

    /**
     * @return True if the role applies to all authenticated realms.
     * @throws Exception
     */
    public boolean getAllAuthenticatedRealms() throws Exception {
        hasAtLeast(6);
        return getBoolean(AppConstants.APPDEPL_ROLE_ALL_AUTH_REALMS);
    }

    /**
     * @param value True if the role should apply to all authenticated realms.
     * @throws Exception
     */
    public void setAllAuthenticatedRealms(boolean value) throws Exception {
        hasAtLeast(6);
        task.setModified();
        setBoolean(AppConstants.APPDEPL_ROLE_ALL_AUTH_REALMS, value);
    }

    /**
     * Clear the user access ID list.
     */
    public void clearUserAccessIds() {
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_EVERYONE, "");
    }

    /**
     * @return A pipe (|) delimited list of group access IDs.
     * @throws Exception
     */
    public String getUserAccessIds() throws Exception {
        hasAtLeast(6);
        return getString(AppConstants.APPDEPL_ROLE_USER_ACCESS_IDS);
    }

    /**
     * @param value A pipe (|) delimited list of user access IDs.
     * @throws Exception
     */
    public void setUserAccessIds(String value) throws Exception {
        hasAtLeast(6);
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_USER_ACCESS_IDS, value);
    }

    /**
     * Clear the group access ID list.
     */
    public void clearGroupAccessIds() {
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_GROUP_ACCESS_IDS, "");
    }

    /**
     * @return A pipe (|) delimited list of group access IDs.
     * @throws Exception
     */
    public String getGroupAccessIds() throws Exception {
        hasAtLeast(6);
        return getString(AppConstants.APPDEPL_ROLE_GROUP_ACCESS_IDS);
    }

    /**
     * @param value A pipe (|) delimited list of group access IDs.
     * @throws Exception
     */
    public void setGroupAccessIds(String value) throws Exception {
        hasAtLeast(6);
        task.setModified();
        setItem(AppConstants.APPDEPL_ROLE_GROUP_ACCESS_IDS, value);
    }

}
