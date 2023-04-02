/*******************************************************************************
 * Copyright (c) 2017,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ibm.ws.config.xml.nester.Nester;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.web.common.AuthConstraint;

public class AuthConstraintImpl implements AuthConstraint {

    private List<String> roleNames;
    private final List<Description> descriptions = new ArrayList<Description>();

    public AuthConstraintImpl(Map<String, Object> config) {
        String[] value = (String[]) config.get("role-name");
        if (value != null)
            roleNames = Arrays.asList(value);

        List<Map<String, Object>> descriptionConfigs = Nester.nest("description", config);
        if (descriptionConfigs != null) {
            for (Map<String, Object> descriptionConfig : descriptionConfigs) {
                descriptions.add(new DescriptionImpl(descriptionConfig));
            }
        }
    }

    @Override
    public List<Description> getDescriptions() {
        return descriptions;
    }

    @Override
    public List<String> getRoleNames() {
        return roleNames;
    }
}
