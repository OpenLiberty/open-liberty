/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.security.jacc;

import java.util.List;

import javax.ejb.EnterpriseBean;
import javax.security.auth.Subject;

import com.ibm.ejs.container.BeanMetaData;

public interface EJBJaccService {

    /**
     * Propagates EJB role mapping information to JACC.
     *
     * @param bmd Bean meta data
     */
    public void propagateEJBRoles(BeanMetaData bmd);

    /**
     * Validates whether given Subject is granted to access the specified resource.
     *
     * @param applicationName  Application name
     * @param moduleName       Module name
     * @param beanName         Bean name
     * @param methodName       Method name
     * @param methodInterface  Method interface
     * @param methodName       Method signature
     * @param methodParameters The list of method parameters. this is optional and null is accepted.
     * @param bean             EnterpriseBean object this is an optional and null is allowed.
     * @param subject          Subject object to be authorized.
     * @return true if the specified subject is granted to access the specified resource.
     */
    public boolean isAuthorized(String applicationName,
                                String moduleName,
                                String beanName,
                                String methodName,
                                String methodInterface,
                                String methodSignature,
                                List<Object> methodParameters,
                                EnterpriseBean bean,
                                Subject subject);

    /**
     * Validates whether given Subject is a member of the specified role
     *
     * @param applicationName  Application name
     * @param moduleName       Module name
     * @param beanName         Bean name
     * @param methodName       Method name
     * @param methodInterface  Method interface
     * @param methodParameters The list of method parameters. this is optional and null is accepted.
     * @param role             Role name
     * @param bean             EnterpriseBean object this is an optional and null is allowed.
     * @param subject          Subject object to be authorized.
     * @return true if the specified subject has a member of the specified role.
     */
    public boolean isSubjectInRole(String applicationName,
                                   String moduleName,
                                   String beanName,
                                   String methodName,
                                   List<Object> methodParameters,
                                   String role,
                                   EnterpriseBean bean,
                                   Subject subject);

    /**
     * Returns whether RequestMethodArguments are required for authorization decision for EJB.
     *
     * @return true if RequestMethodArguments are required. false otherwise.
     */
    public boolean areRequestMethodArgumentsRequired();

    /**
     * Reset the policyContext Handler as per JACC specification
     */
    public void resetPolicyContextHandlerInfo();
}
