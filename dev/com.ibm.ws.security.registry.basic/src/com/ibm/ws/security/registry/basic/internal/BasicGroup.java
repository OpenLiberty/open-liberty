/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.basic.internal;

import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Simple container class for a group defined in the server.xml
 */
@Trivial
class BasicGroup {
    private final String name;
    private final Set<String> members;

    /**
     * BasicGroup representing the name and members of the group.
     * 
     * @param name group securityName
     * @param members group members
     */
    BasicGroup(String name, Set<String> members) {
        this.name = name;
        this.members = members;
    }

    /**
     * Return the group securityName.
     * 
     * @return group securityName
     */
    String getName() {
        return name;
    }

    /**
     * Return the group's members.
     * 
     * @return Set of all group members
     */
    Set<String> getMembers() {
        return members;
    }

    /**
     * {@inheritDoc} Equality of a BasicGroup is based only
     * on the name of the group. The members are not relevant.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicGroup)) {
            return false;
        } else {
            BasicGroup that = (BasicGroup) obj;
            return this.name.equals(that.name);
        }
    }

    /**
     * {@inheritDoc} Return the group name.
     */
    @Override
    public String toString() {
        return name + ", " + members;
    }

    /**
     * {@inheritDoc} Returns a hash of the name.
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
