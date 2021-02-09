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
package com.ibm.ws.security.quickstart.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.felix.scr.ext.annotation.DSExt;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.bnd.metatype.annotation.Ext;
import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * <p>
 * The valid definitions are:
 * <p> {@code <quickStartSecurity userName="user" userPassword="password" /> <!-- This is where we create the
 * QuickStartSecurityRegistry --> } <p>
 * OR
 * <p>
 * (nothing) {@code <!-- Authn and authz are handled by external providers (e.g. SAF) -->}
 */

@ObjectClassDefinition(pid = "com.ibm.ws.security.quickStartSecurity", name = "%quickStartSecurity", description = "%quickStartSecurity.desc", localization = Ext.LOCALIZATION)
@Ext.Alias("quickStartSecurity")
interface QuickStartSecurityConfig {

    @AttributeDefinition(name = "%quickStartSecurity.userName", description = "%quickStartSecurity.userName.desc", required = false)
    String userName();

    @AttributeDefinition(name = "%quickStartSecurity.userPassword", description = "%quickStartSecurity.userPassword.desc", required = false)
    @Ext.Type("passwordHash")
    SerializableProtectedString userPassword();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "*")
    @Ext.ServiceClass(UserRegistry.class)
    @Ext.Final
    String[] UserRegistry();

    @AttributeDefinition(name = Ext.INTERNAL, description = Ext.INTERNAL_DESC, defaultValue = "*")
    @Ext.ServiceClass(ManagementRole.class)
    @Ext.Final
    String[] ManagementRole();

}

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = { "com.ibm.ws.security.quickStartSecurity" },
           immediate = true,
           property = "service.vendor=IBM")
@DSExt.ConfigureWithInterfaces
public class QuickStartSecurity {

    private static final TraceComponent tc = Tr.register(QuickStartSecurity.class);

    static final String KEY_MANAGEMENT_ROLE = "ManagementRole";
    private final Set<ServiceReference<UserRegistry>> urs = new ConcurrentSkipListSet<ServiceReference<UserRegistry>>();
    private final Set<ServiceReference<ManagementRole>> managementRoles = new ConcurrentSkipListSet<ServiceReference<ManagementRole>>();

    static final String QUICK_START_SECURITY_REGISTRY_ID = "com.ibm.ws.management.security.QuickStartSecurity";
    static final String QUICK_START_SECURITY_REGISTRY_TYPE = "QuickStartSecurityRegistry";
    static final String QUICK_START_ADMINISTRATOR_ROLE_NAME = "QuickStartSecurityAdministratorRole";
    static final String CFG_KEY_USER = "userName";
    static final String CFG_KEY_PASSWORD = "userPassword";

    private BundleContext bc = null;
    private ServiceRegistration<UserRegistry> urConfigReg = null;
    private QuickStartSecurityRegistry quickStartRegistry = null;
    private ServiceRegistration<ManagementRole> managementRoleReg = null;
    private ManagementRole managementRole = null;

    private QuickStartSecurityConfig config;

    /**
     * This method will only be called for UserRegistryConfigurations that are not
     * the one we have defined here.
     *
     * @param ref
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = "(!(com.ibm.ws.security.registry.type=QuickStartSecurityRegistry))")
    protected synchronized void setUserRegistry(ServiceReference<UserRegistry> ref) {
        urs.add(ref);
        unregisterQuickStartSecurityRegistryConfiguration();
        unregisterQuickStartSecurityAdministratorRole();
    }

    /**
     * This method will only be called for UserRegistryConfigurations that are not
     * the one we have defined here.
     *
     * @param ref
     */
    protected synchronized void unsetUserRegistry(ServiceReference<UserRegistry> ref) {
        urs.remove(ref);
        registerQuickStartSecurityRegistryConfiguration();
        registerQuickStartSecurityAdministratorRole();
    }

    /**
     * This method will only be called for ManagementRoles that are not
     * the one we have defined here.
     *
     * @param ref
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
               target = "(!(com.ibm.ws.management.security.role.name=QuickStartSecurityAdministratorRole))")
    protected synchronized void setManagementRole(ServiceReference<ManagementRole> ref) {
        managementRoles.add(ref);
        unregisterQuickStartSecurityRegistryConfiguration();
        unregisterQuickStartSecurityAdministratorRole();
    }

    /**
     * This method will only be called for ManagementRoles that are not
     * the one we have defined here.
     *
     * @param ref
     */
    protected synchronized void unsetManagementRole(ServiceReference<ManagementRole> ref) {
        managementRoles.remove(ref);
        registerQuickStartSecurityRegistryConfiguration();
        registerQuickStartSecurityAdministratorRole();
    }

