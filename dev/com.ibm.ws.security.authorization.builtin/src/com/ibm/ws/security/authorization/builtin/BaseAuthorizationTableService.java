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
package com.ibm.ws.security.authorization.builtin;

import java.util.Dictionary;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * WebAppAuthorizationTableService handles the creation
 * of the authorization table when an application is deployed, and its
 * destruction when the application is undeployed.
 * <p>
 * If security is enabled dynamically, then the applications will be
 * re-deployed, which will trigger this listener to be called.
 */
public abstract class BaseAuthorizationTableService implements UserRegistryChangeListener {
    private static final TraceComponent tc = Tr.register(BaseAuthorizationTableService.class);

    static final String KEY_SECURITY_SERVICE = "securityService";
    protected final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    static final String KEY_CONFIG_ADMIN = "configurationAdmin";
    protected final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIG_ADMIN);
    static final String KEY_LDAP_REGISTRY = "(service.factoryPid=com.ibm.ws.security.registry.ldap.config)";
    static final String KEY_IGNORE_CASE = "ignoreCase";
    static final String KEY_CONFIGURATION = "configuration";

    private boolean isIgnoreCaseSet = false;
    private boolean isIgnoreCase = false;

    @Reference(service = SecurityService.class, name = KEY_SECURITY_SERVICE,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    @Reference(service = ConfigurationAdmin.class, name = KEY_CONFIG_ADMIN,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> reference) {
        configAdminRef.setReference(reference);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> reference) {
        configAdminRef.unsetReference(reference);
    }

    protected void activate(ComponentContext cc) {
        securityServiceRef.activate(cc);
        configAdminRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        securityServiceRef.deactivate(cc);
        configAdminRef.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyOfUserRegistryChange() {
        isIgnoreCaseSet = false;
    }

    protected boolean isIgnoreCase() {
        if (!isIgnoreCaseSet) {
            isIgnoreCase = getIgnoreCase();
            isIgnoreCaseSet = true;
        }
        return isIgnoreCase;
    }

    protected boolean isMatch(String a, String b) {
        boolean match = false;
        if (isIgnoreCase() /* && areLDAPNames(a, b) */) {
            match = a.equalsIgnoreCase(b);
        } else {
            match = a.equals(b);
        }
        return match;
    }

    /**
     * @param a
     * @param b
     * @return
     */
    @FFDCIgnore(InvalidNameException.class)
    private boolean areLDAPNames(String a, String b) {
        try {
            new LdapName(getNameFromAccessId(a));
            new LdapName(getNameFromAccessId(b));
        } catch (InvalidNameException e) {
            return false;
        }

        return true;
    }

    /**
     * @param id
     * @return
     */
    private String getNameFromAccessId(String id) {
        if (id == null) {
            return "";
        }
        id = id.trim();
        int realmDelimiterIndex = id.indexOf("/");
        if (realmDelimiterIndex < 0) {
            return "";
        } else {
            return id.substring(realmDelimiterIndex + 1);
        }
    }

    protected Set<String> getRoles(Map<String, Set<String>> table, String key) {
        Set<String> value = null;
        if (isIgnoreCase()) {
            for (Map.Entry<String, Set<String>> entry : table.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        } else {
            value = table.get(key);
        }
        return value;
    }

    protected RoleSet getRoleSet(Map<String, RoleSet> table, String key) {
        RoleSet value = null;
        if (isIgnoreCase()) {
            for (Map.Entry<String, RoleSet> entry : table.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        } else {
            value = table.get(key);
        }
        return value;
    }

    protected boolean getIgnoreCase() {
        boolean value = false;
        if (securityServiceRef != null && configAdminRef != null) {
            try {
                SecurityService securityService = securityServiceRef.getService();

                if (securityService != null) {
                    UserRegistryService userRegistryService = securityService.getUserRegistryService();
                    if (userRegistryService != null && userRegistryService.isUserRegistryConfigured()) {
                        // now user registry is available. now check user registry type.e
                        String type = userRegistryService.getUserRegistryType();
                        if ("LDAP".equalsIgnoreCase(type) || "WIM".equalsIgnoreCase(type)) {
                            // now ldap user registry is used. set default value as true.
                            value = true;
                            // now, examine whether ignoreCase attribute is set.
                            ConfigurationAdmin configAdmin = configAdminRef.getService();
                            if (configAdmin != null) {
                                Configuration ldapRegistryConfigs[] = configAdmin.listConfigurations(KEY_LDAP_REGISTRY);
                                if (ldapRegistryConfigs != null) {
                                    for (int i = 0; i < ldapRegistryConfigs.length; i++) {
                                        Dictionary<String, Object> props = ldapRegistryConfigs[i].getProperties();
                                        if (props != null) {
                                            Object ignoreCaseObject = props.get(KEY_IGNORE_CASE);
                                            if (ignoreCaseObject != null) {
                                                if (ignoreCaseObject instanceof Boolean) {
                                                    value = ((Boolean) ignoreCaseObject).booleanValue();
                                                } else if (ignoreCaseObject instanceof String) {
                                                    if ("false".equalsIgnoreCase((String) ignoreCaseObject)) {
                                                        value = false;
                                                    }
                                                }
                                            }
                                        }
                                        if (!value) {
                                            // if value is false, we can break, since if there are multiple ldapRegistry configurations, if there is at least
                                            // one configuration which ignoreCase is set as false, then Authorization code needs to go to case sensitive comparison.
                                            break;
                                        }
                                    }
                                } else {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "The Ldap Configuration object is null, use the default value which is true.");
                                    }
                                }
                            } else {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "The ConfigurationAdmin object is null, use the default value which is true.");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception is caught while accessing the user registry configuration information. The default value " + value + " is used.", e);
                }
            }
        }
        return value;
    }

}