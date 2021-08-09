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
package com.ibm.ws.security.wim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RepositoryPropertyMap {

    HashMap<String, HashSet<String>> reposPropCache = null;
    Set<String> entityTypes = null;

    /**
     *
     */
    public RepositoryPropertyMap() {
        reposPropCache = new HashMap<String, HashSet<String>>();
        entityTypes = new HashSet<String>();
    }

    public Set<String> getRepositoryPropertySetByEntityType(String entityType) {
        HashSet<String> propNameSet = null;
        if (entityType != null && reposPropCache != null) {
            propNameSet = (HashSet<String>) reposPropCache.get(entityType);
        }
        return propNameSet;
    }

    public void setRepositoryPropertySetByEntityType(String entityType, HashSet<String> propSet) {
        if (entityType != null && propSet != null) {
            reposPropCache.put(entityType, propSet);
            entityTypes.add(entityType);
        }
    }

    public Set<String> getEntityTypes() {
        return entityTypes;
    }
}
