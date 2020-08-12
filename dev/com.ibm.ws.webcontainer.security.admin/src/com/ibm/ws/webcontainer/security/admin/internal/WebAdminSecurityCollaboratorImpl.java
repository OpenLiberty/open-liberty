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
package com.ibm.ws.webcontainer.security.admin.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.webcontainer.security.PostParameterHelper;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfigurationImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollectionImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelper;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM", "com.ibm.ws.security.type=com.ibm.ws.management" })
public class WebAdminSecurityCollaboratorImpl extends WebAppSecurityCollaboratorImpl implements IWebAppSecurityCollaborator {
    protected volatile WebAppSecurityConfig webAdminSecConfig = new WebAdminSecurityConfigImpl(null);
    private SecurityMetadata secMetadata = null;
    private UnauthenticatedSubjectService unauthenticatedSubjectService;

    @Override
    @Activate
    protected void activate(final ComponentContext cc, final Map<String, Object> ignored) {
        super.activate(cc, ignored);
    }

    @Override
    protected void activateComponents() {
        webAppSecConfig = webAdminSecConfig;
        SSOCookieHelper ssoCookieHelper = webAppSecConfig.createSSOCookieHelper();
        authenticateApi = authenticatorFactory.createAuthenticateApi(ssoCookieHelper, securityServiceRef, collabUtils, webAuthenticatorRef, unprotectedResourceServiceRef,
                                                                     unauthenticatedSubjectService, ssoServiceRef);
        postParameterHelper = new PostParameterHelper(webAppSecConfig);
        providerAuthenticatorProxy = authenticatorFactory.createWebProviderAuthenticatorProxy(securityServiceRef, taiServiceRef, interceptorServiceRef, webAppSecConfig,
                                                                                              webAuthenticatorRef);
        authenticatorProxy = authenticatorFactory.createWebAuthenticatorProxy(webAppSecConfig, postParameterHelper, securityServiceRef, providerAuthenticatorProxy, ssoServiceRef);
    }

    @Override
    @Modified
    protected void modified(final Map<String, Object> ignored) {
        //do nothing, config won't change
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        super.deactivate(cc);
    }

    /**
     * Constructs the default Admin Security metadata. This is preserved in
     * the event we have an "admin application" (aka a feature WAB or system WAB)
     * which does not provide a web.xml. This is unlikely, but we want to be
     * safe.
     *
     * @return The default Admin Security metadata
     */
    private SecurityMetadata getDefaultAdminSecurityMetadata() {
        if (secMetadata == null) {
            secMetadata = new SecurityServletConfiguratorHelper(null);

            // It appears that the web.xml is honored now, but add reader role for consistency
            List<String> roles = new ArrayList<String>();
            roles.add(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME);
            roles.add(ManagementSecurityConstants.READER_ROLE_NAME);
            secMetadata.setRoles(roles);

            String urlPattern = "/*";
            List<String> allURLPatternList = new ArrayList<String>();
            allURLPatternList.add(urlPattern);
            WebResourceCollection collection = new WebResourceCollection(allURLPatternList, new ArrayList<String>());
            List<WebResourceCollection> collectionList = new ArrayList<WebResourceCollection>();
            collectionList.add(collection);

            SecurityConstraint constraint = new SecurityConstraint(collectionList, roles, true, false, false, false);
            List<SecurityConstraint> constraints = new ArrayList<SecurityConstraint>();
            constraints.add(constraint);
            SecurityConstraintCollection constraintCollection = new SecurityConstraintCollectionImpl(constraints);
            secMetadata.setSecurityConstraintCollection(constraintCollection);

            // Support CLIENT_CERT login. Fallback to basic auth is enabled.
            LoginConfiguration loginConfiguration = new LoginConfigurationImpl(LoginConfiguration.CLIENT_CERT, collabUtils.getUserRegistryRealm(securityServiceRef), null);
            secMetadata.setLoginConfiguration(loginConfiguration);

            Map<String, String> urlPatternToServletName = new HashMap<String, String>();
            urlPatternToServletName.put(urlPattern, "IBMJMXConnectorREST"); //TODO read from constant - Kevin or Arthur
            secMetadata.setUrlPatternToServletNameMap(urlPatternToServletName);
        }
        return secMetadata;
    }

    /**
     * Get the effective SecurityMetadata for this application.
     * First, try to defer to the application collaborator to see if the application
     * provides data. If so, use it. Otherwise, fallback to the default SecurityMetadata.
     *
     * {@inheritDoc}
     */
    @Override
    public SecurityMetadata getSecurityMetadata() {
        SecurityMetadata sm = super.getSecurityMetadata();
        if (sm == null) {
            return getDefaultAdminSecurityMetadata();
        } else {
            return sm;
        }
    }

    /**
     * The application name influences security checks. All admin applications
     * therefore have the same name.
     */
    @Override
    protected String getApplicationName() {
        return ManagementSecurityConstants.ADMIN_RESOURCE_NAME;
    }

}