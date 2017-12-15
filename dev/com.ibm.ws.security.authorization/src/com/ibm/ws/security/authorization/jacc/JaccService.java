/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc;

import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

public interface JaccService {

    /**
     * Propagates web constraints information to JACC.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param webAppConfig WebAppConfig object. In this interface, it is intentionally declare as Object to avoid adding any dependency to webcontainer project.
     */
    public void propagateWebConstraints(String applicationName,
                                        String moduleName,
                                        Object webAppConfig);

    /**
     * Validates whether SSL is required for web inbound transport.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param uriName Uri
     * @param methodName method Name
     * @param req HttpServletObject of this request. In this interface, it is intentionally declare as Object to avoid adding any dependency to webcontainer project.
     * @return true if SSL is required.
     */
    public boolean isSSLRequired(String applicationName,
                                 String moduleName,
                                 String uriName,
                                 String methodName,
                                 Object req);

    /**
     * Validates whether the http request is excluded.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param uriName Uri
     * @param methodName method Name
     * @param req HttpServletObject of this request. In this interface, it is intentionally declare as Object to avoid adding any dependency to webcontainer project.
     * @return true if SSL is required.
     */
    public boolean isAccessExcluded(String applicationName,
                                    String moduleName,
                                    String uriName,
                                    String methodName,
                                    Object req);

    /**
     * Validates whether given Subject is granted to access the specified resource.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param uriName Uri
     * @param methodName method Name
     * @param req HttpServletObject of this request. In this interface, it is intentionally declare as Object to avoid adding any dependency to webcontainer project.
     * @param subject Subject object to be authorized.
     * @return true if access is granted.
     */
    public boolean isAuthorized(String applicationName,
                                String moduleName,
                                String uriName,
                                String methodName,
                                Object req,
                                Subject subject);

    /**
     * Validates whether given Subject is granted to access the specified resource.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param uriName Uri
     * @param req HttpServletObject of this request. In this interface, it is intentionally declare as Object to avoid adding any dependency to webcontainer project.
     * @param role role name to be examined.
     * @param subject Subject object to be authorized.
     * @return true if the specified subject has the specified role.
     */
    public boolean isSubjectInRole(String applicationName,
                                   String moduleName,
                                   String servletName,
                                   String role,
                                   Object req,
                                   Subject subject);

    /**
     * Propagates EJB role mapping information to JACC.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param beanName Bean name
     * @param roleLinkMap list of role-ref link
     * @param methodMap method to role mapping.
     */
    public void propagateEJBRoles(String applicationName,
                                  String moduleName,
                                  String beanName,
                                  Map<String, String> roleLinkMap,
                                  Map<RoleInfo, List<MethodInfo>> methodMap);

    /**
     * Validates whether given Subject is granted to access the specified resource.
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param beanName Bean name
     * @param methodName Method name
     * @param methodInterface Method interface
     * @param methodName Method signature
     * @param methodParameters The list of method parameters. this is optional and null is accepted.
     * @param bean EnterpriseBean object this is an optional and null is allowed. In this interface, it is intentionally declare as Object to avoid adding any dependency to
     *            ejbcontainer project.
     * @param subject Subject object to be authorized.
     * @return true if the specified subject is granted to access the specified resource.
     */
    public boolean isAuthorized(String applicationName,
                                String moduleName,
                                String beanName,
                                String methodName,
                                String methodInterface,
                                String methodSignature,
                                List<Object> methodParameters,
                                Object bean,
                                Subject subject);

    /**
     * Validates whether given Subject is a member of the specified role
     *
     * @param applicationName Application name
     * @param moduleName Module name
     * @param beanName Bean name
     * @param methodName Method name
     * @param methodInterface Method interface
     * @param methodParameters The list of method parameters. this is optional and null is accepted.
     * @param role Role name
     * @param bean EnterpriseBean object this is an optional and null is allowed. In this interface, it is intentionally declare as Object to avoid adding any dependency to
     *            ejbcontainer project.
     * @param subject Subject object to be authorized.
     * @return true if the specified subject has a member of the specified role.
     */
    public boolean isSubjectInRole(String applicationName,
                                   String moduleName,
                                   String beanName,
                                   String methodName,
                                   List<Object> methodParameters,
                                   String role,
                                   Object bean,
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
