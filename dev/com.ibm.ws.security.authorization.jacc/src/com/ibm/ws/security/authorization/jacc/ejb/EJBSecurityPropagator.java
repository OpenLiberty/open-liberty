/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.ejb;

import java.util.List;
import java.util.Map;

import javax.security.jacc.PolicyConfigurationFactory;

import com.ibm.ws.security.authorization.jacc.MethodInfo;
import com.ibm.ws.security.authorization.jacc.RoleInfo;

/**
 ** this class is for propagating the security constraints for EJB.
 ** since EJB feature might not exist, all of EJB related code is located
 ** to the separate feature which only activated when ejb feature exists.
 **/

public interface EJBSecurityPropagator {

    void propagateEJBRoles(String contextId,
                           String appName,
                           String beanName,
                           Map<String, String> roleLinkMap,
                           Map<RoleInfo, List<MethodInfo>> methodMap);

    void processEJBRoles(PolicyConfigurationFactory pcf, String contextId);
}
