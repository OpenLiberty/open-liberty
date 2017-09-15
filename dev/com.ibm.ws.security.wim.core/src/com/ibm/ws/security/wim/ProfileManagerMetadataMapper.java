/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.ws.security.wim.xpath.util.MetadataMapper;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Entity;

public class ProfileManagerMetadataMapper implements MetadataMapper {

    private List<String> entityTypes = null;
    RepositoryPropertyMap laPropertyNames = null;
    RepositoryPropertyMap reposPropertyNames = null;

    public ProfileManagerMetadataMapper(String reposId, List<String> entityTypes) throws WIMException {
        this.entityTypes = entityTypes;

        PropertyManager propMgr = PropertyManager.singleton();
        laPropertyNames = propMgr.getLookAsidePropertyNameMap();
        reposPropertyNames = propMgr.getPropertyMapByRepositoryId(reposId);
    }

    @Override
    public boolean isPropertyInLookAside(String propertyName, String entityType) {
        boolean inRepository = false;
        if (laPropertyNames != null && propertyName != null) {
            Set<String> propertyNames = laPropertyNames.getRepositoryPropertySetByEntityType(entityType);
            if (propertyNames != null) {
                inRepository = propertyNames.contains(propertyName);
            } else {
                try {
                    Set<String> subEntTypes = Entity.getSubEntityTypes(entityType);
                    if (subEntTypes != null) {
                        Iterator<String> iter = subEntTypes.iterator();
                        while (iter.hasNext() && !inRepository) {
                            propertyNames = laPropertyNames.getRepositoryPropertySetByEntityType(iter.next());
                            if (propertyNames != null) {
                                inRepository = propertyNames.contains(propertyName);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return inRepository;
    }

    @Override
    public boolean isPropertyInRepository(String propertyName, String entityType) {
        boolean inRepository = false;
        if (reposPropertyNames != null && propertyName != null) {
            Set<String> propertyNames = reposPropertyNames.getRepositoryPropertySetByEntityType(entityType);
            if (propertyNames != null) {
                inRepository = propertyNames.contains(propertyName);
            } else {
                try {
                    Set<String> subEntTypes = Entity.getSubEntityTypes(entityType);
                    if (subEntTypes != null) {
                        Iterator<String> iter = subEntTypes.iterator();
                        while (iter.hasNext() && !inRepository) {
                            propertyNames = reposPropertyNames.getRepositoryPropertySetByEntityType(iter.next());
                            if (propertyNames != null) {
                                inRepository = propertyNames.contains(propertyName);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return inRepository;
    }

    @Override
    public boolean isValidEntityType(String entityType) {
        boolean valid = false;
        if (entityTypes != null && entityType != null && entityType.length() != 0) {
            valid = entityTypes.contains(entityType);
        }
        return valid;
    }

}
