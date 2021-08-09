/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.NestingUtils;

public class SecurityRoleImpl implements SecurityRole {

    private final String roleName;
    private final List<Description> descriptions = new ArrayList<Description>();

    /**
     * @param securityRoleConfig
     */
    public SecurityRoleImpl(Map<String, Object> config) {
        roleName = (String) config.get("role-name");
        List<Map<String, Object>> descriptionConfigs = NestingUtils.nest("description", config);
        if (descriptionConfigs != null) {
            for (Map<String, Object> descriptionConfig : descriptionConfigs) {
                descriptions.add(new DescriptionImpl(descriptionConfig));
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.common.Describable#getDescriptions()
     */
    @Override
    public List<Description> getDescriptions() {
        return descriptions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.common.SecurityRole#getRoleName()
     */
    @Override
    public String getRoleName() {
        return roleName;
    }

}
