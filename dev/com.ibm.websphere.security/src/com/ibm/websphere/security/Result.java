/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security;

import java.util.List;

/**
 * This module is used by User Registries in WebSphere when calling the
 * getUsers and getGroups method. The user registries should use this
 * to set the list of users/groups and to indicate if there are more
 * users/groups in the registry than requested.
 * 
 * @ibm-spi
 */
public class Result implements java.io.Serializable {
    /**
     * Default constructor
     */
    public Result() {}

    /**
     * Returns the list of users/groups
     * 
     * @return the list of users/groups
     */
    public List getList() {
        return list;
    }

    /**
     * indicates if there are more users/groups in the registry
     */
    public boolean hasMore() {
        return more;
    }

    /**
     * Set the flag to indicate that there are more users/groups in the registry to true
     */
    public void setHasMore() {
        more = true;
    }

    /*
     * Set the list of user/groups
     * 
     * @param list list of users/groups
     */
    public void setList(List list) {
        this.list = list;
    }

    private boolean more = false;
    private List list;
    private static final long serialVersionUID = -9026260195868247308L; //@vj1: Take versioning into account if incompatible changes are made to this class

}
