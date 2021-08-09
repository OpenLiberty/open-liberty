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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.web.common.LoginConfig;
import com.ibm.ws.javaee.dd.web.common.SecurityConstraint;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceSecurity;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.NestingUtils;

@Component(configurationPid = "com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceSecurity",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = "service.vendor = IBM")
public class WebserviceSecurityComponentImpl implements WebserviceSecurity {

    private LoginConfig loginConfig;
    private final List<SecurityRole> securityRoles = new ArrayList<SecurityRole>();
    private final List<SecurityConstraint> securityConstraints = new ArrayList<SecurityConstraint>();

    @Activate
    protected void activate(Map<String, Object> config) {
        List<Map<String, Object>> loginConfigs = NestingUtils.nest(WebserviceSecurity.LOGIN_CONFIG_ELEMENT_NAME, config);
        if (loginConfigs != null && !loginConfigs.isEmpty())
            this.loginConfig = new LoginConfigImpl(loginConfigs.get(0));

        List<Map<String, Object>> securityConstraintConfigs = NestingUtils.nest(WebserviceSecurity.SECURITY_CONSTRAINT_ELEMENT_NAME, config);
        if (securityConstraintConfigs != null) {
            for (Map<String, Object> securityConstraintConfig : securityConstraintConfigs) {
                securityConstraints.add(new SecurityConstraintImpl(securityConstraintConfig));
            }
        }

        List<Map<String, Object>> securityRoleConfigs = NestingUtils.nest(WebserviceSecurity.SECURITY_ROLE_ELEMENT_NAME, config);
        if (securityRoleConfigs != null) {
            for (Map<String, Object> securityRoleConfig : securityRoleConfigs) {
                securityRoles.add(new SecurityRoleImpl(securityRoleConfig));
            }
        }
    }

    protected void deactivate() {
        this.loginConfig = null;
        this.securityConstraints.clear();
        this.securityRoles.clear();
    }

    //

    @Override
    public List<SecurityConstraint> getSecurityConstraints() {
        return securityConstraints;
    }

    @Override
    public List<SecurityRole> getSecurityRoles() {
        return securityRoles;
    }

    @Override
    public LoginConfig getLoginConfig() {
        return loginConfig;
    }
}
