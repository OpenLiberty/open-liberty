/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.security.internal;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.web.common.AuthConstraint;
import com.ibm.ws.javaee.dd.web.common.LoginConfig;
import com.ibm.ws.javaee.dd.web.common.UserDataConstraint;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleType;
import com.ibm.ws.jaxws.metadata.ServiceSecurityInfo;
import com.ibm.ws.jaxws23.webcontainer.JaxWsWebAppConfigurator;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfigurationImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollectionImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class JaxWsWebAppSecurityConfigurator implements JaxWsWebAppConfigurator {

    private static final TraceComponent tc = Tr.register(JaxWsWebAppSecurityConfigurator.class);

    private static final String ALL_ROLES_MARKER = "*";

    @Override
    public void configure(JaxWsModuleInfo jaxWsModuleInfo, WebAppConfig webAppConfig) {

        SecurityMetadata securityMetaData = (SecurityMetadata) ((WebAppConfigExtended) webAppConfig).getMetaData().getSecurityMetaData();

        ServiceSecurityInfo jaxWsServiceSecurityInfo = jaxWsModuleInfo.getServiceSecurityInfo();

        if (jaxWsServiceSecurityInfo == null) {
            return;
        }

        List<String> roles = securityMetaData.getRoles();

        // process the roles defined in jaxWsModuleInfo
        for (SecurityRole securityRole : jaxWsServiceSecurityInfo.getSecurityRoles()) {
            String role = securityRole.getRoleName();
            if (role != null && !role.equals(ALL_ROLES_MARKER)) {
                if (!roles.contains(role)) {
                    roles.add(role);
                }
            }
        }

        // process login-config defined in jaxWsModuleInfo
        LoginConfig loginConfig = jaxWsServiceSecurityInfo.getLoginConfig();
        if (loginConfig != null) {
            if (!jaxWsModuleInfo.getModuleType().equals(JaxWsModuleType.EJB)) {
                // log warning {when ejb in war, login-config in binding file is ignored}
                Tr.warning(tc, "ibm.ws.bnd.login.config.in.war.is.ingnored");
            } else {
                String authMethod = loginConfig.getAuthMethod();
                String realmName = loginConfig.getRealmName();
                if (authMethod != null || realmName != null) {
                    // convert "CLIENT-CERT" to "CLIENT_CERT"
                    if (LoginConfigurationImpl.CLIENT_CERT_AUTH_METHOD.equalsIgnoreCase(authMethod)) {
                        authMethod = LoginConfiguration.CLIENT_CERT;
                    }
                    if (authMethod == null) {
                        authMethod = LoginConfiguration.BASIC;
                    }
                    if (LoginConfiguration.BASIC.equals(authMethod) || LoginConfiguration.CLIENT_CERT.equals(authMethod)) {
                        securityMetaData.setLoginConfiguration(new LoginConfigurationImpl(authMethod, realmName, null));
                    } else {
                        // log warning (only basic and client_cert are supported in binding file)
                        Tr.warning(tc, "ibm.ws.bnd.auth.method.not.support", authMethod);
                    }
                }
            }
        }

        // process security-constraints defined in jaxWsModuleInfo
        List<SecurityConstraint> securityConstraints = new ArrayList<SecurityConstraint>();
        for (com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveSecurityConstraint : jaxWsServiceSecurityInfo.getSecurityConstraints()) {
            SecurityConstraint securityConstraint = createSecurityConstraint(archiveSecurityConstraint, roles);
            securityConstraints.add(securityConstraint);
        }

        SecurityConstraintCollection securityConstraintCollection = securityMetaData.getSecurityConstraintCollection();
        if (securityConstraintCollection != null) {
            securityConstraintCollection.addSecurityConstraints(securityConstraints);
        } else {
            securityMetaData.setSecurityConstraintCollection(new SecurityConstraintCollectionImpl(securityConstraints));
        }
    }

    private SecurityConstraint createSecurityConstraint(com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveConstraint, List<String> allRoles) {
        List<WebResourceCollection> webResourceCollections = createWebResourceCollections(archiveConstraint);
        List<String> roles = createRoles(archiveConstraint, allRoles);
        boolean sslRequired = isSSLRequired(archiveConstraint);
        boolean accessPrecluded = isAccessPrecluded(archiveConstraint);
        boolean accessUncovered = false;
        boolean fromHttpConstraint = false;
        return new SecurityConstraint(webResourceCollections, roles, sslRequired, accessPrecluded, accessUncovered, fromHttpConstraint);
    }

    private List<WebResourceCollection> createWebResourceCollections(com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveConstraint) {
        List<WebResourceCollection> webResourceCollections = new ArrayList<WebResourceCollection>();
        List<com.ibm.ws.javaee.dd.web.common.WebResourceCollection> archiveWebResourceCollections = archiveConstraint.getWebResourceCollections();
        for (com.ibm.ws.javaee.dd.web.common.WebResourceCollection archiveWebResourceCollection : archiveWebResourceCollections) {
            List<String> urlPatterns = archiveWebResourceCollection.getURLPatterns();
            List<String> methods = archiveWebResourceCollection.getHTTPMethods();
            List<String> omissionMethods = archiveWebResourceCollection.getHTTPMethodOmissions();
            webResourceCollections.add(new WebResourceCollection(urlPatterns, methods, omissionMethods));
        }
        return webResourceCollections;
    }

    private List<String> createRoles(com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveConstraint, List<String> allRoles) {
        List<String> roles = new ArrayList<String>();
        AuthConstraint authConstraint = archiveConstraint.getAuthConstraint();
        if (authConstraint != null) {
            roles = authConstraint.getRoleNames();
            if (roles.contains(ALL_ROLES_MARKER)) {
                roles = allRoles;
            }
        }
        return roles;
    }

    private boolean isSSLRequired(com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveConstraint) {
        boolean sslRequired = false;
        UserDataConstraint dataConstraint = archiveConstraint.getUserDataConstraint();
        if (dataConstraint != null) {
            int transportGuarantee = dataConstraint.getTransportGuarantee();
            if (transportGuarantee != UserDataConstraint.TRANSPORT_GUARANTEE_NONE) {
                sslRequired = true;
            }
        }
        return sslRequired;
    }

    private boolean isAccessPrecluded(com.ibm.ws.javaee.dd.web.common.SecurityConstraint archiveConstraint) {
        boolean accessPrecluded = false;
        AuthConstraint authConstraint = archiveConstraint.getAuthConstraint();
        if (authConstraint != null) {
            List<String> roles = authConstraint.getRoleNames();
            if (roles == null || roles.isEmpty()) {
                accessPrecluded = true;
            }
        }
        return accessPrecluded;
    }
}
