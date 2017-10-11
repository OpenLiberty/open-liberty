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

import java.util.List;
import java.util.Map;

/**
 * This class is an internal representation of a single group.
 */
class GroupInfo {
    String name = null;
    String externalName = null;
    String uniqueName = null;
    List<String> members = null;
    Map<String, Object> props = null;

    /**
     * Construct a new {@link GroupInfo} instance.
     *
     * @param name The group's name.
     * @param externalName The group's external name.
     * @param uniqueName The group's unique name.
     * @param members The members of this group.
     * @param props The properties for this group.
     */
    GroupInfo(String name, String externalName, String uniqueName, List<String> members, Map<String, Object> props) {
        this.name = name;
        this.externalName = externalName;
        this.uniqueName = uniqueName;
        this.members = members;
        this.props = props;

        System.out.println("    Add groupInfo in memory: \n");
        System.out.println("            name: " + name);
        System.out.println("            externalName: " + externalName);
        System.out.println("            uniqueName: " + uniqueName);
        System.out.println("            members: " + (members == null || members.isEmpty() ? "<none>" : members.toString()));
        System.out.println("            props: " + (props == null || props.isEmpty() ? "<none>" : props.toString()));
    }

    /**
     * Get the group's name.
     *
     * @return The group's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get this group's external name.
     *
     * @return The group's external name.
     */
    public String getExternalName() {
        return externalName;
    }

    /**
     * Get this group's unique name. This name should be unique among all repositories.
     *
     * @return The group's unique name.
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Get a list of users that are members of this group.
     *
     * @return The list of users that are a member of this group.
     */
    public List<String> getMembers() {
        return members;
    }

    /**
     * Get a mapping of properties to their values for the group.
     *
     * @return The map of properties to their values.
     */
    public Map<String, Object> getProps() {
        return props;
    }
}
