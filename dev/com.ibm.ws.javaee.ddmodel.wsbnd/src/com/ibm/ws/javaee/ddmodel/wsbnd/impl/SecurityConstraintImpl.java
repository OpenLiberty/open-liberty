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

import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.web.common.AuthConstraint;
import com.ibm.ws.javaee.dd.web.common.SecurityConstraint;
import com.ibm.ws.javaee.dd.web.common.UserDataConstraint;
import com.ibm.ws.javaee.dd.web.common.WebResourceCollection;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.NestingUtils;

public class SecurityConstraintImpl implements SecurityConstraint {

    private final List<DisplayName> displayNames = new ArrayList<DisplayName>();
    private final List<WebResourceCollection> webResourceCollections = new ArrayList<WebResourceCollection>();
    private AuthConstraint authConstraint;
    private UserDataConstraint userDataConstraint;

    /**
     * @param securityConstraintConfig
     */
    public SecurityConstraintImpl(Map<String, Object> config) {
        List<Map<String, Object>> displayNameConfigs = NestingUtils.nest("display-name", config);
        if (displayNameConfigs != null) {
            for (Map<String, Object> displayNameConfig : displayNameConfigs) {
                displayNames.add(new DisplayNameImpl(displayNameConfig));
            }
        }

        List<Map<String, Object>> wrcConfigs = NestingUtils.nest("web-resource-collection", config);
        if (wrcConfigs != null) {
            for (Map<String, Object> wrcConfig : wrcConfigs) {
                webResourceCollections.add(new WebResourceCollectionImpl(wrcConfig));
            }
        }

        List<Map<String, Object>> authConstraintConfigs = NestingUtils.nest("auth-constraint", config);
        if (authConstraintConfigs != null && !authConstraintConfigs.isEmpty()) {
            authConstraint = new AuthConstraintImpl(authConstraintConfigs.get(0));
        }

        List<Map<String, Object>> userDataConstraintConfigs = NestingUtils.nest("user-data-constraint", config);
        if (userDataConstraintConfigs != null && !userDataConstraintConfigs.isEmpty()) {
            userDataConstraint = new UserDataConstraintImpl(userDataConstraintConfigs.get(0));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.web.common.SecurityConstraint#getDisplayNames()
     */
    @Override
    public List<DisplayName> getDisplayNames() {
        return this.displayNames;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.web.common.SecurityConstraint#getWebResourceCollections()
     */
    @Override
    public List<WebResourceCollection> getWebResourceCollections() {
        return this.webResourceCollections;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.web.common.SecurityConstraint#getAuthConstraint()
     */
    @Override
    public AuthConstraint getAuthConstraint() {
        return this.authConstraint;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.web.common.SecurityConstraint#getUserDataConstraint()
     */
    @Override
    public UserDataConstraint getUserDataConstraint() {
        return this.userDataConstraint;
    }

}
