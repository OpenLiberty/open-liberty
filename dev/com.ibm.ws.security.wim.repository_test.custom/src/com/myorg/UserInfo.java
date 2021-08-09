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

package com.myorg;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class is an internal representation of a single user.
 */
class UserInfo {

    String name = null;
    byte[] password = null;
    String externalName = null;
    String uniqueName = null;
    List<String> groups = null;
    Map<String, Object> props = null;

    /**
     * Construct a new {@link UserInfo} instance.
     *
     * @param name The name of the user.
     * @param password The user's password.
     * @param externalName The user's external name.
     * @param uniqueName The user's unique name.
     * @param groups The groups the user is a member of.
     * @param props The user's properties.
     */
    UserInfo(String name, byte[] password, String externalName, String uniqueName, List<String> groups, Map<String, Object> props) {
        this.name = name;
        this.password = password;
        this.externalName = externalName;
        this.uniqueName = uniqueName;
        this.groups = groups;
        this.props = props;

        System.out.println("    Add userInfo in memory: \n");
        System.out.println("            name: " + name);
        System.out.println("            password: " + (password == null || password.length == 0 ? "<null>" : "xxxxxx"));
        System.out.println("            externalName: " + externalName);
        System.out.println("            uniqueName: " + uniqueName);
        System.out.println("            groups: " + (groups == null || groups.isEmpty() ? "<none>" : groups.toString()));
        System.out.println("            props: " + (props == null || props.isEmpty() ? "<none>" : props.toString()));
    }

    /**
     * Check the password for this user.
     *
     * @param inputPassword The password to check against this user's known password.
     * @return True if the passwords match.
     */
    public boolean checkPassword(byte[] inputPassword) {
        return Arrays.equals(inputPassword, this.password);
    }

    /**
     * Get the user's name.
     *
     * @return The user's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get this user's external name.
     *
     * @return The user's external name.
     */
    public String getExternalName() {
        return externalName;
    }

    /**
     * Get this user unique name. This name should be unique among all repositories.
     *
     * @return The user's unique name.
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Get a list of groups this user is a member of.
     *
     * @return The list of groups this user is a member of.
     */
    public List<String> getGroups() {
        return groups;
    }

    /**
     * Get a mapping of properties to their values for the user.
     *
     * @return The map of properties to their values.
     */
    public Map<String, Object> getProps() {
        return props;
    }
}
