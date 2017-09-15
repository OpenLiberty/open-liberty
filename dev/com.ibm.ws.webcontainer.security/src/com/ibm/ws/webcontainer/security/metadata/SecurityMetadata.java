/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.metadata;

import java.util.List;
import java.util.Map;

/**
 * Represents the security metadata as described in web.xml.
 * Provides the security constraint collection and the login configuration.
 */
public interface SecurityMetadata {

    /**
     * Gets the real security role which the specified role name maps to.
     * The request URI is necessary to determine what the {@code <servlet> } definition is in the web.xml for the servlet handling this request
     * because the {@code <servlet> } definition contains the {@code <security-role-ref> } entries.
     * <p>
     * If the specified role name is a security-role-ref name, demonstrated
     * in the following example, then the name would be backed by role-link. {@code
     * <security-role-ref>
     * <role-name>MappedUserRole</role-name>
     * <role-link>user</role-link>
     * </security-role-ref> } <p>
     * If the specified role name is a security-role name, demonstrated
     * in the following example, then that name is returned. {@code
     * <security-role>
     * <role-name>user</role-name>
     * </security-role> } <p>
     * If the requestUri can not be matched, only the global roles for the web.xml
     * are checked.
     * <p>
     * If no matching name can be found, null is returned.
     * 
     * @param requestUri the URI for the request
     * @param rolename the specified role name
     * @return The matched security role name, or {@code null}.
     */
    public String getSecurityRoleReferenced(String servletName, String roleName);


    /**
     * returns List of role-ref.
     * 
     * @param requestUri the URI for the request
     * @return The list of matched security role-ref name, or {@code null}.
     */
    public Map<String, String> getRoleRefs(String servletName);

    /**
     * Gets the security constraint collection.
     * 
     * @return The SecurityContraintCollection object. See {@link com.ibm.ws.webcontainer.security.internal.metadata.SecurityContraintCollection}
     */
    public SecurityConstraintCollection getSecurityConstraintCollection();

    /**
     * Sets the security constraint collection.
     * 
     * @param constraintCollection the collection to set
     */
    public void setSecurityConstraintCollection(SecurityConstraintCollection constraintCollection);

    /**
     * Gets the login configuration.
     * 
     * @return The LoginConfiguration object or <code>null</code> if not available.
     *         See {@link com.ibm.ws.webcontainer.security.metadata.LoginConfiguration}
     */
    public LoginConfiguration getLoginConfiguration();

    /**
     * Gets the runAs role for the given servlet
     * 
     * @param servletName the name of the servlet to look up
     * @return the runAs role
     */
    public String getRunAsRoleForServlet(String servletName);

    /**
     * Returns the mapping of servlet name to run-as role
     * 
     * @return a mapping of servlet name to the runAs role
     */
    public Map<String, String> getRunAsMap();

    /**
     * Gets the list of roles declared in the metadata.
     * 
     * @return list of declared roles
     */
    public List<String> getRoles();

    /**
     * Sets the list of roles declared in the metadata.
     * 
     * @param roles list of roles to set
     */
    public void setRoles(List<String> roles);

    /**
     * Sets the login configuration in the metadata.
     * 
     * @param loginConfiguration the login config to set
     */
    public void setLoginConfiguration(LoginConfiguration loginConfiguration);

    /**
     * Sets the mapping of url patterns to servlet names
     * 
     * @param urlPatternToServletName the map to set
     */
    public void setUrlPatternToServletNameMap(Map<String, String> urlPatternToServletName);

    /**
     * Returns true if the application has requested SYNC-TO-OS-THREAD support.
     * 
     * @return true if the application has requested SYNC-TO-OS-THREAD support; false otherwise.
     */
    public boolean isSyncToOSThreadRequested();

    /**
     * Sets the deny uncovered http method boolean value
     * 
     * @ param denyUncoveredHttpMethods the boolean to set
     */
    public void setDenyUncoveredHttpMethods(boolean denyValue);

    /**
     * Returns true if the deny-uncovered-http-method element is specified in web.xml
     * 
     * @return true if the deny-uncovered-http-method element is specified in web.xml; false otherwise.
     */
    public boolean isDenyUncoveredHttpMethods();

}
