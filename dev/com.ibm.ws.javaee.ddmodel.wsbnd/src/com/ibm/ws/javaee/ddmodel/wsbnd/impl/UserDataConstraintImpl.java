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
import com.ibm.ws.javaee.dd.web.common.UserDataConstraint;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.NestingUtils;

/**
 *
 */
public class UserDataConstraintImpl implements UserDataConstraint {
    private final List<Description> descriptions = new ArrayList<Description>();
    private int transportGuarantee;

    /**
     * @param map
     */
    public UserDataConstraintImpl(Map<String, Object> config) {
        List<Map<String, Object>> descriptionConfigs = NestingUtils.nest("description", config);
        if (descriptionConfigs != null) {
            for (Map<String, Object> descriptionConfig : descriptionConfigs) {
                descriptions.add(new DescriptionImpl(descriptionConfig));
            }
        }

        Object tg = config.get("transport-guarantee");
        if (tg != null)
            transportGuarantee = (Integer) tg;
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
     * @see com.ibm.ws.javaee.dd.web.common.UserDataConstraint#getTransportGuarantee()
     */
    @Override
    public int getTransportGuarantee() {
        return transportGuarantee;
    }

}
