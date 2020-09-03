/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.feature.internal;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.FeatureAuthorizationTableService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class FeatureWebSecurityCollaboratorImpl extends WebAppSecurityCollaboratorImpl implements FeatureAuthorizationTableService {

    protected volatile WebAppSecurityConfig featureSecConfig = new FeatureWebSecurityConfigImpl(null);

    //private static final TraceComponent tc = Tr.register(FeatureWebSecurityCollaboratorImpl.class);

    private final ConcurrentHashMap<String, AuthorizationTableService> featureTables = new ConcurrentHashMap<String, AuthorizationTableService>();

    /**
     * Zero arg constructor required by DS.
     */
    public FeatureWebSecurityCollaboratorImpl() {
        super();
    }

    @Override
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        super.activate(cc, props);
    }

    @Override
    protected void activateComponents() {
        webAppSecConfig = featureSecConfig;
        postParameterHelper = new PostParameterHelper(webAppSecConfig);
        providerAuthenticatorProxy = authenticatorFactory.createWebProviderAuthenticatorProxy(securityServiceRef, taiServiceRef, interceptorServiceRef, webAppSecConfig,
                                                                                              webAuthenticatorRef, ssoAuthFilterRef);
        authenticatorProxy = authenticatorFactory.createWebAuthenticatorProxy(webAppSecConfig, postParameterHelper, securityServiceRef, providerAuthenticatorProxy);
    }

    @Override
    protected void modified(Map<String, Object> newProperties) {
        //do nothing, config won't change
    }

    @Override
    protected void deactivate(ComponentContext cc) {
        super.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public RoleSet getRolesForSpecialSubject(String resourceName, String specialSubject) {
        RoleSet roles = null;
        AuthorizationTableService authzTable = featureTables.get(resourceName);
        if (authzTable != null)
            roles = authzTable.getRolesForSpecialSubject(resourceName, specialSubject);
        return roles;
    }

    /** {@inheritDoc} */
    @Override
    public RoleSet getRolesForAccessId(String resourceName, String accessId) {
        return getRolesForAccessId(resourceName, accessId, null);
    }

    /** {@inheritDoc} */
    @Override
    public RoleSet getRolesForAccessId(String resourceName, String accessId, String realmName) {
        RoleSet roles = null;
        AuthorizationTableService authzTable = featureTables.get(resourceName);
        if (authzTable != null)
            roles = authzTable.getRolesForAccessId(resourceName, accessId, realmName);
        return roles;
    }

    /** {@inheritDoc} */
    @Override
    public void addAuthorizationTable(String resourceName, AuthorizationTableService authzTable) {
        featureTables.put(resourceName, authzTable);
    }

    /** {@inheritDoc} */
    @Override
    public void removeAuthorizationTable(String resourceName) {
        featureTables.remove(resourceName);
    }

    /** {@inheritDoc} */
    @Override
    protected String getApplicationName() {
        return getFeatureAuthzRoleHeaderValue();
    }

    /** {@inheritDoc} */
    @Override
    public String getFeatureAuthzRoleHeaderValue() {
        String name = null;
        WebAppConfig wac = getWebAppConfig();
        if (wac != null && wac instanceof WebAppConfiguration) {
            Dictionary<String, String> headers = ((WebAppConfiguration) wac).getBundleHeaders();
            if (headers != null)
                name = headers.get("IBM-Authorization-Roles");
        }
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAuthzInfoAvailableForApp(String resourceName) {
        AuthorizationTableService authzTable = featureTables.get(resourceName);
        return (authzTable != null && authzTable.isAuthzInfoAvailableForApp(resourceName) == true ? true : false);
    }
}