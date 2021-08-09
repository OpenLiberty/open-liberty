/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.feature.internal;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.FeatureAuthorizationTableService;
import com.ibm.ws.security.authorization.builtin.AbstractSecurityAuthorizationTable;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This class is an authorization table for a feature web bundle. It contains the
 * mapping of bundles web jee roles to user, groups, and special subjects.
 */
//service = { FeatureAuthorizationTable.class,
//            ConfigurationListener.class },
@Component(configurationPid = "com.ibm.ws.webcontainer.security.feature.authorizationConfig",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class FeatureAuthorizationTable extends AbstractSecurityAuthorizationTable
                implements AuthorizationTableService, UserRegistryChangeListener, ConfigurationListener {
    private static final TraceComponent tc = Tr.register(FeatureAuthorizationTable.class);

    static final String CFG_KEY_ID = "id";
    static final String CFG_KEY_ROLE = "security-role";

    static final String KEY_FEATURE_SECURITY_COLLAB = "featureCollabAuthzTable";
    private final AtomicServiceReference<FeatureAuthorizationTableService> featureCollabAuthzTableRef = new AtomicServiceReference<FeatureAuthorizationTableService>(KEY_FEATURE_SECURITY_COLLAB);

    protected String id = null;

    private String[] rolePids;

    /*
     * Process the feature role mapping in server.xml
     */
    public FeatureAuthorizationTable() {}

    @Activate
    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        super.activate(cc);
        featureCollabAuthzTableRef.activate(cc);
        processConfigProps(props);
        if (id != null) {
            Tr.info(tc, "FEATURE_ROLE_CONFIG_PROCESSED", new Object[] { id });
        }
    }

    @Modified
    protected synchronized void modified(Map<String, Object> props) {
        processConfigProps(props);
        if (id != null) {
            Tr.info(tc, "FEATURE_ROLE_CONFIG_PROCESSED", new Object[] { id });
        }
    }

    @Override
    @Deactivate
    protected synchronized void deactivate(ComponentContext cc) {
        FeatureAuthorizationTableService featureCollabAuthzTable = featureCollabAuthzTableRef.getServiceWithException();
        featureCollabAuthzTable.removeAuthorizationTable(id);
        super.deactivate(cc);
        featureCollabAuthzTableRef.deactivate(cc);
    }

    //TODO really not dynamic?
    @Reference(name = KEY_FEATURE_SECURITY_COLLAB,
               service = FeatureAuthorizationTableService.class)
    protected void setFeatureCollabAuthzTable(ServiceReference<FeatureAuthorizationTableService> ref) {
        featureCollabAuthzTableRef.setReference(ref);
    }

    protected void unsetFeatureCollabAuthzTable(ServiceReference<FeatureAuthorizationTableService> ref) {
        featureCollabAuthzTableRef.unsetReference(ref);
    }

    /**
     * Process the sytemRole properties from the server.xml.
     * 
     * @param props
     */
    private void processConfigProps(Map<String, Object> props) {
        if (props == null || props.isEmpty())
            return;
        id = (String) props.get(CFG_KEY_ID);
        if (id == null) {
            Tr.error(tc, "AUTHZ_ROLE_ID_IS_NULL");
            return;
        }
        rolePids = (String[]) props.get(CFG_KEY_ROLE);
//        if (rolePids == null || rolePids.length < 1)
//            return;
        processRolePids();
    }

    /**
     */
    private synchronized void processRolePids() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processRolePids");
        }
        ConfigurationAdmin configAdmin = configAdminRef.getServiceWithException();
        setFeatureRoleConfiguration(rolePids, configAdmin);
        FeatureAuthorizationTableService featureCollabAuthzTable = featureCollabAuthzTableRef.getServiceWithException();
        featureCollabAuthzTable.addAuthorizationTable(id, this);
    }

    public synchronized String getFeatureAuthorizationId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    protected synchronized String getApplicationName() {
        return id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
     */
    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ConfigurationEvent:" + event.getType() + " pid=" + event.getPid() + " event:" + event);
        }
        if (event.getType() != ConfigurationEvent.CM_DELETED && pids.contains(event.getPid())) {
            processRolePids();
        }
    }

}
