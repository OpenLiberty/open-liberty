/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.internal;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.FeatureAuthorizationTableService;
import com.ibm.ws.security.authorization.builtin.AbstractSecurityAuthorizationTable;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This class implements the builtin authorization table for the Oauth 2.0
 * web application
 */
@Component(configurationPid = "com.ibm.ws.security.oauth20.roles", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, property = { "service.vendor=IBM",
        "com.ibm.ws.security.authorization.table.name=oauth20" })
public class OAuth20WebAppAuthorizationTable
        extends AbstractSecurityAuthorizationTable
        implements AuthorizationTableService, UserRegistryChangeListener {

    private static final TraceComponent tc = Tr.register(OAuth20WebAppAuthorizationTable.class);

    public static final String OAUTH_WEB_APP_NAME = "com.ibm.ws.security.oauth20";
    public static final String OAUTH_FEATURE_ROLE_MAP_NAME = "com.ibm.ws.security.oauth20";
    static final String[] roleNames = new String[] { "authenticated", "clientManager", "tokenManager" };

    static final String KEY_FEATURE_SECURITY_AUTHZ_SERVICE = "featureAuthzTableService";
    private final AtomicServiceReference<FeatureAuthorizationTableService> featureAuthzTableServiceRef = new AtomicServiceReference<FeatureAuthorizationTableService>(KEY_FEATURE_SECURITY_AUTHZ_SERVICE);

    private Map<String, Object> properties;

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> properties) {
        super.activate(cc);
        featureAuthzTableServiceRef.activate(cc);
        this.properties = properties;
        setConfiguration(roleNames, configAdminRef.getService(), properties);
        FeatureAuthorizationTableService featureCollabAuthzTable = featureAuthzTableServiceRef.getServiceWithException();
        featureCollabAuthzTable.addAuthorizationTable(OAUTH_FEATURE_ROLE_MAP_NAME, this);
        Tr.info(tc, "OAUTH_ROLE_CONFIG_PROCESSED");
    }

    @Modified
    protected synchronized void modify(Map<String, Object> properties) {
        this.properties = properties;
        modify();
    }

    /**
     *
     */
    private synchronized void modify() {
        setConfiguration(roleNames, configAdminRef.getService(), this.properties);
        FeatureAuthorizationTableService featureCollabAuthzTable = featureAuthzTableServiceRef.getServiceWithException();
        featureCollabAuthzTable.addAuthorizationTable(OAUTH_FEATURE_ROLE_MAP_NAME, this);
        Tr.info(tc, "OAUTH_ROLE_CONFIG_PROCESSED");
    }

    @Override
    @Deactivate
    protected synchronized void deactivate(ComponentContext cc) {
        super.deactivate(cc);
        featureAuthzTableServiceRef.deactivate(cc);
    }

    // Take off optional. It's required
    @Reference(name = KEY_FEATURE_SECURITY_AUTHZ_SERVICE, service = FeatureAuthorizationTableService.class, policy = ReferencePolicy.DYNAMIC)
    // cardinality=ReferenceCardinality.OPTIONAL)
    protected void setFeatureAuthzTableService(ServiceReference<FeatureAuthorizationTableService> ref) {
        featureAuthzTableServiceRef.setReference(ref);
    }

    protected void unsetFeatureAuthzTableService(ServiceReference<FeatureAuthorizationTableService> ref) {
        featureAuthzTableServiceRef.unsetReference(ref);
    }

    /** {@inheritDoc} */
    @Override
    protected String getApplicationName() {
        return OAUTH_FEATURE_ROLE_MAP_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void configurationEvent(ConfigurationEvent event) {
        if (event.getType() != ConfigurationEvent.CM_DELETED && pids.contains(event.getPid())) {
            modify();
        }
    }

    /*
     * if it is OAUTH, return true. Otherwise, return false.
     * Note: The OAUTH do not support useRoleAsGroupName for authorization
     */
    /** {@inheritDoc} */
    @Override
    public boolean isAuthzInfoAvailableForApp(String appName) {
        return (OAUTH_FEATURE_ROLE_MAP_NAME.equals(appName) == true ? true : false);
    }

}
