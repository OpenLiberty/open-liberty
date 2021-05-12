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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.web.common.AuthConstraint;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.NestingUtils;

public class AuthConstraintImpl implements AuthConstraint {

    private List<String> roleNames;
    private final List<Description> descriptions = new ArrayList<Description>();

    public AuthConstraintImpl(Map<String, Object> config) {
        String[] value = (String[]) config.get("role-name");
        if (value != null)
            roleNames = Arrays.asList(value);

        List<Map<String, Object>> descriptionConfigs = NestingUtils.nest("description", config);
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
