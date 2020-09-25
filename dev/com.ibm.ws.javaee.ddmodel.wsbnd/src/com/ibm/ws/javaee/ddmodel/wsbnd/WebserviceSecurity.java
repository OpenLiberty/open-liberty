/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd;

import java.util.List;

import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.web.common.LoginConfig;
import com.ibm.ws.javaee.dd.web.common.SecurityConstraint;

public interface WebserviceSecurity {
    String SECURITY_CONSTRAINT_ELEMENT_NAME = "security-constraint";
    String SECURITY_ROLE_ELEMENT_NAME = "security-role";
    String LOGIN_CONFIG_ELEMENT_NAME = "login-config";

    List<SecurityConstraint> getSecurityConstraints();
    List<SecurityRole> getSecurityRoles();
    LoginConfig getLoginConfig();
}
