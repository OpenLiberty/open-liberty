/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.spec;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.security.authorization.jacc.role.FileRoleMapping;

@Component(immediate = true, name = "com.ibm.ws.security.authorization.jacc.provider", configurationPolicy = ConfigurationPolicy.OPTIONAL, property = { "service.vendor=IBM" })
public class UserFeatureService {

    private static final String CFG_ROLE_MAPPING_FILE = "roleMappingFile";

    public UserFeatureService() {
    }

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        FileRoleMapping.initialize(getRoleMappingFile(props));
    }

    @Modified
    protected synchronized void modify(Map<String, Object> props) {
        FileRoleMapping.initialize(getRoleMappingFile(props));
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
    }

    private String getRoleMappingFile(Map<String, Object> props) {
        String roleMappingFile = null;
        if (props != null) {
            roleMappingFile = (String) props.get(CFG_ROLE_MAPPING_FILE);
        }
        return roleMappingFile;
    }
}
