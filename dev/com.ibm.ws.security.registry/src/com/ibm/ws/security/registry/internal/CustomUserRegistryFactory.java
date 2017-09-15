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
package com.ibm.ws.security.registry.internal;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

/**
 * Registers a UserRegistry wrapping each com.ibm.websphere.security.UserRegistry (custom registry)
 * with the OSGi service registry.
 */
@Component(service = CustomUserRegistryFactory.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class CustomUserRegistryFactory {

    static final String KEY_CUSTOM = "customUserRegistry";
    static final String KEY_CONFIG_ID = "config.id";
    static final String KEY_SERVICE_ID = "service.id";
    static final String KEY_TYPE_CUSTOM = "CUSTOM";
    static final String ID_SEPARATOR = "_";

    private final Map<String, com.ibm.websphere.security.UserRegistry> customUserRegistries = new ConcurrentHashMap<String, com.ibm.websphere.security.UserRegistry>();
    private final Map<String, ServiceRegistration<UserRegistry>> registrynRegistrationsToUnregister = new ConcurrentHashMap<String, ServiceRegistration<UserRegistry>>();

    /**
     * Method will be called for each com.ibm.websphere.security.UserRegistry that is
     * registered in the OSGi service registry. We maintain an internal set
     * of these for easy access.
     *
     * @param ref Reference to a registered com.ibm.websphere.security.UserRegistry
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setCustomUserRegistry(com.ibm.websphere.security.UserRegistry externalUserRegistry, Map<String, Object> props) {
        String id = getId(props);
        customUserRegistries.put(id, externalUserRegistry);
    }

    private String getId(Map<String, Object> ref) {
        String id = (String) ref.get(KEY_CONFIG_ID);
        if (id == null) {
            id = Long.toString((Long) ref.get(KEY_SERVICE_ID));
        }
        id = KEY_TYPE_CUSTOM + ID_SEPARATOR + id;
        return id;
    }

    /**
     * Method will be called for each com.ibm.websphere.security.UserRegistry that is
     * unregistered in the OSGi service registry. We must remove this instance
     * from our internal set of listeners.
     *
     * @param ref Reference to an unregistered com.ibm.websphere.security.UserRegistry
     */
    protected synchronized void unsetCustomUserRegistry(Map<String, Object> props) {
        String id = getId(props);
        customUserRegistries.remove(id);
        ServiceRegistration<UserRegistry> registration = registrynRegistrationsToUnregister.remove(id);
        if (registration != null) {
            registration.unregister();
        }
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        registerUserRegistryConfigurations(bundleContext);
    }

    private void registerUserRegistryConfigurations(BundleContext bundleContext) {
        for (Map.Entry<String, com.ibm.websphere.security.UserRegistry> entry : customUserRegistries.entrySet()) {
            Hashtable<String, Object> userRegistryCProperties = new Hashtable<String, Object>();
            userRegistryCProperties.put(UserRegistryService.REGISTRY_TYPE, KEY_TYPE_CUSTOM);
            userRegistryCProperties.put(KEY_CONFIG_ID, entry.getKey());
            UserRegistry userRegistry = new CustomUserRegistryWrapper(entry.getValue());
            ServiceRegistration<UserRegistry> userRegistryConfigurationRegistration = bundleContext.registerService(UserRegistry.class,
                                                                                                                    userRegistry,
                                                                                                                    userRegistryCProperties);
            registrynRegistrationsToUnregister.put(entry.getKey(), userRegistryConfigurationRegistration);
        }
    }

    @Deactivate
    protected void deactivate() {
        for (String id : registrynRegistrationsToUnregister.keySet()) {
            ServiceRegistration<UserRegistry> registration = registrynRegistrationsToUnregister.remove(id);
            registration.unregister();
        }
    }

}