    @Activate
    protected synchronized void activate(BundleContext bc, QuickStartSecurityConfig config) {
        this.bc = bc;

        this.config = config;
        validateConfigurationProperties();

        registerQuickStartSecurityRegistryConfiguration();
        registerQuickStartSecurityAdministratorRole();
    }

    /**
     * Push the new user and password into the registry's configuration.
     */
    @Modified
    protected synchronized void modify(QuickStartSecurityConfig config) {
        this.config = config;

        validateConfigurationProperties();

        if (urConfigReg == null) {
            registerQuickStartSecurityRegistryConfiguration();
        } else {
            updateQuickStartSecurityRegistryConfiguration();
        }

        unregisterQuickStartSecurityAdministratorRole();
        registerQuickStartSecurityAdministratorRole();
    }

    @Deactivate
    protected synchronized void deactivate() {
        bc = null;
        config = null;

        unregisterQuickStartSecurityRegistryConfiguration();
        unregisterQuickStartSecurityAdministratorRole();
    }

    /**
     * Check if the value is non-null, not empty, and not all white-space.
     *
     * @return {@code false} if the string has characters, {@code true} otherwise.
     */
    @Trivial
    private boolean isStringValueUndefined(Object str) {
        if (str instanceof SerializableProtectedString) {
            // Avoid constructing a String from a ProtectedString
            char[] contents = ((SerializableProtectedString) str).getChars();
            for (char ch : contents)
                if (ch > '\u0020')
                    return false; // See the description of String.trim()
            return true;
        } else {
            return (str == null || ((String) str).trim().isEmpty());
        }

    }

    /**
     * Validate the configuration properties. The following conditions are
     * invalid:
     * 1. Any attribute is missing.
     * 2. Another registry is defined and <quickStartSecurity> is also defined.
     */
    private void validateConfigurationProperties() {
        if (isStringValueUndefined(config.userName()) && !isStringValueUndefined(config.userPassword())) {
            Tr.error(tc, "QUICK_START_SECURITY_MISSING_ATTIRBUTES", CFG_KEY_USER);
        }
        if (!isStringValueUndefined(config.userName()) && isStringValueUndefined(config.userPassword())) {
            Tr.error(tc, "QUICK_START_SECURITY_MISSING_ATTIRBUTES", CFG_KEY_PASSWORD);
        }
        errorOnAnotherRegistry();
        errorOnAnotherManagementRole();
    }

    /**
     * Issue an error message when <quickStartSecurity> is (at least partially)
     * configured and another registry is also configured.
     */
    private void errorOnAnotherRegistry() {
        if ((!isStringValueUndefined(config.userName()) || !isStringValueUndefined(config.userPassword())) && config.UserRegistry() != null
            && config.UserRegistry().length > 0) {
            Tr.error(tc, "QUICK_START_SECURITY_WITH_ANOTHER_REGISTRY");
        }
    }

    /**
     *
     */
    private void errorOnAnotherManagementRole() {
        if ((!isStringValueUndefined(config.userName()) || !isStringValueUndefined(config.userPassword())) && config.ManagementRole() != null
            && config.ManagementRole().length > 0) {
            Tr.error(tc, "QUICK_START_SECURITY_WITH_OTHER_MANAGEMENT_AUTHORIZATION");
        }
    }

    /**
     * Build the UserRegistryConfiguration properties based on the current
     * user and password.
     *
     * @return UserRegistryConfiguration properties
     */
    private Dictionary<String, Object> buildUserRegistryConfigProps() {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put("config.id", QUICK_START_SECURITY_REGISTRY_ID);
        properties.put("id", QUICK_START_SECURITY_REGISTRY_ID);
        properties.put(UserRegistryService.REGISTRY_TYPE, QUICK_START_SECURITY_REGISTRY_TYPE);
        properties.put(CFG_KEY_USER, config.userName());
        properties.put("service.vendor", "IBM");
        return properties;
    }

    /**
     * Create, register and return the ServiceRegistration for the quick start
     * security UserRegistryConfiguration.
     *
     * @param bc
     */
    private void registerQuickStartSecurityRegistryConfiguration() {
        if (bc == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "BundleContext is null, we must be deactivated.");
            }
            return;
        }
        if (urConfigReg != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "QuickStartSecurityRegistry configuration is already registered.");
            }
            return;
        }
        if (isStringValueUndefined(config.userName()) || isStringValueUndefined(config.userPassword())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Incomplete configuration. This should already have been reported. Will not register QuickStartSecurityRegistry configuration.");
            }
            return;
        }
        if (config.UserRegistry() != null && config.UserRegistry().length > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Other UserRegistryConfiguration are present, will not register the QuickStartSecurityRegistry configuration.");
            }
            return;
        }
        if (!managementRoles.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Other ManagementRole are present, will not register the QuickStartSecurityRegistry configuration.");
            }
            return;
        }
        Dictionary<String, Object> props = buildUserRegistryConfigProps();

        quickStartRegistry = new QuickStartSecurityRegistry(config.userName(), Password.create(config.userPassword()));
        urConfigReg = bc.registerService(UserRegistry.class,
                                         quickStartRegistry,
                                         props);
    }

    /**
     * Set a QuickStartSecurityRegistry configuration into the OSGi
     * registry if NO other registry one does not
     * exist. Remove the registered one (if it is registered) the moment another
     * registry comes into the picture.
     *
     * * Update the quick start security UserRegistryConfiguration
     * service with the new properties.
     */
    private void updateQuickStartSecurityRegistryConfiguration() {

        // A registry ONLY makes sense with a user AND a password
        if (!isStringValueUndefined(config.userName()) && !isStringValueUndefined(config.userPassword())
            && (config.UserRegistry() == null || config.UserRegistry().length == 0)
            && (config.ManagementRole() == null || config.ManagementRole().length == 0)) {
            quickStartRegistry.update(config.userName(), Password.create(config.userPassword()));

            // Update the service registration with the new configuration
            Dictionary<String, Object> newConfigProps = buildUserRegistryConfigProps();
            urConfigReg.setProperties(newConfigProps);

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Lost required configuration information, removing the configuration (if its registered).");
            }
            unregisterQuickStartSecurityRegistryConfiguration();
        }
    }

    /**
     * Unregister the quick start security security UserRegistryConfiguration.
     */
    private void unregisterQuickStartSecurityRegistryConfiguration() {
        if (urConfigReg != null) {
            urConfigReg.unregister();
            urConfigReg = null;
            quickStartRegistry = null;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "QuickStartSecurityRegistry configuration is not registered.");
            }
        }
    }

    /**
     * Register the quick start security management role.
     */
    private void registerQuickStartSecurityAdministratorRole() {
        if (bc == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "BundleContext is null, we must be deactivated.");
            }
            return;
        }
        if (managementRoleReg != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "QuickStartSecurityAdministratorRole is already registered.");
            }
            return;
        }
        if (urConfigReg == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "QuickStartSecurityRegistry configuration is not registered, will not register QuickStartSecurityAdministratorRole.");
            }
            return;
        }
        if (isStringValueUndefined(config.userName())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "User is not set, can not register the QuickStartSecurityAdministratorRole");
            }
            return;
        }
        if (!managementRoles.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Other managment roles are present, will not register the QuickStartSecurityAdministratorRole");
            }
            return;
        }

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ManagementRole.MANAGEMENT_ROLE_NAME, QUICK_START_ADMINISTRATOR_ROLE_NAME);
        props.put("service.vendor", "IBM");

        managementRole = new QuickStartSecurityAdministratorRole(config.userName());
        managementRoleReg = bc.registerService(ManagementRole.class,
                                               managementRole,
                                               props);
    }

    /**
     * Unregister the quick start security management role.
     * <p>
     * If the management role is already unregistered, do nothing.
     */
    private void unregisterQuickStartSecurityAdministratorRole() {
        if (managementRoleReg != null) {
            managementRoleReg.unregister();
            managementRoleReg = null;
            managementRole = null;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "QuickStartSecurityAdministratorRole is not registered.");
            }
        }
    }
}
